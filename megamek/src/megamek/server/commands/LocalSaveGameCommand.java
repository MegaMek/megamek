/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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


/*
 * SaveGameCommand.java
 *
 * Created on February 28, 2003, 4:55 PM
 */

package megamek.server.commands;

import java.io.File;

import megamek.MMConstants;
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
            String fileName = MMConstants.DEFAULT_SAVEGAME_NAME;
            String localPath = MMConstants.SAVEGAME_DIR + File.separator;
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
