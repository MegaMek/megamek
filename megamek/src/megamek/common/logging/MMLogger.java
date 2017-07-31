package megamek.common.logging;

import org.apache.log4j.Logger;

/**
 * Interface for the Logger object.
 *
 * @author Deric Page (deric dot page at usa dot net)
 * @version %Id%
 * @since 7/31/2017 9:12 AM
 */
public interface MMLogger {

    Logger getLogger(String loggerName);

    <T extends Throwable> T log(String className,
                                String methodName,
                                LogLevel logLevel,
                                String message,
                                T throwable);

    <T extends Throwable> T log(Class callingClass, String methodName, T throwable);

    <T extends Throwable> T log(Class callingClass, String methodName, LogLevel logLevel, T throwable);

    <T extends Throwable> T log(Class callingClass, String methodName, LogLevel level,
                                String message, T throwable);

    void log(Class callingClass, String methodName, LogLevel level,
             String message);

    void log(Class callingClass, String methodName, LogLevel level,
             StringBuilder message);

    void methodBegin(final Class callingClass, final String methodName);

    void methodEnd(final Class callingClass, final String methodName);

    void methodCalled(final Class callingClass, final String methodName);

    boolean willLog(Class callingClass, LogLevel level);

    void setLogLevel(String category, LogLevel level);

    LogLevel getLogLevel(String category);

    void newTransaction();

    void newTransaction(int transactionId);

    void setServerIp(String ip);

    void setClientIp(String ip);

    void setUserName(String userName);

    void removeLoggingProperties();
}
