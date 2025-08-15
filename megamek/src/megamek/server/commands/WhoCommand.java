/*
  Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.net.connections.AbstractConnection;
import megamek.common.preference.PreferenceManager;
import megamek.server.Server;

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
        server.forEachConnection(conn ->
              server.sendServerChat(connId, getConnectionDescription(conn, includeIPAddress)));

        server.sendServerChat(connId, "end list");
    }

    private String getConnectionDescription(AbstractConnection conn, boolean includeIPAddress) {
        StringBuilder cb = new StringBuilder();
        cb.append(conn.getId()).append(" : ");
        cb.append(server.getPlayer(conn.getId()).getName()).append(", ");
        cb.append(includeIPAddress ? conn.getInetAddress() : "<hidden>");
        cb.append(", ").append(conn.hasPending()).append(", ");
        cb.append(conn.getBytesSent());
        cb.append(", ").append(conn.getBytesReceived());
        return cb.toString();
    }
}
