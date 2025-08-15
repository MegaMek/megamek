/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
 * This command votes to allow another player to assume the elevated Game Master role
 *
 * @author pakfront
 */
public class AllowGameMasterCommand extends ServerCommand {

    private final TWGameManager gameManager;

    public AllowGameMasterCommand(Server server, TWGameManager gameManager) {
        super(server, "allowGM", "Allows a player become Game Master "
              + "Usage: /allowGM used in respond to another " +
              "Player's request to become Game Master.  All players assigned to" +
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

        if (!gameManager.isGameMasterRequestInProgress()) {
            server.sendServerChat(connId, "No vote to for Game Master is progess!");
            return;
        }
        voteYes(server, player);
    }

    protected static void voteYes(Server server, Player player) {
        player.setVotedToAllowGameMaster(true);

        // Tally votes
        boolean allowGameMaster = true;
        int voteCount = 0;
        int eligiblePlayerCount = 0;
        for (Player p : server.getGame().getPlayersList()) {
            if (p.getTeam() != Player.TEAM_UNASSIGNED) {
                allowGameMaster &= p.getVotedToAllowGameMaster();
                if (p.getVotedToAllowGameMaster()) {
                    voteCount++;
                }
                eligiblePlayerCount++;
            }
        }

        TWGameManager gameManager = (TWGameManager) server.getGameManager();

        // Inform all players about the vote
        server.sendServerChat(player.getName() + " has voted to allow "
              + gameManager.getPlayerRequestingGameMaster().getName()
              + " to become Game Master"
              + ", " + voteCount
              + " vote(s) received out of " + eligiblePlayerCount
              + " vote(s) needed");

        // If all votes are received, perform team change
        if (allowGameMaster) {
            server.sendServerChat("All votes received, "
                  + gameManager.getPlayerRequestingGameMaster().getName()
                  + " will become Game Master.");
            gameManager.processGameMasterRequest();
        }
    }

}
