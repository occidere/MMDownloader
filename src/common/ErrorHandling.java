package common;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import sys.SystemInfo;

/**
 * 공통적으로 사용되는 에러 처리 클래스
 * @author occidere
 *
 */
public class ErrorHandling {
	private ErrorHandling() {}
	
	//운영체제별 파일 구분자
	private	static final String fileSeparator = File.separator; //윈도우는 \, 나머지는 /
	//운영체제별 줄바꿈 문자
	private	static final String lineSeparator = System.getProperty("line.separator"); //윈도우는 \r\n, 유닉스 계열은 \n, 맥은 \r
	
	/**
	 * 에러 출력 메서드.
	 * @param msg 출력할 에러 내용
	 * @param exitProgram (false: 종료하지 않음, true: 프로그램 종료)
	 */
	public static void printError(String msg, boolean exitProgram){
		System.err.println("[Error] "+msg);
		if(exitProgram) System.exit(1);
	}
	
	/**
	 * 다운로드 도중 에러 발생시 로그로 남김
	 * 로그파일 제목 예: 2017-04-28_17-17-24_원피스_17화_003.txt
	 * @param name 에러 발생한 만화제목(제목+회차)
	 * @param message 추가적으로 기입할 메세지
	 * @param e 예외 발생 객체
	 */
	public static void saveErrLog(String name, String message, Exception e) {
		try {
			//에러 발생 시간정보. 연-월-일_시-분-초
			String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
			
			//로그파일 제목 예: 2017-04-28_17-17-24_원피스_17화_003.txt
			String logfile = String.format("%s_%s.txt", time, name).replace(" ", "_");
			
			printError(logfile, false);
			
			SystemInfo.makeDir(SystemInfo.ERROR_LOG_PATH); //로그파일 저장 경로 없을 시를 대비해 만듦
			
			/* 로그 저장 (이어쓰기) */
			PrintWriter pw = new PrintWriter(new FileWriter(SystemInfo.ERROR_LOG_PATH + fileSeparator + logfile, true));
			pw.write(logfile + lineSeparator);
			pw.write(message + lineSeparator);
			e.printStackTrace(pw);
			pw.write(lineSeparator);
			pw.close();
			
		} catch (Exception ex) {
			printError("로그 저장 실패", false);
			ex.printStackTrace();
		}
	}
}