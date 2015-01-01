/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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
 * DefeatCommand.java
 *
 * Created on September 2, 2003, 1:01 PM
 */

package megamek.server.commands;

import megamek.common.IPlayer;
import megamek.server.Server;

/**
 * Acknowledges another players victory command.
 * 
 * @author Ben
 * @version
 */
public class DefeatCommand extends ServerCommand {

    public static final String commandName = "defeat";
    public static final String helpText = "Acknowledges another players victory command.  Usage: /defeat";
    public static final String restrictedResponse = "Observers are restricted from declaring defeat.";
    public static final String admitsDefeat = " admits defeat.";
    public static final String wantsDefeat = " wants to admit defeat - type /victory to accept the surrender at the " +
                                             "end of the turn.";
    public static final String note = "note you need to type /defeat again after your opponent declares victory";

    /**
     * Creates new DefeatCommand
     */
    public DefeatCommand(Server server) {
        super(server, commandName, helpText);    }

    public static String getAdmitsDefeat(String playerName) {
        return playerName + admitsDefeat;
    }

    public static String getWantsDefeat(String playerName) {
        return playerName + wantsDefeat;
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {
        if (!canRunRestrictedCommand(connId)) {
            server.sendServerChat(connId, restrictedResponse);
            return;
        }

        IPlayer player = server.getPlayer(connId);
        if (server.getGame().isForceVictory()) {
            server.sendServerChat(getAdmitsDefeat(player.getName()));
            player.setAdmitsDefeat(true);
        } else {
            server.sendServerChat(getWantsDefeat(player.getName()));
            server.sendServerChat(connId, note);
        }
    }
}
