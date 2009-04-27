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
 * WhoCommand.java
 *
 * Created on March 30, 2002, 7:35 PM
 */

package megamek.server.commands;

import java.util.Enumeration;

import megamek.common.net.IConnection;
import megamek.server.Server;

/**
 * Team Chat
 * 
 * @author Torren
 * @version
 */
public class TeamCommand extends ServerCommand {

    /** Creates new WhoCommand */
    public TeamCommand(Server server) {
        super(server, "t",
                "Allows players on the same team to chat with each other in the game.");
    }

    public void run(int connId, String[] args) {

        if (args.length > 1) {

            int team = server.getPlayer(connId).getTeam();

            if (team < 1 || team > 8) {
                server.sendServerChat(connId, "You are not on a team!");
                return;
            }

            StringBuilder message = new StringBuilder();

            String origin = "Team Chat[" + server.getPlayer(connId).getName()
                    + "]";

            for (int pos = 1; pos < args.length; pos++) {
                message.append(" ");
                message.append(args[pos]);
            }

            for (Enumeration<IConnection> i = server.getConnections(); i.hasMoreElements();) {
                IConnection conn = i.nextElement();

                if (server.getPlayer(conn.getId()).getTeam() == team)
                    server.sendChat(conn.getId(), origin, message.toString());
            }
        }
    }

}
