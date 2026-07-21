/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.List;

import megamek.client.ui.Messages;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.BooleanArgument;
import megamek.server.commands.arguments.UnitArgument;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Luana Coppio
 */
public class KillCommand extends GamemasterServerCommand {

    public static final String UNIT_ID = "unitID";
    public static final String EJECT = "eject";

    /** Creates new KillCommand */
    public KillCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "kill", Messages.getString("Gamemaster.cmd.kill.help"),
              Messages.getString("Gamemaster.cmd.kill.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(new UnitArgument(UNIT_ID, Messages.getString("Gamemaster.cmd.kill.unitID")),
              new BooleanArgument(EJECT, Messages.getString("Gamemaster.cmd.kill.eject"), false));
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    protected void runCommand(int connId, Arguments args) {
        int unitId = (int) args.get(UNIT_ID).getValue();
        // is the unit on the board?
        var unit = gameManager.getGame().getEntity(unitId);
        if (unit == null) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.missingUnit"));
            return;
        }
        boolean eject = (boolean) args.get(EJECT).getValue();
        if (eject) {
            // The crew must leave before the unit is destroyed: a crew that is already dead or doomed cannot
            // abandon it, and destroying a unit this way is not survivable.
            gameManager.addReport(gameManager.abandonEntity(unit));
        }
        gameManager.destroyEntity(unit, Messages.getString("Gamemaster.cmd.kill.reason"), false, false);
        server.sendServerChat(unit.getDisplayName() + Messages.getString("Gamemaster.cmd.kill.success"));
    }
}
