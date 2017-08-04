/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.logging;

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA.
 *
 * @version $Id$
 * @author: Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 9/7/13 9:33 AM
 */
public class DefaultMmLogger implements MMLogger {

    private static final MMLogger instance = new DefaultMmLogger();

    private static final String METHOD_BEGIN = "Begin ";
    private static final String METHOD_END = "End ";
    private static final String METHOD_CALLED = "Called ";

    private final Map<String, Logger> nameToLogger = new ConcurrentHashMap<>();

    private static AtomicBoolean initialized =
            new AtomicBoolean(false);

    // Prevent instantiation.
    private DefaultMmLogger() {

    }

    public static MMLogger getInstance() {
        return instance;
    }

    @Override
    public Logger getLogger(final String loggerName) {
        Logger logger = nameToLogger.get(loggerName);
        if (null == logger) {
            logger = Logger.getLogger(loggerName);
            nameToLogger.put(loggerName, logger);
        }
        return logger;
    }

    @Override
    public <T extends Throwable> T log(final String className,
                                       final String methodName,
                                       final LogLevel logLevel,
                                       final String message,
                                       final T throwable) {

        // Make sure logging has been initialized.
        if (!initialized.get()) {
            if (!Logger.getRootLogger().getAllAppenders().hasMoreElements()) {
                LogConfig.getInstance().enableSimplifiedLogging();
            }
            initialized.set(true);
        }

        Logger logger = getLogger(className);

        // If logging has been turned off simply return.
        if (!logger.isEnabledFor(logLevel.getLevel())) {
            return throwable;
        }

        // Track the methods logged.
        LoggingProperties.getInstance().putProperty("method",
                                                    methodName);

        // Write the log entry.
        logger.log(logLevel.getLevel(), message, throwable);

        return throwable;
    }

    @Override
    public <T extends Throwable> T log(final Class callingClass,
                                       final String methodName,
                                       final T throwable) {
        return log(callingClass, methodName, LogLevel.ERROR, throwable);
    }

    @Override
    public <T extends Throwable> T log(final Class callingClass,
                                       final String methodName,
                                       final LogLevel logLevel,
                                       final T throwable) {

        // Construct the message from the Throwable's message.
        String message = "";
        if (null != throwable) {
            message = throwable.getMessage();
        }
        return log(callingClass.getName(), methodName, logLevel, message,
                   throwable);
    }

    @Override
    public <T extends Throwable> T log(final Class callingClass,
                                       final String methodName,
                                       final LogLevel level,
                                       final String message,
                                       final T throwable) {
        return log(callingClass.getName(), methodName, level, message,
                   throwable);
    }

    @Override
    public void log(final Class callingClass,
                    final String methodName,
                    final LogLevel level,
                    final String message) {
        log(callingClass.getName(), methodName, level, message, null);
    }

    @Override
    public void log(final Class callingClass,
                    final String methodName,
                    final LogLevel level,
                    final StringBuilder message) {
        log(callingClass, methodName, level, message.toString());
    }

    @Override
    public void methodBegin(Class callingClass, String methodName) {
        log(callingClass, methodName, LogLevel.DEBUG,
            METHOD_BEGIN + methodName);
    }

    @Override
    public void methodEnd(Class callingClass, String methodName) {
        log(callingClass, methodName, LogLevel.DEBUG,
            METHOD_END + methodName);
    }

    @Override
    public void methodCalled(Class callingClass, String methodName) {
        log(callingClass, methodName, LogLevel.DEBUG,
            METHOD_CALLED + methodName);
    }

    @Override
    public boolean willLog(final Class callingClass, final LogLevel level) {
        Logger logger = getLogger(callingClass.getName());
        return logger.isEnabledFor(level.getLevel());
    }

    @Override
    public void setLogLevel(final String category, final LogLevel level) {
        Logger logger = getLogger(category);
        logger.setLevel(level.getLevel());
    }

    @Override
    public LogLevel getLogLevel(String category) {
        return LogLevel.getFromLog4jLevel(getLogger(category).getLevel().toInt());
    }

    @Override
    public void removeLoggingProperties() {
        LoggingProperties.getInstance().remove();
    }
}
