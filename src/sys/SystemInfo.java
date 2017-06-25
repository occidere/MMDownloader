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

/**
 * <p>시스템과 관련된 클래스
 * <p>폴더생성, 브라우저 오픈등의 기능이 static으로 내장
 * <p>Instantiation 불가(private 생성자)
 * @author occidere
 */
@SuppressWarnings("unused")
public class SystemInfo {
	private SystemInfo(){}
	
	//운영체제별 줄바꿈 문자
	private static transient final String lineSeperator = System.getProperty("line.seperator");
	//OS이름 ex) Windows 10, Linux 등..
	public static transient final String OS_NAME = System.getProperty("os.name");
	//디폴트 저장 디렉토리 ex) C:\Users\occid\Marumaru 또는 home/occidere/marumaru
	public static transient final String DEFAULT_PATH = System.getProperty("user.home")+"/Marumaru/";
	//다운로드 저장할 디렉토리. 
	public static transient String PATH = DEFAULT_PATH;
	//에러 로그 저장 경로(무조건 디폴트 경로에 위치)
	public static final String ERROR_LOG_PATH = DEFAULT_PATH + "log/";
	//마루마루 브라우저 주소
	public static final String MARU_ADDR = "http://marumaru.in/";
	//최신 버전 공지할 페이지 주소
	private transient static final String LATEST_VERSION_URL = "https://github.com/occidere/MMDownloader/blob/master/VERSION_INFO";
	
	//conf파일 이름
	private transient final static String CONF_FILE = "MMDownloader.conf";
	
	//conf파일 어레이
	private transient static String[] confArr = new String[0];
	
	/* <수정 금지> 프로그램 정보 */
	private static final String VERSION = "0.3.1.0"; //프로그램 버전
	private static final String UPDATED_DATE = "2017.06.25"; //업데이트 날짜
	private static final String DEVELOPER = "제작자: occidere"; //제작자 정보
	private static final String VERSION_INFO = String.format("현재버전: %s (%s)", VERSION, UPDATED_DATE);
	
	/* 최신 버전 버전. 서버 연결해서 정보 받아오기 전까진 null */
	private static String LATEST_VERSION = null;
	private static String LATEST_UPDATED_DATE = null;
	private static String LATEST_VERSION_INFO = null;
	private static String LATEST_WINDOWS = null; //윈도우용 최신버전 링크
	private static String LATEST_OTHERS = null; //다른OS 최신버전 링크
	
