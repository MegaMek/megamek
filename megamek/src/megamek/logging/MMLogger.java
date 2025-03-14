/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.logging;

import io.sentry.Sentry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

import javax.swing.*;
import java.util.Collections;

/**
 * <p>
 * Utility class to handle logging functions as well as reporting exceptions to
 * Sentry. To deal with general recommendations of confirming log level before
 * logging, additional checks are added to ensure we're only ever logging data
 * based upon the currently active log level.
 * </p>
 * <h2>How to use:</h2>
 * <p>
 * To utilize this class properly, it must be initialized within each class that
 * will use it. For example for use with the {@code megamek.MegaMek.class}.
 * </p>
 *
 * <pre>{@code
 * private static final MMLogger logger = MMLogger.create(MegaMek.class);
 * }</pre>
 * <p>
 * And then for use, use {@code logger.info(message)} and pass a string to it.
 * Currently
 * supported levels include trace, info, warn, debug, error, and fatal. Warn,
 * Error and Fatal
 * can take any Throwable for sending to Sentry and has an overload for a title
 * to allow for displaying of a dialog box.
 * </p>
 * <p>
 * This class also implements both the parametric pattern and the string format
 * for the functions that are overriden and added here. This means that you can
 * use the following formats for the messages in some cases:
 * </p>
 *
 * <pre>{@code
 * logger.info("number: {} string: {}", 42, "Hello");
 * logger.info("number: %d string: %s", 42, "Hello");
 * }</pre>
 * <p>
 * Due to how the logger works, the first format using curly braces is
 * preferred, but the second one
 * is grandfathered exclusively for logs already existing, and it is not
 * recommended for any new log.
 * </p>
 */
public class MMLogger extends ExtendedLoggerWrapper {
    private final ExtendedLoggerWrapper exLoggerWrapper;

    private static final String FQCN = MMLogger.class.getName();

    /**
     * Package protected constructor, this class should not be created directly.
     */
    MMLogger(final Logger logger) {
        super((AbstractLogger) logger, logger.getName(), logger.getMessageFactory());
        this.exLoggerWrapper = this;
    }

    /**
     * Returns a custom Logger with the name of the calling class.
     *
     * @return The custom Logger for the calling class.
     */
    public static MMLogger create() {
        final Logger wrapped = LogManager.getLogger();
        return new MMLogger(wrapped);
    }

    /**
     * Returns a custom Logger using the fully qualified name of the Class as
     * the Logger name.
     *
     * @param loggerName The Class whose name should be used as the Logger name.
     *                   If null it will default to the calling class.
     * @return The custom Logger.
     */
    public static MMLogger create(final Class<?> loggerName) {
        final Logger wrapped = LogManager.getLogger(loggerName);
        return new MMLogger(wrapped);
    }

