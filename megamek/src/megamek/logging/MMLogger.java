/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */


package megamek.logging;

import java.util.Objects;
import javax.swing.JOptionPane;

import io.sentry.Sentry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

/**
 * <p>
 * Utility class to handle logging functions as well as reporting exceptions to Sentry. To deal with general
 * recommendations of confirming log level before logging, additional checks are added to ensure we're only ever logging
 * data based upon the currently active log level.
 * </p>
 * <h2>How to use:</h2>
 * <p>
 * To utilize this class properly, it must be initialized within each class that will use it. For example for use with
 * the {@code megamek.MegaMek.class}.
 * </p>
 *
 * <pre>{@code
 * private static final MMLogger LOGGER = MMLogger.create(MegaMek.class);
 * }</pre>
 * <p>
 * And then for use, use {@code LOGGER.info(message)} and pass a string to it. Currently supported levels include trace,
 * info, warn, debug, error, and fatal. Warn, Error and Fatal can take any Throwable for sending to Sentry and has an
 * overload for a title to allow for displaying of a dialog box.
 * </p>
 * <p>
 * This class also implements both the parametric pattern and the string format for the functions that are overridden
 * and added here. This means that you can use the following formats for the messages in some cases:
 * </p>
 *
 * <pre>{@code
 * LOGGER.info("number: {} string: {}", 42, "Hello");
 * LOGGER.info("number: %d string: %s", 42, "Hello");
 * }</pre>
 * <p>
 * Due to how the LOGGER works, the first format using curly braces is preferred, but the second one is grandfathered
 * exclusively for logs already existing, and it is not recommended for any new log.
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
     * Returns a custom Logger using the fully qualified name of the Class as the Logger name.
     *
     * @param loggerName The Class whose name should be used as the Logger name. If null it will default to the calling
     *                   class.
     *
     * @return The custom Logger.
     */
    public static MMLogger create(final Class<?> loggerName) {
        final Logger wrapped = LogManager.getLogger(loggerName);
        return new MMLogger(wrapped);
    }

    /**
     * Returns a custom Logger using a specific string as the Logger name.
     *
     * @param loggerName The name to be used as the Logger name. Cannot be null nor blank.
     *
     * @return The custom Logger.
     */
    public static MMLogger create(final String loggerName) {
        Objects.requireNonNull(loggerName == null ? null : (loggerName.isBlank() ? null : loggerName),
              "loggerName cannot be null nor blank");
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
        message = parametrizedStringIfEnabled(Level.WARN, message, args);
        Sentry.captureException(exception);
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
     * Error Level Logging w/ Exception This one was made to make it easier to replace the Log4J Calls
     *
     * @param message   Message to be logged.
     * @param exception Exception to be logged.
     */
    @Override
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
     * Formatted Error Level Logging w/ Exception w/ Dialog.
     *
     * @param exception Exception to be logged.
     * @param message   Message to write to the log file AND be displayed in the error pane.
     * @param title     Title of the error message box.
     * @param args      Variable list of arguments for the message.
     */
    public void errorDialog(Throwable exception, String message, String title, Object... args) {
        Sentry.captureException(exception);
        message = parametrizedStringAnyway(message, args);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.ERROR, null, message, exception);
        // An exception reached a dialog, so this is a defect rather than something the player can put right: offer
        // the reporting button.
        showErrorDialog(title, message, true);
    }

    /**
     * Formatted Error Level Logging w/o Exception w/ Dialog.
     *
     * @param message Message to write to the log file AND be displayed in the error pane.
     * @param title   Title of the error message box.
     * @param args    Variable list of arguments for the message
     */
    public void errorDialog(String title, String message, Object... args) {
        message = parametrizedStringAnyway(message, args);
        exLoggerWrapper.logIfEnabled(MMLogger.FQCN, Level.ERROR, null, message);
        popupErrorDialog(title, message, args);
    }

    /**
     * An extra button offered on error dialogs, installed by the user interface layer.
     *
     * <p>The label of the button that merely closes the dialog is supplied here as well. Offering a choice of buttons
     * means naming both of them, and {@code megamek.logging} sits below the user interface and so has no access to the
     * translated strings; whoever installs the hook does.</p>
     *
     * @param label        the reporting button text, already localized by whoever installs it
     * @param dismissLabel the text of the button that simply closes the dialog, already localized
     * @param action       what to do when the reporting button is pressed
     */
    public record ErrorDialogButton(String label, String dismissLabel, Runnable action) {}

    /**
     * The extra error-dialog button, or {@code null} when none is installed.
     *
     * <p>This is a hook rather than a direct call because {@code megamek.logging} is a lower layer than the user
     * interface and must not depend on it: MMLogger is also used by MegaMekLab, MekHQ and the headless dedicated
     * server. Marked {@code volatile} because an uncaught exception can surface on any thread.</p>
     */
    private static volatile ErrorDialogButton errorDialogButton;

    /**
     * Installs an extra button shown on every error dialog, typically one offering to report the problem.
     *
     * <p>An error dialog is the moment a player is most likely to want to file a bug, so this lets the user
     * interface put the reporting tools directly in front of them instead of expecting them to find a menu.</p>
     *
     * @param errorDialogButton the button to offer, or {@code null} to show a plain OK dialog as before
     */
    public static void setErrorDialogButton(ErrorDialogButton errorDialogButton) {
        MMLogger.errorDialogButton = errorDialogButton;
    }

    /**
     * Formatted Error Level Logging w/o Exception w/ Dialog.
     *
     * @param message Message to write to the log file AND be displayed in the error pane.
     * @param title   Title of the error message box.
     * @param args    Variable list of arguments for the message
     */
    private void popupErrorDialog(String title, String message, Object... args) {
        showErrorDialog(title, parametrizedStringAnyway(message, args), false);
    }

    /**
     * Shows the error dialog, optionally offering the reporting button alongside OK.
     *
     * <p>The button is offered only for errors that carry a {@link Throwable}, because those are the ones that
     * represent something going wrong in the code. The dialogs raised without one are largely conditions the player
     * has to put right themselves - a mismatched server version, a MegaMekLab path that needs configuring - and
     * inviting a bug report for those would send the maintainers noise rather than defects.</p>
     *
     * @param title           Title of the error message box.
     * @param message         The fully formatted message to display.
     * @param offerBugReport  Whether to offer the installed reporting button, if there is one.
     */
    private void showErrorDialog(String title, String message, boolean offerBugReport) {
        ErrorDialogButton extraButton = errorDialogButton;
        try {
            if (!offerBugReport || (extraButton == null)) {
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
                return;
            }

            Object[] options = { extraButton.label(), extraButton.dismissLabel() };
            int choice = JOptionPane.showOptionDialog(null, message, title, JOptionPane.DEFAULT_OPTION,
                  JOptionPane.ERROR_MESSAGE, null, options, options[1]);
            if (choice == 0) {
                extraButton.action().run();
            }
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
     * @param message Message to report to the log file.
     * @param title   Title of the error message box.
     */
    public void fatalDialog(String message, String title) {
        fatal(message);
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Takes the passed in Level and checks if the current log level is more specific than provided. This is a helper
     * method around the default LOGGER's method.
     *
     * @param checkedLevel Passed in Level to compare to.
     *
     * @return True if the current log level is more specific than the passed in
     */
    public boolean isLevelMoreSpecificThan(Level checkedLevel) {
        return exLoggerWrapper.getLevel().isMoreSpecificThan(checkedLevel);
    }

    /**
     * Takes the passed in Level and checks if the current log level is less specific than provided. This is a helper
     * method around the default LOGGER's method.
     *
     * @param checkedLevel Passed in Level to compare to.
     *
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
