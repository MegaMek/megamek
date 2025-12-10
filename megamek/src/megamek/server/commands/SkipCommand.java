/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Skips the current player's turn, if possible.
 *
 * @author Ben
 * @since February 19, 2003, 12:16 PM
 */
public class SkipCommand extends ServerCommand {

    private final TWGameManager gameManager;

    /** Creates a new instance of SkipCommand */
    public SkipCommand(Server server, TWGameManager gameManager) {
        super(server, "skip",
              "Skips the current turn, if possible.  Usage: /skip");
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {
        if (!canRunRestrictedCommand(connId)) {
            server.sendServerChat(connId,
                  "Observers are restricted from skipping turns.");
            return;
        }

        if (gameManager.isTurnSkippable()) {
            server.sendServerChat(server.getPlayer(connId).getName()
                  + " has issued the skip command...");
            gameManager.skipCurrentTurn();
        } else {
            server.sendServerChat("/skip : skip failed.");
        }
    }

}
