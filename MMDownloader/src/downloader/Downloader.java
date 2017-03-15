package downloader;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.LinkedList;
import java.util.logging.Level;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.File;
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
	private String title; //원피스
	private String titleNo; //337화
	
	/**
	 * 순수 이미지 주소가 담긴 링크드리스트를 가지고 HttpURLConnection을 이용해 실제 다운로드 및 저장을 담당
	 * @param rawAddress 순수 이미지주소를 포함한 HTML 태그 내용
	 * @param defaultPath 저장될 경로(C:\Marumaru\)
	 */
	public void download(String rawAddress, String defaultPath) {
		
		LinkedList<String> archiveAddress = getArchiveAddress(rawAddress);
		System.out.printf("총 %d개\n", archiveAddress.size());
		
		//다운로드용 버퍼
		byte[] buf = new byte[1024*1024];
		int pageNum, numberOfPages, len;
		
		//http://www.shencomics.com/archives/533456와 같은 아카이브 주소
		for(String realAddress : archiveAddress){
			
			System.out.println("다운로드 시도중...");
			
			//순수 이미지주소가 담길 imgList 링크드리스트
			LinkedList<String> imgList = getImgList(realAddress);
			
			//저장경로 = "기본경로\제목\제목 n화\" = "C:\Marumaru\제목\제목 n화\"
			String path = String.format("%s\\%s\\%s %s\\", defaultPath, title, title, titleNo);
			
			pageNum = 0;
			numberOfPages = imgList.size();
				
			makeDir(path); //저장경로 폴더 생성
			
			System.out.printf("제목 : %s\n다운로드 폴더 : %s\n", title, path);
			System.out.printf("다운로드 시작 (전체 %d개)\n", numberOfPages);
			
			//imgList에 담긴 순수 이미지 주소들 foreach 탐색: O(N)
			for(String imgURL : imgList){
				
				//try...catch를 foreach 내부에 사용해서 이미지 한개 다운로드가 실패해도 전체가 종료되는 불상사 방지
				try {
					/*
					 * FileOutputStream을 그때그때마다 생성 & 종료하게 하여 빠른 디스크 IO 처리
					 * 페이지 번호는 001.jpg, 052.jpg, 337.jpg같은 형식
					 */
					FileOutputStream fos = new FileOutputStream(path + String.format("%03d", ++pageNum) + getExt(imgURL));
				
					HttpURLConnection conn = (HttpURLConnection)new URL(imgURL).openConnection();
					conn.setConnectTimeout(300000); //최대 5분까지 시간 지연 기다려줌
					conn.setRequestMethod("GET");
					conn.setRequestProperty("User-Agent", "Mozilla/5.0");
				
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
	 * wasabisyrup, yuncomics, shencomics와 같은 직접주소가 들어오는 경우 -> 단편 다운로드에 해당 -> 바로 htmlunit 처리
	 * mangaup/과 같은 업데이트 주소가 들어오는 경우,
	 * manga/와 같은 신 전체보기 주소가 들어오는 경우,
	 * uid= 와 같이 구 전체보기 주소가 들어오는 경우  -> 직접주소 찾아내기
	 * @param rawAddress 위에 언급된 요소들이 포함된 처리 전 주소
	 * @return wasabisyrup과 같은 직접주소가 담긴 링크드리스트
	 */
	private LinkedList<String> getArchiveAddress(String rawAddress){
		LinkedList<String> archiveAddress = new LinkedList<>();
		
		//wasabisyrup, yuncomics, shencomics와 같은 직접주소가 들어오는 경우
		if(rawAddress.contains("http") && rawAddress.contains("archives")){
			archiveAddress.add(rawAddress);
			return archiveAddress;
		}
		
		try{
			String hrefURL;
			//Jsoup을 이용하여 파싱. timeout은 5분
			Document doc = Jsoup.connect(rawAddress).header("User-Agent", "Mozilla/5.0").timeout(30000).get();
			
			Elements divContent = doc.select("div.content").select("a[target]");
			for(Element e : divContent){
				hrefURL = e.attr("href"); //만화 아카이브 주소(wasabisyrup)
				
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
	 * HtmlUnit을 이용하여 이미지 divID와 일치하는 부분(=이미지들이 있는 곳)을 추출
	 * @param rawAddress 순수 이미지주소를 포함한 HTML 태그 내용
	 * @return 순수 이미지주소가 담긴 링크드리스트 반환
	 */
	private LinkedList<String> getImgList(String realAddress) {
		
		System.out.println("이미지 추출중...");
		
		/* 필수! 로그 메세지 출력 안함 -> HtmlUnit 이용시 Verbose한 로그들이 너무 많아서 다 끔 */
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF); 
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		
		//만화 jpg 파일들의 주소가 담길 링크드리스트
		LinkedList<String> imgURL = new LinkedList<>();
		
		//http://wasabisyrup.com <-마지막 / 안붙음!
		String domain = realAddress.substring(0, realAddress.indexOf("/archives"));
		
		//브라우저는 인터넷 익스플로러로 설정
		WebClient webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER);
		
		try{
			HtmlPage page = webClient.getPage(realAddress);
			Document doc = Jsoup.parse(page.asXml()); //HtmlUnit을 이용해 Html코드 파싱 후 jsoup doc 형식으로 생성
			
			//<span class=title-subject, title-no> 태그를 바탕으로 만화 제목 추출
			//제목은 폴더명으로 사용되므로 폴더명생성규칙 위반되는 특수문자 제거
			title = removeSpecialCharacter(doc.select("span.title-subject").first().text());
			titleNo = doc.select("span.title-no").first().text();
			
			/*
			 * <img class="lz-lazyload" src="/template/images/transparent.png" data-src="/storage/gallery/OrXeaIqMbEc/m0035_T6THtV9OvWI.jpg">
			 * 위의 data-src부분을 찾아서 Elements에 저장한 뒤, foreach로 linkedlist에 저장
			 */
			Elements data_src = doc.select("img[data-src]");
			for(Element url : data_src) imgURL.add(domain+url.attr("data-src"));
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		webClient.close();
		
		return imgURL;
	}
	
	//특수문자 제거 메서드
	private String removeSpecialCharacter(String rawText){
		return rawText.replaceAll("[\\/:*?<>|.]", " ").trim();
	}
	
	//폴더 생성 메서드
	public void makeDir(String path){
		File f = new File(path);
		if(!f.exists()) f.mkdirs();
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
