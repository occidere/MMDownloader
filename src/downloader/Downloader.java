package downloader;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

import common.ErrorHandling;
import sys.Configuration;
import sys.SystemInfo;
import util.ImageMerge;
import util.UserAgent;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.Connection.Response;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class Downloader {
	private Downloader(){}
	
	// DCL 싱글톤 패턴
	private static volatile Downloader instance = null;
	public static Downloader getInstance(){
		if(instance == null)
			synchronized(Downloader.class) {
				if(instance == null)
					instance = new Downloader();
			}
		return instance;
	}
	
	//만화 제목 = 폴더 이름
	private final String PASSWORD = "qndxkr"; //만화 비밀번호
	private final String DOMAIN = "http://wasabisyrup.com";
	
	final int BUF_SIZE = 1048576;
	final int MAX_WAIT_TIME = 60000; //최대 대기시간 1분
	
	/**
	 * 선택된 페이지들만 다운로드
	 * @param archiveAddress 전체 아카이브 주소가 담긴 리스트(어레이리스트 권장)
	 * @param pages 선택된 페이지들의 번호가 담긴 리스트
	 */
	public void selectiveDownload(List<Comic> archiveAddress, List<Integer> pages){
		try{
			Comic comics[] = archiveAddress.toArray(new Comic[archiveAddress.size()]);

			// 선택한 페이지들만 가져와서 옮겨담음
			List<Comic> selectedArchiveAddress = pages.stream()
					.map(x-> comics[x])
					.collect(Collectors.toList());
			
			//선택된 페이지들로 구성된 리스트들만 다운로드 시도
			download(selectedArchiveAddress);
		}
		catch(Exception e){
			ErrorHandling.saveErrLog("선택적 다운로드 실패", "", e);
		}
	}
	
	/**
	 * 순수 이미지 주소가 담긴 {@code List}를 가지고 
	 * HttpURLConnection을 이용해 실제 다운로드 및 저장을 담당<br>
	 * Private Inner Class와 Parallel Stream 을 이용한 멀티 스레딩 다운로드
	 * @param archiveAddress 다운받을 만화 아카이브 주소들이 담긴 리스트(ex. 원피스 1화~3화 아카이브주소)
	 */
	public void download(List<Comic> archiveAddress) {
		//아카이브 리스트에 담긴 개수 출력
		System.out.printf("총 %d개\n", archiveAddress.size());
		
		String path = "", subFolder = ""; //subFolder = 원피스 3화
		
		// http://www.shencomics.com/archives/533456와 같은 아카이브 주소
		for(Comic comic : archiveAddress){
			System.out.println("다운로드 시도중...");
			
			// 아카이브주소를 바탕으로 이미지 URL 파싱해 comic객체 내부에 저장
			if(parseImageURL(comic) == false){
				//페이지 파싱 실패 -> 건너뛰고 다음 주소 시도
				continue;
			}
			
			// 아카이브주소에서 파싱한 이미지들의 URL이 담긴 리스트
			List<String> imgList = comic.getImgURL();
			subFolder = (comic.getTitle()+" "+comic.getTitleNo()).trim();
			
			/* 저장경로 = "기본경로\제목\제목 n화\" = "C:\Marumaru\제목\제목 n화\" 또는,
			 * 저장경로 = "사용자 설정 경로\제목\제목 n화\" = "C:\Marumaru\제목\제목 n화\" */
			path = String.format("%s/%s/%s/", SystemInfo.PATH, comic.getTitle(), subFolder);
			
			int pageNum = 0; // 매 회차 당 0으로 초기화 필수
			int numberOfPages = imgList.size(); //전체 이미지의 개수

			SystemInfo.makeDir(path); // 저장경로 폴더 생성
			
			System.out.printf("제목 : %s\n다운로드 폴더 : %s\n", comic.getTitle(), path);
			System.out.printf("다운로드 시작 (전체 %d개)\n", numberOfPages);
			
			Worker workers[] = new Worker[numberOfPages]; // 다운로드용 inner class 객체
			// 다운로드 필수 정보들 주입
			for(String imgURL : imgList)
				workers[pageNum] = new Worker(imgURL, path, subFolder, ++pageNum, numberOfPages);
			
			// 사용가능한 코어 수. 최소 1개는 보장
			final int CORE_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors());
			int numberOfThreads, multi = Configuration.getInt("MULTI", 2); // value of MULTI property
			
			/* 0: Sequential (Single Thread Download)
			 * 1: Thread count = available core count / 2
			 * 2: Thread count = available core count (DEFAULT)
			 * 3: Thread count = available core count * 2
			 * 4: Thread count = Unlimited (Equal to numberOfPages)
			 */
			if(multi == 0) numberOfThreads = 1;
			else if(multi == 1) numberOfThreads = CORE_COUNT >>> 1;
			else if(multi == 2) numberOfThreads = CORE_COUNT;
			else if(multi == 3) numberOfThreads = CORE_COUNT << 1;
			else numberOfThreads = numberOfPages;
			
			// 스레드 개수가 전체 페이지 수를 초과하지 않게 조정
			numberOfThreads = Math.min(numberOfThreads, numberOfPages);
			
			// 다운로드 스트림
			Stream<Worker> downloadStream = Arrays.stream(workers).parallel();

			ForkJoinPool pool = new ForkJoinPool(numberOfThreads); // 1: Sequential, N: Parallel
			try {
				pool.submit(()->{
					downloadStream.forEach(w-> w.run()); // start() -> run()
				}).get(); // Blocking until finished (= Join)
			}
			catch (Exception e) {
				ErrorHandling.saveErrLog("다운로드 실패", "제목: "+subFolder, e);
			}
		}
		
		/* 다운받은 만화들을 하나로 합치는 property 값이 true면 합침(기본: false) */
		try {
			Configuration.refresh();
			if(Configuration.getBoolean("MERGE", false))
				new ImageMerge(path).mergeAll(subFolder);
		}
		catch(Exception e) {
			ErrorHandling.saveErrLog("이미지 병합 실패", "", e);
		}
	}
	
	/**
	 * Comic 타임 객체에 담긴 아카이브 주소(address)를 바탕으로 
	 * 해당 페이지에 포함된 모든 이미지의 URL을 파싱해 저장하는 메서드.
	 * @param comic download()메서드에서 불릴 comic 객체
	 */
	private boolean parseImageURL(Comic comic) {
		try {
			String pageSource = getHtmlPage(comic.getAddress());
			System.out.println("이미지 추출중...");
			
			//Html코드 파싱 후 Jsoup doc 형식으로 생성
			Document doc = Jsoup.parse(pageSource);
			
			/* <span class=title-subject, title-no> 태그를 바탕으로 만화 제목 추출
			 * 제목은 폴더명으로 사용되므로 폴더명생성규칙 위반되는 특수문자 제거 */
			comic.setTitle(removeSpecialCharacter(doc.select("span.title-subject").first().text()));
			comic.setTitleNo(doc.select("span.title-no").first().text());
			
			/* <img class="lz-lazyload" src="/template/images/transparent.png" data-src="/storage/gallery/OrXeaIqMbEc/m0035_T6THtV9OvWI.jpg">
			 * 위의 data-src부분을 찾아 attribute만 추출하여 List에 담고, 최종적으로 comic 객체 내부에 옮김 */
			List<String> imgURL = doc.select("img[data-src]").stream()
					.map(x-> DOMAIN + encoding(x.attr("data-src"))) //아카이브 이름을 항상 최신으로 정해놓고 시작
					.collect(Collectors.toList());
			comic.setImgURL(imgURL); //파싱한 이미지 파일들의 URL을 comic객체에 저장
		}
		catch (Exception e) {
			/* Jsoup, HtmlUnit을 모두 사용했으나 페이지 파싱에 실패한 경우
			 * 또는 태그 추출 과정에서 에러가 발생하여 이미지 파싱이 실패한 경우 */
			ErrorHandling.saveErrLog("ImageURL 파싱 실패!", "페이지 주소: "+comic.getAddress(), e);
			return false;
		}
		return true;
	}
	
	/**
	 * <b> Archive의 Html Source code를 가져오는 메서드</b>
	 * <ol>1. Jsoup을 이용하여 아카이브 고속 파싱 시도</ol>
	 * <ol>2. 실패하면({@code div.gallery-template}가 없으면) HtmlUnit을 이용해 아카이브 일반 파싱 시도</ol>
	 * <ol>3. 파싱된 Html코드를 포함한 소스 전문을 스트링값에 담아서 리턴</ol>
	 * @param eachArchiveAddress 실제 만화가 담긴 아카이브 주소
	 * @return 이미지(.jpg) 주소가 포함된 Archive의  HTML 소스코드
	 */
	private String getHtmlPage(String eachArchiveAddress) throws Exception {
		String pageSource = null;
		
		try { //우선 Jsoup을 이용한 고속 파싱 시도
			pageSource = getHtmlPageJsoup(eachArchiveAddress);
		}
		catch(Exception e) {
			ErrorHandling.saveErrLog("Jsoup 파싱 실패", eachArchiveAddress, e); 
		}
		
		try { //실패시 null이 담겨있고, HtmlUnit이용해 일반 파싱 재시도
			if(pageSource == null)
				pageSource = getHtmlPageHtmlUnit(eachArchiveAddress);
		}
		catch(Exception e) {
			ErrorHandling.saveErrLog("HtmlUnit 파싱 실패", eachArchiveAddress, e);
		}
		
		//Jsoup과 HtmlUnit 모두 파싱 실패시 에러메세지 출력
		if(pageSource == null){
			throw new Exception("Page Parsing Failed");
		}
		
		return pageSource; //아카이브 페이지를 파싱한 결과 리턴
	}
	
	/**
	 * Jsoup을 이용한 HTML 코드 파싱.
	 * @param eachArchiveAddress 실제 만화가 담긴 아카이브 주소
	 * @return 성공하면 html 코드를 리턴
	 */
	private String getHtmlPageJsoup(String eachArchiveAddress) throws Exception {
		System.out.print("고속 연결 시도중 ... ");

		// pageSource = Html코드를 포함한 페이지 소스코드가 담길 스트링, domain = http://wasabisyrup.com <-마지막 / 안붙음!
		String pageSource = null;
		
		// POST방식으로 아예 처음부터 비밀번호를 body에 담아 전달
		Response response = Jsoup.connect(eachArchiveAddress)
			.userAgent(UserAgent.getUserAgent())
			.header("charset", "utf-8")
			.header("Accept-Encoding", "gzip") //20171126 gzip 추가
			.data("pass", PASSWORD)
			.followRedirects(true)
			.execute();
		
		Document preDoc = response.parse(); //받아온 HTML 코드를 저장
			
		// <div class="gallery-template">이 만화 담긴 곳.
		if(preDoc.select("div.gallery-template").isEmpty()) {
			throw new Exception("Jsoup Parsing Failed");
		}
		else { // 만약 Jsoup 파싱 시 내용 있으면 성공
			pageSource = preDoc.toString();
		}

		System.out.println("성공");
		return pageSource; //성공 시 html코드 리턴
	}
	
	/**
	 * HtmlUnit을 이용한 HTML 코드 파싱.
	 * @param eachArchiveAddress 실제 만화가 담긴 아카이브 주소
	 * @return 성공 시 html 코드를 리턴
	 */
	private String getHtmlPageHtmlUnit(String eachArchiveAddress) throws Exception {
		/* 필수! 로그 메세지 출력 안함 -> HtmlUnit 이용시 Verbose한 로그들이 너무 많아서 다 끔 */
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF); 
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		
		System.out.print("일반 연결 시도중 ... ");
		
		WebClient webClient = new WebClient();
		webClient.getOptions().setRedirectEnabled(true);
		
		WebRequest req = new WebRequest(new URL(eachArchiveAddress));
		req.setHttpMethod(HttpMethod.POST);
		req.setAdditionalHeader("User-Agent", UserAgent.getUserAgent());
		req.setAdditionalHeader("Accept-Encoding", "gzip"); //20171126 gzip 추가
		req.getRequestParameters().add(new NameValuePair("pass", PASSWORD)); //비밀번호 post 방식 전송
		
		HtmlPage page = webClient.getPage(req);
		
		//Html코드를 포함한 페이지 소스코드가 담길 스트링
		String pageSource = page.asXml();
		
		/** 여기도 페이지 파싱 실패 시 검증하는 코드 들어가야 됨 **/
		
		webClient.close();
		System.out.println("성공");
		return pageSource;
	}
	
	/**
	 * 이미지 URL에 영어 이외의 문자(ascii값이 256 이상)가 포함된 경우
	 * UTF-8로 인코딩 시켜주는 메서드
	 * @param url 이미지 URL
	 * @return UTF-8 형식의 이미지 URL
	 */
	private String encoding(String url){
		final StringBuilder utf8 = new StringBuilder(url.length() << 1);
		
		url.chars().forEach(x-> {
			String enc = ((char)x)+"";
			
			if(x == ' ') enc = "%20";	// 띄어쓰기도 변환
			else if(256 <= x) {			// ASCII의 범위는 [0,255] -> 이걸 초과하는 영문 이외 글자를 변환
				try { enc = URLEncoder.encode(enc, "UTF-8"); }
				catch (Exception e) {	//지원하지 않는 인코딩인 경우 캐치
					ErrorHandling.saveErrLog("UTF-8 변환 실패", "URL: "+url, e);
				}
			}
			utf8.append(enc);
		});
		
		return utf8.toString();
	}
	
	/**
	 * <b>특수문자 제거 메서드</b><br>
	 * {@code \ / : * ? < > | . }는 공백으로 대체됨<br>
	 * {@code " }는 {@code ' }로 대체됨 
	 * @param rawText 특수문자가 포함된 스트링
	 * @return 특수문자가 제거된 스트링
	 */
	private String removeSpecialCharacter(String rawText){
		return rawText.replaceAll("[\\\\/:*?<>|.]", " ").replaceAll("[\"]", "'").trim();
	}
	
	/**
	 * 다운로드 속도를 스트링 형식으로 반환
	 * <i>ex) 3.21 MB/s</i>
	 * @param byteSize 다운로드 받을 파일의 바이트 사이즈
	 * @param milliElapsed 다운로드에 걸린 시간(밀리초)
	 * @return 다운로드 속도의 스트링 포맷
	 */
	private String getStrSpeed(long byteSize, long milliElapsed) {
		String unit[] = { "B", "KB", "MB", "GB", "TB" };
		if(milliElapsed == 0) milliElapsed = 1;
		double spd = (byteSize*1000 / milliElapsed);
		int i;
		for(i=0; spd>=1000; i++, spd/=1000);
		return String.format("%6.2f %s/s", spd, unit[i]);
	}
	
	/**
	 * 이미지 주소에서 마지막 {@code . }을 기준으로 확장자 추출(없다면 jpg로 디폴트)
	 * @param imgUrl 이미지 주소
	 * @return {@code . }을 포함한 확장자
	 */
	private String getExt(String imgUrl){
		int lastIndexOfDot = imgUrl.lastIndexOf(".");
		String ext = lastIndexOfDot == -1 ? ".jpg" : imgUrl.substring(lastIndexOfDot);
		return ext;
	}
	
	/**
	 * 소멸자
	 */
	public void close(){
		instance = null;
	}
	
	/**
	 * 다운로드 전용 private inner class
	 * @author occidere
	 */
	private class Worker extends Thread {
		private final String imgURL, path, subFolder;
		private final int pageNum, numberOfPages;
		
		public Worker(String imgURL, String path, String subFolder, int pageNum, int numberOfPages) {
			this.imgURL = imgURL;
			this.path = path;
			this.subFolder = subFolder;
			this.pageNum = pageNum; // 페이지 번호는 001.jpg, 052.jpg, 337.jpg같은 형식
			this.numberOfPages = numberOfPages;
		}
		
		@Override
		public void run() {
			try {	//try...catch를 Worker 내부에 사용해서 이미지 한개 다운로드가 실패해도 전체가 종료되는 불상사 방지
				long st = System.currentTimeMillis();
				int imageSize = download();
				long elapsed = (System.currentTimeMillis() - st);

				System.out.printf("%3d / %3d ...... 완료! (%s)", pageNum, numberOfPages, getStrSpeed(imageSize, elapsed));
				
				// DEBUG값이 true이면 다운받은 이미지 용량 & 메모리 정보, 스레드 & 날짜 정보 출력
				if(Configuration.getBoolean("DEBUG", false) == true) {
					System.out.printf("[%3d KB]\n", imageSize/1000);
					util.MemInfo.printMemInfo(); //메모리 정보 출력(줄바꿈 안함)
					System.out.printf("\n(Thread Info: %s)", Thread.currentThread()); // 스레드 정보 출력
				}
				System.out.println();
			}
			catch(Exception e) {
				//다운로드 중 에러 발생시 에러 로그를 txt형태로 저장
				ErrorHandling.saveErrLog(String.format("%s_%03d", subFolder, pageNum), "", e);
			}
		}
		
		/**
		 * 내부적으로 이용되는 download 메서드<br>
		 * String 형식의 이미지 주소를 받아서 다운로드<br>
		 * <i>추후 HttpComponent로 변경 예정</i>
		 * @return 다운로드한 이미지의 byte size
		 * @throws Exception
		 */
		private int download() throws Exception {
			HttpURLConnection conn = (HttpURLConnection)new URL(imgURL).openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(MAX_WAIT_TIME);
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("User-Agent", UserAgent.getUserAgent());
			//conn.setRequestProperty("Accept-Encoding", "gzip");
			
			int len, imageSize = conn.getContentLength(); // byte size
			InputStream inputStream = conn.getInputStream(); //속도저하의 원인
			
			String savePath = String.format("%s%03d%s", path, pageNum, getExt(imgURL));

			BufferedInputStream bis = new BufferedInputStream(inputStream, BUF_SIZE);
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(savePath), BUF_SIZE);
			
			while((len = bis.read())!=-1) bos.write(len);

			bos.close();
			bis.close();
			
			return imageSize;
		}
		
		@Override
		public String toString() {
			return String.format("ImageURL: %s, SubFolder: %s, PageNumber: %d",
					imgURL, subFolder, pageNum);
		}
	}
}
/*
변경사항
스레드 개수가 전체 페이지 수를 넘지 않게 조정
*/