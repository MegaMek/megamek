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

import java.io.File;

import megamek.common.options.OptionsConstants;
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
    @Override
    public void run(int connId, String[] args) {
        if (server.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND) &&
                server.getGame().getOptions().booleanOption(OptionsConstants.BASE_DISABLE_LOCAL_SAVE)) {
            server
                    .sendServerChat("Local Save only outside double blind games.");
        } else {
            String fileName = "savegame.sav";
            String localPath = "savegames"+ File.separator;
            if (args.length > 1) {
                fileName = args[1];
            }
            if (args.length > 2) {
                localPath = args[2];
            }
            server.sendSaveGame(connId, fileName, localPath);
        }
    }
}
