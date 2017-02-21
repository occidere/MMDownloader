package downloader;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.util.LinkedList;
import java.util.logging.Level;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Downloader {
	
	//생성자 호출 시 마다 타이틀 초기화
	private Downloader(){
		title = smallTitle = "";
	}
	
	//싱글톤 패턴
	private static Downloader instance = null;
	public static Downloader getInstance(){
		if(instance == null) instance = new Downloader();
		return instance;
	}
	
	//HTML태그 중 div의 class값. 이 값이 있는 곳이 이미지 파일들이 있는 곳
	private final String divID = "gallery_vertical";
	
	//만화 제목 = 폴더 이름
	private String title; //원피스
	private String smallTitle; //원피스337화
	
	/**
	 * 순수 이미지 주소가 담긴 링크드리스트를 가지고 HttpURLConnection을 이용해 실제 다운로드 및 저장을 담당
	 * @param rawAddress 순수 이미지주소를 포함한 HTML 태그 내용
	 * @param defaultPath 저장될 경로(C:\Marumaru\)
	 */
	public void download(String rawAddress, String defaultPath) {
		
		LinkedList<String> archiveAddress = getArchiveAddress(rawAddress);
		System.out.printf("총 %d개\n", archiveAddress.size());
		
		//http://www.shencomics.com/archives/533456와 같은 아카이브 주소
		for(String realAddress : archiveAddress){
			
			System.out.println("다운로드 시도중...");
			
			//순수 이미지주소가 담길 imgList 링크드리스트
			LinkedList<String> imgList = getImgList(realAddress);
			
			//저장경로 = "기본경로 + 만화제목 + \" = "C:\Marumaru\만화제목\"
			String path = defaultPath + title + "\\";
			if(!smallTitle.equals("") && !smallTitle.equals(title)) path += smallTitle + "\\";
			
			//다운로드용 버퍼
			byte[] buf = new byte[1024*1024];
			int pageNum, numberOfPages = imgList.size(), len;
			
			try {
				pageNum = 0; //0으로 초기화
				
				makeDir(path); //저장경로 폴더 생성
				
				System.out.printf("제목 : %s\n다운로드 폴더 : %s\n",title, path);
				System.out.printf("다운로드 시작 (전체 %d개)\n", numberOfPages);
				
				//imgList에 담긴 순수 이미지 주소들 foreach 탐색: O(N)
				for(String imgURL : imgList){
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
			} 
			catch (Exception e) {
				e.printStackTrace();
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
			HttpURLConnection conn = (HttpURLConnection)new URL(rawAddress).openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			conn.setConnectTimeout(30000);
			
			String line;
			
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			
			while((line = in.readLine()) != null){
				if(line.contains("<h1>"))
					title = line.replaceAll("<[^>]*>", "").replaceAll("[\\/:*?<>|.]", " ").trim();
				else if(line.contains("http") && line.contains("archives")){
					archiveAddress = parser(line, "", "href=\"", "\"");
					break;
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return archiveAddress;
	}
	
	/**
	 * raw형태의 스트링값이 들어오면 KMP알고리즘을 이용하여 
	 * 접두사와 접미사 사이의 문자열을 모두 찾은 뒤, 링크드리스트에 저장해서 리턴
	 * @param xml 순수 이미지주소를 포함한 HTML 태그 내용 또는 일반 만화 주소
	 * @param domain http://wasabisyrup.com <-마지막 / 안붙음! 또는 일반 만화 주소 처리의 경우는 공백
	 * @param prefix 잘라낼 시발점이 되는 접두사
	 * @param suffix 잘라낼 종점이 되는 접미사
	 * @return http://wasabisyrup.com/storage/gallery/asdfa.jpg처럼 순수 이미지주소만 담긴 링크드리스트
	 *  또는 wasabisyrup과 같은 직접주소가 담긴 링크드리스트
	 */
	private LinkedList<String> parser(String xml, String domain, String prefix, String suffix){
		//싱글톤 KMP
		KMP kmp = KMP.getInstance();
		
		//순수 이미지주소는 접두사(data-src=\)부터 접미사("/>) 사이의 값
		//순수 이미지 주소가 잠시 담길 스트링 값
		String parsedURL;
		
		//순수 이미지 주소가 담길 링크드리스트
		LinkedList<String> url = new LinkedList<>();
		
		//kmp탐색 결과로 나온 주소 시작 인덱스가 담길 링크드리스트
		LinkedList<Integer> addressIdx = kmp.getList(xml, prefix);
		
		//foreach로 주소 시작 인덱스들을 탐색해서 순수 이미지 주소를 걸러낸 뒤, imgURL 링크드리스트에 저장
		//시간복잡도 O(N)
		for(int i : addressIdx){
			parsedURL = xml.substring(i);
			//System.out.println(parsedURL);
			
			//원치않는 주소부분이 검색된 경우 제외 <-대부분 인덱스가 음수값이 된다.
			int prefixIdx = prefix.length();
			if(prefixIdx>0) parsedURL = parsedURL.substring(prefixIdx);
			
			int suffixIdx = parsedURL.indexOf(suffix);
			if(suffixIdx>0) parsedURL = parsedURL.substring(0, suffixIdx);
			
			parsedURL = domain + parsedURL;
			// 만화 업데이트(mangaup) 주소를 넣을 경우 아카이브주소 + 전편 보러가기 주소(marumaru.in/manga/번호)가 같이 들어옴
			// 때문에 아카이브 주소만 남기고 전편 보러가기 주소를 걸러주는 작업을 아래와 같이 처리
			// 의존성이 너무 높아 추후 수정 작업 필요
			if (!parsedURL.contains("marumaru")) url.add(parsedURL);
		}
		kmp.close();
		return url;
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
			
			//<title> 태그를 바탕으로 만화 제목 추출. 제목은 폴더명으로 사용되므로 폴더명생성규칙 위반되는 특수문자 제거
			smallTitle = page.getTitleText();
			smallTitle = smallTitle.substring(0, smallTitle.indexOf("|")).replaceAll("[\\/:*?<>|.]", " ").trim();
			
			//divID에 해당되는 div 추출후 xml형식(=Javascript와 HTML값 전부)으로 imgParser로 넘김
			HtmlDivision htmlDivision = (HtmlDivision)page.getElementById(divID);
			imgURL = parser(htmlDivision.asXml(), domain, "data-src=\"", "\"/>");
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		webClient.close();
		
		return imgURL;
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
