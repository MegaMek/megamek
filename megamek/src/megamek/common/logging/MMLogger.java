/*
 * MegaMek - Copyright (C) 2018 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * This class is being phased out.
 * <p/>
 * For your logging needs, please use log4j2 directly.
 */
public class MMLogger {

    static final MMLogger INSTANCE = new MMLogger();

    private MMLogger() {
        // singleton
    }

    /**
     * Returns the specified logger.
     *
     * @param loggerName The name of the logger as defined in log4j.xml.
     * @return The named logger.
     */
    static Logger getLogger(String loggerName) {
        return LogManager.getLogger(loggerName);
    }

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
    static <T extends Throwable> T log(String className, @SuppressWarnings("unused") String methodName, LogLevel logLevel, String message, T throwable) {
        LogManager.getLogger(className).log(logLevel.getLevel(), message, throwable);
        return throwable;
    }

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#ERROR} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be
     *         re-thrown if desired.
     *
     * @deprecated Use {@link MMLogger#error(Class, String, Throwable)} instead.
     */
    @Deprecated
    public static <T extends Throwable> T log(Class<?> callingClass, @SuppressWarnings("unused") String methodName, T throwable) {
        LogManager.getLogger(callingClass).error(throwable != null ? throwable.getMessage() : "", throwable); //$NON-NLS-1$
        return throwable;
    }

    /**
     * Writes the passed log entry to the file.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param logLevel     The priority of the log entry.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    public static <T extends Throwable> T log(Class<?> callingClass, @SuppressWarnings("unused") String methodName, LogLevel logLevel, T throwable) {
        LogManager.getLogger(callingClass).log(logLevel.getLevel(), throwable != null ? throwable.getMessage() : "", throwable); //$NON-NLS-1$
        return throwable;
    }

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
    public static <T extends Throwable> T log(Class<?> callingClass, @SuppressWarnings("unused") String methodName, LogLevel level, String message, T throwable) {
        LogManager.getLogger(callingClass).log(level.getLevel(), message, throwable);
        return throwable;
    }

    /**
     * Writes the passed message to the log file.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param level        The priority of the log entry.
     * @param message      The message to be logged.
     */
    public static void log(Class<?> callingClass, @SuppressWarnings("unused") String methodName, LogLevel level, String message) {
        LogManager.getLogger(callingClass).log(level.getLevel(), message);
    }

    /**
     * Writes the passed log entry to the file.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param level        The priority of the log entry.
     * @param message      The message to be logged.
     */
    public static void log(Class<?> callingClass, @SuppressWarnings("unused") String methodName, LogLevel level, StringBuilder message) {
        LogManager.getLogger(callingClass).log(level.getLevel(), message.toString());
    }

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
    static <T extends Throwable> T debug(String callingClass, @SuppressWarnings("unused") String methodName, String message, T throwable) {
        LogManager.getLogger(callingClass).debug(message, throwable);
        return throwable;
    }

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
    static <T extends Throwable> T debug(Class<?> callingClass, @SuppressWarnings("unused") String methodName, String message, T throwable) {
        LogManager.getLogger(callingClass).debug(message, throwable);
        return throwable;
    }

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#DEBUG} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    static <T extends Throwable> T debug(Class<?> callingClass, @SuppressWarnings("unused") String methodName, T throwable) {
        LogManager.getLogger(callingClass).debug(throwable != null ? throwable.getMessage() : "", throwable); //$NON-NLS-1$
        return throwable;
    }

