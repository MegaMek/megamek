/*
  Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.game;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;

import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;

/**
 * @author Ben
 * @since March 30, 2002, 2:40 PM Renamed from ServerLog to GameLog in July 2005
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
