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

import megamek.common.IPlayer;
import megamek.common.Team;
import megamek.server.Server;

public class CheckBVTeamCommand extends ServerCommand {

    public CheckBVTeamCommand(Server server) {
        super(server, "checkbvTeam",
                "Shows the remaining BV of each team in the game.");
    }

    @Override
    public void run(int connId, String[] args) {
        server.sendServerChat(connId, "Remaining BV:");
        Enumeration<Team> teamEnum = server.getGame().getTeams();
        while (teamEnum.hasMoreElements()) {
            Team team = teamEnum.nextElement();
            int initialTeamBV = 0;
            int currentTeamBV = 0;
            Enumeration<IPlayer> playersEnum = team.getPlayers();
            while (playersEnum.hasMoreElements()) {
                IPlayer player = playersEnum.nextElement();
                initialTeamBV += player.getInitialBV();
                currentTeamBV += player.getBV();
            }
            double percentage = 0;
            if (initialTeamBV != 0) {
                percentage = ((currentTeamBV + 0.0) / initialTeamBV) * 100;
            }
            StringBuffer cb = new StringBuffer();
            cb.append(team.toString()).append(": ");
            cb.append(currentTeamBV).append("/").append(initialTeamBV);
            cb.append(String.format(" (%1$3.2f%%)",percentage));
            server.sendServerChat(connId, cb.toString());
        }
        server.sendServerChat(connId, "end list");
    }

}