	/**
	 * <p>제작자 정보와 현재 버전 출력.
	 * <p>프로그램 첫 시작시 보여줄 정보.
	 */
	public static void printVersionInfo(){
		System.out.print(DEVELOPER+"\t");
		System.out.println(VERSION_INFO);
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
			}
			catch(Exception e){
				LATEST_VERSION = LATEST_UPDATED_DATE = "연결실패";
				e.printStackTrace();
			}
			finally{
				LATEST_VERSION_INFO = String.format("최신버전: %s (%s)", LATEST_VERSION, LATEST_UPDATED_DATE);
			}
		}
		
		System.out.println(VERSION_INFO); //현재 버전 출력
		System.out.println(LATEST_VERSION_INFO); //서버의 최신 버전 출력
		
		//최신버전 체크가 제대로 됬을 때, 현재 버전이 최신 버전보다 낮으면 업데이트 여부 물어봄.
		if(!LATEST_VERSION_INFO.contains("연결실패")){
			int curVersion = Integer.parseInt(VERSION.replace(".", ""));
			int latestVersion = Integer.parseInt(LATEST_VERSION.replace(".", ""));
			
			if(curVersion < latestVersion) downloadLatestVersion(in);
			else if(curVersion == latestVersion) System.out.println("현재 최신버전입니다!");
		}
	}
	
	/**
	 * <p>최신버전 다운로드 메서드.
	 * <p>최신버전 확인 메서드 내부에서 불러지는 용도로만 사용하게 제한.
	 * <p>사용자의 OS 값에 따라서 윈도우 / 그 이외(맥, 리눅스) 버전을 다운로드
	 * @param in 키보드 입력용 버퍼리더 객체
	 */
	private static void downloadLatestVersion(BufferedReader in){
		try{
			final byte buf[] = new byte[10485760]; //10MB 버퍼
			//다운받은 바이트 길이, MB로 나누기 위한 값, 직전 다운로드 MB 값
			int len = 0, MB = 1024*1024, preDownloadedMB = 0;
			double accumSize = 0, totalSize = 0;//누적 다운로드 바이트, 총 파일 크기 바이트
			
			FileOutputStream fos;
			InputStream is;
			URLConnection conn;
			
			String select, fileName = null, fileURL = null;
			boolean isCorrectlySelected = false;
			
			while(!isCorrectlySelected){ //다운로드 받거나(y) 취소(n)를 제대로 선택할 때 까지 반복.
				System.out.printf("최신 버전(%s)을 다운받으시겠습니까? (Y/n): ", LATEST_VERSION);
				
				select = in.readLine();
				
				if(select.equalsIgnoreCase("y")){
					isCorrectlySelected = true;
					
					makeDir(); //Marumaru폴더 생성
					
					//OS가 윈도우면, 파일 이름 = MMdownloader_0.2.9_Windows.zip
					if(OS_NAME.contains("Windows")){
						fileName = LATEST_WINDOWS.substring(LATEST_WINDOWS.lastIndexOf("/")+1);
						fileURL = LATEST_WINDOWS;
					}
					//OS가 윈도우 이외면 파일 이름 = MMdownloader_0.2.9_Mac,Linux.zip
					else{
						fileName = LATEST_OTHERS.substring(LATEST_OTHERS.lastIndexOf("/")+1);
						fileURL = LATEST_OTHERS;
					}
					
					System.out.println("다운로드중...");
					System.out.println("저장 위치: "+DEFAULT_PATH+fileName);
					
					fos = new FileOutputStream(DEFAULT_PATH+fileName);
					conn = (HttpURLConnection)new URL(fileURL).openConnection();
					conn.setConnectTimeout(300000);
					is = conn.getInputStream();
					
					totalSize = (double)conn.getContentLength()/MB; //KB
					
					while((len = is.read(buf)) != -1){
						fos.write(buf, 0, len);
						accumSize+=(double)len/MB;
						
						if(preDownloadedMB < (int)accumSize){ //MB단위로만 로그 출력
							System.out.printf("%5.2fMB / %5.2fMB ... 완료!\n", accumSize, totalSize);
							preDownloadedMB = (int)accumSize;
						}
					}
					fos.close();
					is.close();
					
					//마지막 로그 출력
					System.out.printf("%5.2fMB / %5.2fMB ... 완료!\n", accumSize, totalSize);
					System.out.println("최신버전 다운로드 완료!");
				}
				else if(select.equalsIgnoreCase("n")){
					isCorrectlySelected = true;
					System.out.println("업데이트가 취소되었습니다.");
				}
			}
		}
		catch(Exception e){
			System.err.println("업데이트 에러!");
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
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
		File f = new File(path);
		if(!f.exists()) f.mkdirs();
	}
	
	/**
	 * <p>도움말 출력 메서드
	 */
	public static void help(){
		final String message = 
				"## 도움말 ##\n"
				+ "1. 만화 다운로드\n"
				+ " - 다운받을 만화 주소를 입력합니다. 인식가능한 주소는 크게 3가지가 있습니다.\n"
				+ "  1) wasabisyrup과 같은 아카이브 주소: 입력한 1편만 다운로드 합니다.\n"
				+ "     ex) http://wasabisyrup.com/archives/807EZuSyjwA\n"
				+ "  2) mangaup 등이 포함된 만화 업데이트 주소: 해당 페이지에 있는 모든 아카이브 주소를 찾아 다운로드합니다.\n"
				+ "     ex) http://marumaru.in/b/mangaup/204237\n"
				+ "  3) 전편 보러가기 주소: 해당 페이지에 있는 모든 아카이브 주소를 찾아 다운로드 합니다.\n"
				+ "     ex) http://marumaru.in/b/manga/198822\n"
				+ "\n2. 선택적 다운로드\n"
				+ " - 받고 싶은 만화만 골라서 다운로드 합니다.\n"
				+ " - 주소 입력창에 '전편 보러가기'주소를 입력하면 다운로드 가능한 목록이 출력됩니다.\n"
				+ " - 이후 다운받을 페이지들을 정규식 형태로 입력하여 선택적 다운로드를 진행합니다.\n"
				+ " - 사용가능한 정규식은 다음과 같습니다.\n"
				+ "  1) 페이지 번호는 1번부터 시작합니다.\n"
				+ "  2) 각 만화의 번호 구분은 , 으로 합니다.\n"
				+ "     ex) 0, 3, 1, 7\n"
				+ "  3) 여러편을 이어서 받고 싶으면 각 회차 사이에 ~ 나 - 를 입력합니다.\n"
				+ "     ex) 4 ~ 10, 9-1\n"
				+ "  4) 각 번호 사이에 띄어쓰기는 해도 되고 안해도 됩니다.\n"
				+ "     ex) 0, 1, 2 나 0,1,  2  나 전부 같습니다.\n"
				+ "  5) 페이지 번호 입력 순서도 딱히 상관 없습니다(알아서 자동 오름차순 정렬이 됩니다.)\n"
				+ "     ex) 17 ~ 15, 0-3 과 같이 입력하면 0,1,2,3,15,16,17로 최종 변환이 됩니다.\n"
				+ "  6) 중복된 페이지는 알아서 제거됩니다.\n"
				+ "     ex) 0, 3, 3, 2~4와 같은 경우 최종적으로 0,2,3,4로 변환이 됩니다.\n"
				+ "  7) 잘못된 페이지 번호는 걸러낸 뒤 다운로드 할 수 있는 최대한 다운로드를 시도합니다.\n"
				+ "     ex) 5번까지만 있는 만화에서, 4~6을 입력시 4~5까지만 다운로드.\n"
				+ "         단, 6,8 처럼 아예 정상 페이지가 하나도 없을시엔 다운로드 안함\n"
				+ "\n3. 다운로드 폴더 열기\n"
				+ " - 기본적으로 GUI가 지원되야 합니다. 만일 폴더가 없더라도 자동으로 생성되고 열리게 됩니다.\n"
				+ "  1) Windows의 경우 C:\\Users\\사용자\\Marumaru\\ 폴더가 열립니다.\n"
				+ "  2) Mac과 Linux의 경우 home/사용자/marumaru/ 폴더가 열립니다.\n"
				+ "\n4. 마루마루 접속\n"
				+ " - 기본적으로 GUI가 지원되어야 합니다. 사용자 PC의 기본 브라우저를 이용하여 마루마루 페이지에 접속합니다.\n"
				+ "\n8. 설정\n"
				+ "  1) 업데이트 확인\n"
				+ "   - 서버에 등록된 최신 버전을 확인하고 보여줍니다.\n"
				+ "   - 현재 프로그램 보다 최신 버전이 있다면 다운로드 여부를 물어봅니다(Y/n)\n"
				+ "   - 다운로드를 선택하면 사용자의 OS를 바탕으로 Windows / 그 외(Mac, Linux) 버전을 자동 선택하여 다운로드합니다.\n"
				+ "   - 다운로드 경로는 만화 다운로드 경로(Marumaru 폴더)와 동일합니다\n"
				+ "  2) 저장경로 변경\n"
				+ "   - 입력한 경로로 저장 경로를 변경합니다. 잘못된 경로 입력 시 기본 경로로 설정됩니다.\n"
				+ "   - 단, 만화가 저장되는 경로만 변경되는 것이며, 로그 폴더, 업데이트 다운로드 폴더 등은 기존 기본 경로로 유지됩니다.\n"
				+ "\n0. 종료\n"
				+ " - 모든 작업을 중단하고 프로그램을 종료합니다.\n"
				+ "\n작성자: occidere\t작성일: 2017.06.25\n\n";
		System.out.println(message);
	}
	
	/**
	 * 다운로드 도중 에러 발생시 로그로 남김
	 * 로그파일 제목 예: 2017-04-28_17-17-24_원피스_17화_003.txt
	 * @param name 에러 발생한 만화제목(제목+회차)
	 * @param e 예외 발생 객체
	 */
	public static void saveErrLog(String name, String message, Exception e) {
		try {
			//에러 발생 시간정보. 연-월-일_시-분-초
			String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
			
			//로그파일 제목 예: 2017-04-28_17-17-24_원피스_17화_003.txt
			String logfile = String.format("%s_%s.txt", time, name).replace(" ", "_");
			
			printError("에러 발생! "+logfile, false);
			
			makeDir(ERROR_LOG_PATH); //로그파일 저장 경로 없을 시를 대비해 만듦
			
			/* 로그 저장 (이어쓰기) */
			PrintWriter pw = new PrintWriter(new FileWriter(ERROR_LOG_PATH + "/" + logfile, true));
			pw.write(logfile + lineSeperator);
			pw.write(message + lineSeperator);
			e.printStackTrace(pw);
			pw.write(lineSeperator);
			pw.close();
			
		} catch (Exception ex) {
			System.err.println("로그 저장 실패!");
			ex.printStackTrace();
		}
	}
	
	/**
	 * 에러 출력 메서드.
	 * @param msg 출력할 에러 내용
	 * @param exitProgram (0: 종료하지 않음, 1: 프로그램 종료)
	 */
	public static void printError(String msg, boolean exitProgram){
		System.err.println(msg);
		if(exitProgram) System.exit(1);
	}
	
	
	/**************** 경로 변경 제대로 안되고, newline 출력도 안되고, 읽기 & 쓰기 모두 에러남 *****************************/
	
	/**
	 * <b>저장 경로 변경 메서드</b></br>
	 * path 값으로 주어진 저장경로 폴더를 생성 시도</br>
	 * 폴더 생성에 실패하면 저장경로를 이전경로 유지 및 에러 출력</br>
	 * @param path 변경할 새로운 저장경로
	 */
	public static void changePath(String path){
		File file = new File(path);
		if(file.mkdirs()) {
			PATH = path;
			writeConf("path", PATH);
			System.out.println("경로 변경 완료");
			System.out.println("현재 저장경로: "+PATH);
		}
		else if(file.exists() == false){ //이미 존재하는 폴더도 아니면서 생성도 실패한 "잘못된 경로" 인 경우
			printError("잘못된 경로입니다.", false);
		}
	}
	
	/**
	 * <b>설정파일 읽는 메서드</b></br>
	 * 설정파일은 반드시 디폴트 경로 내부에 MMDownloader.conf 로 존재해야 하며,</br>
	 * 내부에는 {@code name=value} 형태로 매핑이 되어있어야 한다.</br>
	 * conf파일 내부 주석은 #로 시작한다.</br>
	 * 20170625기준 저장경로(path)만 읽어들여 처리함</br>
	 */
	public static synchronized void readConf(){
		
		try{
			File confFile = new File(DEFAULT_PATH + "/" + CONF_FILE);
			if(confFile.exists() == false){
				//설정파일이 존재하지 않으면 그냥 메서드 종료
				System.out.println("없으므로 종료");
				return;
			}
			
			StringBuilder readStr = new StringBuilder();
			String line, name, value;
			int indexOfEqualSign = -1; // =의 위치
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(confFile)));
			
			//conf파일 읽어서 배열에 저장
			while((line = br.readLine()) != null){
				if(line.startsWith("#") == false){ //주석이 아닌것 공백은 그때그때 제거
					line = line.replaceAll(" ", "");
				}
				readStr.append(line+"\n");
			}
			br.close();
			System.out.println(readStr);
			confArr = readStr.toString().replaceAll(" ", "").split(lineSeperator);
			
			//배열값을 읽어서 실제 처리
			for(String confLine : confArr){
				
				confLine  = confLine.replaceAll(" ", "");
				
				//주석이나, =가 없는 비정상 라인의 경우 건너뛴다.
				if( confLine.startsWith("#") || confLine.contains("=")==false ){
					continue;
				}
				
				indexOfEqualSign = confLine.indexOf("=");
				name = confLine.substring(0, indexOfEqualSign);
				value = confLine.substring(indexOfEqualSign + 1);
				
				//저장경로(path) 읽어들여 반영
				if(confLine.startsWith("path")){
					changePath(value);
				}
				
			}
			
		}
		catch(Exception e){
			e.printStackTrace();
			printError("설정 파일 읽기 실패", false);
		}
	}
	
	/**
	 * <b>설정파일 쓰기 메서드</b></br>
	 * name=value 형태로 설정파일에 추가한다.</br>
	 * 설정파일이 없으면 새로 생성된다.</br>
	 * @param name 설정 변수 이름
	 * @param value 설정 변수 값
	 */
	public static synchronized void writeConf(String name, String value){
		try{
			File confFile = new File(DEFAULT_PATH + "/" + CONF_FILE);
			if(confFile.exists() == false){ //설정파일 없으면 새로 만듦
				System.out.println("새로 생성");
				confFile.createNewFile();
			}
			
			String newConfArr[] = new String[confArr.length + 1]; //1개의 설정파일이 추가되므로 기존 설정파일크기 + 1
			
			String trimmedConfLine; //공백 제거된 라인

			boolean isWrote = false;
			int idx = 0;
			
			//배열값을 읽어서 실제 처리
			for(String confLine : confArr){
				trimmedConfLine = confLine;
				if(confLine.contains(" ")) trimmedConfLine  = confLine.replaceAll(" ", "");
				
				//주석이나, =가 없는 비정상 라인의 경우 건너뛴다.
				if( trimmedConfLine.startsWith("#") || trimmedConfLine.contains("=")==false ){
					newConfArr[idx++] = confLine; //주석 등은 원형 그대로 저장
					continue;
				}
				
				//원하는 설정값 찾았으면 변경
				//공백 등이 제거된 최적의 상태로 저장
				if(trimmedConfLine.startsWith(name)){
					trimmedConfLine = name+"="+value;
					isWrote = true; //쓰기 성공 체크
				}
				newConfArr[idx++] = trimmedConfLine;
			}
			
			//설정파일 내에 원하는 설정값이 없어서 바꾸기 실패한 경우 새로 입력
			if(isWrote == false){
				newConfArr[0] = name+"="+value;
			}
			
			//새로 변경된 설정파일 모두 입력
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(confFile, true))); //이어쓰기 모드
			for(String confLine : newConfArr){
				bw.write(confLine + lineSeperator);
			}
			bw.flush();
			bw.close();
			
			confArr = newConfArr; //기존 설정파일 내역을 새로 생성된 설정파일로 갱신
		}
		catch(Exception e){
			e.printStackTrace();
			printError("설정 파일 쓰기 실패", false);
		}
	}
	
	/**
	 * 개발 참고용 시스템 사양 출력 메서드
	 */
	private static void printSystemProperties(){
		System.getProperties().list(System.out);
	}
}