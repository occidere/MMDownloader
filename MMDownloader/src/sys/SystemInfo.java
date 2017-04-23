package sys;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;

/**
 * <p>시스템과 관련된 클래스
 * <p>폴더생성, 브라우저 오픈등의 기능이 static으로 내장
 * <p>Instantiation 불가(private 생성자)
 * @author occidere
 */
public class SystemInfo {
	private SystemInfo(){}
	//OS이름 ex) Windows 10, Linux 등..
	public static transient final String OS_NAME = System.getProperty("os.name");
	//home디렉토리 ex) C:\Users\occid\Marumaru 또는 home/occidere/marumaru
	public static transient final String DEFAULT_PATH = System.getProperty("user.home")+"/Marumaru/";
	//마루마루 브라우저 주소
	public static final String MARU_ADDR = "http://marumaru.in/";
	//프로그램 버전. * 수정금지 *
	public static final String VERSION = "제작자: occidere\t버전: 0.2.9 (2017.04.23)";
	
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
	
	public static void help(){
		final String message = 
				"## 도움말 ##\n"
				+ "1. 만화 다운로드\n"
				+ " - 다운받을 만화 주소를 입력합니다. 인식가능한 주소는 크게 3가지가 있습니다.\n"
				+ "  0) wasabisyrup과 같은 아카이브 주소: 입력한 1편만 다운로드 합니다.\n"
				+ "     ex) http://wasabisyrup.com/archives/807EZuSyjwA\n"
				+ "  1) mangaup 등이 포함된 만화 업데이트 주소: 해당 페이지에 있는 모든 아카이브 주소를 찾아 다운로드합니다.\n"
				+ "     ex) http://marumaru.in/b/mangaup/204237\n"
				+ "  2) 전편 보러가기 주소: 해당 페이지에 있는 모든 아카이브 주소를 찾아 다운로드 합니다.\n"
				+ "     ex) http://marumaru.in/b/manga/198822\n"
				+ "\n2. 선택적 다운로드\n"
				+ " - 받고 싶은 만화만 골라서 다운로드 합니다.\n"
				+ " - 주소 입력창에 '전편 보러가기'주소를 입력하면 다운로드 가능한 목록이 출력됩니다.\n"
				+ " - 이후 다운받을 페이지들을 정규식 형태로 입력하여 선택적 다운로드를 진행합니다.\n"
				+ " - 사용가능한 정규식은 다음과 같습니다.\n"
				+ "  0) 페이지 번호는 0번부터 시작합니다.\n"
				+ "  1) 각 만화의 번호 구분은 , 으로 합니다.\n"
				+ "     ex) 0, 3, 1, 7\n"
				+ "  2) 여러편을 이어서 받고 싶으면 각 회차 사이에 ~ 나 - 를 입력합니다.\n"
				+ "     ex) 4 ~ 10, 9-1\n"
				+ "  3) 각 번호 사이에 띄어쓰기는 해도 되고 안해도 됩니다.\n"
				+ "     ex) 0, 1, 2 나 0,1,  2  나 전부 같습니다.\n"
				+ "  4) 페이지 번호 입력 순서도 딱히 상관 없습니다(알아서 자동 오름차순 정렬이 됩니다.)\n"
				+ "     ex) 17 ~ 15, 0-3 과 같이 입력하면 0,1,2,3,15,16,17로 최종 변환이 됩니다.\n"
				+ "  5) 중복된 페이지는 알아서 제거됩니다.\n"
				+ "     ex) 0, 3, 3, 2~4와 같은 경우 최종적으로 0,2,3,4로 변환이 됩니다.\n"
				+ "  6) 잘못된 페이지 번호는 걸러낸 뒤 다운로드 할 수 있는 최대한 다운로드를 시도합니다.\n"
				+ "     ex) 5번까지만 있는 만화에서, 4~6을 입력시 4~5까지만 다운로드.\n"
				+ "         단, 6,8 처럼 아예 정상 페이지가 하나도 없을시엔 다운로드 안함\n"
				+ "\n3. 다운로드 폴더 열기\n"
				+ " - 기본적으로 GUI가 지원되야 합니다. 만일 폴더가 없더라도 자동으로 생성되고 열리게 됩니다.\n"
				+ "  0) Windows의 경우 C:\\Users\\사용자\\Marumaru\\ 폴더가 열립니다.\n"
				+ "  1) Mac과 Linux의 경우 home/사용자/marumaru/ 폴더가 열립니다.\n"
				+ "\n4. 마루마루 접속\n"
				+ " - 기본적으로 GUI가 지원되어야 합니다. 사용자 PC의 기본 브라우저를 이용하여 마루마루 페이지에 접속합니다.\n"
				+ "\n0. 종료\n"
				+ " - 모든 작업을 중단하고 프로그램을 종료합니다.\n"
				+ "\n작성자: occidere\t작성일: 2017.04.07\n\n";
		System.out.println(message);
	}
	
	/**
	 * 개발 참고용 시스템 사양 출력 메서드
	 */
	@SuppressWarnings("unused")
	private static void printSystemProperties(){
		System.getProperties().list(System.out);
	}
}
