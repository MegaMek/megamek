/*

 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2014 Nicholas Walczak (walczak@cs.umn.edu)
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
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;

/**
 * This command allows a player to allow another player to switch teams.
 *
 * @author arlith
 */
public class AllowTeamChangeCommand extends ServerCommand {

    private final TWGameManager gameManager;

    public AllowTeamChangeCommand(Server server, TWGameManager gameManager) {
        super(server, "allowTeamChange", "Allows a player to switch their team "
              + "Usage: /allowTeamChange used in respond to another " +
              "Player's request to change teams.  All players assigned to" +
              " a team must allow the change.");
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        Player player = server.getPlayer(connId);

        if (!gameManager.isTeamChangeRequestInProgress()) {
            server.sendServerChat(connId, "No vote to change teams in progress!");
            return;
        }
        voteYes(server, player);
    }

    public static void voteYes(Server server, Player player) {
        player.setVotedToAllowTeamChange(true);

        // Tally votes
        boolean changeTeam = true;
        int voteCount = 0;
        int eligiblePlayerCount = 0;
        for (Player p : server.getGame().getPlayersList()) {
            if (p.getTeam() != Player.TEAM_UNASSIGNED) {
                changeTeam &= p.getVotedToAllowTeamChange();
                if (p.getVotedToAllowTeamChange()) {
                    voteCount++;
                }
                eligiblePlayerCount++;
            }
        }

        TWGameManager gameManager = (TWGameManager) server.getGameManager();

        // Inform all players about the vote
        server.sendServerChat(player.getName() + " has voted to allow "
              + gameManager.getPlayerRequestingTeamChange().getName()
              + " to join Team " + gameManager.getRequestedTeam()
              + ", " + voteCount
              + " vote(s) received out of " + eligiblePlayerCount
              + " vote(s) needed");

        // If all votes are received, perform team change
        if (changeTeam) {
            server.sendServerChat("All votes received, "
                  + gameManager.getPlayerRequestingTeamChange().getName()
                  + " will join Team " + gameManager.getRequestedTeam()
                  + " at the end of the turn.");
            gameManager.allowTeamChange();
        }
    }

}
