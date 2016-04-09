package com.lukekorth.auto_fi.utilities;

import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Logger {

    public static void info(String message) {
        getLogger().info(message);
    }

    public static void error(String message) {
        getLogger().error(message);
    }

    public static void error(Exception exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        error(stringWriter.toString());
    }

    public static void warn(String message) {
        getLogger().warn(message);
    }

    public static void debug(String message) {
        getLogger().debug(message);
    }

    private static org.slf4j.Logger getLogger() {
        return LoggerFactory.getLogger(Logger.class);
    }
}
