/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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

import megamek.common.Player;
import megamek.common.Team;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

import java.util.Enumeration;

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
