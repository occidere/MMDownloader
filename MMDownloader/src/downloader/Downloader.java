package downloader;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import sys.SystemInfo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.LinkedList;
import java.util.logging.Level;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.FileOutputStream;
import java.io.InputStream;

public class Downloader {
	
	//생성자 호출 시 마다 타이틀 초기화
	private Downloader(){
		title = titleNo = "";
	}
	
	//싱글톤 패턴
	private static Downloader instance = null;
	public static Downloader getInstance(){
		if(instance == null) instance = new Downloader();
		return instance;
	}
	
	//만화 제목 = 폴더 이름
	private String title, titleNo; //title=원피스, titleNo=337화
	private final String USER_AGENT = "Chrome/30.0.0.0 Mobile"; //크롬 모바일 User-Agent
	//private String USER_AGENT = "Mozilla/5.0"; //익스플로러11 User-Agent
	private final int MAX_WAIT_TIME = 300000; //최대 대기시간 5분
	
	/**
	 * 순수 이미지 주소가 담긴 링크드리스트를 가지고 HttpURLConnection을 이용해 실제 다운로드 및 저장을 담당
	 * @param rawAddress 순수 이미지주소를 포함한 HTML 태그 내용
	 * @param defaultPath 저장될 경로(C:\Marumaru\)
	 */
	public void download(String rawAddress) {
		
		LinkedList<String> archiveAddress = getArchiveAddress(rawAddress);
		System.out.printf("총 %d개\n", archiveAddress.size());
		
		byte[] buf = new byte[1048576]; //다운로드용 1MB 버퍼
		int pageNum, numberOfPages, len;
		
		//http://www.shencomics.com/archives/533456와 같은 아카이브 주소
		for(String realAddress : archiveAddress){
			
			System.out.println("다운로드 시도중...");
			
			//순수 이미지주소가 담길 imgList 링크드리스트
			LinkedList<String> imgList = getImgList(realAddress);
			
			//저장경로 = "기본경로\제목\제목 n화\" = "C:\Marumaru\제목\제목 n화\"
			String path = String.format("%s/%s/%s %s/", SystemInfo.DEFAULT_PATH, title, title, titleNo);
			
			pageNum = 0;
			numberOfPages = imgList.size();
				
			SystemInfo.makeDir(path); //저장경로 폴더 생성
			
			System.out.printf("제목 : %s\n다운로드 폴더 : %s\n", title, path);
			System.out.printf("다운로드 시작 (전체 %d개)\n", numberOfPages);
			
			//imgList에 담긴 순수 이미지 주소들 foreach 탐색: O(N)
			for(String imgURL : imgList){
				
				//try...catch를 foreach 내부에 사용해서 이미지 한개 다운로드가 실패해도 전체가 종료되는 불상사 방지
				try {
					/* FileOutputStream을 그때그때마다 생성 & 종료하게 하여 빠른 디스크 IO 처리
					 * 페이지 번호는 001.jpg, 052.jpg, 337.jpg같은 형식 */
					FileOutputStream fos = new FileOutputStream(String.format("%s%03d%s", path, ++pageNum, getExt(imgURL)));
				
					HttpURLConnection conn = (HttpURLConnection)new URL(imgURL).openConnection();
					conn.setConnectTimeout(MAX_WAIT_TIME); //최대 5분까지 시간 지연 기다려줌
					conn.setRequestMethod("GET");
					conn.setRequestProperty("User-Agent", USER_AGENT);
				
					InputStream in = conn.getInputStream(); //속도저하의 원인. 느린 이유는 결국 사이트가 느려서...
				
					while((len = in.read(buf))>0) fos.write(buf, 0, len);
					System.out.printf("%3d / %3d ...... 완료!\n", pageNum, numberOfPages);
					fos.close();
					in.close();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}			
		}
	}
	
	/**
	 * <p>wasabisyrup, yuncomics, shencomics와 같은 아카이브주소가 들어오는 경우 -> 단편 다운로드에 해당 -> 바로 담아서 return
	 * <p>mangaup/과 같은 업데이트 주소, manga/와 같은 신 전체보기 주소,
	 * uid= 와 같이 구 전체보기 주소가 들어오는 경우 -> 아카이브 주소 파싱
	 * @param rawAddress 위에 언급된 요소들이 포함된 처리 전 주소
	 * @return wasabisyrup과 같은 아카이브 주소가 담긴 링크드리스트
	 */
	private LinkedList<String> getArchiveAddress(String rawAddress){
		LinkedList<String> archiveAddress = new LinkedList<>();
		
		//wasabisyrup, yuncomics, shencomics와 같은 아카이브 주소가 들어오는 경우
		if(rawAddress.contains("http") && rawAddress.contains("archives")){
			archiveAddress.add(rawAddress);
			return archiveAddress;
		}
		
		try{
			//Jsoup을 이용하여 파싱. timeout은 5분
			Document doc = Jsoup.connect(rawAddress).header("User-Agent", USER_AGENT).timeout(MAX_WAIT_TIME).get();
			Elements divContent = doc.select("div.content").select("a[target]");
			
			for(Element e : divContent){
				String hrefURL = e.attr("href"); //만화 아카이브 주소(wasabisyrup)
				
				//쓸데없는 주소가 한두개씩 꼭 있어서 archives를 포함하는 아카이브 주소만 저장
				if(hrefURL.contains("archives")) archiveAddress.add(hrefURL);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return archiveAddress;
	}

	
	/**
	 * <p>1. Jsoup을 이용하여 아카이브 고속 파싱 시도
	 * <p>2. 실패하면(=div.gallery-template가 없으면) HtmlUnit 이용한 아카이브 일반 파싱 시도
	 * <p>3. 파싱된 Html코드를 포함한 소스 원문을 스트링값에 담음
	 * <p>4. Jsoup을 이용하여 만화제목, 회차, 이미지 URL만 걸러냄
	 * <p>5. foreach 돌려서 LinkedList에 순수 이미지 URL 저장후 리턴
	 * @param rawAddress 순수 이미지주소를 포함한 Html 소스코드
	 * @return 순수 이미지주소가 담긴 링크드리스트 반환
	 */
	private LinkedList<String> getImgList(String realAddress) {
		
		/* 필수! 로그 메세지 출력 안함 -> HtmlUnit 이용시 Verbose한 로그들이 너무 많아서 다 끔 */
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF); 
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		
		//pageSource = Html코드를 포함한 페이지 소스코드가 담길 스트링, domain = http://wasabisyrup.com <-마지막 / 안붙음!
		String pageSource = "", domain = realAddress.substring(0, realAddress.indexOf("/archives"));
		boolean highSpeed = false; //고속 파싱 성공시 true, 실패시 false
		LinkedList<String> imgURL = new LinkedList<>();//만화 jpg 파일들의 주소가 담길 링크드리스트
		
		/*********************** Jsoup 이용한 고속 다운로드 부분 ******************************/
		System.out.print("고속 연결 시도중 ... ");
		try {
			Document preDoc = Jsoup.connect(realAddress).header("User-Agent", USER_AGENT).get();
			
			//<div class="gallery-template">이 만화 담긴 곳. 만약 Jsoup 파싱시 내용 있으면 성공
			if(!(preDoc.select("div.gallery-template").isEmpty())){
				highSpeed = true;
				pageSource = preDoc.toString();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			System.out.println(highSpeed ? "성공" : "실패");
		}
		/**************************************************************************************/
		
		/*********************** HtmlUnit을 이용한 일반 다운로드 부분 *************************/
		if(!highSpeed){ //고속 연결 실패하면 시도
			System.out.println("일반 연결 시도중 ...");
			
			WebClient webClient = new WebClient();
			webClient.getOptions().setRedirectEnabled(true);
			
			try{
				WebRequest req = new WebRequest(new URL(realAddress));
				req.setAdditionalHeader("User-Agent", USER_AGENT);
				
				HtmlPage page = webClient.getPage(req);
				
				pageSource = page.asXml();
			}
			catch(Exception e){
				e.printStackTrace();
			}
			finally{
				webClient.close();
			}
		}
		/***************************************************************************************/
		
		System.out.println("이미지 추출중...");
		Document doc = Jsoup.parse(pageSource); //HtmlUnit을 이용해 Html코드 파싱 후 jsoup doc 형식으로 생성
		
		/* <span class=title-subject, title-no> 태그를 바탕으로 만화 제목 추출
		 * 제목은 폴더명으로 사용되므로 폴더명생성규칙 위반되는 특수문자 제거 */
		title = removeSpecialCharacter(doc.select("span.title-subject").first().text());
		titleNo = doc.select("span.title-no").first().text();
		
		/* <img class="lz-lazyload" src="/template/images/transparent.png" data-src="/storage/gallery/OrXeaIqMbEc/m0035_T6THtV9OvWI.jpg">
		 * 위의 data-src부분을 찾아서 Elements에 저장한 뒤, foreach로 linkedlist에 저장 */
		Elements data_src = doc.select("img[data-src]");
		for(Element url : data_src) imgURL.add(domain+url.attr("data-src"));
		
		return imgURL;
	}
	
	//특수문자 제거 메서드
	private String removeSpecialCharacter(String rawText){
		return rawText.replaceAll("[\\/:*?<>|.]", " ").trim();
	}
	
	//확장자 설정 메서드
	private String getExt(String imgUrl){
		String ext="";
		int size = imgUrl.length();
		while(size-->-1 && imgUrl.charAt(size)!='.') 
			ext = imgUrl.charAt(size)+ext;
		return "."+ext;
	}
	
	//소멸자
	public void close(){
		instance = null;
	}
}
