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
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.commands.arguments.UnitArgument;
import megamek.server.totalWarfare.EquipmentExplosionHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Sets off one piece of a unit's equipment as if a critical hit had just exploded it: an ammo bin, a
 * hyper-velocity autocannon, a RISC hyper laser, a charged capacitor, or anything else the critical-hit rules
 * would explode. The gamemaster says it explodes, so no to-hit roll and no Edge reroll stand in the way; the
 * damage, the pilot hits and any resulting destruction are resolved the same way as in play. The Edit Damage
 * dialog's Explode buttons send this command.
 */
public class ExplodeEquipmentCommand extends GamemasterServerCommand {

    public static final String UNIT_ID = "unitID";
    public static final String EQUIPMENT_NUMBER = "equipmentNumber";

    public ExplodeEquipmentCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "explode", Messages.getString("Gamemaster.cmd.explode.help"),
              Messages.getString("Gamemaster.cmd.explode.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(new UnitArgument(UNIT_ID, Messages.getString("Gamemaster.cmd.explode.unitID")),
              new IntegerArgument(EQUIPMENT_NUMBER, Messages.getString("Gamemaster.cmd.explode.equipmentNumber"),
                    0, Integer.MAX_VALUE));
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        int unitId = (int) args.get(UNIT_ID).getValue();
        Entity unit = gameManager.getGame().getEntity(unitId);
        if (unit == null) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.missingUnit"));
            return;
        }
        int equipmentNumber = (int) args.get(EQUIPMENT_NUMBER).getValue();
        Mounted<?> mounted = unit.getEquipment(equipmentNumber);
        if (mounted == null) {
            server.sendServerChat(connId,
                  Messages.getString("Gamemaster.cmd.explode.missingEquipment", equipmentNumber,
                        unit.getDisplayName()));
            return;
        }

        EquipmentExplosionHandler.Outcome outcome = gameManager.explodeEquipmentForGamemaster(unit, mounted);
        String message = switch (outcome) {
            case EXPLODED -> Messages.getString("Gamemaster.cmd.explode.success", mounted.getName(),
                  unit.getDisplayName());
            case ALREADY_DESTROYED -> Messages.getString("Gamemaster.cmd.explode.alreadyDestroyed",
                  mounted.getName(), unit.getDisplayName());
            case NOT_EXPLOSIVE -> Messages.getString("Gamemaster.cmd.explode.notExplosive", mounted.getName(),
                  unit.getDisplayName());
        };
        if (outcome == EquipmentExplosionHandler.Outcome.EXPLODED) {
            // everyone hears the bang; a refusal is the gamemaster's business alone
            server.sendServerChat(message);
        } else {
            server.sendServerChat(connId, message);
        }
    }
}
