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

/**
 *
 * @author  Ben
 * @version 
 */
public class ServerLog {
    
    public static final String LOG_FILE = "serverlog.txt";
    
    Writer writer;

    /** Appends to/Creates ServerLog named @filename */
    public ServerLog(String filename, boolean append) {
        try {
            writer = new BufferedWriter(new FileWriter(filename, append));
            append("Log file opened " + new Date().toString());
        } catch (IOException ex) {
            //TODO: I dunno.  report this... to the log? ;)
            writer = null;
        }
    }
    
    /** Creates new ServerLog */
    public ServerLog() {
        this(LOG_FILE,false);
    }

    public void append(String toLog) {
        if (writer == null) {
            return;
        }
        try {
            writer.write(toLog);
            writer.write("\n");
            writer.flush();
        } catch (IOException ex) {
            //duhhhh...
            writer = null;
        }
    }
}
