/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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
package megamek.server.commands;

import java.util.Enumeration;

import megamek.common.Player;
import megamek.server.Server;

public class CheckBVCommand extends ServerCommand {

    public CheckBVCommand(Server server) {
        super(server, "checkbv",
                "Shows the remaining BV of each player in the game.");
    }

    @Override
    public void run(int connId, String[] args) {
        server.sendServerChat(connId, "Remaining BV:");
        for (Enumeration<Player> i = server.getGame().getPlayers(); i.hasMoreElements();) {
            Player player = i.nextElement();
            StringBuffer cb = new StringBuffer();
            cb.append(player.getName()).append(": ");
            cb.append(player.getBV()).append("/").append(player.getInitialBV());
            server.sendServerChat(connId, cb.toString());
        }
        server.sendServerChat(connId, "end list");
    }

}
