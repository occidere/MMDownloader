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
	public static final String VERSION = "제작자: occidere\t버전: 0.2.6 (2017.03.30)";
	
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
	 * 개발 참고용 시스템 사양 출력 메서드
	 */
	@SuppressWarnings("unused")
	private static void printSystemProperties(){
		System.getProperties().list(System.out);
	}
}
