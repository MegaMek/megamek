/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *         - Copyright (C) 2021 The MegaMek Team
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

import megamek.common.net.AbstractConnection;
import megamek.common.preference.PreferenceManager;
import megamek.server.Server;

import java.util.Enumeration;

/**
 * Lists all the players connected and some info about them.
 */
public class WhoCommand extends ServerCommand {

    /** Creates new WhoCommand */
    public WhoCommand(Server server) {
        super(server, "who",
                "Lists all of the players connected to the server.");
    }

    @Override
    public void run(int connId, String[] args) {
        server.sendServerChat(connId, "Listing all connections...");
        server.sendServerChat(
                connId, "[id#] : [name], [address], [pending], [bytes sent], [bytes received]");

        final boolean includeIPAddress = PreferenceManager.getClientPreferences().getShowIPAddressesInChat();
        for (Enumeration<AbstractConnection> i = server.getConnections(); i.hasMoreElements();) {
            AbstractConnection conn = i.nextElement();
            server.sendServerChat(connId, getConnectionDescription(conn, includeIPAddress));
        }

        server.sendServerChat(connId, "end list");
    }

    private String getConnectionDescription(AbstractConnection conn, boolean includeIPAddress) {
        StringBuilder cb = new StringBuilder();
        cb.append(conn.getId()).append(" : ");
        cb.append(server.getPlayer(conn.getId()).getName()).append(", ");
        cb.append(includeIPAddress ? conn.getInetAddress() : "<hidden>");
        cb.append(", ").append(conn.hasPending()).append(", ");
        cb.append(conn.bytesSent());
        cb.append(", ").append(conn.bytesReceived());
        return cb.toString();
    }
}
