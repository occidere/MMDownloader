package sys;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Properties;

import common.ErrorHandling;

/**
 * 환경설정을 담당하는 static 클래스
 * @author occidere
 */
public class Configuration {
	private Configuration() {}
	
	// 환경설정 파일 이름
	private static final String CONF_NAME = "MMDownloader.properties";
	// 환경설정 파일 위치: Marumaru/MMDownloader.properties로 고정
	private static String CONF_PATH = SystemInfo.DEFAULT_PATH + SystemInfo.fileSeparator + CONF_NAME;
	
	private static File profile = new File(CONF_PATH);
	private static Properties prop = new Properties();
	
	/** 
	 * 설정파일 initialize 메서드<br>
	 * load -> set -> refresh(store -> load -> apply) 의 과정
	 */
	public static void init() {
		/* 시작과 동시에 설정파일(MMDownloader.properties) 읽어들여 적용 */
		try { loadProperty(); }
		catch(Exception e) { 
			ErrorHandling.saveErrLog("설정파일 읽기 실패", "", e);
		}
		
		/***** 이 사이엔 알맞은 시스템 변수들을 설정하는 내용이 들어가야 됨 *****/
		
		if(prop.containsKey("PATH")==false) prop.setProperty("PATH", SystemInfo.DEFAULT_PATH);
		if(prop.containsKey("MERGE")==false) prop.setProperty("MERGE", "false");
		if(prop.containsKey("DEBUG")==false) prop.setProperty("DEBUG", "false");
		if(prop.containsKey("MULTI")==false) prop.setProperty("MULTI", "2"); // MULTI = 0, 1, 2, 3, 4
		
		/************************************************************************/
		
		/* property 새로고침(store -> load -> apply) */
		try { refresh(); }
		catch(Exception e) {
			ErrorHandling.saveErrLog("설정파일 새로고침 실패", "", e);
		}
	}
	
	/**
	 * Property를 load하는 메서드
	 * @throws Exception
	 */
	public static void loadProperty() throws Exception {
		createDefaultFiles();
		prop.load(new BufferedInputStream(new FileInputStream(profile)));
	}

	/**
	 * Property를 저장하는 메서드
	 * @throws Exception
	 */
	public static void storeProperty() throws Exception {
		createDefaultFiles();
		prop.store(new BufferedOutputStream(new FileOutputStream(profile)), "");
	}
	
	/**
	 * 외부 class에 존재하는 프로퍼티 필드들에 대해 
	 * 새로 부여한 설정값을 적용하는 메서드(Reflection 이용)
	 * <li> 대상: SystemInfo(PATH) </li>
	 */
	private static void applyProperty() {
		Arrays.stream(SystemInfo.class.getDeclaredFields()) // SystemInfo 클래스의 모든 필드들을 불러옴
			.filter(f-> prop.containsKey(f.getName())) // 프로퍼티에 존재하는 필드들만 걸러냄
			.forEach(f-> {
				try { // 변수명에 맞는 이름을 찾아서 설정 적용
					f.set(SystemInfo.class, prop.getProperty(f.getName()));
				}
				catch (Exception e) {
					ErrorHandling.saveErrLog("설정값 적용 실패: "+f.getName(), f.toString(), e);
				}
			});
	}
	
	/**
	 * Store-> Load -> Apply를 순차적으로 실행
	 * @throws Exception
	 */
	public static void refresh() throws Exception {
		storeProperty();
		loadProperty();
		applyProperty();
	}
	
	/**
	 * 필수 파일들이 없으면 생성하는 메서드
	 * <li>{@code File profile = new File(CONF_PATH);}</li>
	 * <li>{@code Properties prop = new Properties();}</li>
	 * @throws Exception
	 */
	private static void createDefaultFiles() throws Exception {
		if(profile.exists() == false) {
			profile = new File(CONF_PATH);
			profile.createNewFile();
		}
		if(prop == null) prop = new Properties();
	}
	
	public static void setProperty(String key, String value) {
		prop.setProperty(key, value);
	}
	
	public static short getShort(String name, short def) {
		String tmp = prop.getProperty(name);
		return tmp == null ? def : Short.parseShort(tmp);
	}
	
	public static short getByte(String name, byte def) {
		String tmp = prop.getProperty(name);
		return tmp == null ? def : Byte.parseByte(tmp);
	}
	
	public static int getInt(String name, int def) {
		String tmp = prop.getProperty(name);
		return tmp == null ? def : Integer.parseInt(tmp);
	}

	public static float getFloat(String name, float def) {
		String tmp = prop.getProperty(name);
		return tmp == null ? def : Float.parseFloat(tmp);
	}
	
	public static double getDouble(String name, double def) {
		String tmp = prop.getProperty(name);
		return tmp == null ? def : Double.parseDouble(tmp);
	}
	
	public static String getString(String name, String def) {
		String tmp = prop.getProperty(name);
		return tmp == null ? def : tmp;
	}
	
	public static boolean getBoolean(String name, boolean def) {
		String tmp = prop.getProperty(name);
		return tmp == null ? def : Boolean.parseBoolean(tmp);
	}
	
	public static boolean exist() {
		return profile.exists();
	}
}
/*
변경사항
MULTI 프로퍼티 추가
설정파일 이름 변수로 분리(CONF_NAME = "MMDownloader.properties"
applyProperty을 stream으로 변경 & 시간복잡도 O(N^2) -> O(N)
applyProperty의 접근제어자 public 에서 private로 수정
File profile과 Properties prop을 생성하는 메서드 추가(createDefaultFiles)
*/