package de.tuberlin.vs_12.utils;

import java.util.logging.Level;

public class Configuration {
    private static final String logPath = "./logs/";

    public static String getLogPath() {
        return logPath;
    }

    public static Level getRootLogLevel() {
        // Set the global default log level here
        return Level.INFO;
    }

    public static Level getConsoleLogLevel() {
        // Set the console handler's log level
        return Level.INFO;
    }
}