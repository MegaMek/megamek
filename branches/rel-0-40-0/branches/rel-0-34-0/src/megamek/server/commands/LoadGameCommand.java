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
 * LoadGameCommand.java
 *
 * Created on November 19, 2008
 */

package megamek.server.commands;

import java.io.File;

import megamek.server.Server;

/**
 * Resets the server
 * 
 * @author Taharqa
 * @version
 */
public class LoadGameCommand extends ServerCommand {

    /** Creates new ResetCommand */
    public LoadGameCommand(Server server) {
        super(server, "load",
                "load a saved game from the savegames directory.  Usage: /load [filename]");
    }

    /**
     * Run this command with the arguments supplied
     */
    public void run(int connId, String[] args) {
        if (!canRunRestrictedCommand(connId)) {
            server.sendServerChat(connId,
                    "Observers are restricted from loading games.");
            return;
        }
        if (args.length > 1) {
            load(new File("savegames", args[1]), connId);
        } else {
            server.sendServerChat(connId, "you must provide a file name");
        }
    }

    private void load(File f, int connId) {
        server.sendServerChat(server.getPlayer(connId).getName()
                + " loaded a new game.");
        if(server.loadGame(f)) {
            //reset the connections or the current clients will not get loaded to their own game
            server.resetConnections();
        } else {
            server.sendServerChat(f.getName() + " could not be loaded");
        }
    }

}
