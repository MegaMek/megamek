/*
 * * MegaMek - Copyright (C) 2025 The MegaMek Team
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

package megamek.utilities;

import io.sentry.Sentry;
import megamek.MMLoggingConstants;
import megamek.SuiteConstants;
import megamek.common.net.marshalling.SanityInputFilter;
import megamek.logging.MMLogger;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import java.io.*;

/**
 * Common tasks to be performed when any of the suite programs first start.
 */
public final class StartupUtil {

    /**
     * Enable ObjectInputFilter, logging and Sentry, set global exception handler,
     * and other startup tasks to prepare the execution environment.
     * @param topLevelLogger The logger for uncaught exceptions
     * @param sentryAttributes The parameters for sentry for the specific project being started
     */
    public static void setupEnvironment(MMLogger topLevelLogger, SuiteConstants.SentryAttributes sentryAttributes) {
        setOif();
        setSentry(sentryAttributes);
        setTooltips();
        setExceptionHandler(topLevelLogger);
    }


    private static void setExceptionHandler(MMLogger logger) {
        Thread.setDefaultUncaughtExceptionHandler((thread, t) -> {
            final String name = t.getClass().getName();
            final String message = String.format(MMLoggingConstants.UNHANDLED_EXCEPTION, name);
            final String title = String.format(MMLoggingConstants.UNHANDLED_EXCEPTION_TITLE, name);
            logger.error(t, message, title);
        });
    }

    private static void setTooltips() {
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
    }

    private static final SanityInputFilter sanityInputFilter = new SanityInputFilter();
    private static void setOif() {
        ObjectInputFilter.Config.setSerialFilter(sanityInputFilter);
    }

    private static void setSentry(SuiteConstants.SentryAttributes sentryAttributes) {
        querySentry();
        initSentry(sentryAttributes);
    }

    /**
     * Configure Sentry with defaults.
     * The properties file is used to disable it and additional configuration can be
     * done inside the sentry.properties file. The defaults for everything else is set here.
     */
    private static void initSentry(SuiteConstants.SentryAttributes sentryAttributes) {
        Sentry.init(options -> {
            options.setEnableExternalConfiguration(true);
            options.setDsn(sentryAttributes.dsn());
            options.setEnvironment("production");
            options.setTracesSampleRate(0.2);
            options.setDebug(true);
            options.setServerName(sentryAttributes.serverName());
            options.setRelease(SuiteConstants.VERSION.toString());
        });

    }

    // Logging doesn't exist yet, so we just print stack trace
    @SuppressWarnings("CallToPrintStackTrace")
    private static void querySentry() {
        var propFile = new File("sentry.properties");
        if (propFile.isFile()) {
            return;
        }

        var window = new JFrame();
        var shouldSentry = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
            window,
            // todo: bother Tex and Hammer about appropriate legalese
            "Would you like to enable Sentry", "Sentry",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        try (var writer = new FileWriter(propFile, false)) {
            if (shouldSentry) {
                writer.write("enabled=true\n");
            } else {
                writer.write("enabled=false\n");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(window,
                "Fatal error trying to save Sentry settings. The program will now exit.\n"
                + ExceptionUtils.getStackTrace(e)
            );
            e.printStackTrace();
            System.exit(1);
        }
        window.dispose();

    }

    private StartupUtil() {}
}
