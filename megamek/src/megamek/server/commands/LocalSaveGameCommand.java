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
 * SaveGameCommand.java
 *
 * Created on February 28, 2003, 4:55 PM
 */

package megamek.server.commands;

import megamek.server.Server;

/**
 * Saves the current game.
 * 
 * @author Ben
 */
public class LocalSaveGameCommand extends ServerCommand {

    /** Creates a new instance of SaveGameCommand */
    public LocalSaveGameCommand(Server server) {
        super(server, "localsave",
                "Saves the game to a file on the client.  Usage: /localsave [filename]");
    }

    /**
     * Run this command with the arguments supplied
     */
    public void run(int connId, String[] args) {
        if (server.getGame().getOptions().booleanOption("double_blind") &&
                server.getGame().getOptions().booleanOption("disable_local_save")) {
            server
                    .sendServerChat("Local Save only outside double blind games.");
        } else {
            String fileName = "savegame.sav";
            if (args.length > 1) {
                fileName = args[1];
            }
            server.sendSaveGame(connId, fileName);
        }
    }
}
