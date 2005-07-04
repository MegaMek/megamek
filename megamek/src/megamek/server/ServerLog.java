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
 * ServerLog.java
 *
 * Created on March 30, 2002, 2:40 PM
 */

package megamek.server;

import java.io.*;
import java.util.*;

import megamek.common.preference.PreferenceManager;

/**
 *
 * @author  Ben
 * @version 
 */
public class ServerLog {
    
    public static final String LOG_DIR = PreferenceManager.getClientPreferences().getLogDirectory();

    public static final String LOG_FILE = "serverlog.txt"; //$NON-NLS-1$
    
    private long maxFilesize = Long.MAX_VALUE;
    private File logfile;
    
    Writer writer;

    /** Appends to/Creates ServerLog named @filename */
    public ServerLog(String filename, boolean append, long maxSize) {
        try {
            File logDir = new File(LOG_DIR);
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            logfile = new File(LOG_DIR + File.separator + filename);
            maxFilesize = maxSize;
            writer = new BufferedWriter(new FileWriter(LOG_DIR + File.separator + filename, append));
            append("Log file opened " + new Date().toString()); //$NON-NLS-1$
        } catch (IOException ex) {
            writer = null;
            System.err.println("ServerLog:" + ex.getMessage());
        }
    }
    
    /** Creates new ServerLog */
    public ServerLog() {
        this(LOG_FILE,false,Long.MAX_VALUE);
    }

    public void append(String toLog) {
        if (writer == null || logfile.length() > maxFilesize) {
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
