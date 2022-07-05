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
package megamek.server.commands;

import megamek.common.net.connections.AbstractConnection;
import megamek.server.Server;

import java.util.Enumeration;

/**
 * Team Chat
 * 
 * @author Torren
 */
public class TeamCommand extends ServerCommand {
    public TeamCommand(Server server) {
        super(server, "t",
                "Allows players on the same team to chat with each other in the game.");
    }

    @Override
    public void run(int connId, String[] args) {
        if (args.length > 1) {
            int team = server.getPlayer(connId).getTeam();
            if ((team < 1) || (team > 8)) {
                server.sendServerChat(connId, "You are not on a team!");
                return;
            }

            StringBuilder message = new StringBuilder();
            String origin = "Team Chat[" + server.getPlayer(connId).getName() + "]";

            for (int pos = 1; pos < args.length; pos++) {
                message.append(" ");
                message.append(args[pos]);
            }

            server.forEachConnection(conn -> {
                if (server.getPlayer(conn.getId()).getTeam() == team) {
                    server.sendChat(conn.getId(), origin, message.toString());
                }
            });
        }
    }
}
