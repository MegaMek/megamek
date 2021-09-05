/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

/*
 * GameLog.java
 *
 * Created on March 30, 2002, 2:40 PM
 * Renamed from ServerLog to GameLog in July 2005
 */
package megamek.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

import megamek.MegaMek;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.GameReportEvent;
import megamek.common.util.StringUtil;


/**
 * @author Ben
 * @version
 */
public class GameLog {

    private BufferedWriter writer;
    private Map<Integer,String> entityImageCache;


    /**
     * Creates GameLog named
     *
     * @filename
     */
    public GameLog(Game game,
                   Map<Integer,String> entityImageCache,
                   String dir, String filename, boolean useTimestamp)
        throws IOException {
        this.entityImageCache = entityImageCache;

        var parent = new File(dir);
        if (!parent.exists()) {
            parent.mkdir();
        }
        if (useTimestamp) {
            filename = StringUtil.addDateTimeStamp(filename);
        }
        writer = new BufferedWriter(new FileWriter(new File(parent, filename)));
        append("<html><body>"); //$NON-NLS-1$
        append("Log file opened " + LocalDate.now().toString());

        game.addGameListener(new GameListenerAdapter() {
                @Override
                public void gamePlayerChat(GamePlayerChatEvent e) {
                    append(e.getMessage());
                }
                @Override
                public void gameReport(GameReportEvent e) {
                    append("<pre>");
                    for (var report: e.getReports()) {
                        append(report.getHtml(GameLog.this.entityImageCache));
                    }
                    append("</pre>");
                }
            }
        );
    }

    protected void append(String toLog) {
        try {
            writer.write(toLog);
            writer.flush();
        } catch (IOException ex) {
            MegaMek.getLogger().error("Error writing game log: " + ex.toString(), ex); //$NON-NLS-1$
        }
    }

    public void close() throws IOException {
        append("</body></html>"); //$NON-NLS-1$
        if (writer != null) {
            writer.close();
        }
    }

}
