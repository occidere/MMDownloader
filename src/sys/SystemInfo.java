package sys;

import java.awt.Desktop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.URI;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Properties;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Document;

import common.ErrorHandling;

/**
 * <p>시스템과 관련된 클래스
 * <p>폴더생성, 브라우저 오픈등의 기능이 static으로 내장
 * <p>Instantiation 불가(private 생성자)
 * @author occidere
 */
@SuppressWarnings("unused")
public class SystemInfo {
	private SystemInfo(){}
	
	//운영체제별 파일 구분자
	static final String fileSeparator = File.separator; //윈도우는 \, 나머지는 /
	//운영체제별 줄바꿈 문자
	static final String lineSeparator = System.getProperty("line.separator"); //윈도우는 \r\n, 유닉스 계열은 \n, 맥은 \r
	//OS이름 ex) Windows 10, Linux 등..
	static transient final String OS_NAME = System.getProperty("os.name");
	//디폴트 저장 디렉토리 ex) C:\Users\occid\Marumaru 또는 home/occidere/marumaru
	public static transient final String DEFAULT_PATH = System.getProperty("user.home")+ fileSeparator + "Marumaru";
	//다운로드 저장할 디렉토리. 
	public static transient String PATH = DEFAULT_PATH;
	//에러 로그 저장 경로(무조건 디폴트 경로에 위치)
	public static final String ERROR_LOG_PATH = DEFAULT_PATH + fileSeparator + "log";
	//마루마루 브라우저 주소
	public static final String MARU_ADDR = "http://marumaru.in/";
	//최신 버전 공지할 페이지 주소
	private transient static final String LATEST_VERSION_URL = "https://github.com/occidere/MMDownloader/blob/master/VERSION_INFO";
	
	/* <수정 금지> 프로그램 정보 */
	private static final String VERSION = "0.5.0.8"; //프로그램 버전
	private static final String UPDATED_DATE = "2018.04.29"; //업데이트 날짜
	private static final String DEVELOPER = "제작자: occidere"; //제작자 정보
	private static final String VERSION_INFO = String.format("현재버전: %s (%s)", VERSION, UPDATED_DATE);
	
	/* 최신 버전 버전. 서버 연결해서 정보 받아오기 전까진 null */
	private static String LATEST_VERSION = null;
	private static String LATEST_UPDATED_DATE = null;
	private static String LATEST_VERSION_INFO = null;
	private static String LATEST_WINDOWS = null; //윈도우용 최신버전 링크
	private static String LATEST_OTHERS = null; //다른OS 최신버전 링크
	
	/**
	 * <p>제작자 정보와 현재 버전, 저장경로 등 프로그램 정보 출력.
	 * <p>프로그램 첫 시작시 보여줄 정보.
	 */
	public static void printProgramInfo(){
		try { Configuration.refresh(); }
		catch (Exception e) {}
		System.out.print(DEVELOPER+"\t");
		System.out.println(VERSION_INFO);
		System.out.println("저장경로: "+Configuration.getString("PATH", DEFAULT_PATH));
		System.out.printf("(이미지 병합: %s, 디버깅 모드: %s, 멀티스레딩: %d)\n",
				Configuration.getBoolean("MERGE", false),
				Configuration.getBoolean("DEBUG", false),
				Configuration.getInt("MULTI", 2));
	}

