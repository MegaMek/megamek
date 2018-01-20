package megamek.common.logging;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.xml.DOMConfigurator;

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
    public static final String SIMPLE_LOGGING_PATTERN =
            "%d{HH:mm:ss,SSS} %p [%c] {%t} %m%n";  //NON-NLS
    public static final String CAT_MEGAMEK = "megamek"; //NON-NLS

    private static final LogConfig instance = new LogConfig();

    private final AtomicBoolean simpleLoggingConfigured =
            new AtomicBoolean(false);

    private static final String LOG_CONFIG_NAME = "log4j.xml"; //NON-NLS

    private static final String LOG_CONFIG_FILE_PATH =
            "mmconf/" + LOG_CONFIG_NAME; //NON-NLS

    private LogConfig() {

    }

    public static LogConfig getInstance() {
        return instance;
    }

    /**
     * Turns all logging off.
     */
    public void disableAll() {
        LogManager.getLoggerRepository().setThreshold(Level.OFF);
    }

    public void enableSimplifiedLogging() {
        if (simpleLoggingConfigured.get()) {
            return;
        }

        simpleLoggingConfigured.set(true);
        DefaultMmLogger.getInstance().setLogLevel(CAT_MEGAMEK,
                                                  LogLevel.INFO);

        // Check for updated logging properties every 30 seconds.
        DOMConfigurator.configureAndWatch(LOG_CONFIG_FILE_PATH, 30000);
    }
}
