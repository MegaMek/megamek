/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2013-2020 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.logging;

import megamek.common.annotations.Nullable;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author: Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class DefaultMmLogger implements MMLogger {
    private static final MMLogger instance = new DefaultMmLogger();

    private static final String METHOD_BEGIN = "--- Method Begin ---";
    private static final String METHOD_END = "--- Method End ---";
    private static final String METHOD_CALLED = "--- Method Called ---";

    private final Map<String, Logger> nameToLogger = new ConcurrentHashMap<>();

    private static AtomicBoolean initialized = new AtomicBoolean(false);

    // Prevent instantiation.
    private DefaultMmLogger() {}

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
    public <T extends Throwable> T log(final String className, final String methodName,
                                       final LogLevel logLevel, String message, final T throwable) {
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
        LoggingProperties.getInstance().putProperty("method", methodName);

        message = methodName + " : " + message;

        // Write the log entry.
        logger.log(logLevel.getLevel(), message, throwable);

        return throwable;
    }

    @Override
    public <T extends Throwable> T log(final LogLevel logLevel, String message, final T throwable) {
        return log(getCallingClass(), getCallingMethod(), logLevel, message, throwable);
    }
    
    public <T extends Throwable> T log(final String className, final String methodName,
            final LogLevel logLevel, final T throwable) {
        // Construct the message from the Throwable's message.
        String message = "";
        if (null != throwable) {
            message = throwable.getMessage();
        }
        return log(className, methodName, logLevel, message, throwable);
    }

    //region Debug
    @Override
    public void debug(final String message) {
        log(getCallingClass(), getCallingMethod(), LogLevel.DEBUG, message, null);
    }

    @Override
    public <T extends Throwable> T debug(final T throwable) {
        return log(getCallingClass(), getCallingMethod(), LogLevel.DEBUG, throwable);
    }
    
    @Override
    public <T extends Throwable> T debug(final String message, final T throwable) {
        return log(getCallingClass(), getCallingMethod(), LogLevel.DEBUG, message, throwable);
    }
    //endregion Debug

    //region Error
    @Override
    public void error(final String message) {
        log(getCallingClass(), getCallingMethod(), LogLevel.ERROR, message, null);
    }
    
    @Override
    public <T extends Throwable> T error(final T throwable) {
        return log(getCallingClass(), getCallingMethod(), LogLevel.ERROR, throwable);
    }
    
    @Override
    public <T extends Throwable> T error(final String message, final T throwable) {
        return log(getCallingClass(), getCallingMethod(), LogLevel.ERROR, message, throwable);
    }
    //endregion Error

    //region Fatal
    @Override
    public void fatal(final String message) {
        log(getCallingClass(), getCallingMethod(), LogLevel.FATAL, message, null);
    }

    @Override
    public <T extends Throwable> T fatal(final T throwable) {
        return log(getCallingClass(), getCallingMethod(), LogLevel.FATAL, throwable);
    }

    @Override
    public <T extends Throwable> T fatal(final String message, final T throwable) {
        return log(getCallingClass(), getCallingMethod(), LogLevel.FATAL, message, throwable);
    }
    //endregion Fatal

    //region Info
    @Override
    public void info(final String message) {
        log(getCallingClass(), getCallingMethod(), LogLevel.INFO, message, null);
    }

    @Override
    public <T extends Throwable> T info(final T throwable) {
        return log(getCallingClass(), getCallingMethod(), LogLevel.INFO, throwable);
    }

    @Override
    public <T extends Throwable> T info(final String message, final T throwable) {
        return log(getCallingClass(), getCallingMethod(), LogLevel.INFO, message, throwable);
    }
    //endregion Info

    //region Trace
    @Override
    public void trace(final String message) {
        log(getCallingClass(), getCallingMethod(), LogLevel.TRACE, message, null);
    }

    @Override
    public <T extends Throwable> T trace(final T throwable) {
        return log(getCallingClass(), getCallingMethod(), LogLevel.TRACE, throwable);
    }

    @Override
    public <T extends Throwable> T trace(final String message, final T throwable) {
        return log(getCallingClass(), getCallingMethod(), LogLevel.TRACE, message, throwable);
    }
    //endregion Trace

    //region Warning
    @Override
    public void warning(final String message) {
        log(getCallingClass(), getCallingMethod(), LogLevel.WARNING, message, null);
    }

    @Override
    public <T extends Throwable> T warning(final T throwable) {
        return log(getCallingClass(), getCallingMethod(), LogLevel.WARNING, throwable);
    }

    @Override
    public <T extends Throwable> T warning(final String message, final T throwable) {
        return log(getCallingClass(), getCallingMethod(), LogLevel.WARNING, message, throwable);
    }
    //endregion Warning

    @Override
    public void methodBegin() {
        log(getCallingClass(), getCallingMethod(), LogLevel.DEBUG, METHOD_BEGIN, null);
    }
    
    @Override
    public void methodEnd() {
        log(getCallingClass(), getCallingMethod(), LogLevel.DEBUG, METHOD_END, null);
    }
    
    @Override
    public void methodCalled() {
        log(getCallingClass(), getCallingMethod(), LogLevel.DEBUG, METHOD_CALLED, null);
    }

    /**
     * Retrieves the name of the method and the line number 
     * calling log/info/error etc. from the stack trace.
     *
     * This method MUST be called from a top-level method (such as info or fatal)
     * as it depends on the call stack depth of exactly three methods between
     * internally calling getStackTrace and the user's code.
     */
    private String getCallingMethod() {
        try {
            String result = Thread.currentThread().getStackTrace()[3].getMethodName() + "()";
            result += ", line " + Thread.currentThread().getStackTrace()[3].getLineNumber();
            return result;
        } catch (Exception e) {
            return "DefaultMMLogger Error: Could not obtain method name.";
        }
    }
    
    /**
     * Retrieves the name of the class calling log/info/error etc.
     * from the stack trace.
     *
     * This method MUST be called from a top-level method (such as info or fatal)
     * as it depends on the call stack depth of exactly three methods between
     * internally calling getStackTrace and the user's code.
     */
    private String getCallingClass() {
        try {
            return Thread.currentThread().getStackTrace()[3].getClassName();
        } catch (Exception e) {
            return "DefaultMMLogger Error: Could not obtain class name.";
        }
    }

    @Override
    public boolean willLog(final Class<?> callingClass, final LogLevel level) {
        Logger logger = getLogger(callingClass.getName());
        return logger.isEnabledFor(level.getLevel());
    }

    @Override
    public void setLogLevel(final String category, final LogLevel level) {
        Logger logger = getLogger(category);
        logger.setLevel(level.getLevel());
    }
    
    @Override
    public void setLogLevel(final LogLevel level) {
        Logger logger = getLogger(getCallingClass());
        logger.setLevel(level.getLevel());
    }

    @Override
    public LogLevel getLogLevel(final String category) {
        return LogLevel.getFromLog4jLevel(getLogger(category).getLevel().toInt());
    }
    
    @Override
    public LogLevel getLogLevel() {
        return LogLevel.getFromLog4jLevel(getLogger(getCallingClass()).getLevel().toInt());
    }

    @Override
    public void removeLoggingProperties() {
        LoggingProperties.getInstance().remove();
    }

    @Override
    public void resetLogFile(@Nullable final String logFileName) {
        if (logFileName == null) {
            return;
        }
        File file = new File(logFileName);
        if (file.exists()) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.print("");
            } catch (FileNotFoundException e) {
                // This should not happen, but if it does we log it
                error(e);
                error("Error resetting log file. Please submit a bug report at "
                                + "https://github.com/MegaMek/megamek/issues");
            }
        }
    }
}
