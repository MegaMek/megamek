/*

 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.Team;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

public class CheckBVTeamCommand extends ServerCommand {

    public CheckBVTeamCommand(Server server) {
        super(server, "checkbvTeam",
              "Shows the remaining BV of each team in the game.");
    }

    @Override
    public void run(int connId, String[] args) {
        boolean suppressEnemyBV = server.getGame().getOptions()
              .booleanOption(OptionsConstants.ADVANCED_SUPPRESS_DB_BV)
              && server.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND);
        // Connection Ids match player Ids
        Player requestingPlayer = server.getGame().getPlayer(connId);

        server.sendServerChat(connId, "Remaining BV:");
        for (Team team : server.getGame().getTeams()) {
            int initialTeamBV = 0;
            int currentTeamBV = 0;
            boolean enemyTeam = false;
            for (Player player : team.players()) {
                initialTeamBV += player.getInitialBV();
                currentTeamBV += player.getBV();
                if (player.isEnemyOf(requestingPlayer)) {
                    enemyTeam = true;
                }
            }
            double percentage = 0;
            if (initialTeamBV != 0) {
                percentage = ((currentTeamBV + 0.0) / initialTeamBV) * 100;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(team).append(": ");
            if (suppressEnemyBV && enemyTeam) {
                sb.append(" Enemy BV suppressed");
            } else {
                sb.append(currentTeamBV).append("/").append(initialTeamBV);
                sb.append(String.format(" (%1$3.2f%%)", percentage));
            }
            server.sendServerChat(connId, sb.toString());
        }
        server.sendServerChat(connId, "end list");
    }

}
