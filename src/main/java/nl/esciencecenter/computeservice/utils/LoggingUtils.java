package nl.esciencecenter.computeservice.utils;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

public class LoggingUtils {
	/**
	 * Adding a specific file logger for a job. This is Logback specific!
	 * 
	 * Taken from:
	 * https://stackoverflow.com/questions/7824620/logback-set-log-file-name-programmatically
	 * 
	 * @param name
	 */
	public static void addFileAppenderToLogger(String name, String logName) {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

		FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
		fileAppender.setContext(loggerContext);
		fileAppender.setName(name);
		// set the file name
		fileAppender.setFile(logName);

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(loggerContext);
		encoder.setPattern("%d{yyyy-MMM-dd HH:mm:ss.SSS} %level - %msg%n");
		encoder.start();

		fileAppender.setEncoder(encoder);
		fileAppender.start();

		// attach the rolling file appender to the logger of your choice
		ch.qos.logback.classic.Logger logbackLogger = loggerContext.getLogger(name);
		logbackLogger.addAppender(fileAppender);
	}
}
