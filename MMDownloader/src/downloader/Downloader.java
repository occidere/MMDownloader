package downloader;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

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
		title = null;
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
	private String title;
	
	/**
	 * HtmlUnit을 이용하여 이미지 divID와 일치하는 부분(=이미지들이 있는 곳)을 추출
	 * @param rawAddress 순수 이미지주소를 포함한 HTML 태그 내용
	 * @return 순수 이미지주소가 담긴 링크드리스트 반환
	 */
	private LinkedList<String> getImgList(String rawAddress) {
		
		System.out.println("이미지 추출중...");
		
		/* 필수! 로그 메세지 출력 안함 -> HtmlUnit 이용시 Verbose한 로그들이 너무 많아서 다 끔 */
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF); 
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		
		//만화 jpg 파일들의 주소가 담길 링크드리스트
		LinkedList<String> imgURL = new LinkedList<>();
		
		//http://wasabisyrup.com <-마지막 / 안붙음!
		String domain = rawAddress.substring(0, rawAddress.indexOf("/archives"));
		
		//브라우저는 인터넷 익스플로러로 설정
		WebClient webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER);
		
		try{
			HtmlPage page = webClient.getPage(rawAddress);
			
			//<title> 태그를 바탕으로 만화 제목 추출. 제목은 폴더명으로 사용되므로 폴더명생성규칙 위반되는 특수문자 제거
			title = page.getTitleText();
			title = title.substring(0, title.indexOf("|")).replaceAll("[\\/:*?<>|.]", " ").trim();;
			
			//divID에 해당되는 div 추출후 xml형식(=Javascript와 HTML값 전부)으로 imgParser로 넘김
			HtmlDivision htmlDivision = (HtmlDivision)page.getElementById(divID);
			imgURL = imgParser(htmlDivision.asXml(), domain);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		webClient.close();
		
		return imgURL;
	}
	
	/**
	 * .jpg등의 이미지 주소와 기타 태그들이 담긴 raw형태의 스트링값이 들어오면
	 * KMP알고리즘을 이용하여 data-src=\로 시작해서 "/>로 끝나는 부분을 찾은 뒤,
	 * imgURL 링크드리스트에 순수 이미지 주소 저장해서 리턴
	 * @param xml 순수 이미지주소를 포함한 HTML 태그 내용
	 * @param domain http://wasabisyrup.com <-마지막 / 안붙음!
	 * @return http://wasabisyrup.com/storage/gallery/asdfa.jpg처럼 순수 이미지주소만 담긴 링크드리스트
	 */
	private LinkedList<String> imgParser(String xml, String domain){
		//싱글톤 KMP
		KMP kmp = KMP.getInstance();
		//순수 이미지주소는 접두사(data-src=\)부터 접미사("/>) 사이의 값
		final String prefix = "data-src=\"", suffix = "\"/>";
		//순수 이미지 주소가 잠시 담길 스트링 값
		String parsedURL;
		
		//순수 이미지 주소가 담길 링크드리스트
		LinkedList<String> imgURL = new LinkedList<>();
		//kmp탐색 결과로 나온 주소 시작 인덱스가 담길 링크드리스트
		LinkedList<Integer> addressIdx = kmp.getList(xml, prefix);
		
		//foreach로 주소 시작 인덱스들을 탐색해서 순수 이미지 주소를 걸러낸 뒤, imgURL 링크드리스트에 저장
		//시간복잡도 O(N)
		for(int i : addressIdx){
			parsedURL = xml.substring(i);
			parsedURL = domain + parsedURL.substring(prefix.length(), parsedURL.indexOf(suffix));
			imgURL.add(parsedURL);
		}
		
		kmp.close();
		return imgURL;
	}

	/**
	 * 순수 이미지 주소가 담긴 링크드리스트를 가지고 HttpURLConnection을 이용해 실제 다운로드 및 저장을 담당
	 * @param rawAddress 순수 이미지주소를 포함한 HTML 태그 내용
	 * @param defaultPath 저장될 경로(C:\Marumaru\)
	 */
	public void download(String rawAddress, String defaultPath) {
		
		System.out.println("다운로드 시도중...");
		
		//순수 이미지주소가 담길 imgList 링크드리스트
		LinkedList<String> imgList = getImgList(rawAddress);
		
		//저장경로 = "기본경로 + 만화제목 + \" = "C:\Marumaru\만화제목\"
		String path = defaultPath + title + "\\";
		
		FileOutputStream fos = null;
		HttpURLConnection conn;
		InputStream in;
		byte[] buf; //다운로드용 버퍼
		int pageNum, numberOfPages = imgList.size(), len;
		
		try {
			pageNum = 0; //0으로 초기화
			
			makeDir(path); //저장경로 폴더 생성
			
			System.out.printf("제목 : %s\n다운로드 폴더 : %s\n",title, path);
			System.out.printf("다운로드 시작 (전체 %d개)\n", numberOfPages);
			
			//imgList에 담긴 순수 이미지 주소들 foreach 탐색: O(N)
			for(String imgURL : imgList){
				//페이지 번호는 001.jpg, 052.jpg, 337.jpg같은 형식
				fos = new FileOutputStream(path + String.format("%03d", ++pageNum) + getExt(imgURL));
				
				conn = (HttpURLConnection)new URL(imgURL).openConnection();
				conn.setConnectTimeout(30000); //최대 30초까지 시간 지연 기다려줌
				conn.setRequestMethod("GET");
				conn.setRequestProperty("User-Agent", "Mozilla/5.0");
				
				in = conn.getInputStream();
				
				//다운로드 부분. 버퍼 크기 1024*1024B(1024Kb)로 조정
				buf = new byte[1024*1024];
				while((len = in.read(buf))>0) fos.write(buf, 0, len);
				
				System.out.printf("%2d / %2d ...... 완료!\n", pageNum, numberOfPages);
			}
			fos.close();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
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
