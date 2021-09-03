/*
 * Copyright (c) 2017-2020 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.logging;

import megamek.common.annotations.Nullable;
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
    <T extends Throwable> T log(final String className, final String methodName, final LogLevel logLevel,
                                String message, final T throwable);
    
    /**
     * Writes the passed log entry to the file.
     *
     * @param logLevel   The priority of the log entry.
     * @param message    The message to be logged.
     * @param throwable  The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T log(final LogLevel logLevel, String message, final T throwable);

    //region Convenience Methods
    //region Debug
    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#DEBUG} level. Extracts the calling method
     * automatically.
     *
     * @param message       The message to be logged.
     */
    void debug(final String message);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#DEBUG} level. Extracts the calling method
     * automatically.
     *
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T debug(final T throwable);
    
    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#DEBUG} level. Extracts the calling method
     * automatically.
     *
     * @param message       The message to be logged.
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T debug(final String message, final T throwable);

    //endregion Debug

    //region Error
    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#ERROR} level. Extracts the calling method
     * automatically.
     *
     * @param message       The message to be logged.
     */
    void error(final String message);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#ERROR} level. Extracts the calling method
     * automatically.
     *
     * @param throwable    The error object to be logged.
     */
    <T extends Throwable> T error(final T throwable);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#ERROR} level. Extracts the calling method
     * automatically.
     *
     * @param message       The message to be logged.
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T error(final String message, final T throwable);
    //endregion Error

    //region Fatal
    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#FATAL} level. Extracts the calling method
     * automatically.
     *
     * @param message       The message to be logged.
     */
    void fatal(final String message);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#FATAL} level. Extracts the calling method
     * automatically.
     *
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T fatal(final T throwable);
    
    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#FATAL} level. Extracts the calling method
     * automatically.
     *
     * @param message       The message to be logged.
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T fatal(final String message, final T throwable);
    //endregion Fatal

    //region Info
    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#INFO} level. Extracts the calling method
     * and class automatically.
     *
     * @param message       The message to be logged.
     */
    void info(final String message);
    
    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#INFO} level. Extracts the calling method
     * and class automatically.
     *
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T info(final T throwable);
    
    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#INFO} level. Extracts the calling method
     * and class automatically.
     *
     * @param message       The message to be logged.
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T info(final String message, final T throwable);
    //endregion Info

    //region Trace
    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#TRACE} level. Extracts the calling method
     * and class automatically.
     *
     * @param message       The message to be logged.
     */
    void trace(final String message);
    
    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#TRACE} level. Extracts the calling method
     * and class automatically.
     *
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T trace(final T throwable);
    
    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#TRACE} level. Extracts the calling method
     * and class automatically.
     *
     * @param message       The message to be logged.
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T trace(final String message, final T throwable);
    //endregion Trace

    //region Warning
    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#WARNING} level. Extracts the calling method
     * and class automatically.
     *
     * @param message       The message to be logged.
     */
    void warning(final String message);
    
    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#WARNING} level. Extracts the calling method
     * and class automatically.
     *
     * @param throwable    The error object to be logged.
     */
    <T extends Throwable> T warning(final T throwable);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#WARNING} level. Extracts the calling method
     * and class automatically.
     *
     * @param message       The message to be logged.
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T warning(final String message, final T throwable);
    //endregion Warning
    //endregion Convenience Methods
    
    /**
     * Used to log entry into a method.  The log entry is written at the
     * {@link LogLevel#DEBUG} level.
     */
    void methodBegin();

    /**
     * Used to log exit from a method.  The log entry is written at the
     * {@link LogLevel#DEBUG} level.
     */
    void methodEnd();

    /**
     * Used to log when a method has been called.  The log entry is written at
     * the {@link LogLevel#DEBUG} level.
     */
    void methodCalled();

    /**
     * Returns true if the given class will have log entries written for the
     * given {@link LogLevel}.
     *
     * @param callingClass The class whose logging is to be verified.
     * @param level        The level of logging to be verified.
     * @return Whether a log entry will be written or not.
     */
    boolean willLog(Class<?> callingClass, LogLevel level);

    /**
     * Sets the {@link LogLevel} for the given category.
     *
     * @param category A fully-qualified package or class name.
     * @param level    The logging level to be set.
     */
    void setLogLevel(String category, LogLevel level);
    
    /**
     * Sets the {@link LogLevel} for the calling class. 
     *
     * @param level    The logging level to be set.
     */
    void setLogLevel(LogLevel level);

    /**
     * Returns the {@link LogLevel} for the given category.
     *
     * @param category A fully-qualified package or class name.
     * @return The given category's log level.
     */
    LogLevel getLogLevel(String category);
    
    /**
     * Returns the {@link LogLevel} for the calling class.
     *
     * @return The given category's log level.
     */
    LogLevel getLogLevel();

    /**
     * Clears all of the logging properties.
     */
    void removeLoggingProperties();

    /**
     * Resets the supplied log file
     */
    void resetLogFile(final @Nullable String logFileName);
}