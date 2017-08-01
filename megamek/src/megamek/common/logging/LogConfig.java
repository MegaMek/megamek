package megamek.common.logging;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class providing a simple interface for log4j configuration.
 *
 * @version %Id%
 * @author: Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 7/31/2017 9:27 AM
 */
public class LogConfig {

    // timestamp priority [category] {thread} message
    private static final String SIMPLE_LOGGING_PATTERN =
            "%d{HH:mm:ss,SSS} %p [%c#N] {%t} %m%n"; 
    private static final LogConfig instance = new LogConfig();

    private final AtomicBoolean simpleLoggingConfigured = new AtomicBoolean(false);

    public static final String LOG_CONFIG_NAME = "logging.properties";

    public static final String LOG_CONFIG_FILE_PATH = System.getProperty("user.home") + File.separator + LOG_CONFIG_NAME;

    private LogConfig() {

    }

    public static LogConfig getInstance() {
        return instance;
    }

    public void disableAll() {
        LogManager.getLoggerRepository().setThreshold(Level.OFF);
    }

    public void enableSimplifiedLogging() {
        if (simpleLoggingConfigured.get()) {
            return;
        }

        simpleLoggingConfigured.set(true);
        Layout layout = new PatternLayout(SIMPLE_LOGGING_PATTERN);
        LogManager.getRootLogger().addAppender(new ConsoleAppender(layout, ConsoleAppender.SYSTEM_OUT));

        DefaultMmLogger.getInstance().setLogLevel("megamek", LogLevel.ERROR);

        // Check for updated logging properties every 30 seconds.
        PropertyConfigurator.configureAndWatch(LOG_CONFIG_FILE_PATH, 30000);
    }

    public void enableClientLogging(final String applicationName) {
        try {
            Logger.getRootLogger().setLevel(Level.ERROR);
            Logger.getRootLogger()
                  .addAppender(new ClientAppender(new PatternLayout(SIMPLE_LOGGING_PATTERN),
                                                  System.getProperty("user.home") + File.separator +
                                                  applicationName + ".log"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ServerConfig {
        public ServerConfig() {
            if (!LogManager.getRootLogger().getAllAppenders().hasMoreElements()) {
                LogConfig.getInstance().enableSimplifiedLogging();
            }
        }
    }
}
