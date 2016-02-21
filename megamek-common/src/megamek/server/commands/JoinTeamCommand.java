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
 * This command allows a player to join a specified team.
 * 
 * @author arlith
 */
public class JoinTeamCommand extends ServerCommand {

    public static String SERVER_VOTE_PROMPT_MSG = "All players with an assigned team "
            + "must allow this change.  Use /allowTeamChange "
            + "to allow this change.";
    
    public JoinTeamCommand(Server server) {
        super(server, "joinTeam", "Switches a player's team at the end phase. "
                + "Usage: /joinTeam # where the first number is the team "
                + "number to join.  0 is for no team, " +
                "-1 is for unassigned team");
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
            int numEntities = server.getGame().getEntitiesOwnedBy(player);
            
            if (args.length != 2) {
                server.sendServerChat(connId, "Incorrect number of arguments "
                        + "for joinTeam command!  Expected 1, received, "
                        + (args.length - 1) + ".");
                server.sendServerChat(connId, getHelp());
                return;
            }
            
            int teamId = Integer.parseInt(args[1]);
            
            if(IPlayer.TEAM_UNASSIGNED == teamId && numEntities != 0) {
                server.sendServerChat(connId, "Player must have no more " +
                        "units to join the unassigned team!");
                return;
            }
            String teamString = "join Team " + teamId + ".  ";
            if (teamId == IPlayer.TEAM_UNASSIGNED){
                teamString = " leave their team and go unassigned.  ";
            } else if (teamId == IPlayer.TEAM_NONE){
                teamString = " go lone wolf!  ";
            }
            for (IPlayer p : server.getGame().getPlayersVector()){
                if (p.getId() != player.getId()){
                    server.sendServerChat(p.getId(), player.getName()
                            + " wants to " + teamString
                            + SERVER_VOTE_PROMPT_MSG);
                }
            }
            
            server.requestTeamChange(teamId, player);
            
            for (IPlayer p : server.getGame().getPlayersVector()){
                p.setAllowTeamChange(false);
            }
            player.setAllowTeamChange(true);
        } catch (NumberFormatException nfe) {
            server.sendServerChat(connId,"Failed to parse team number!");
        }
    }

}
