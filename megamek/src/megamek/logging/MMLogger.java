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
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

import io.sentry.Sentry;

/**
 * MMLogger
 *
 * Utility class to handle logging functions as well as reporting
 * exceptions to Sentry. To deal with general recommendations of confirming
 * log level before logging, additional checks are added to ensure
 * we're only ever logging data based upon the currently active log level.
 */
public class MMLogger extends ExtendedLoggerWrapper {
    private final ExtendedLoggerWrapper exLoggerWrapper;

    private static final String FQCN = MMLogger.class.getName();

    /**
     * Private constructor as there should never be an instance of this
     * class.
     */
    private MMLogger(final Logger logger) {
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
     */
    @Override
    public void info(String message) {
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.INFO, null, message);
    }

    /**
     * Warning Level Logging
     *
     * @param message Message to be written to the log file.
     */
    @Override
    public void warn(String message) {
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.WARN, null, message);
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
     *
     * @param exception Exception that was caught.
     * @param message   Message to write to the log file AND be displayed in the
     *                  error pane.
     * @param title     Title of the error message box.
     */
    public void error(Throwable exception, String message, String title) {
        error(exception, message);
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
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
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
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
     * @param exception Exception that was triggered. Probably uncaught.
     * @param message   Message to report to the log file.
     * @param title     Title of the error message box.
     *
     */
    public void fatal(Throwable exception, String message, String title) {
        fatal(exception, message);
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Fatal Level Logging w/o Exception w/ Dialog
     *
     * @param message Message to report to the log file.
     * @param title   Title of the error message box.
     *
     */
    public void fatal(String message, String title) {
        fatal(message);
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
