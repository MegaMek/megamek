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

import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;
import org.apache.logging.log4j.LogManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;

/**
 * @author Ben
 * @since March 30, 2002, 2:40 PM
 * Renamed from ServerLog to GameLog in July 2005
 */
public class GameLog {

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
            append("Log file opened " + LocalDate.now());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            writer = null;
        }
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
            LogManager.getLogger().error("", ex);
            writer = null;
        }
    }

    public void close() throws Exception {
        if (writer != null) {
            writer.close();
        }
    }
}
