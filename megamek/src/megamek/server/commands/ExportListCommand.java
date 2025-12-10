/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server.commands;

import java.io.FileWriter;
import java.io.PrintWriter;

import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.server.Server;

/**
 * @author fastsammy
 * @since April 18, 2002, 11:53 PM
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
    @Override
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
            PrintWriter printWriter = new PrintWriter(new FileWriter(path));

            MekSummary[] mekSummaries = MekSummaryCache.getInstance().getAllMeks();

            for (MekSummary mekSummary : mekSummaries) {
                printWriter.println(mekSummary.getChassis() + ", " + mekSummary.getModel() + ", "
                      + mekSummary.getBV());
            }

            printWriter.flush();
            printWriter.close();
            server.sendServerChat(server.getPlayer(connId).getName() + " has exported a unit list.");
        } catch (Exception e) {
            server.sendServerChat(connId, "/exportlist: execution failed");
            server.sendServerChat(connId, e.toString());
        }
    }
}
