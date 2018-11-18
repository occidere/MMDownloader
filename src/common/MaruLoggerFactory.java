package common;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import lombok.experimental.UtilityClass;
import org.slf4j.LoggerFactory;

/**
 * 로거 팩토리
 *
 * @author occidere
 * @since 2018-11-17
 */
@UtilityClass
public final class MaruLoggerFactory {

	private static Logger printLogger;
	private static ConsoleAppender consoleAppender;
	private static FileAppender fileAppender;

	/**
	 * 에러 내용을 파일로 저장하는 Logger 를 가져온다.
	 *
	 * @param logFileName 저장될 로그파일의 절대경로
	 * @return 에러 출력 & 저장용 Logger 객체
	 */
	public static Logger getErrorLogger(String logFileName) {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		PatternLayoutEncoder encoder = getEncoder(loggerContext, "[%d{yyyy-MM-dd HH:mm:ss}][%-5level][%thread][%class{0}:%line] %msg%n");

		String loggerName = "ErrorLogger";
		FileAppender fileAppender = getFileAppender(loggerContext, encoder, logFileName, loggerName);

		Logger logger = loggerContext.getLogger(loggerName);
		logger.addAppender(fileAppender);
		logger.setAdditive(false);

		return logger;
	}

	/**
	 * 개행 없이 메시지 자체만 출력하는 프린트용 Logger 를 가져온다.
	 *
	 * @return 출력용 Logger 객체
	 */
	public static Logger getPrintLogger() {
		if (printLogger != null) {
			return printLogger;
		}

		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		PatternLayoutEncoder encoder = getEncoder(loggerContext, "%msg");

		String loggerName = "printLogger";
		ConsoleAppender consoleAppender = getConsoleAppender(loggerContext, encoder);

		printLogger = loggerContext.getLogger(loggerName);
		printLogger.addAppender(consoleAppender);
		printLogger.setAdditive(false);

		return printLogger;
	}

	private static PatternLayoutEncoder getEncoder(LoggerContext loggerContext, String pattern) {
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(loggerContext);
		encoder.setPattern(pattern);
		encoder.start();
		return encoder;
	}

	private static FileAppender getFileAppender(LoggerContext loggerContext, PatternLayoutEncoder encoder, String fileName, String loggerName) {
		if (fileAppender != null) {
			fileAppender.stop();
		}
		fileAppender = new FileAppender();
		fileAppender.setContext(loggerContext);
		fileAppender.setName(loggerName);
		fileAppender.setFile(fileName);
		fileAppender.setEncoder(encoder);
		fileAppender.start();
		return fileAppender;
	}

	private static ConsoleAppender getConsoleAppender(LoggerContext loggerContext, PatternLayoutEncoder encoder) {
		if (consoleAppender != null) {
			return consoleAppender;
		}
		consoleAppender = new ConsoleAppender();
		consoleAppender.setContext(loggerContext);
		consoleAppender.setEncoder(encoder);
		consoleAppender.start();
		return consoleAppender;
	}
}
