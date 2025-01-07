/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;

/**
 * @author Ben
 * @since March 30, 2002, 2:40 PM
 *        Renamed from ServerLog to GameLog in July 2005
 */
public class GameLog {
    private static final MMLogger logger = MMLogger.create(GameLog.class);

    public static final String LOG_DIR = PreferenceManager.getClientPreferences().getLogDirectory();

    private File logfile;

    BufferedWriter writer;

    /**
     * Creates GameLog named
     */
    public GameLog(String filename) {
        try {
            File logDir = new File(LOG_DIR);
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            if (PreferenceManager.getClientPreferences().stampFilenames()) {
                filename = StringUtil.addDateTimeStamp(filename);
            }
            logfile = new File(LOG_DIR + File.separator + filename);
            writer = new BufferedWriter(new FileWriter(logfile));
            initialize();
        } catch (Exception ex) {
            logger.error("", ex);
            writer = null;
        }
    }

    protected void initialize() {
        append("Log file opened " + LocalDateTime.now());
    }

    public File getLogFile() {
        return logfile;
    }

    public void append(String toLog) {
        if (writer == null) {
            return;
        }

        try {
            writer.write("<pre>" + toLog + "</pre>");
            writer.newLine();
            writer.flush();
        } catch (Exception ex) {
            logger.error("", ex);
            writer = null;
        }
    }

    public void appendRaw(String toLog) {
        if (writer == null) {
            return;
        }
        try {
            writer.write(toLog);
            writer.flush();
        } catch (Exception ex) {
            logger.error("", ex);
            writer = null;
        }
    }

    public void close() throws Exception {
        if (writer != null) {
            writer.close();
        }
    }
}
