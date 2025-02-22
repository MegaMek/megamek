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
import megamek.server.commands.arguments.HexNumberArgument;

import java.util.List;

/**
 * Command to target a ground hex for strategic building.
 * @author Luana Coppio
 */
public class TargetGroundCommand implements ChatCommand {
    private static final String HEX = "hexNumber";
    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new HexNumberArgument(HEX, Messages.getString("Princess.command.targetGround.hex")),
            quietArgument()
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
        HexNumberArgument hexArg = arguments.get(HEX, HexNumberArgument.class);
        BooleanArgument quietArg = arguments.get(quietArgument().getName(), BooleanArgument.class);
        if (!princess.getGame().getBoard().contains(hexArg.getValue())) {
            if (!quietArg.getValue()) {
                princess.sendChat(Messages.getString("Princess.command.targetGround.hexNotFound", hexArg.getValue().toFriendlyString()));
            }
            return;
        }

        princess.addStrategicBuildingTarget(hexArg.getValue());
        if (!quietArg.getValue()) {
            princess.sendChat(Messages.getString("Princess.command.targetGround.success", hexArg.getValue().toFriendlyString()));
        }
    }
}
