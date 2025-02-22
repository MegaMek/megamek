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
import megamek.server.commands.arguments.IncDecSetIntegerArgument;

import java.util.List;

/**
 * Command to change the aggression level of the bot.
 * @author Luana Coppio
 */
public class AggressionCommand implements ChatCommand {

    private static final String AGGRESSION = "aggression";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new IncDecSetIntegerArgument(
                AGGRESSION,
                Messages.getString("Princess.command.aggression.aggression"),
                0,
                10)
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {

        IncDecSetIntegerArgument value = arguments.get(AGGRESSION, IncDecSetIntegerArgument.class);
        int currentIndex = princess.getBehaviorSettings().getHyperAggressionIndex();

        if (value.getOperation().equals(IncDecSetIntegerArgument.Operation.SET)) {
            princess.getBehaviorSettings().setHyperAggressionIndex(value.getValue());
        } else {
            princess.getBehaviorSettings().setHyperAggressionIndex(
                princess.getBehaviorSettings().getHyperAggressionIndex() + value.getValue());
        }

        princess.sendChat(Messages.getString("Princess.command.aggression.aggressionChanged", currentIndex, princess.getBehaviorSettings().getHyperAggressionIndex()));
    }
}
