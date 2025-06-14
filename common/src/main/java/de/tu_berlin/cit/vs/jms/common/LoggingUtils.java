package de.tuberlin.vs_12.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

public class LoggingUtils {
    static {
        try {
            // Configure the root logger and handlers
            Logger rootLogger = Logger.getLogger("");
            rootLogger.setLevel(Configuration.getRootLogLevel()); // Set default level

            // Remove existing handlers to avoid duplicates
            for (java.util.logging.Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }

            // Add console handler
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Configuration.getConsoleLogLevel());
            consoleHandler.setFormatter(new CleanFormatter());
            rootLogger.addHandler(consoleHandler);

            // Log initialization success
            Logger initLogger = Logger.getLogger(LoggingUtils.class.getName());
            initLogger.info("Logging system initialized");
        } catch (Exception e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Logger getLogger(Class<?> clazz) {
        return Logger.getLogger(clazz.getName());
    }

    private static class CleanFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return "[" + record.getLevel() + "] " + record.getMessage() + System.lineSeparator();
        }
    }
}
