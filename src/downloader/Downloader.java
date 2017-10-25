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
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.Connection.Response;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Level;

import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.io.FileOutputStream;
import java.io.InputStream;

public class Downloader {
	private Downloader(){}
	
	//싱글톤 패턴
	private static Downloader instance = null;
	public static Downloader getInstance(){
		if(instance == null) instance = new Downloader();
		return instance;
	}
	
	//만화 제목 = 폴더 이름
	private final String PASSWORD = "qndxkr"; //만화 비밀번호
	private final String DOMAIN = "http://wasabisyrup.com";
	
	final int MAX_WAIT_TIME = 60000; //최대 대기시간 1분
	
	private int bufSize = 1048576;
	private byte[] buf = new byte[bufSize]; //다운로드용 1MB 버퍼
	
	/* 다운로드에 직접적으로 사용되는 객체들. */
	/* 지역변수로 선언시 매번 객체생성 & 제거가 불필요하게 많이 발생하여 아예 전역변수로 빼버림 */
	private FileOutputStream fos;
	private HttpURLConnection conn;
	private InputStream inputStream;
	
	/**
	 * 선택된 페이지들만 다운로드
	 * @param archiveAddress 전체 아카이브 주소가 담긴 리스트(어레이리스트 권장)
	 * @param pages 선택된 페이지들의 번호가 담긴 리스트
	 */
	void selectiveDownload(List<Comic> archiveAddress, List<Integer> pages){
		List<Comic> selectedArchiveAddress = new ArrayList<>(pages.size());
		
		try{
			//선택한 페이지들만 가져와서 옮겨담음
			for(int pageNum : pages)
				selectedArchiveAddress.add(archiveAddress.get(pageNum));
			
			//선택된 페이지들로 구성된 리스트들만 다운로드 시도
			download(selectedArchiveAddress);
		}
		catch(Exception e){
			//어지간한 페이지 에러는 다 여기서 잡힌다.
			ErrorHandling.saveErrLog("선택적 다운로드 실패", "", e);
		}
	}
	
	/**
	 * 순수 이미지 주소가 담긴 링크드리스트를 가지고 HttpURLConnection을 이용해 실제 다운로드 및 저장을 담당
	 * @param archiveAddress 다운받을 만화 아카이브 주소들이 담긴 리스트(ex. 원피스 1화~3화 아카이브주소)
	 */
	void download(List<Comic> archiveAddress) {
		//아카이브 리스트에 담긴 개수 출력
		System.out.printf("총 %d개\n", archiveAddress.size());
		
		List<String> imgList = null;
		String path = "", title = "", titleNo = ""; //원피스, 3화
		int pageNum, numberOfPages, imageSize, len; //이미지 크기
		long st, elapsed;
		
		//http://www.shencomics.com/archives/533456와 같은 아카이브 주소
		for(Comic comic : archiveAddress){
			System.out.println("다운로드 시도중...");
			
			//아카이브주소를 바탕으로 이미지 URL 파싱해 comic객체 내부에 저장
			if(parseImageURL(comic) == false){
				//페이지 파싱에 실패했다면 다음 주소 시도
				continue;
			}
			
			//아카이브주소에서 파싱한 이미지들의 URL이 담긴 리스트
			imgList = comic.getImgURL();
			
			title = comic.getTitle();
			titleNo = comic.getTitleNo();
			
			//저장경로 = "기본경로\제목\제목 n화\" = "C:\Marumaru\제목\제목 n화\" 또는,
			//저장경로 = "사용자 설정 경로\제목\제목 n화\" = "C:\Marumaru\제목\제목 n화\"
			path = String.format("%s/%s/%s %s/", SystemInfo.PATH, title, title, titleNo);
			
			pageNum = 0;
			numberOfPages = imgList.size();
				
			SystemInfo.makeDir(path); //저장경로 폴더 생성
			
			System.out.printf("제목 : %s\n다운로드 폴더 : %s\n", comic.getTitle(), path);
			System.out.printf("다운로드 시작 (전체 %d개)\n", numberOfPages);

			//imgList에 담긴 순수 이미지 주소들 foreach 탐색: O(N)
			for(String imgURL : imgList){

				//try...catch를 foreach 내부에 사용해서 이미지 한개 다운로드가 실패해도 전체가 종료되는 불상사 방지
				try {
					/* FileOutputStream을 그때그때마다 생성 & 종료하게 하여 빠른 디스크 IO 처리
					 * 페이지 번호는 001.jpg, 052.jpg, 337.jpg같은 형식 */
					fos = new FileOutputStream(String.format("%s%03d%s", path, ++pageNum, getExt(imgURL)));

					conn = (HttpURLConnection)new URL(imgURL).openConnection();
					conn.setRequestMethod("GET");
					conn.setConnectTimeout(MAX_WAIT_TIME);
					conn.setRequestProperty("charset", "utf-8");
					conn.setRequestProperty("User-Agent", UserAgent.getUserAgent());
					
					imageSize = conn.getContentLength(); // byte size
					inputStream = conn.getInputStream(); //속도저하의 원인
					
					st = System.currentTimeMillis();
					while((len = inputStream.read(buf))!=-1) fos.write(buf, 0, len);
					elapsed = (System.currentTimeMillis() - st);

					fos.flush();
					fos.close();
					inputStream.close();
					
					System.out.printf("%3d / %3d ...... 완료! (%s)", pageNum, numberOfPages, getStrSpeed(imageSize, elapsed));
					// DEBUG값이 true이면 다운받은 이미지 용량 & 메모리 정보 출력
					if(Configuration.getBoolean("DEBUG", false)) {
						System.out.printf("[%3d KB]\n", imageSize/1000);
						util.MemInfo.printMemInfo(); //메모리 정보 출력(줄바꿈 안함)
					}
					System.out.println();
				}
				catch (Exception e) {
					//다운로드 중 에러 발생시 에러 로그를 txt형태로 저장
					ErrorHandling.saveErrLog(String.format("%s_%s_%03d", title, titleNo, pageNum), "", e);
				}
				
			}
		}
		
		/* 다운받은 만화들을 하나로 합치는 property 값이 true면 합침(기본: false) */
		try {
			Configuration.refresh();
			if(Configuration.getBoolean("MERGE", false)) {
				ImageMerge im = new ImageMerge(path);
				im.mergeAll(title+" "+titleNo);
			}
		}
		catch(Exception e) {
			ErrorHandling.saveErrLog("이미지 병합 실패", "", e);
		}
	}

	
	/**
	 * <p>1. Jsoup을 이용하여 아카이브 고속 파싱 시도
	 * <p>2. 실패하면(=div.gallery-template가 없으면) HtmlUnit 이용한 아카이브 일반 파싱 시도
	 * <p>3. 파싱된 Html코드를 포함한 소스 원문을 스트링값에 담아서 리턴
	 * @param eachArchiveAddress 실제 만화가 담긴 아카이브 주소
	 * @return 이미지(.jpg) 주소가 포함된 HTML 소스코드, 실패시 null
	 */
	private String getHtmlPage(String eachArchiveAddress) {
		String pageSource = null;
		
		//우선 Jsoup을 이용한 고속 파싱 시도
		pageSource = getHtmlPageJsoup(eachArchiveAddress);
		
		//실패시 null이 담겨있고, HtmlUnit이용해 일반 파싱 재시도
		if(pageSource == null) pageSource = getHtmlPageHtmlUnit(eachArchiveAddress);
		
		//Jsoup과 HtmlUnit 모두 파싱 실패시 에러메세지 출력
		if(pageSource == null){
			ErrorHandling.printError("페이지 파싱 실패!", false);
		}
		
		return pageSource; //아카이브 페이지를 파싱한 결과 리턴
	}
	
