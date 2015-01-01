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
 * RollCommand.java
 *
 * Created on April 18, 2002, 11:53 PM
 */

package megamek.server.commands;

import java.io.FileWriter;
import java.io.PrintWriter;

import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.server.Server;

/**
 * @author fastsammy
 * @version
 */
public class ExportListCommand extends ServerCommand {

    /** Creates new RollCommand */
    public ExportListCommand(Server server) {
        super(server, "exportlist",
                "Exports a unit list.  Usage: /exportlist <filename>");
    }

    /**
     * Run this command with the arguments supplied
     */
    public void run(int connId, String[] args) {
        String path = "exportlist.txt";
        try {
            if (args.length > 1) {
                path = args[1];
            }
        } catch (Exception ex) {
            server.sendServerChat(connId, "/exportlist: error parsing command");
            return;
        }
        exportList(connId, path);
    }

    private void exportList(int connId, String path) {
        try {
            PrintWriter pw1 = new PrintWriter(new FileWriter(path));

            MechSummary[] msums = MechSummaryCache.getInstance().getAllMechs();

            for (MechSummary ms1 : msums) {
                pw1.println(ms1.getChassis() + ", " + ms1.getModel() + ", "
                        + ms1.getBV());
            }

            pw1.flush();
            pw1.close();
            server.sendServerChat(server.getPlayer(connId).getName()
                    + " has exported a unit list.");
        } catch (Exception e) {
            server.sendServerChat(connId, "/exportlist: execution failed");
            server.sendServerChat(connId, e.toString());
        }
    }
}
