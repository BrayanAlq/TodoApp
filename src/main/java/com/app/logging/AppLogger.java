package com.app.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.*;

public class AppLogger {

    private static AppLogger instance;
    private final Logger logger;

    private static final String LOGGING_CONFIG = "resources/logging/logging.properties";

    private AppLogger() {
        logger = Logger.getLogger("com.app");
        logger.setUseParentHandlers(false);
        setupHandlers();
    }

    public static synchronized AppLogger getInstance() {
        if (instance == null) {
            instance = new AppLogger();
        }
        return instance;
    }

    private void setupHandlers() {
        try {
            java.util.Properties props = loadLoggingConfig();

            Level level       = Level.parse(props.getProperty("logging.level", "INFO"));
            String filePath   = props.getProperty("logging.file", "logs/app.log");
            int maxSize       = Integer.parseInt(props.getProperty("logging.file.maxSize", "1048576"));
            int maxCount      = Integer.parseInt(props.getProperty("logging.file.maxCount", "3"));
            boolean toConsole = Boolean.parseBoolean(props.getProperty("logging.console.enabled", "true"));

            logger.setLevel(level);

            Files.createDirectories(Paths.get(filePath).getParent());

            FileHandler fileHandler = new FileHandler(filePath, maxSize, maxCount, true);
            fileHandler.setLevel(level);
            fileHandler.setFormatter(new AppFormatter());
            logger.addHandler(fileHandler);

            if (toConsole) {
                ConsoleHandler consoleHandler = new ConsoleHandler();
                consoleHandler.setLevel(level);
                consoleHandler.setFormatter(new AppFormatter());
                logger.addHandler(consoleHandler);
            }

        } catch (IOException e) {
            System.err.println("[AppLogger] Error al configurar handlers: " + e.getMessage());
        }
    }

    private java.util.Properties loadLoggingConfig() {
        java.util.Properties props = new java.util.Properties();
        try (java.io.FileInputStream fis = new java.io.FileInputStream(LOGGING_CONFIG)) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("[AppLogger] No se encontr\u00f3 " + LOGGING_CONFIG + ". Usando defaults.");
        }
        return props;
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warning(String message) {
        logger.warning(message);
    }

    public void severe(String message) {
        logger.severe(message);
    }

    public void severe(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    public void logUserAction(String action) {
        logger.info("[USER_ACTION] " + action);
    }

    public void logDataOperation(String operation) {
        logger.info("[DATA] " + operation);
    }

    public void logConfigChange(String change) {
        logger.info("[CONFIG] " + change);
    }

    public void logError(String context, Throwable e) {
        logger.log(Level.SEVERE, "[ERROR] " + context, e);
    }

    public void debug(String message) {
        logger.fine(message);
    }

    public void trace(String message) {
        logger.finer(message);
    }

    public boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    private static class AppFormatter extends Formatter {

        private static final java.time.format.DateTimeFormatter DTF =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            String timestamp = java.time.LocalDateTime.now().format(DTF);
            String level     = record.getLevel().getName();
            String message   = formatMessage(record);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("[%s] [%-7s] %s%n", timestamp, level, message));

            if (record.getThrown() != null) {
                sb.append("  Causa: ").append(record.getThrown()).append(System.lineSeparator());
                for (StackTraceElement el : record.getThrown().getStackTrace()) {
                    sb.append("    at ").append(el).append(System.lineSeparator());
                }
            }
            return sb.toString();
        }
    }
}
