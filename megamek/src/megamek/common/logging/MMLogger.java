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
    <T extends Throwable> T log(String className, String methodName, LogLevel logLevel,
                                String message, T throwable);

    /**
     * Writes the passed log entry to the file.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param logLevel     The priority of the log entry.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T log(Class<?> callingClass, String methodName,
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
    <T extends Throwable> T log(Class<?> callingClass, String methodName, LogLevel level,
                                String message, T throwable);

    /**
     * Writes the passed message to the log file.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param level        The priority of the log entry.
     * @param message      The message to be logged.
     */
    void log(Class<?> callingClass, String methodName, LogLevel level, String message);

    //region Convenience Methods
    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#DEBUG} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T debug(String callingClass, String methodName, String message, T throwable);

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#DEBUG} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T debug(Class<?> callingClass, String methodName, String message, T throwable);

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#DEBUG} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T debug(Class<?> callingClass, String methodName, T throwable);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#DEBUG} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    void debug(Class<?> callingClass, String methodName, String message);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#DEBUG} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param message      The message to be logged.
     */
    void debug(Object callingObject, String message);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#DEBUG} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T debug(Object callingObject, T throwable);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#DEBUG} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param message       The message to be logged.
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T debug(Object callingObject, String message, T throwable);

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#ERROR} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T error(String callingClass, String methodName, String message, T throwable);

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#ERROR} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T error(Class<?> callingClass, String methodName, String message, T throwable);

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#ERROR} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T error(Class<?> callingClass, String methodName, T throwable);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#ERROR} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    void error(Class<?> callingClass, String methodName, String message);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#ERROR} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param message       The message to be logged.
     */
    void error(Object callingObject, String message);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#ERROR} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T error(Object callingObject, T throwable);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#ERROR} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param message       The message to be logged.
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T error(Object callingObject, String message, T throwable);

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#FATAL} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T fatal(String callingClass, String methodName, String message, T throwable);

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#FATAL} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T fatal(Class<?> callingClass, String methodName, String message, T throwable);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#FATAL} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    void fatal(Class<?> callingClass, String methodName, String message);
    
    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#FATAL} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param message       The message to be logged.
     */
    void fatal(Object callingObject, String message);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#FATAL} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T fatal(Object callingObject, T throwable);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#FATAL} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param message       The message to be logged.
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T fatal(Object callingObject, String message, T throwable);

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#INFO} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T info(String callingClass, String methodName, String message, T throwable);

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#INFO} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T info(Class<?> callingClass, String methodName, String message, T throwable);

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#INFO} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T info(Class<?> callingClass, String methodName, T throwable);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#INFO} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    void info(Class<?> callingClass, String methodName, String message);
    
    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#INFO} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param message      The message to be logged.
     */
    void info(Object callingObject, String message);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#INFO} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T info(Object callingObject, T throwable);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#INFO} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param message       The message to be logged.
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T info(Object callingObject, String message, T throwable);
    
    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#TRACE} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T trace(String callingClass, String methodName, String message, T throwable);

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#TRACE} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T trace(Class<?> callingClass, String methodName, String message, T throwable);

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#TRACE} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T trace(Class<?> callingClass, String methodName, T throwable);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#TRACE} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    void trace(Class<?> callingClass, String methodName, String message);
    
    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#TRACE} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param message       The message to be logged.
     */
    void trace(Object callingObject, String message);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#TRACE} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T trace(Object callingObject, T throwable);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#TRACE} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param message       The message to be logged.
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T trace(Object callingObject, String message, T throwable);
    
    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#WARNING} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T warning(String callingClass, String methodName, String message, T throwable);

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#WARNING} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T warning(Class<?> callingClass, String methodName, String message, T throwable);

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#WARNING} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    <T extends Throwable> T warning(Class<?> callingClass, String methodName, T throwable);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#WARNING} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    void warning(Class<?> callingClass, String methodName, String message);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#WARNING} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param message       The message to be logged.
     */
    void warning(Object callingObject, String message);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#WARNING} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T warning(Object callingObject, T throwable);

    /**
     * Writes the passed log entry to the file at the
     * {@link LogLevel#WARNING} level. Extracts the calling method
     * automatically.
     *
     * @param callingObject The object calling this method. Pass <code><I>this</I></code> as callingObject
     * @param message       The message to be logged.
     * @param throwable     The error object to be logged.
     */
    <T extends Throwable> T warning(Object callingObject, String message, T throwable);
    //endregion Convenience Methods
    
    /**
     * Used to log entry into a method.  The log entry is written at the
     * {@link LogLevel#DEBUG} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     */
    void methodBegin(final Class<?> callingClass, final String methodName);

    /**
     * Used to log exit from a method.  The log entry is written at the
     * {@link LogLevel#DEBUG} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     */
    void methodEnd(final Class<?> callingClass, final String methodName);

    /**
     * Used to log when a method has been called.  The log entry is written at
     * the {@link LogLevel#DEBUG} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     */
    void methodCalled(final Class<?> callingClass, final String methodName);

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
     * Sets the {@link LogLevel} for the class of callingObject. 
     * Should be called using <code><I>this</I></code> as the first parameter.  
     *
     * @param callingObject the calling object. Use <code><I>this</I></code>
     * @param level    The logging level to be set.
     */
    void setLogLevel(Object callingObject, LogLevel level);

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

    /**
     * Resets the supplied log file
     */
    void resetLogFile(@Nullable final String logFileName);
}
