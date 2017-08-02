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

    /**
     * Returns the specified logger.
     *
     * @param loggerName The name of the logger as defined in log4j.xml.
     * @return The named logger.
     */
    Logger getLogger(String loggerName);

    /**
     * Writes the passed log entry to the file.
     *
     * @param className  The name of the originating class.
     * @param methodName The name of the originating method.
     * @param logLevel   The priority of the log entry.
     * @param message    The message to be logged.
     * @param throwable  The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T log(String className,
                                String methodName,
                                LogLevel logLevel,
                                String message,
                                T throwable);


    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#ERROR} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T log(Class callingClass, String methodName,
                                T throwable);


    /**
     * Writes the passed log entry to the file.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param logLevel     The priority of the log entry.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T log(Class callingClass, String methodName,
                                LogLevel logLevel, T throwable);


    /**
     * Writes the passed log entry to the file.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param level        The priority of the log entry.
     * @param message      The message to be logged.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T log(Class callingClass, String methodName,
                                LogLevel level, String message, T throwable);


    /**
     * Writes the passed message to the log file.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param level        The priority of the log entry.
     * @param message      The message to be logged.
     */
    void log(Class callingClass, String methodName, LogLevel level,
             String message);


    /**
     * Writes the passed log entry to the file.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param level        The priority of the log entry.
     * @param message      The message to be logged.
     */
    void log(Class callingClass, String methodName, LogLevel level,
             StringBuilder message);

    /**
     * Used to log entry into a method.  The log entry is written at the
     * {@link LogLevel#DEBUG} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     */
    void methodBegin(final Class callingClass, final String methodName);

    /**
     * Used to log exit from a method.  The log entry is written at the
     * {@link LogLevel#DEBUG} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     */
    void methodEnd(final Class callingClass, final String methodName);

    /**
     * Used to log when a method has been called.  The log entry is written at
     * the {@link LogLevel#DEBUG} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     */
    void methodCalled(final Class callingClass, final String methodName);

    /**
     * Returns true if the given class will have log entries written for the
     * given {@link LogLevel}.
     *
     * @param callingClass The class whose logging is to be verified.
     * @param level        The level of logging to be verified.
     * @return Whether a log entry will be written or not.
     */
    boolean willLog(Class callingClass, LogLevel level);

    /**
     * Sets the {@link LogLevel} for the given category.
     *
     * @param category A fully-qualified package or class name.
     * @param level    The logging level to be set.
     */
    void setLogLevel(String category, LogLevel level);

    /**
     * Returns the {@link LogLevel} for the given category.
     *
     * @param category A fully-qualified package or class name.
     * @return The given category's log level.
     */
    LogLevel getLogLevel(String category);

    /**
     * Clears all of the logging properties.
     */
    void removeLoggingProperties();
}
