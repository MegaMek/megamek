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
 * LocalLoadGameCommand.java
 *
 * Created on November 19, 2008
 */

package megamek.server.commands;

import megamek.server.Server;

/**
 * Saves the current game.
 *
 * @author Taharqa
 */
public class LocalLoadGameCommand extends ServerCommand {

    /** Creates a new instance of SaveGameCommand */
    public LocalLoadGameCommand(Server server) {
        super(server, "localload",
                "loads a game from the savegame directory of the client. ATTENTION: This will overwrite a savegame on the server of the same filename. Usage: /localload [filename]");
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {
        if (!canRunRestrictedCommand(connId)) {
            server.sendServerChat(connId,
                    "Observers are restricted from loading games.");
            return;
        }
        if (args.length > 1) {
            server.sendLoadGame(connId, args[1]);
        } else {
            server.sendServerChat(connId, "you must provide a file name");
        }
    }
}
