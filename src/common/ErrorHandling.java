package common;

import ch.qos.logback.classic.Logger;
import sys.SystemInfo;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 공통적으로 사용되는 에러 처리 static 클래스
 *
 * @author occidere
 */
public class ErrorHandling {
	private ErrorHandling() {}

	//운영체제별 파일 구분자
	private static final String fileSeparator = File.separator; //윈도우는 \, 나머지는 /
	// 프린트용 로거
	private static final Logger printer = MaruLoggerFactory.getPrintLogger();

	/**
	 * 에러 출력 메서드.
	 *
	 * @param msg         출력할 에러 내용
	 * @param exitProgram (false: 종료하지 않음, true: 프로그램 종료)
	 */
	public static void printError(String msg, boolean exitProgram) {
		printer.error(msg);
		if (exitProgram) {
			System.exit(1);
		}
	}

	/**
	 * 다운로드 도중 에러 발생시 로그로 남김
	 * 로그파일 제목 예: 2017-04-28_17-17-24_원피스_17화_003.txt
	 *
	 * @param name    에러 발생한 만화제목(제목+회차)
	 * @param message 추가적으로 기입할 메세지
	 * @param t       예외 발생 객체
	 */
	public static void saveErrLog(String name, String message, Throwable t) {
		try {
			//에러 발생 시간정보. 연-월-일_시-분-초
			String time = LocalDateTime.now()
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

			//로그파일 제목 예: 2017-04-28_17-17-24_원피스_17화_003.txt
			String logfile = String.format("%s_%s.txt", time, name).replace(" ", "_");

			printError(logfile, false);

			String logFilePath = SystemInfo.ERROR_LOG_PATH + fileSeparator + logfile;
			Logger errorLogger = MaruLoggerFactory.getErrorLogger(logFilePath);
			errorLogger.error(logfile);
			errorLogger.error(message, t);
		} catch (Exception ex) {
			printError("로그 저장 실패", false);
			ex.printStackTrace();
		}
	}
}