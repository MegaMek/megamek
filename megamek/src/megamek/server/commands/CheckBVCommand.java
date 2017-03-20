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
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

public class CheckBVCommand extends ServerCommand {

    public CheckBVCommand(Server server) {
        super(server, "checkbv",
                "Shows the remaining BV of each player in the game.");
    }

    @Override
    public void run(int connId, String[] args) {
        boolean suppressEnemyBV = server.getGame().getOptions()
                .booleanOption(OptionsConstants.ADVANCED_SUPPRESS_DB_BV)
                && server.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND);
        // Connection Ids match player Ids
        IPlayer requestingPlayer = server.getGame().getPlayer(connId);
        
        server.sendServerChat(connId, "Remaining BV:");        
        for (Enumeration<IPlayer> i = server.getGame().getPlayers(); i
                .hasMoreElements();) {
            IPlayer player = i.nextElement();
            StringBuffer cb = new StringBuffer();
            double percentage = 0;
            if (player.getInitialBV() != 0) {
                percentage = ((player.getBV() + 0.0) / player.getInitialBV()) * 100;
            }
            cb.append(player.getName()).append(": ");
            if (suppressEnemyBV && player.isEnemyOf(requestingPlayer)) {
                cb.append(" Enemy BV suppressed");
            } else {
                cb.append(player.getBV()).append("/").append(player.getInitialBV());
                cb.append(String.format(" (%1$3.2f%%)",percentage));
            }
            server.sendServerChat(connId, cb.toString());
        }
        server.sendServerChat(connId, "end list");
    }

}
