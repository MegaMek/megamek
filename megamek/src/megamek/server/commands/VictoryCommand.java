/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

/*
 * VictoryCommand.java
 *
 * Created on July 11, 2002, 2:24 PM
 */

package megamek.server.commands;

import megamek.common.IPlayer;
import megamek.server.Server;

/**
 * Causes automatic victory at the end of the current turn.
 * 
 * @author Ben
 * @version
 */
public class VictoryCommand extends ServerCommand {

    public static final String commandName = "victory";
    public static final String helpText = "Causes automatic victory for the issuing player or his/her team at the " +
                                          "end of this turn. Must be acknowledged by all opponents using the " +
                                          "/defeat command. Usage: /victory <password>";
    public static final String restrictedUse = "Observers are restricted from declaring victory.";
    public static final String badPassword = "The password is incorrect.  Usage: /victory <password>";
    private static final String declareIndividual = " declares individual victory at the end of the turn. This must " +
                                                    "be acknowledged by all opponents using the /defeat command or " +
                                                    "no victory will occur.";
    private static final String declareTeam = " declares team victory at the end of the turn. This must be " +
                                              "acknowledged by all opponents using the /defeat command or no " +
                                              "victory will occur.";

    /**
     * Creates new VictoryCommand
     */
    public VictoryCommand(Server server) {
        super(server, commandName, helpText);
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {
        if (!canRunRestrictedCommand(connId)) {
            server.sendServerChat(connId, restrictedUse);
            return;
        }

        if (!server.isPassworded()
                || (args.length > 1 && server.isPassword(args[1]))) {
            reset(connId);
        } else {
            server.sendServerChat(connId, badPassword);        }
    }

    public static String getDeclareIndividual(String playerName) {
        return playerName + declareIndividual;
    }

    public static String getDeclareTeam(String playerName) {
        return playerName + declareTeam;
    }

    private void reset(int connId) {
        IPlayer player = server.getPlayer(connId);
        /*
         * // are we cancelling victory? if (server.getGame().isForceVictory()) {
         * server.sendServerChat(player.getName() + " cancels the force
         * victory."); server.cancelVictory(); return; }
         */// okay, declare force victory
        if (player.getTeam() == player.TEAM_NONE) {
            server.sendServerChat(getDeclareIndividual(player.getName()));
        } else {
            server.sendServerChat(getDeclareTeam(player.getName()));
        }
        server.forceVictory(player);
    }

}