	/**
	 * Jsoup을 이용한 HTML 코드 파싱.
	 * @param eachArchiveAddress 실제 만화가 담긴 아카이브 주소
	 * @return 성공하면 html 코드를 리턴, 실패시 null 리턴
	 */
	private String getHtmlPageJsoup(String eachArchiveAddress){
		//pageSource = Html코드를 포함한 페이지 소스코드가 담길 스트링, domain = http://wasabisyrup.com <-마지막 / 안붙음!
		String pageSource = null;
		
		System.out.print("고속 연결 시도중 ... ");
		try {
			//POST방식으로 아예 처음부터 비밀번호를 body에 담아 전달
			Response response = Jsoup.connect(eachArchiveAddress)
					.userAgent(UserAgent.getUserAgent())
					.header("charset", "utf-8")
					.data("pass", PASSWORD)
					.followRedirects(true)
					.execute();
			Document preDoc = response.parse(); //받아온 HTML 코드를 저장
			
			//<div class="gallery-template">이 만화 담긴 곳.
			//만약 Jsoup 파싱 시 내용 있으면 성공
			if(preDoc.select("div.gallery-template").isEmpty() == false){
				pageSource = preDoc.toString();
			}
		}
		catch (Exception e) {
			ErrorHandling.saveErrLog("Jsoup 파싱 실패", eachArchiveAddress, e); //페이지 파싱 실패하면 에러 로그 저장
		}
		
		System.out.println("성공");
		return pageSource; //성공하면 html코드, 실패하면 null 리턴
	}
	
	/**
	 * HtmlUnit을 이용한 HTML 코드 파싱.
	 * @param eachArchiveAddress 실제 만화가 담긴 아카이브 주소
	 * @return 성공하면 html 코드를 리턴, 실패시 null 리턴
	 */
	private String getHtmlPageHtmlUnit(String eachArchiveAddress){
		/* 필수! 로그 메세지 출력 안함 -> HtmlUnit 이용시 Verbose한 로그들이 너무 많아서 다 끔 */
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF); 
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		
		//pageSource = Html코드를 포함한 페이지 소스코드가 담길 스트링, domain = http://wasabisyrup.com <-마지막 / 안붙음!
		String pageSource = null;
		System.out.print("일반 연결 시도중 ... ");
		
