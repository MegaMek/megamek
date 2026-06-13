/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
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

        if (saveGames != null) {
            for (File saveGame : saveGames) {
                if (saveGame.isFile()) {
                    if (saveGame.getName().endsWith(MMConstants.SAVE_FILE_EXT)
                          || saveGame.getName().endsWith(MMConstants.SAVE_FILE_GZ_EXT)) {
                        server.sendServerChat("  " + saveGame.getName());
                        listedAFile = true;
                    }
                }
            }
        }
        if (!listedAFile) {
            server.sendServerChat("No saved games!");
        }
    }
}
