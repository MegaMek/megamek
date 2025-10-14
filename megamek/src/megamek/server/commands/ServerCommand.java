/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.Player;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * @author Ben
 * @since March 30, 2002, 6:55 PM
 */
public abstract class ServerCommand {

    protected Server server;

    private final String name;
    private final String helpText;

    /** Creates new ServerCommand */
    public ServerCommand(Server server, String name, String helpText) {
        this.server = server;
        this.name = name;
        this.helpText = helpText;
    }

    /**
     * Return the string trigger for this command
     */
    public String getName() {
        return name;
    }

    /**
     * Returns some help text for this command
     */
    public String getHelp() {
        return helpText;
    }

    /**
     * Run this command with the arguments supplied
     */
    public abstract void run(int connId, String[] args);

    /**
     * Utility Function for "Restricted Commands." Restricted commands are not password-protected, they are restricted
     * to non-Observers. In the case where there are only Ghosts and/or Observers, the Observers can run restricted
     * commands.
     */
    public boolean canRunRestrictedCommand(int connId) {
        if (!server.getGame().getOptions().booleanOption(
              OptionsConstants.BASE_RESTRICT_GAME_COMMANDS)) {
            return true;
        }

        if (server.getPlayer(connId).isGhost()) {
            return false; // Just in case something funky happens
        }

        if (server.getPlayer(connId).isObserver()) {
            for (Player p : server.getGame().getPlayersList()) {
                if (!p.isObserver() && !p.isGhost()) {
                    // There are non-Observer, non-Ghosts in the game, so
                    // Observers are locked out.
                    return false;
                }
            }
        }

        return true;
    }

}
