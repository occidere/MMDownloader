package downloader;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import sys.SystemInfo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.Connection.Response;

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
	private final String PASSWORD = "qndxkr"; //만화 비밀번호
	
	private final int MAX_WAIT_TIME = 300000; //최대 대기시간 5분
	
	/* 중요! 아카이브명이 옛날 주소 그대로면 HttpURLConnection을 이용한 다운로드시 
	 * 제대로 다운로드가 안됨 -> 항상 최신 아카이브명으로 집어넣어줘야 됨 */
	private final String OLD_ARCHIVES_NAME[] = { "shencomics", "yuncomics" }; //옛날 아카이브명
	private final String NEW_ARCHIVES_NAME = "wasabisyrup"; //현재 아카이브명
	
	
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
			String path = String.format("%s%s/%s %s/", SystemInfo.DEFAULT_PATH, title, title, titleNo);
			
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

					InputStream in = conn.getInputStream(); //속도저하의 원인
					
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
			archiveAddress.add(toNewArchivesName(rawAddress)); //현재 아카이브명으로 변경
			return archiveAddress;
		}
		
		try{
			//Jsoup을 이용하여 파싱. timeout은 5분
			Document doc = Jsoup.connect(rawAddress).userAgent(USER_AGENT).timeout(MAX_WAIT_TIME).get();
			
			/* 정규식 좀더 강력하게 수정-> <div class="Content">에서 href=".../archives/.."가 포함된 모든 주소 파싱 */
			Elements divContent = doc.select("div.content").select("[href*=/archives/]");
			
			for(Element e : divContent)
				archiveAddress.add(toNewArchivesName(e.attr("href"))); //아카이브 주소를 최신으로 항상 맞춰줌
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return archiveAddress;
	}
	
	/**
	 * <p>1. Jsoup을 이용하여 아카이브 고속 파싱 시도
	 * <p>2. 실패하면(=div.gallery-template가 없으면) HtmlUnit 이용한 아카이브 일반 파싱 시도
	 * <p>3. 파싱된 Html코드를 포함한 소스 원문을 스트링값에 담아서 리턴
	 * @param realAddress 실제 만화가 담긴 아카이브 주소
	 * @return 이미지(.jpg) 주소가 포함된 HTML 소스코드
	 */
	private String getHtmlPage(String realAddress) {
		/* 필수! 로그 메세지 출력 안함 -> HtmlUnit 이용시 Verbose한 로그들이 너무 많아서 다 끔 */
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF); 
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		
		//pageSource = Html코드를 포함한 페이지 소스코드가 담길 스트링, domain = http://wasabisyrup.com <-마지막 / 안붙음!
		String pageSource = null;
		
		boolean highSpeed = false; //고속 파싱 성공시 true, 실패시 false
		
		/*********************** Jsoup 이용한 고속 다운로드 부분 ******************************/
		System.out.print("고속 연결 시도중 ... ");
		try {
			//POST방식으로 아예 처음부터 비밀번호를 body에 담아 전달
			Response response = Jsoup.connect(realAddress).userAgent(USER_AGENT).data("pass", PASSWORD).followRedirects(true).execute();
			Document preDoc = response.parse();
			
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
		return pageSource;
	}

	/**
	 * <p>1. Jsoup을 이용하여 만화제목, 회차, 이미지 URL만 걸러냄
	 * <p>2. foreach 돌려서 LinkedList에 순수 이미지 URL 저장후 리턴
	 * @param realAddress 실제 만화가 담긴 아카이브 주소
	 * @return 순수 이미지주소가 담긴 링크드리스트 반환
	 */
	private LinkedList<String> getImgList(String realAddress) {
		LinkedList<String> imgURL = new LinkedList<>();//만화 jpg 파일들의 주소가 담길 링크드리스트
		
		String domain = realAddress.substring(0, realAddress.indexOf("/archives"));
		String pageSource = getHtmlPage(realAddress);
		
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
		for(Element url : data_src) imgURL.add(toNewArchivesName(domain)+url.attr("data-src"));
		return imgURL;
	}

	
	/**
	 * <p> 이미지 다운로드시 과거 아카이브명(shencomics, yuncomincs 등)이 들어가 있으면
	 * <p> 이를 현재 아카이브명(wasabisyrup)으로 변경하지 않고 그대로 다운로드 시도하게 됨
	 * <p> 따라서 제대로 다운로드가 되지 않는 현상 빈번
	 * <p> 이를 해결하기 위해 수동으로 아카이브명을 항상 최신으로 유지
	 * <p> 단순 순차탐색 사용 O(N)
	 * @param archivesAddress 아카이브명이 포함되어 있을 수 있는 주소
	 * @return 최신 아카이브명으로 변경된 주소
	 */
	private String toNewArchivesName(String archivesAddress){
		String newArchivesName = archivesAddress;
		for(int i=0;i<OLD_ARCHIVES_NAME.length;i++)
			if(newArchivesName.contains(OLD_ARCHIVES_NAME[i])){
				newArchivesName = newArchivesName.replace(OLD_ARCHIVES_NAME[i], NEW_ARCHIVES_NAME);
				break;
			}
		return newArchivesName;
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