    /**
     * Writes the passed log entry to the file at the {@link LogLevel#DEBUG} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    public static void debug(Class<?> callingClass, @SuppressWarnings("unused") String methodName, String message) {
        LogManager.getLogger(callingClass).debug(message);
    }

    /**
     * Writes the passed log entry to the file at the {@link LogLevel#DEBUG} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    static void debug(Class<?> callingClass, @SuppressWarnings("unused") String methodName, StringBuilder message) {
        LogManager.getLogger(callingClass).debug(message.toString());
    }

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
    static <T extends Throwable> T error(String callingClass, @SuppressWarnings("unused") String methodName, String message, T throwable) {
        LogManager.getLogger(callingClass).error(message, throwable);
        return throwable;
    }

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
    public static <T extends Throwable> T error(Class<?> callingClass, @SuppressWarnings("unused") String methodName, String message, T throwable) {
        LogManager.getLogger(callingClass).error(message, throwable);
        return throwable;
    }

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#ERROR} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    public static <T extends Throwable> T error(Class<?> callingClass, @SuppressWarnings("unused") String methodName, T throwable) {
        LogManager.getLogger(callingClass).error(throwable != null ? throwable.getMessage() : "", throwable); //$NON-NLS-1$
        return throwable;
    }

    /**
     * Writes the passed log entry to the file at the {@link LogLevel#ERROR} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    public static void error(Class<?> callingClass, @SuppressWarnings("unused") String methodName, String message) {
        LogManager.getLogger(callingClass).error(message);
    }

    /**
     * Writes the passed log entry to the file at the {@link LogLevel#ERROR} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    static void error(Class<?> callingClass, @SuppressWarnings("unused") String methodName, StringBuilder message) {
        LogManager.getLogger(callingClass).error(message.toString());
    }

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
    static <T extends Throwable> T fatal(String callingClass, @SuppressWarnings("unused") String methodName, String message, T throwable) {
        LogManager.getLogger(callingClass).fatal(message, throwable);
        return throwable;
    }

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
    static <T extends Throwable> T fatal(Class<?> callingClass, @SuppressWarnings("unused") String methodName, String message, T throwable) {
        LogManager.getLogger(callingClass).fatal(message, throwable);
        return throwable;
    }

    /**
     * Writes the passed log entry to the file at the {@link LogLevel#FATAL} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    static void fatal(Class<?> callingClass, @SuppressWarnings("unused") String methodName, String message) {
        LogManager.getLogger(callingClass).fatal(message);
    }

    /**
     * Writes the passed log entry to the file at the {@link LogLevel#FATAL} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    static void fatal(Class<?> callingClass, @SuppressWarnings("unused") String methodName, StringBuilder message) {
        LogManager.getLogger(callingClass).fatal(message.toString());
    }

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
    static <T extends Throwable> T info(String callingClass, @SuppressWarnings("unused") String methodName, String message, T throwable) {
        LogManager.getLogger(callingClass).info(message, throwable);
        return throwable;
    }

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
    static <T extends Throwable> T info(Class<?> callingClass, @SuppressWarnings("unused") String methodName, String message, T throwable) {
        LogManager.getLogger(callingClass).info(message, throwable);
        return throwable;
    }

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#INFO} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    public static <T extends Throwable> T info(Class<?> callingClass, @SuppressWarnings("unused") String methodName, T throwable) {
        LogManager.getLogger(callingClass).info(throwable != null ? throwable.getMessage() : "", throwable); //$NON-NLS-1$
        return throwable;
    }

    /**
     * Writes the passed log entry to the file at the {@link LogLevel#INFO} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    public static void info(Class<?> callingClass, @SuppressWarnings("unused") String methodName, String message) {
        LogManager.getLogger(callingClass).info(message);
    }

    /**
     * Writes the passed log entry to the file at the {@link LogLevel#INFO} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    static void info(Class<?> callingClass, @SuppressWarnings("unused") String methodName, StringBuilder message) {
        LogManager.getLogger(callingClass).info(message.toString());
    }

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
    static <T extends Throwable> T trace(String callingClass, @SuppressWarnings("unused") String methodName, String message, T throwable) {
        LogManager.getLogger(callingClass).trace(message, throwable);
        return throwable;
    }

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
    static <T extends Throwable> T trace(Class<?> callingClass, @SuppressWarnings("unused") String methodName, String message, T throwable) {
        LogManager.getLogger(callingClass).trace(message, throwable);
        return throwable;
    }

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#TRACE} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    static <T extends Throwable> T trace(Class<?> callingClass, @SuppressWarnings("unused") String methodName, T throwable) {
        LogManager.getLogger(callingClass).trace(throwable != null ? throwable.getMessage() : "", throwable); //$NON-NLS-1$
        return throwable;
    }

    /**
     * Writes the passed log entry to the file at the {@link LogLevel#TRACE} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    static void trace(Class<?> callingClass, @SuppressWarnings("unused") String methodName, String message) {
        LogManager.getLogger(callingClass).trace(message);
    }

    /**
     * Writes the passed log entry to the file at the {@link LogLevel#TRACE} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    static void trace(Class<?> callingClass, @SuppressWarnings("unused") String methodName, StringBuilder message) {
        LogManager.getLogger(callingClass).trace(message.toString());
    }

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
    static <T extends Throwable> T warning(String callingClass, @SuppressWarnings("unused") String methodName, String message, T throwable) {
        LogManager.getLogger(callingClass).warn(message, throwable);
        return throwable;
    }

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
    static <T extends Throwable> T warning(Class<?> callingClass, @SuppressWarnings("unused") String methodName, String message, T throwable) {
        LogManager.getLogger(callingClass).warn(message, throwable);
        return throwable;
    }

    /**
     * Writes the passed {@link Throwable} to the log file at the
     * {@link LogLevel#WARNING} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param throwable    The error object to be logged.
     * @return The same throwable passed in to this method so that it may be re-thrown if desired.
     */
    static <T extends Throwable> T warning(Class<?> callingClass, @SuppressWarnings("unused") String methodName, T throwable) {
        LogManager.getLogger(callingClass).warn(throwable != null ? throwable.getMessage() : "", throwable); //$NON-NLS-1$
        return throwable;
    }

