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

import java.io.*;
import java.util.*;

import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;

/**
 *
 * @author  Ben
 * @version 
 */
public class GameLog {
    
    public static final String LOG_DIR = PreferenceManager.getClientPreferences().getLogDirectory();

    public static final String LOG_FILE = "gamelog.txt"; //$NON-NLS-1$
    
    //private long maxFilesize = Long.MAX_VALUE;
    private File logfile;
    
    Writer writer;

    /** Creates GameLog named @filename */
    //public GameLog(String filename, boolean append, long maxSize) {
    public GameLog(String filename) {
        try {
            File logDir = new File(LOG_DIR);
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            logfile = new File(LOG_DIR + File.separator + filename);
            //maxFilesize = maxSize;
            if (PreferenceManager.getClientPreferences().stampFilenames()) {
                filename = StringUtil.addDateTimeStamp(filename);
            }
            //writer = new BufferedWriter(new FileWriter(LOG_DIR + File.separator + filename, append));
            writer = new BufferedWriter(new FileWriter(LOG_DIR + File.separator + filename));
            append("Log file opened " + new Date().toString()); //$NON-NLS-1$
        } catch (IOException ex) {
            writer = null;
            System.err.println("GameLog:" + ex.getMessage());
        }
    }
    
    /** Creates new GameLog */
    //public GameLog() {
    //this(LOG_FILE,false,Long.MAX_VALUE);
    //}

    public void append(String toLog) {
        //if (writer == null || logfile.length() > maxFilesize) {
        if (writer == null) {
            return;
        }
        try {
            writer.write(toLog);
            writer.write("\r\n"); //$NON-NLS-1$
            writer.flush();
        } catch (IOException ex) {
            //duhhhh...
            writer = null;
        }
    }
    
    public void close() throws java.io.IOException {
        writer.close();
    }
}
