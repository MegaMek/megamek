/*
  Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import java.util.HashMap;
import java.util.Map;

import megamek.MMConstants;
import megamek.common.Player;
import megamek.server.Server;

/**
 * Loads a saved game on the server
 *
 * @author Taharqa
 * @since November 19, 2008
 */
public class LoadGameCommand extends ServerCommand {
    public LoadGameCommand(Server server) {
        super(server, "load",
              "load a saved game from the savegames directory.  Usage: /load [filename]");
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
            String sFinalFile = args[1];
            if (!sFinalFile.endsWith(MMConstants.SAVE_FILE_EXT)
                  && !sFinalFile.endsWith(MMConstants.SAVE_FILE_GZ_EXT)) {
                sFinalFile = sFinalFile + MMConstants.SAVE_FILE_EXT;
            }
            if (!sFinalFile.endsWith(MMConstants.GZ_FILE_EXT)) {
                sFinalFile = sFinalFile + MMConstants.GZ_FILE_EXT;
            }
            load(new File(MMConstants.SAVEGAME_DIR, sFinalFile), connId);
        } else {
            server.sendServerChat(connId, "you must provide a file name");
        }
    }

    private void load(File f, int connId) {
        server.sendServerChat(server.getPlayer(connId).getName() + " loaded a new game.");
        // Keep track of the current id to name mapping
        Map<String, Integer> nameToIdMap = new HashMap<>();
        Map<Integer, String> idToNameMap = new HashMap<>();
        for (Player p : server.getGame().getPlayersList()) {
            nameToIdMap.put(p.getName(), p.getId());
            idToNameMap.put(p.getId(), p.getName());
        }

        if (!server.loadGame(f, false)) {
            server.sendServerChat(f.getName() + " could not be loaded");
        } else {
            server.remapConnIds(nameToIdMap, idToNameMap);
            // update all the clients with the new game info
            server.forEachConnection(conn -> server.sendCurrentInfo(conn.getId()));
        }
    }
}
