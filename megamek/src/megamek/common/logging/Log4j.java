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

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.io.IoBuilder;

/**
 * Utility methods for configuring log4j2.
 */
public class Log4j {

    private static final String FILE_PROPERTY_NAME = "megamek.log.file"; //$NON-NLS-1$

    private Log4j() {
        // no instances
    }

    /**
     * Reconfigures log4j using the configuration from the given stream.
     *
     * @throws IllegalArgumentException
     *         if the input stream can't be read
     */
    public static void useAlternateConfig(URI config) {
        ((LoggerContext) LogManager.getContext(false)).setConfigLocation(config);
    }

    /**
     * Redirects {@linkplain System#err} and {@linkplain System#out}
     * to the logging categories <tt>System.err</tt> and <tt>System.out</tt>,
     * where output is logged with a level of {@linkplain Level#ERROR} and
     * {@linkplain Level#INFO} respectively.
     */
    public static void snatchSytemStreams() {
        System.setErr(IoBuilder.forLogger("System.err").setLevel(Level.ERROR).buildPrintStream()); //$NON-NLS-1$
        System.setOut(IoBuilder.forLogger("System.out").setLevel(Level.INFO ).buildPrintStream()); //$NON-NLS-1$
    }

    /**
     * Causes logs to be written to the specified file instead of the default
     * one.
     * <p/>
     * This methods sets the system property <tt>megamek.log.file</tt> to the
     * path of the given file and refreshes the log4j configuration - see
     * <tt>log4j2.xml</tt> to see how the property <tt>megamek.log.file</tt>
     * is referenced.
     */
    public static void overrideMainFileAppender(File logFile) {
        // A system property survives log4j reconfigurations (eg: reloads and
        // the monitorinterval behaviour), while programmatically editing the
        // config would not.
        System.setProperty(FILE_PROPERTY_NAME, logFile.getAbsolutePath());
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
    }

}
