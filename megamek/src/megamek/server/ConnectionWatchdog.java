/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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
package megamek.server;

import megamek.MegaMek;

import java.util.TimerTask;

public class ConnectionWatchdog extends TimerTask {
    private Server server;
    private int id;
    private int failCount;

    public ConnectionWatchdog(Server server, int id) {
        this.server = server;
        this.id = id;
        failCount = 0;
    }

    @Override
    public void run() {
        if (server.getPlayer(id) != null) {
            // fully connected
            cancel();
            return;
        }
        if (server.getPendingConnection(id) == null) {
            // dropped
            cancel();
            return;
        }

        MegaMek.getLogger().error("Bark Bark");
        if (failCount > 120) {
            server.getPendingConnection(id).close();
            cancel();
            MegaMek.getLogger().error("Growl\n\n\n\n\n");
            return;
        }

        server.clientVersionCheck(id);
        failCount++;
    }
}
