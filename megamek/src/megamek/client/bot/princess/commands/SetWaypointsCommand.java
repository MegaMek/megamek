/*
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
package megamek.client.bot.princess.commands;

import java.util.List;
import java.util.Optional;

import megamek.client.bot.Messages;
import megamek.client.bot.princess.Princess;
import megamek.common.units.Entity;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.BooleanArgument;
import megamek.server.commands.arguments.MultiHexNumberArgument;
import megamek.server.commands.arguments.UnitArgument;

/**
 * Command to set waypoints for a unit.
 *
 * @author Luana Coppio
 */
public class SetWaypointsCommand implements ChatCommand {
    private static final String UNIT_ID = "unitID";
    private static final String HEX_NUMBER = "hexNumber";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
              new UnitArgument(UNIT_ID, Messages.getString("Princess.command.setWaypoints.unitID")),
              new MultiHexNumberArgument(HEX_NUMBER, Messages.getString("Princess.command.setWaypoints.waypoints")),
              quietArgument()
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
        UnitArgument unitArgument = arguments.get(UNIT_ID, UnitArgument.class);
        MultiHexNumberArgument multiHexNumberArgument = arguments.get(HEX_NUMBER, MultiHexNumberArgument.class);
        BooleanArgument quietArgument = arguments.get(QUIET, BooleanArgument.class);

        Optional<Entity> unitOpt = princess.getEntitiesOwned().stream()
              .filter(entity -> entity.getId() == unitArgument.getValue())
              .findFirst();

        if (unitOpt.isEmpty()) {
            if (!quietArgument.getValue()) {
                princess.sendChat(Messages.getString("Princess.command.setWaypoints.unitNotFound",
                      unitArgument.getValue()));
            }
            return;
        }

        for (var coords : multiHexNumberArgument.getValue()) {
            if (!princess.getGame().getBoard().contains(coords) && !quietArgument.getValue()) {
                princess.sendChat(Messages.getString("Princess.command.setWaypoints.boardNoHex",
                      coords.toFriendlyString()));
                return;
            }
        }

        princess.getUnitBehaviorTracker()
              .setEntityWaypoints(unitOpt.get(), multiHexNumberArgument.getValue(), princess);

        if (!quietArgument.getValue()) {
            princess.sendChat(Messages.getString("Princess.command.setWaypoints.success",
                  unitOpt.get().getDisplayName()));
        }
    }
}
