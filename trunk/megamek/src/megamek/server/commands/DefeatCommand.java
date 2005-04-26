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

import megamek.server.*;
import megamek.common.Player;

/**
 * Acknowledges another players victory command.
 *
 * @author  Ben
 * @version 
 */
public class DefeatCommand extends ServerCommand {

    /** Creates new DefeatCommand */
    public DefeatCommand(Server server) {
        super(server, "defeat", "Acknowledges another players victory command.  Usage: /defeat");
    }

    /**
     * Run this command with the arguments supplied
     */
    public void run(int connId, String[] args) {
        Player player = server.getPlayer(connId);
        if (server.getGame().isForceVictory()) {
            server.sendServerChat(player.getName() + " admits defeat.");
            player.setAdmitsDefeat(true);
        } else {
            server.sendServerChat(connId, "Nobody has declared victory, you can't declare a defeat.");
        }
    }
}

