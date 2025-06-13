/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014 Nicholas Walczak (walczak@cs.umn.edu)
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

import megamek.common.Player;
import megamek.server.Server;

/**
 * This command allows a player to join a specified team.
 *
 * @author arlith
 * @deprecated Planned to be removed, use the GM command {@link ChangeTeamCommand} instead. References removed in
 *       0.50.06. Remove in 0.50.07
 */
@Deprecated(since = "0.50.06", forRemoval = true)
public class JoinTeamCommand extends ServerCommand {

    public static String SERVER_VOTE_PROMPT_MSG = "All players with an assigned team " +
                                                        "must allow this change.  Use /allowTeamChange " +
                                                        "to allow this change.";

    public JoinTeamCommand(Server server) {
        super(server,
              "joinTeam",
              "Planned to be removed, use the GM command /changeTeam instead. " +
                    "Switches a player's team at the end phase. " +
                    "Usage: /joinTeam # where the first number is the team " +
                    "number to join. 0 is for no team, " +
                    "-1 is for unassigned team. " +
                    "Only one player can be changed teams per round.");
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        try {
            Player player = server.getPlayer(connId);
            int numEntities = server.getGame().getEntitiesOwnedBy(player);

            if (args.length != 2) {
                server.sendServerChat(connId,
                      "Incorrect number of arguments " +
                            "for joinTeam command!  Expected 1, received, " +
                            (args.length - 1) +
                            ".");
                server.sendServerChat(connId, getHelp());
                return;
            }

            int teamId = Integer.parseInt(args[1]);

            if ((Player.TEAM_UNASSIGNED == teamId) && (numEntities != 0)) {
                server.sendServerChat(connId, "Player must have no more " + "units to join the unassigned team!");
                return;
            }
            String teamString = "join Team " + teamId + ".  ";
            if (teamId == Player.TEAM_UNASSIGNED) {
                teamString = " leave their team and go unassigned.  ";
            } else if (teamId == Player.TEAM_NONE) {
                teamString = " go lone wolf!  ";
            }

            for (Player p : server.getGame().getPlayersList()) {
                if (p.getId() != player.getId()) {
                    server.sendServerChat(p.getId(),
                          player.getName() + " wants to " + teamString + SERVER_VOTE_PROMPT_MSG);
                }
            }

            server.requestTeamChangeForPlayer(teamId, player);

            for (Player p : server.getGame().getPlayersList()) {
                p.setVotedToAllowTeamChange(false);
            }

            // requester automatically votes yes
            AllowTeamChangeCommand.voteYes(server, player);
        } catch (NumberFormatException nfe) {
            server.sendServerChat(connId, "Failed to parse team number \"" + args[1] + "\"!");
        }
    }

}
