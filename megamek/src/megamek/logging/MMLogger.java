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

import javax.swing.JOptionPane;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

import io.sentry.Sentry;

import java.util.Collections;

/**
 * MMLogger
 *
 * Utility class to handle logging functions as well as reporting exceptions to
 * Sentry. To deal with general recommendations of confirming log level before
 * logging, additional checks are added to ensure we're only ever logging data
 * based upon the currently active log level.
 *
 * To utilize this class properly, it must be initialized within each class that
 * will use it. For example for use with the megamek.MegaMek class,
 *
 * private static final MMLogger logger = MMLogger.create(MegaMek.class);
 *
 * And then for use, use logger.info(message) and pass a string to it. Currently
 * supported levels include info, warn, debug, error, and fatal. Error and Fatal
 * can take any Throwable for sending to Sentry and has an overload for a title
 * to allow for displaying of a dialog box
 */
public class MMLogger extends ExtendedLoggerWrapper {
    private final ExtendedLoggerWrapper exLoggerWrapper;

    private static final String FQCN = MMLogger.class.getName();

    /**
     * Private constructor as there should never be an instance of this
     * class.
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
     * @param args    Variable list of arguments for message to be passed to
     *                String.format()
     */
    @Override
    public void info(String message, Object... args) {
        message = parametrizedStringIfEnabled(Level.INFO, message, args);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.INFO, null, message);
    }

    /**
     * Warning Level Logging
     *
     * @param message Message to be written to the log file.
     * @param args    Variable list of arguments for message to be
     *                passed to String.format()
     *
     */
    @Override
    public void warn(String message, Object... args) {
        message = parametrizedStringIfEnabled(Level.WARN, message, args);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.WARN, null, message);
    }

    /**
     * Warning Level Logging
     *
     * @param exception Exception that was caught via a try/catch block.
     * @param message   Message to be written to the log file.
     * @param args      Variable list of arguments for message to be
     *                  passed to String.format()
     *
     */
    public void warn(Throwable exception, String message, Object... args) {
        Sentry.captureException(exception);
        message = parametrizedStringIfEnabled(Level.WARN, message, args);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.WARN, null, message, exception);
    }

    /**
     * Debug Level Logging
     *
     * @param message Message to be written to the log file.
     * @param p0 parameter to the message.
     * @param p1 parameter to the message.
     * @param p2 parameter to the message.
     */
    @Override
    public void debug(final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0, p1, p2);
    }

    /**
     * Debug Level Logging
     *
     * @param message Message to be written to the log file.
     * @param args    Variable list of arguments for message to be passed to
     *                String.format()
     */
    @Override
    public void debug(String message, Object... args) {
        message = parametrizedStringIfEnabled(Level.DEBUG, message, args);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.DEBUG, null, message);
    }

    /**
     * Debug Level Logging
     *
     * @param exception Exception that was caught via a try/catch block.
     * @param message   Message to be written to the log file.
     * @param args      Variable list of arguments for message to be
     *                  passed to String.format()
     *
     */
    public void debug(Throwable exception, String message, Object... args) {
        Sentry.captureException(exception);
        message = parametrizedStringIfEnabled(Level.DEBUG, message, args);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.DEBUG, null, message, exception);
    }

    /**
     * Error Level Logging w/ Exception
     *
     * @param exception Exception that was caught via a try/catch block.
     * @param message   Additional message to report to the log file.
     */
    public void error(Throwable exception, String message) {
        Sentry.captureException(exception);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.ERROR, null, message, exception);
    }

    /**
     * Error Level Logging w/ Exception
     *
     * @param exception Exception that was caught via a try/catch block.
     * @param message   Additional message to report to the log file.
     */
    public void error(Throwable exception, String message, Object... args) {
        message = parametrizedStringIfEnabled(Level.ERROR, message, args);
        Sentry.captureException(exception);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.ERROR, null, message, exception);
    }

    /**
     * Error Level Logging w/ Exception
     *
     * This one was made to make it easier to replace the Log4J Calls
     *
     * @param message   Additional message to report to the log file.
     * @param exception Exception that was caught via a try/catch block.
     */
    public void error(String message, Throwable exception) {
        Sentry.captureException(exception);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.ERROR, null, message, exception);
    }

    /**
     * Error Level Logging w/o Exception.
     *
     * @param message Message to be written to the log file.
     */
    @Override
    public void error(String message) {
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.ERROR, null, message);
    }

    /**
     * Error Level Logging w/ Exception w/ Dialog.
     * @deprecated (since 01-feb-2025) Use {@link #errorDialog(Throwable, String, String, Object...)} instead.
     * @param exception Exception that was caught.
     * @param message   Message to write to the log file AND be displayed in the
     *                  error pane.
     * @param title     Title of the error message box.
     */
    @Deprecated
    public void error(Throwable exception, String message, String title) {
        errorDialog(exception, message, title, Collections.emptyList());
    }

    /**
     * Formatted Error Level Logging w/ Exception w/ Dialog.
     *
     * @param message Message to write to the log file AND be displayed in the
     *                error pane.
     * @param title   Title of the error message box.
     */
    public void errorDialog(Throwable e, String message, String title, Object... args) {
        Sentry.captureException(e);
        message = parametrizedStringAnyway(message, args);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.ERROR, null, message, e);
        popupErrorDialog(title, message);
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

    /**
     * Formatted Error Level Logging w/o Exception w/ Dialog.
     *
     * @param message Message to write to the log file AND be displayed in the
     *                error pane.
     * @param title   Title of the error message box.
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
     * @deprecated (since 01-feb-2025) Use {@link #fatalDialog(Throwable, String, String)} instead.
     * @param exception Exception that was triggered. Probably uncaught.
     * @param message   Message to report to the log file.
     * @param title     Title of the error message box.
     *
     */
    @Deprecated
    public void fatal(Throwable exception, String message, String title) {
        fatalDialog(exception, message, title);
    }

    /**
     * Fatal Level Logging w/ Exception w/ Dialog
     *
     * @param exception Exception that was triggered. Probably uncaught.
     * @param message   Message to report to the log file.
     * @param title     Title of the error message box.
     *
     */
    public void fatalDialog(Throwable exception, String message, String title) {
        fatal(exception, message);
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     *  Fatal Level Logging w/o Exception w/ Dialog
     * @deprecated (since 01-feb-2025) Use {@link #fatalDialog(String, String)} instead.
     * @param message Message to report to the log file.
     * @param title   Title of the error message box.
     *
     */
    @Deprecated
    public void fatal(String message, String title) {
        fatalDialog(message, title);
    }

    /**
     * Fatal Level Logging w/o Exception w/ Dialog
     *
     * @param message Message to report to the log file.
     * @param title   Title of the error message box.
     *
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
     * @return
     */
    public boolean isLevelLessSpecificThan(Level checkedLevel) {
        return exLoggerWrapper.getLevel().isLessSpecificThan(checkedLevel);
    }

}
