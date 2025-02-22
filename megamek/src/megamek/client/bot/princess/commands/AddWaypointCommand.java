/*
 * MegaMek - Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.bot.princess.commands;

import megamek.client.bot.Messages;
import megamek.client.bot.princess.Princess;
import megamek.common.Entity;
import megamek.server.commands.arguments.*;

import java.util.List;
import java.util.Optional;

/**
 * Add a waypoint to a unit
 * @author Luana Coppio
 */
public class AddWaypointCommand implements ChatCommand {
    private static final String UNIT_ID = "unitID";
    private static final String HEX_NUMBER = "hexNumber";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new UnitArgument(UNIT_ID, Messages.getString("Princess.command.addWaypoint.unitID")),
            new MultiHexNumberArgument(HEX_NUMBER, Messages.getString("Princess.command.addWaypoint.hexNumber")),
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
                princess.sendChat(Messages.getString("Princess.command.addWaypoint.unitNotFound", unitArgument.getValue()));
            }
            return;
        }

        for (var coords : multiHexNumberArgument.getValue()) {
            if (!princess.getGame().getBoard().contains(coords) && !quietArgument.getValue()) {
                princess.sendChat(Messages.getString("Princess.command.addWaypoint.boardNoHex", coords.toFriendlyString()));
                return;
            }
        }

        princess.getUnitBehaviorTracker().addEntityWaypoint(unitOpt.get(), multiHexNumberArgument.getValue(), princess);
        if (!quietArgument.getValue()) {
            princess.sendChat(Messages.getString("Princess.command.addWaypoint.success", unitOpt.get().getDisplayName()));
        }
    }
}
