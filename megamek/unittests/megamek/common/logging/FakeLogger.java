package megamek.common.logging;

import org.apache.log4j.Logger;

/**
 * Fake logger implementation for unit testing.
 * 
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 * @version %Id%
 * @since 7/31/2017 2:35 PM
 */
public class FakeLogger implements MMLogger {
    @Override
    public Logger getLogger(String loggerName) {
        return null;
    }

    @Override
    public <T extends Throwable> T log(String className, String methodName, LogLevel logLevel, String message, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T log(Class<?> callingClass, String methodName, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T log(Class<?> callingClass, String methodName, LogLevel logLevel, T throwable) {
        return null;
    }

    @Override
    public <T extends Throwable> T log(Class<?> callingClass, String methodName, LogLevel level, String message, T throwable) {
        return null;
    }

    @Override
    public void log(Class callingClass, String methodName, LogLevel level, String message) {

    }

    @Override
    public void log(Class callingClass, String methodName, LogLevel level, StringBuilder message) {

    }

    @Override
    public void methodBegin(Class callingClass, String methodName) {

    }

    @Override
    public void methodEnd(Class callingClass, String methodName) {

    }

    @Override
    public void methodCalled(Class callingClass, String methodName) {

    }

    @Override
    public boolean willLog(Class callingClass, LogLevel level) {
        return false;
    }

    @Override
    public void setLogLevel(String category, LogLevel level) {

    }

    @Override
    public LogLevel getLogLevel(String category) {
        return null;
    }

    @Override
    public void removeLoggingProperties() {

    }
}
