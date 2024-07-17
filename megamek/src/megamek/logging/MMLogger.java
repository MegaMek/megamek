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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.sentry.Sentry;

/**
 * MMLogger
 *
 * Utility class to handle logging functions as well as reporting
 * exceptions to Sentry. To deal with general recommendations of confirming
 * log level before logging, additional checks are added to ensure
 * we're only ever logging data based upon the currently active log level.
 */
public class MMLogger {
    /**
     * Private constructor as there should never be an instance of this
     * class.
     */
    private MMLogger() {
        throw new IllegalStateException("MMLogger Utility Class");
    }

    /**
     * Info Level Logging.
     *
     * @param message Message to be sent to the log file.
     */
    public static void info(String message) {
        Logger logger = LogManager.getLogger();

        if (logger.isInfoEnabled()) {
            logger.info(message);
        }
    }

    /**
     * Warning Level Logging
     *
     * @param message Message to be written to the log file.
     */
    public static void warn(String message) {
        Logger logger = LogManager.getLogger();

        if (logger.isWarnEnabled()) {
            logger.warn(message);
        }
    }

    /**
     * Error Level Logging w/ Exception
     *
     * @param exception Exception that was caught via a try/catch block.
     * @param message   Additional message to report to the log file.
     */
    public static void error(Throwable exception, String message) {
        Sentry.captureException(exception);
        Logger logger = LogManager.getLogger();

        if (logger.isErrorEnabled()) {
            logger.error(message, exception);
        }
    }

    /**
     * Error Level Logging w/o Exception.
     *
     * @param message Message to be written to the log file.
     */
    public static void error(String message) {
        Logger logger = LogManager.getLogger();

        if (logger.isErrorEnabled()) {
            logger.error(message);
        }
    }

    /**
     * Error Level Logging w/ Exception w/ Dialog.
     *
     * @param exception Exception that was caught.
     * @param message   Message to write to the log file AND be displayed in the
     *                  error pane.
     * @param title     Title of the error message box.
     */
    public static void error(Throwable exception, String message, String title) {
        MMLogger.error(exception, message);
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Error Level Logging w/o Exception w/ Dialog.
     *
     * @param message Message to write to the log file AND be displayed in the
     *                error pane.
     * @param title   Title of the error message box.
     */
    public static void error(String message, String title) {
        MMLogger.error(message);
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Fatal Level Logging w/ Exception.
     *
     * @param exception Exception that was triggered. Probably uncaught.
     * @param message   Message to report to the log file.
     */
    public static void fatal(Throwable exception, String message) {
        Sentry.captureException(exception);

        Logger logger = LogManager.getLogger();
        if (logger.isFatalEnabled()) {
            logger.fatal(message, exception);
        }
    }

    /**
     * Fatal Level Logging w/ Exception w/ Dialog
     *
     * @param exception Exception that was triggered. Probably uncaught.
     * @param message   Message to report to the log file.
     * @param title     Title of the error message box.
     *
     */
    public static void fatal(Throwable exception, String message, String title) {
        MMLogger.fatal(exception, message);
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Fatal Level Logging w/o Exception w/ Dialog
     *
     * @param message Message to report to the log file.
     * @param title   Title of the error message box.
     *
     */
    public static void fatal(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

}
