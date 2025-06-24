package de.tu_berlin.cit.vs.jms.common;



import org.slf4j.bridge.SLF4JBridgeHandler;
import java.util.logging.*;

public class LoggingUtils {

    static {
        try {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();


            // Configure the root logger and handlers
            Logger rootLogger = Logger.getLogger("");
            rootLogger.setLevel(Configuration.getRootLogLevel()); // Set default level

            // Remove existing handlers to avoid duplicates
            for (Handler handler : rootLogger.getHandlers()) {
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
