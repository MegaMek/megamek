/*
  Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

import megamek.common.Player;
import megamek.server.Server;

/**
 * Acknowledges another players victory command.
 *
 * @author Ben
 * @since September 2, 2003, 1:01 PM
 */
public class DefeatCommand extends ServerCommand {

    public static final String commandName = "defeat";
    public static final String helpText = "Acknowledges another players victory command.  Usage: /defeat";
    public static final String restrictedResponse = "Observers are restricted from declaring defeat.";
    public static final String admitsDefeat = " admits defeat.";
    public static final String wantsDefeat = " wants to admit defeat - type /victory to accept the surrender at the " +
          "end of the turn.";
    public static final String note = "note you need to type /defeat again after your opponent declares victory";

    /**
     * Creates new DefeatCommand
     */
    public DefeatCommand(Server server) {
        super(server, commandName, helpText);
    }

    public static String getAdmitsDefeat(String playerName) {
        return playerName + admitsDefeat;
    }

    public static String getWantsDefeat(String playerName) {
        return playerName + wantsDefeat;
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {
        if (!canRunRestrictedCommand(connId)) {
            server.sendServerChat(connId, restrictedResponse);
            return;
        }

        Player player = server.getPlayer(connId);
        if (server.getGame().isForceVictory()) {
            server.sendServerChat(getAdmitsDefeat(player.getName()));
            player.setAdmitsDefeat(true);
        } else {
            server.sendServerChat(getWantsDefeat(player.getName()));
            server.sendServerChat(connId, note);
        }
    }
}
