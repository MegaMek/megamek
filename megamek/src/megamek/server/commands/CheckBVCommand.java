/*
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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

import megamek.common.Player;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

public class CheckBVCommand extends ServerCommand {

    public CheckBVCommand(Server server) {
        super(server, "checkbv", "Shows the remaining BV of each player in the game.");
    }

    @Override
    public void run(int connId, String[] args) {
        boolean suppressEnemyBV = server.getGame().getOptions()
              .booleanOption(OptionsConstants.ADVANCED_SUPPRESS_DB_BV)
              && server.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND);
        // Connection Ids match player Ids
        Player requestingPlayer = server.getGame().getPlayer(connId);

        server.sendServerChat(connId, "Remaining BV:");
        for (Player player : server.getGame().getPlayersList()) {
            StringBuilder cb = new StringBuilder();
            double percentage = 0;
            if (player.getInitialBV() != 0) {
                percentage = ((player.getBV() + 0.0) / player.getInitialBV()) * 100;
            }
            cb.append(player.getName()).append(": ");
            if (suppressEnemyBV && player.isEnemyOf(requestingPlayer)) {
                cb.append(" Enemy BV suppressed");
            } else {
                cb.append(player.getBV()).append("/").append(player.getInitialBV());
                cb.append(String.format(" (%1$3.2f%%)", percentage));
            }
            server.sendServerChat(connId, cb.toString());
        }
        server.sendServerChat(connId, "end list");
    }

}
