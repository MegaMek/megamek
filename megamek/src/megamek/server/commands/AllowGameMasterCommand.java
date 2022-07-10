/*
 * MegaMek -
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2014 Nicholas Walczak (walczak@cs.umn.edu)
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
import megamek.server.GameManager;
import megamek.server.Server;

/**
 * This command allows a player to allow another player to switch teams.
 *
 * @author arlith
 */
public class AllowGameMasterCommand extends ServerCommand {

    private final GameManager gameManager;

    public AllowGameMasterCommand(Server server, GameManager gameManager) {
        super(server, "allowGM", "Allows a player become Game Master "
                + "Usage: /allowGameMaster used in respond to another " +
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
            player.setAllowGameMaster(true);

            if (!gameManager.isGameMasterRequestInProgress()) {
                server.sendServerChat(connId, "No vote to change teams in progress!");
                return;
            }

            // Tally votes
            boolean allowGameMaster = true;
            int voteCount = 0;
            int eligiblePlayerCount = 0;
            for (Player p : server.getGame().getPlayersVector()) {
                if (p.getTeam() != Player.TEAM_UNASSIGNED) {
                    allowGameMaster &= p.isAllowingGameMaster();
                    if (p.isAllowingGameMaster()) {
                        voteCount++;
                    }
                    eligiblePlayerCount++;
                }
            }

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
                        + " will become Game Master"
                        + " at the end of the turn.");
                gameManager.allowGameMaster();
            }

    }

}
