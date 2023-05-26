package com.gs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.*;

public class LoggerUtil {
    private static final Logger logger = Logger.getLogger(LoggerUtil.class.getName());
    private static String logPropertiesFile;

    private static boolean isInitialized = false;

    public static void initialize() {
        if (!isInitialized) {
            try {
                logPropertiesFile = System.getProperty("log_properties_file");
                if (logPropertiesFile != null) {
                    LogManager.getLogManager().readConfiguration(new FileInputStream(logPropertiesFile));
                }
                else {
                    ClassLoader classLoader = LoggerUtil.class.getClassLoader();
                    File file = new File(classLoader.getResource("logging.properties").getFile());
                    LogManager.getLogManager().readConfiguration(new FileInputStream(file));
                }
                isInitialized = true;
                registerShutdownHook();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error loading logging configuration.", e);
            }
        }
    }

    public static Logger getLogger(String loggerName) {
        if (!isInitialized) {
            initialize();
        }
        return Logger.getLogger(loggerName);
    }
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LogManager.getLogManager().reset();
        }));
    }
}