	/**
	 * <p>최신 버전 출력(현재 버전과 최신 버전을 같이 보여준다)
	 * <p>깃허브에 등록한 최신버전 명시해놓은 파일을 불러와서 파싱해 출력.
	 * <p>최초 1회만 연결 작업 수행하며, 이후엔 변수에 저장된 내용 계속 사용.
	 * @param in 입력받을 버퍼리더 객체
	 */
	public static void printLatestVersionInfo(BufferedReader in){
		//최신 버전 정보가 null 인 경우에만 실행 == 최초 1회만 실행
		if(LATEST_VERSION_INFO == null){
			System.out.println("최신 버전 확인중...");
			try{
				Document doc = Jsoup.connect(LATEST_VERSION_URL).get();
				Elements e = doc.getElementsByClass("highlight tab-size js-file-line-container");
				LATEST_VERSION = e.select("#LC1").text().split("=")[1];
				LATEST_UPDATED_DATE = e.select("#LC2").text().split("=")[1];
				LATEST_WINDOWS = e.select("#LC3").text().split("=")[1];
				LATEST_OTHERS = e.select("#LC4").text().split("=")[1];
				
				LATEST_VERSION_INFO = String.format("최신버전: %s (%s)", LATEST_VERSION, LATEST_UPDATED_DATE);
			}
			catch(Exception e){
				ErrorHandling.saveErrLog("업데이트 서버 연결 실패", "", e);
				return; //업데이트 실패 시 버전 체크 작업 종료
			}
		}
		
		System.out.println(VERSION_INFO); //현재 버전 출력
		System.out.println(LATEST_VERSION_INFO); //서버의 최신 버전 출력
		
		//최신버전 체크가 제대로 됬을 때, 현재 버전이 최신 버전보다 낮으면 업데이트 여부 물어봄.
		if(LATEST_VERSION_INFO != null && LATEST_VERSION_INFO.length() > 0){
			int curVersion = Integer.parseInt(VERSION.replace(".", ""));
			int latestVersion = Integer.parseInt(LATEST_VERSION.replace(".", ""));
			
			if(curVersion < latestVersion) downloadLatestVersion(in);
			else if(curVersion == latestVersion) System.out.println("현재 최신버전입니다!");
			else System.out.println("버전이 이상합니다! ᕙ(•̀‸•́‶)ᕗ");
		}
	}
	