		WebClient webClient = new WebClient();
		webClient.getOptions().setRedirectEnabled(true);
		
		try{
			WebRequest req = new WebRequest(new URL(eachArchiveAddress));
			req.setAdditionalHeader("User-Agent", UserAgent.getUserAgent());
			req.setHttpMethod(HttpMethod.POST);
			req.getRequestParameters().add(new NameValuePair("pass", PASSWORD)); //비밀번호 post 방식 전송
			HtmlPage page = webClient.getPage(req);
			pageSource = page.asXml();
		}
		catch(Exception e){
			ErrorHandling.saveErrLog("HtmlUnit 파싱 실패", eachArchiveAddress, e); //페이지 파싱 실패하면 에러 로그 저장
		}
		finally{
			webClient.close();
		}
		System.out.println("성공");
		return pageSource;
	}
	
	/**
	 * Comic 타임 객체에 담긴 아카이브 주소(address)를 바탕으로 
	 * 해당 페이지에 포함된 모든 이미지의 URL을 파싱해 저장하는 메서드.
	 * @param comic download()메서드에서 불릴 comic 객체
	 */
	private boolean parseImageURL(Comic comic) {
		List<String> imgURL = new LinkedList<>();//comic.addr 아카이브 주소에 포함된 모든 이미지의 URL이 담길 리스트
		
		//String domain = realAddress.substring(0, realAddress.indexOf("/archives"));
		String pageSource = getHtmlPage(comic.getAddress());
		
		//Jsoup과 HtmlUnit의 파싱을 실패했다면 pageSource == null
		if(pageSource == null){
			return false;
		}
		
		System.out.println("이미지 추출중...");
		
		//Html코드 파싱 후 jsoup doc 형식으로 생성
		//실패시 pageSource = null 상태-> IllegalArgumentException 에러
		Document doc = Jsoup.parse(pageSource);
		
		/* <span class=title-subject, title-no> 태그를 바탕으로 만화 제목 추출
		 * 제목은 폴더명으로 사용되므로 폴더명생성규칙 위반되는 특수문자 제거 */
		comic.setTitle(removeSpecialCharacter(doc.select("span.title-subject").first().text()));
		comic.setTitleNo(doc.select("span.title-no").first().text());
		
		/* <img class="lz-lazyload" src="/template/images/transparent.png" data-src="/storage/gallery/OrXeaIqMbEc/m0035_T6THtV9OvWI.jpg">
		 * 위의 data-src부분을 찾아서 Elements에 저장한 뒤, foreach로 linkedlist에 저장 */
		Elements data_src = doc.select("img[data-src]");
		
		//아카이브 이름을 항상 최신으로 정해놓고 시작
		for(Element url : data_src){
			imgURL.add(DOMAIN + encoding(url.attr("data-src")));
			//imgURL.add(encoding( toNewArchivesName(domain) + url.attr("data-src") ));
		}
		
		comic.setImgURL(imgURL); //파싱한 이미지 파일들의 URL을 comic객체에 저장
		return true;
	}
	
	/**
	 * <p> 이미지 URL에 영어 이외의 문자가 포함된 경우 UTF-8로 인코딩 시켜주는 메서드
	 * @param url 이미지 URL
	 * @return UTF-8 형식의 이미지 URL
	 */
	private String encoding(String url){
		StringBuilder utf8 = new StringBuilder();
		int i, code;
		for(i=0;i<url.length();i++){
			code = url.charAt(i);
			if(256<=code){ //ascii의 범위는 [0,255]
				try {
					utf8.append(URLEncoder.encode(url.substring(i, i+1), "utf-8"));
				}
				catch (Exception e) {
					utf8 = new StringBuilder(url);
					ErrorHandling.saveErrLog("UTF-8 변환 실패", "", e);
				}
			}
			else utf8.append((char)code);
		}
		//띄어쓰기까지 UTF-8로 바꿔줌
		String utf8String = utf8.toString().replace(" ", "%20");
		//System.out.println(utf8String);
		return utf8String;
	}
	
	/**
	 * 다운로드 속도를 스트링 형식으로 반환
	 * ex) 3.21 MB/s
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
	 * 특수문자 제거 메서드
	 * @param rawText 특수문자가 포함된 스트링
	 * @return 특수문자가 제거된 스트링
	 */
	private String removeSpecialCharacter(String rawText){
		return rawText.replaceAll("[\\/:*?<>|.]", " ").trim();
	}
	
	/**
	 * 이미지 주소에서 마지막 . 을 기준으로 확장자 추출(없다면 jpg로 디폴트)
	 * @param imgUrl 이미지 주소
	 * @return .을 포함한 확장자
	 */
	private String getExt(String imgUrl){
		int lastIndexOfDot = imgUrl.lastIndexOf(".");
		String ext = lastIndexOfDot == -1 ? ".jpg" : imgUrl.substring(lastIndexOfDot);
		return ext;
	}
	
	//소멸자
	public void close(){
		instance = null;
	}
}
