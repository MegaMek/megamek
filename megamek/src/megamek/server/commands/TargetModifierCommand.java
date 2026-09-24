/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
import megamek.common.compute.Compute;
import megamek.common.units.Entity;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.commands.arguments.UnitArgument;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Gives a unit a gamemaster's change to its target movement modifier for the rest of the round: added to the
 * modifier the unit earns by moving, then held to the range the movement table allows, so attackers never see
 * less than no modifier or more than the table's top. Zero clears it. The Edit Damage dialog's Target Modifier
 * spinner sets the same value.
 */
public class TargetModifierCommand extends GamemasterServerCommand {

    public static final String UNIT_ID = "unitID";
    public static final String DELTA = "delta";

    public TargetModifierCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "targetMod", Messages.getString("Gamemaster.cmd.targetMod.help"),
              Messages.getString("Gamemaster.cmd.targetMod.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(new UnitArgument(UNIT_ID, Messages.getString("Gamemaster.cmd.targetMod.unitID")),
              new IntegerArgument(DELTA, Messages.getString("Gamemaster.cmd.targetMod.delta"),
                    -Compute.MAX_GAMEMASTER_TARGET_MODIFIER, Compute.MAX_GAMEMASTER_TARGET_MODIFIER, 0));
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        int unitId = (int) args.get(UNIT_ID).getValue();
        int delta = (int) args.get(DELTA).getValue();
        Entity entity = gameManager.getGame().getEntity(unitId);
        if (entity == null) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.missingUnit"));
            return;
        }
        entity.setGamemasterTargetModifier(delta);
        if (delta == 0) {
            server.sendServerChat(Messages.getString("Gamemaster.cmd.targetMod.cleared", entity.getDisplayName()));
        } else {
            server.sendServerChat(Messages.getString("Gamemaster.cmd.targetMod.success", entity.getDisplayName(),
                  (delta > 0) ? "+" + delta : String.valueOf(delta)));
        }
        gameManager.entityUpdate(entity.getId());
    }
}
