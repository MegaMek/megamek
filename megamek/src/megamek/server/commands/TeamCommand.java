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

import megamek.server.Server;

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
