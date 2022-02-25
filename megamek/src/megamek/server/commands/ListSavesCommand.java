/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.server.commands;

import java.io.File;

import megamek.MMConstants;
import megamek.server.Server;

/**
 * Lists all the save games in the savegames directory
 * 
 * @author Taharqa
 * @since March 30, 2002, 7:35 PM
 */
public class ListSavesCommand extends ServerCommand {

    /** Creates new WhoCommand */
    public ListSavesCommand(Server server) {
        super(server, "listSaves",
                "List all saved games in the saved games directory.");
    }

    @Override
    public void run(int connId, String[] args) {
        File sDir = new File(MMConstants.SAVEGAME_DIR);
        if (!sDir.exists()) {
            server.sendServerChat("savegames directory not found.");
        }
        server.sendServerChat(connId, "Listing all saved games...");
        File[] saveGames = sDir.listFiles();
        boolean listedAFile = false;
        for (int i = 0; i < saveGames.length; i++) {
            if (saveGames[i].isFile()) {
                File save = saveGames[i];
                if (save.getName().endsWith(MMConstants.SAVE_FILE_EXT)
                        || save.getName().endsWith(MMConstants.SAVE_FILE_GZ_EXT)) {
                    server.sendServerChat("  " + save.getName());
                    listedAFile = true;
                }
            }
        }
        if (!listedAFile) {
            server.sendServerChat("No saved games!");
        }
    }
}