    /**
     * Info Level Logging.
     *
     * @param message Message to be sent to the log file.
     * @param args    Variable list of arguments for the message
     */
    @Override
    public void info(String message, Object... args) {
        message = parametrizedStringIfEnabled(Level.INFO, message, args);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.INFO, null, message);
    }

    /**
     * Warning Level Logging
     *
     * @param message Message to be logged.
     * @param args    Variable list of arguments for the message
     */
    @Override
    public void warn(String message, Object... args) {
        message = parametrizedStringIfEnabled(Level.WARN, message, args);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.WARN, null, message);
    }

    /**
     * Warning Level Logging
     *
     * @param exception Exception to be logged.
     * @param message   Message to be logged.
     * @param args      Variable list of arguments for the message
     */
    public void warn(Throwable exception, String message, Object... args) {
        Sentry.captureException(exception);
        message = parametrizedStringIfEnabled(Level.WARN, message, args);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.WARN, null, message, exception);
    }

    /**
     * Debug Level Logging
     *
     * @param message Message to be logged.
     * @param args    Variable list of arguments for the message
     */
    @Override
    public void debug(String message, Object... args) {
        message = parametrizedStringIfEnabled(Level.DEBUG, message, args);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.DEBUG, null, message);
    }

    /**
     * Debug Level Logging
     *
     * @param exception Exception to be logged.
     * @param message   Message to be logged.
     * @param args      Variable list of arguments for the message
     */
    public void debug(Throwable exception, String message, Object... args) {
        Sentry.captureException(exception);
        message = parametrizedStringIfEnabled(Level.DEBUG, message, args);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.DEBUG, null, message, exception);
    }

    /**
     * Error Level Logging w/ Exception
     *
     * @param exception Exception to be logged.
     * @param message   Additional message.
     */
    public void error(Throwable exception, String message) {
        Sentry.captureException(exception);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.ERROR, null, message, exception);
    }

    /**
     * Error Level Logging w/ Exception
     *
     * @param exception Exception to be logged.
     * @param message   Additional message to be logged.
     * @param args      Variable list of arguments for the message
     */
    public void error(Throwable exception, String message, Object... args) {
        message = parametrizedStringIfEnabled(Level.ERROR, message, args);
        Sentry.captureException(exception);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.ERROR, null, message, exception);
    }

    /**
     * Error Level Logging w/ Exception
     * This one was made to make it easier to replace the Log4J Calls
     *
     * @param message   Message to be logged.
     * @param exception Exception to be logged.
     */
    public void error(String message, Throwable exception) {
        Sentry.captureException(exception);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.ERROR, null, message, exception);
    }

    /**
     * Error Level Logging w/o Exception.
     *
     * @param message Message to be logged.
     */
    @Override
    public void error(String message) {
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.ERROR, null, message);
    }

    /**
     * Error Level Logging w/ Exception w/ Dialog.
     *
     * @deprecated (since 21-feb-2025) Use
     *             {@link #errorDialog(Throwable, String, String, Object...)}
     *             instead.
     * @param exception Exception to be logged.
     * @param message   Message to write to the log file AND be displayed in the
     *                  error pane.
     * @param title     Title of the error message box.
     */
    @Deprecated(since = "0.50.04", forRemoval = true)
    public void error(Throwable exception, String message, String title) {
        errorDialog(exception, message, title, Collections.emptyList());
    }

    /**
     * Formatted Error Level Logging w/ Exception w/ Dialog.
     *
     * @param exception Exception to be logged.
     * @param message   Message to write to the log file AND be displayed in the
     *                  error pane.
     * @param title     Title of the error message box.
     * @param args      Variable list of arguments for the message.
     */
    public void errorDialog(Throwable exception, String message, String title, Object... args) {
        Sentry.captureException(exception);
        message = parametrizedStringAnyway(message, args);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.ERROR, null, message, exception);
        popupErrorDialog(title, message);
    }

    /**
     * Formatted Error Level Logging w/o Exception w/ Dialog.
     *
     * @param message Message to write to the log file AND be displayed in the
     *                error pane.
     * @param title   Title of the error message box.
     * @param args    Variable list of arguments for the message
     */
    public void errorDialog(String title, String message, Object... args) {
        message = parametrizedStringAnyway(message, args);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.ERROR, null, message);
        popupErrorDialog(title, message, args);
    }

    /**
     * Formatted Error Level Logging w/o Exception w/ Dialog.
     *
     * @param message Message to write to the log file AND be displayed in the
     *                error pane.
     * @param title   Title of the error message box.
     * @param args    Variable list of arguments for the message
     */
    private void popupErrorDialog(String title, String message, Object... args) {
        message = parametrizedStringAnyway(message, args);
        try {
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
        } catch (Exception ignored) {
            // if the message dialog crashes, we don't really care
        }
    }

    /**
     * Error Level Logging w/o Exception w/ Dialog.
     *
     * @param message Message to write to the log file AND be displayed in the
     *                error pane.
     * @param title   Title of the error message box.
     */
    public void error(String message, String title) {
        error(message);
        try {
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
        } catch (Exception ignored) {
            // if the message dialog crashes, we don't really care
        }
    }

    /**
     * Fatal Level Logging w/ Exception.
     *
     * @param exception Exception that was triggered. Probably uncaught.
     * @param message   Message to report to the log file.
     */
    public void fatal(Throwable exception, String message) {
        Sentry.captureException(exception);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.FATAL, null, message, exception);
    }

    /**
     * Fatal Level Logging w/ Exception w/ Dialog
     *
     * @deprecated (since 21-feb-2025) Use
     *             {@link #fatalDialog(Throwable, String, String)} instead.
     * @param exception Exception that was triggered. Probably uncaught.
     * @param message   Message to report to the log file.
     * @param title     Title of the error message box.
     */
    @Deprecated(since = "0.50.04", forRemoval = true)
    public void fatal(Throwable exception, String message, String title) {
        fatalDialog(exception, message, title);
    }

    /**
     * Fatal Level Logging w/ Exception w/ Dialog
     *
     * @param exception Exception that was triggered. Probably uncaught.
     * @param message   Message to report to the log file.
     * @param title     Title of the error message box.
     */
    public void fatalDialog(Throwable exception, String message, String title) {
        fatal(exception, message);
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Fatal Level Logging w/o Exception w/ Dialog
     *
     * @deprecated (since 21-feb-2025) Use {@link #fatalDialog(String, String)}
     *             instead.
     * @param message Message to report to the log file.
     * @param title   Title of the error message box.
     */
    @Deprecated(since = "0.50.04", forRemoval = true)
    public void fatal(String message, String title) {
        fatalDialog(message, title);
    }

    /**
     * Fatal Level Logging w/o Exception w/ Dialog
     *
     * @param message Message to report to the log file.
     * @param title   Title of the error message box.
     */
    public void fatalDialog(String message, String title) {
        fatal(message);
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Takes the passed in Level and checks if the current log level is more
     * specific than provided. This is a helper method around the default logger's
     * method.
     *
     * @param checkedLevel Passed in Level to compare to.
     * @return True if the current log level is more specific than the passed in
     */
    public boolean isLevelMoreSpecificThan(Level checkedLevel) {
        return exLoggerWrapper.getLevel().isMoreSpecificThan(checkedLevel);
    }

    /**
     * Takes the passed in Level and checks if the current log level is less
     * specific than provided. This is a helper method around the default logger's
     * method.
     *
     * @param checkedLevel Passed in Level to compare to.
     * @return True if the current log level is less specific than the passed in
     */
    public boolean isLevelLessSpecificThan(Level checkedLevel) {
        return exLoggerWrapper.getLevel().isLessSpecificThan(checkedLevel);
    }

    private String parametrizedStringIfEnabled(Level level, String message, Object... args) {
        if (isEnabled(level, null, message) && args.length > 0) {
            message = parametrizedStringAnyway(message, args);
        }
        return message;
    }

    private String parametrizedStringAnyway(String message, Object... args) {
        if (args.length > 0) {
            if (message.contains("{}")) {
                message = new ParameterizedMessage(message, args).getFormattedMessage();
            } else {
                message = String.format(message, args);
            }
        }
        return message;
    }

}
