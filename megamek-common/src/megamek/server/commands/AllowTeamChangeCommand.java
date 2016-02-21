/*
 * MegaMek -
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2014 Nicholas Walczak (walczak@cs.umn.edu)
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

import megamek.common.IPlayer;
import megamek.server.Server;

/**
 * This command allows a player to allow another player to switch teams.
 * 
 * @author arlith
 */
public class AllowTeamChangeCommand extends ServerCommand {

    public AllowTeamChangeCommand(Server server) {
        super(server, "allowTeamChange", "Allows a player to switch their team "
                + "Usage: /allowTeamChange used in responsed to another " +
                "Player's request to change teams.  All players assigned to" +
                " a team must allow the change.");
    }

    /**
     * Run this command with the arguments supplied
     * 
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        try {
            IPlayer player = server.getPlayer(connId);
            player.setAllowTeamChange(true);
            
            if (!server.isTeamChangeRequestInProgress()){
                server.sendServerChat(connId, "No vote to change " +
                        "teams in progress!");
                return;
            }
            
            // Tally votes
            boolean changeTeam = true;
            int voteCount = 0;
            int eligiblePlayerCount = 0;
            for (IPlayer p : server.getGame().getPlayersVector()){
                if (p.getTeam() != IPlayer.TEAM_UNASSIGNED){
                    changeTeam &= p.isAllowingTeamChange();
                    if (p.isAllowingTeamChange()){
                        voteCount++;
                    }
                    eligiblePlayerCount++;
                }
                
            }
            
            // Inform all players about the vote
            server.sendServerChat(player.getName() + " has voted to allow " 
                    + server.getPlayerRequestingTeamChange().getName()
                    + " to join Team " + server.getRequestedTeam()
                    + ", " + voteCount
                    + " vote(s) received out of " + eligiblePlayerCount
                    + " vote(s) needed");
            
            // If all votes are received, perform team change
            if (changeTeam){
                server.sendServerChat("All votes received, "
                        + server.getPlayerRequestingTeamChange().getName()
                        + " will join Team " + server.getRequestedTeam()
                        + " at the end of the turn.");
                server.allowTeamChange();                
            }
        } catch (NumberFormatException nfe) {
            server.sendServerChat(connId,"Failed to parse team number!");
        }
    }

}