	/**
	 * <p>최신버전 다운로드 메서드.
	 * <p>최신버전 확인 메서드 내부에서 불러지는 용도로만 사용하게 제한.
	 * <p>사용자의 OS 값에 따라서 윈도우 / 그 이외(맥, 리눅스) 버전을 다운로드
	 * @param in 키보드 입력용 버퍼리더 객체
	 */
	private static void downloadLatestVersion(final BufferedReader in){
		try{
			final int MB = 1048576;
			final int BUF_SIZE = MB * 10;
			
			String select, fileName = null, fileURL = null;
			boolean isCorrectlySelected = false;
			
			while(!isCorrectlySelected){ //다운로드 받거나(y) 취소(n)를 제대로 선택할 때 까지 반복.
				System.out.printf("최신 버전(%s)을 다운받으시겠습니까? (Y/n): ", LATEST_VERSION);
				select = in.readLine();
				
				if(select.equalsIgnoreCase("y")){
					isCorrectlySelected = true;
					makeDir(); //Marumaru폴더 생성
					
					/* OS가 윈도우면, 파일 이름 = MMdownloader_0.5.0.0_Windows.zip */
					if(OS_NAME.contains("Windows")){
						fileName = LATEST_WINDOWS.substring(LATEST_WINDOWS.lastIndexOf("/")+1);
						fileURL = LATEST_WINDOWS;
					}
					/* OS가 윈도우 이외면 파일 이름 = MMdownloader_0.5.0.0_Mac,Linux.zip */
					else{
						fileName = LATEST_OTHERS.substring(LATEST_OTHERS.lastIndexOf("/")+1);
						fileURL = LATEST_OTHERS;
					}
					
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(DEFAULT_PATH+fileSeparator+fileName), BUF_SIZE);
					HttpURLConnection conn = (HttpURLConnection)new URL(fileURL).openConnection();
					conn.setConnectTimeout(30000); // 타임아웃 30초 
					conn.setReadTimeout(60000); // 타임아웃 1분
					
					BufferedInputStream bis = new BufferedInputStream(conn.getInputStream(), BUF_SIZE);
					
					final double MB_SIZE = (double) conn.getContentLength() / MB;	// 전체 파일 사이즈의 MB 값
					int readByte = 0;	// 읽은 바이트 값
					int accum = 0;		// 누적 바이트 길이
					int count = 0;		// MB 를 넘기 전 까지 읽어들인 바이트 길이
					
					System.out.println("다운로드중 ...");
					
					while((readByte = bis.read()) != -1){
						bos.write(readByte);
						
						/* MB 별로 다운로드 진행 상황을 출력함 */
						if(++count >= MB) {
							accum += count;
							count = 0;
							System.out.printf("%,3.2f MB / %,3.2f MB 완료!\n", (double) accum / MB, MB_SIZE);
						}
					}
					bos.close();
					bis.close();
					
					System.out.printf("%,3.2f MB / %,3.2f MB 완료!\n", (double) (accum + count) / MB, MB_SIZE);
					System.out.println("완료! (위치: "+DEFAULT_PATH+fileSeparator+fileName+")");
				}
				else if(select.equalsIgnoreCase("n")){
					isCorrectlySelected = true;
					System.out.println("업데이트가 취소되었습니다.");
				}
			}
		}
		catch(Exception e){
			ErrorHandling.saveErrLog("최신버전 다운로드 에러", "", e);
		}
	}
	
	/**
	 * <p>마루마루 인터넷창 열기
	 * <p>매개변수 void면 MARU_ADDR(http://marumaru.in/)을 호출
	 */
	public static void openBrowser(){
		openBrowser(MARU_ADDR);
	}
	
	/**
	 * <p>마루마루 인터넷창 열기
	 * <p>매개변수로 들어온 스트링 주소를 new URI(uri)를 통해 오픈
	 * <p>확장성을 위해 만들어 놓은 것
	 * @param uri String 형식 커스텀 uri주소
	 */
	public static void openBrowser(String uri){
		try{
			Desktop.getDesktop().browse(new URI(uri));
		}
		catch(Exception e){
			ErrorHandling.saveErrLog(uri+" 브라우저 접속 실패", "", e);
		}
	}
	
	/**
	 * <p>폴더 열기 기능
	 * <p>매개변수 void이면 DEFAULT_PATH(System.getProperty("user.home")) 자동 호출
	 */
	public static void openDir(){
		openDir(DEFAULT_PATH);
	}
	
	/**
	 * <p>폴더 열기 기능
	 * <p>매개변수로 들어온 스트링 경로를 오픈
	 * <p>확장성을 위해 만들어 놓은 것
	 * @param path String 형식 커스텀 path
	 */
	public static void openDir(String path){
		try{
			Desktop.getDesktop().open(new File(path));
		} 
		catch(Exception e){
			ErrorHandling.saveErrLog("폴더 오픈 실패", "", e);
		}
	}
	
	/**
	 * <p>폴더 생성 메서드
	 * <p>매개변수가 없으면 DEFAULT_PATH를 자동으로 생성
	 */
	public static void makeDir(){
		makeDir(DEFAULT_PATH);
	}
	
	/**
	 * <p>폴더 생성 메서드
	 * <p>매개변수로 경로를 받아 생성
	 * @param path String 타입의 경로
	 */
	public static void makeDir(String path){
		new File(path).mkdirs(); //존재하지 않는 경우만 새로 생성함
	}
	
	/**
	 * <p>도움말 출력 메서드
	 */
	public static void help(){
		System.out.println(MESSAGE);
	}
	
	/**
	 * 개발 참고용 시스템 사양 출력 메서드
	 */
	private static void printSystemProperties(){
		System.getProperties().list(System.out);
	}
	
	/* 도움말 */
	private final static String MESSAGE = 
		"## 도움말 ##\n"
		+ "1. 만화 다운로드\n"
		+ " - 다운받을 만화 주소를 입력합니다. 인식가능한 주소는 크게 3가지가 있습니다.\n"
		+ "  -- wasabisyrup과 같은 아카이브 주소: 입력한 1편만 다운로드 합니다.\n"
		+ "     ex) http://wasabisyrup.com/archives/807EZuSyjwA\n"
		+ "  -- mangaup 등이 포함된 만화 업데이트 주소: 해당 페이지에 있는 모든 아카이브 주소를 찾아 다운로드합니다.\n"
		+ "     ex) http://marumaru.in/b/mangaup/204237\n"
		+ "  -- 전편 보러가기 주소: 해당 페이지에 있는 모든 아카이브 주소를 찾아 다운로드 합니다.\n"
		+ "     ex) http://marumaru.in/b/manga/198822\n"
		+ "\n2. 선택적 다운로드\n"
		+ " - 받고 싶은 만화만 골라서 다운로드 합니다.\n"
		+ " - 주소 입력창에 '전편 보러가기'주소를 입력하면 다운로드 가능한 목록이 출력됩니다.\n"
		+ " - 이후 다운받을 페이지들을 정규식 형태로 입력하여 선택적 다운로드를 진행합니다.\n"
		+ " - 사용가능한 정규식은 다음과 같습니다.\n"
		+ "  -- 페이지 번호는 1번부터 시작합니다.\n"
		+ "  -- 각 만화의 번호 구분은 , 으로 합니다.\n"
		+ "     ex) 5, 3, 1, 7\n"
		+ "  -- 여러편을 이어서 받고 싶으면 각 회차 사이에 ~ 나 - 를 입력합니다.\n"
		+ "     ex) 4 ~ 10, 9-1\n"
		+ "  -- 각 번호 사이에 띄어쓰기는 해도 되고 안해도 됩니다.\n"
		+ "     ex) 1, 2, 3 이나 1,  2,3  이나 전부 같습니다.\n"
		+ "  -- 페이지 번호 입력 순서도 딱히 상관 없습니다(알아서 자동 오름차순 정렬이 됩니다)\n"
		+ "     ex) 17 ~ 15, 1-3 과 같이 입력하면 1,2,3,15,16,17로 최종 변환이 됩니다.\n"
		+ "  -- 중복된 페이지는 알아서 제거됩니다.\n"
		+ "     ex) 1, 3, 3, 2~4와 같은 경우 최종적으로 1,2,3,4로 변환이 됩니다.\n"
		+ "  -- 잘못된 페이지 번호는 걸러낸 뒤 다운로드 할 수 있는 최대한 다운로드를 시도합니다.\n"
		+ "     ex) 5번까지만 있는 만화에서, 4~6을 입력시 4~5까지만 다운로드.\n"
		+ "         단, 6,8 처럼 아예 정상 페이지가 하나도 없을시엔 다운로드 안함\n"
		+ "\n3. 다운로드 폴더 열기\n"
		+ " - 기본적으로 GUI가 지원되야 합니다. 만일 폴더가 없더라도 자동으로 생성되고 열리게 됩니다.\n"
		+ "  -- Windows의 경우 C:\\Users\\사용자\\Marumaru\\ 폴더가 열립니다.\n"
		+ "  -- Mac과 Linux의 경우 home/사용자/marumaru/ 폴더가 열립니다.\n"
		+ "\n4. 마루마루 접속\n"
		+ " - 기본적으로 GUI가 지원되야 합니다. 사용자 PC의 기본 브라우저를 이용하여 마루마루 페이지에 접속합니다.\n"
		+ "\n8. 설정\n"
		+ "  1) 업데이트 확인\n"
		+ "   - 서버에 등록된 최신 버전을 확인하고 보여줍니다.\n"
		+ "   - 현재 프로그램 보다 최신 버전이 있다면 다운로드 여부를 물어봅니다(Y/n)\n"
		+ "   - 다운로드를 선택하면 사용자의 OS를 바탕으로 Windows / 그 외(Mac, Linux) 버전을 자동 선택하여 다운로드합니다.\n"
		+ "   - 다운로드 경로는 만화 다운로드 경로(Marumaru 폴더)와 동일합니다\n"
		+ "  2) 저장경로 변경\n"
		+ "   - 입력한 경로로 저장 경로를 변경합니다. 잘못된 경로 입력 시 기존 경로를 유지합니다.\n"
		+ "   - 단, 만화가 저장되는 경로만 변경되는 것이며, 로그 폴더, 업데이트 다운로드 폴더 등은 기존 기본 경로로 유지됩니다.\n"
		+ "   - 기본값: C\\Users\\사용자\\Marumaru 또는 /home/사용자/Marumaru\n"
		+ "  3) 이미지 병합\n"
		+ "   - 다운받은 만화 폴더에 이미지들을 세로로 이어붙인 긴 이미지를 추가로 생성합니다.\n"
		+ "     기존의 좌, 우로 넘겨보던 방식 대신, 하나의 긴 이미지를 확대하여 스크롤 해서 볼 수 있습니다.\n"
		+ "   - 기본값: false\n"
		+ "  4) 디버깅 모드\n"
		+ "   - 만화 다운로드 시 파일의 용량과 메모리 사용량을 같이 출력합니다.\n"
		+ "   - 기본값: false\n"
		+ "  5) 멀티스레딩 다운로드\n"
		+ "   - 다운로드 시 멀티스레딩의 정도를 설정합니다.\n"
		+ "   - 대체로 값이 커질수록 성능은 좋아지나 메모리 사용량이 증가합니다.\n"
		+ "    -- 0: 멀티스레딩을 하지 않습니다 (초저성능)\n"
		+ "    -- 1: 코어 개수의 절반 만큼을 할당합니다 (저성능)\n"
		+ "    -- 2: 코어 개수 만큼을 할당합니다 (기본값, 권장)\n"
		+ "    -- 3: 코어 개수의 2배 만큼을 할당합니다 (고성능)\n"
		+ "    -- 4: 사용할 수 있는 최대한 할당합니다 (초고성능)\n"
		+ "   - 기본값: 2\n"
		+ "\n0. 종료\n"
		+ " - 모든 작업을 중단하고 프로그램을 종료합니다.\n"
		+ "\n작성자: occidere\t작성일: 2018.02.04\n\n";
}