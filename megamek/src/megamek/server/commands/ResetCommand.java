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

import megamek.server.Server;

/**
 * Resets the server
 *
 * @author Ben
 * @since March 30, 2002, 7:48 PM
 */
public class ResetCommand extends ServerCommand {

    /** Creates new ResetCommand */
    public ResetCommand(Server server) {
        super(server, "reset",
              "Resets the server back to the lobby.  Usage: /reset <password>");
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {
        if (!canRunRestrictedCommand(connId)) {
            server.sendServerChat(connId,
                  "Observers are restricted from resetting.");
            return;
        }

        if (!server.isPassworded()
              || (args.length > 1 && server.isPassword(args[1]))) {
            reset(connId);
        } else {
            server.sendServerChat(connId,
                  "The password is incorrect.  Usage: /reset <password>");
        }
    }

    private void reset(int connId) {
        server.sendServerChat(server.getPlayer(connId).getName()
              + " reset the server.");
        server.resetGame();
    }
}
