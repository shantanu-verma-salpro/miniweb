package com.minihttp.LogWrapper;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogWrapper {
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT);

    public static void log(String message) {
        String logMessage = buildLogMessage(LogLevel.ERROR, message);
        System.out.println(logMessage);
    }

    public static void log(LogLevel level, String message) {
        String logMessage = buildLogMessage(level, message);
        System.out.println(logMessage);
    }

    private static String buildLogMessage(LogLevel level, String message) {
        String timestamp = dateFormatter.format(new Date());
        String logLevelStr = level.toString();
        return String.format("[%s] [%s] - %s", timestamp, logLevelStr, message);
    }

    public enum LogLevel {
        INFO, WARNING, ERROR
    }
}