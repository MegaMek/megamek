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
public class AllowTeamChangeCommand extends ServerCommand {

    private final GameManager gameManager;

    public AllowTeamChangeCommand(Server server, GameManager gameManager) {
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
        try {
            Player player = server.getPlayer(connId);
            player.setAllowTeamChange(true);
            
            if (!gameManager.isTeamChangeRequestInProgress()) {
                server.sendServerChat(connId, "No vote to change teams in progress!");
                return;
            }
            
            // Tally votes
            boolean changeTeam = true;
            int voteCount = 0;
            int eligiblePlayerCount = 0;
            for (Player p : server.getGame().getPlayersVector()) {
                if (p.getTeam() != Player.TEAM_UNASSIGNED) {
                    changeTeam &= p.isAllowingTeamChange();
                    if (p.isAllowingTeamChange()) {
                        voteCount++;
                    }
                    eligiblePlayerCount++;
                }
                
            }
            
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
        } catch (NumberFormatException nfe) {
            server.sendServerChat(connId,"Failed to parse team number!");
        }
    }

}