    /**
     * Writes the passed log entry to the file at the {@link LogLevel#WARNING}
     * level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    public static void warning(Class<?> callingClass, @SuppressWarnings("unused") String methodName, String message) {
        LogManager.getLogger(callingClass).warn(message);
    }

    /**
     * Writes the passed log entry to the file at the {@link LogLevel#WARNING}
     * level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     * @param message      The message to be logged.
     */
    static void warning(Class<?> callingClass, @SuppressWarnings("unused") String methodName, StringBuilder message) {
        LogManager.getLogger(callingClass).warn(message.toString());
    }

    // End convenience methods

    /**
     * Used to log entry into a method. The log entry is written at the
     * {@link LogLevel#DEBUG} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     */
    public static void methodBegin(Class<?> callingClass, String methodName) {
        LogManager.getLogger(callingClass).debug("Begin " + methodName); //$NON-NLS-1$
    }

    /**
     * Used to log exit from a method. The log entry is written at the
     * {@link LogLevel#DEBUG} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     */
    public static void methodEnd(Class<?> callingClass, String methodName) {
        LogManager.getLogger(callingClass).debug("End " + methodName); //$NON-NLS-1$
    }

    /**
     * Used to log when a method has been called. The log entry is written at the
     * {@link LogLevel#DEBUG} level.
     *
     * @param callingClass The name of the originating class.
     * @param methodName   The name of the originating method.
     */
    public static void methodCalled(Class<?> callingClass, String methodName) {
        LogManager.getLogger(callingClass).debug("Called " + methodName); //$NON-NLS-1$
    }

    /**
     * Returns true if the given class will have log entries written for the given
     * {@link LogLevel}.
     *
     * @param callingClass The class whose logging is to be verified.
     * @param level        The level of logging to be verified.
     * @return Whether a log entry will be written or not.
     */
    static boolean willLog(Class<?> callingClass, LogLevel level) {
        return LogManager.getLogger(callingClass).isEnabled(level.getLevel());
    }

    /**
     * Sets the {@link LogLevel} for the given category.
     *
     * @param category A fully-qualified package or class name.
     * @param level    The logging level to be set.
     */
    public static void setLogLevel(String category, LogLevel level) {
        Configurator.setLevel(category, level.getLevel());
    }

    /**
     * Returns the {@link LogLevel} for the given category.
     *
     * @param category A fully-qualified package or class name.
     * @return The given category's log level.
     */
    public static LogLevel getLogLevel(String category) {
        return LogLevel.getFromLog4jLevel(LogManager.getLogger(category).getLevel().intLevel());
    }

    /**
     * Clears all of the logging properties.
     */
    static void removeLoggingProperties() {
        // nop
        //
        // The ThreadContex/MDC was only used to store the method name (which
        // was then not printed in the log). Shall you want the method name in
        // the log, the log4j PatternLayout has build-in facilities to print
        // the location (class/method/file/line) a log call occurred
        // (see log4j2.xml) 
    }

}
