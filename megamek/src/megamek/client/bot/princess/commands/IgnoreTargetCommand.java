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
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.BooleanArgument;
import megamek.server.commands.arguments.UnitArgument;

import java.util.List;

/**
 * Command to ignore a target unit for the bot.
 * @author Luana Coppio
 */
public class IgnoreTargetCommand implements ChatCommand {
    private static final String UNIT_ID = "unitID";
    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new UnitArgument(UNIT_ID, Messages.getString("Princess.command.ignoreTarget.unitID"))
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
        UnitArgument unitArg = arguments.get(UNIT_ID, UnitArgument.class);
        princess.getBehaviorSettings().addIgnoredUnitTarget(unitArg.getValue());
    }
}
