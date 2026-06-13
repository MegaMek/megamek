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
        super(server,
              "localload",
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
