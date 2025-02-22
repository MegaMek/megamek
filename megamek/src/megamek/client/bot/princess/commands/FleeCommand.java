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
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.EnumArgument;

import java.util.List;

/**
 * Command to set the bot to flee to a specific edge of the map.
 * @author Luana Coppio
 */
public class FleeCommand implements ChatCommand {
    private static final String EDGE = "edge";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new EnumArgument<>(EDGE, Messages.getString("Princess.command.flee.edge"), CardinalEdge.class)
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
        CardinalEdge edge = arguments.getEnum(EDGE, CardinalEdge.class);
        princess.sendChat("Received flee order - " + edge.name());
        princess.getBehaviorSettings().setDestinationEdge(edge);
        princess.setFallBack(true, "Received flee order - " + edge.name());
    }
}
