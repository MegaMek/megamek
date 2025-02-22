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
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.StringArgument;

import java.util.List;

/**
 * Command to change the behavior of the bot.
 * @author Luana Coppio
 */
public class BehaviorCommand implements ChatCommand {
    private static final String BEHAVIOR = "behavior";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new StringArgument(BEHAVIOR, Messages.getString("Princess.command.behavior.behavior"))
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
       String behavior = arguments.getString(BEHAVIOR);

        BehaviorSettings newBehavior = BehaviorSettingsFactory.getInstance().getBehavior(behavior);

        if (newBehavior == null) {
            princess.sendChat(Messages.getString("Princess.command.behavior.unknownBehavior", behavior));
            return;
        }

        princess.setBehaviorSettings(newBehavior);
        princess.sendChat(Messages.getString("Princess.command.behavior.behaviorChanged", behavior));
    }
}
