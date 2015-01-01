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
 * ResetCommand.java
 *
 * Created on March 30, 2002, 7:48 PM
 */

package megamek.server.commands;

import megamek.server.Server;

/**
 * Resets the server
 * 
 * @author Ben
 * @version
 */
public class ResetCommand extends ServerCommand {

    /** Creates new ResetCommand */
    public ResetCommand(Server server) {
        super(server, "reset",
                "Resets the server back to the lobby.  Usage: /reset <password>");
    }

    /**
     * Run this command with the arguments supplied
     */
    public void run(int connId, String[] args) {
        if (!canRunRestrictedCommand(connId)) {
            server.sendServerChat(connId,
                    "Observers are restricted from resetting.");
            return;
        }

        if (!server.isPassworded()
                || (args.length > 1 && server.isPassword(args[1]))) {
            reset(connId);
        } else {
            server.sendServerChat(connId,
                    "The password is incorrect.  Usage: /reset <password>");
        }
    }

    private void reset(int connId) {
        server.sendServerChat(server.getPlayer(connId).getName()
                + " reset the server.");
        server.resetGame();
    }

}
