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
 * Lists all the players connected and some info about them.
 * 
 * @author Ben
 * @version
 */
public class WhoCommand extends ServerCommand {

    /** Creates new WhoCommand */
    public WhoCommand(Server server) {
        super(server, "who",
                "Lists all of the players connected to the server.");
    }

    public void run(int connId, String[] args) {
        server.sendServerChat(connId, "Listing all connections...");
        server
                .sendServerChat(connId,
                        "[id#] : [name], [address], [pending], [bytes sent], [bytes received]");
        for (Enumeration<IConnection> i = server.getConnections(); i.hasMoreElements();) {
            IConnection conn = i.nextElement();
            StringBuffer cb = new StringBuffer();
            cb.append(conn.getId()).append(" : ");
            cb.append(server.getPlayer(conn.getId()).getName()).append(", ");
            cb.append(conn.getInetAddress());
            cb.append(", ").append(conn.hasPending()).append(", ");
            cb.append(conn.bytesSent());
            cb.append(", ").append(conn.bytesReceived());
            server.sendServerChat(connId, cb.toString());
        }
        server.sendServerChat(connId, "end list");
    }

}
