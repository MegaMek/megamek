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

import java.util.List;

/**
 * Interface for chat commands for the bot.
 * @author Luana Coppio
 */
public interface ChatCommand {
    String QUIET = "quiet";

    /**
     * Define the arguments for the command.
     * @return List of arguments
     */
    default List<Argument<?>> defineArguments() {
        return List.of();
    }

    /**
     * Execute the command.
     * @param princess The bot
     * @param arguments The arguments for the command
     */
    void execute(Princess princess, Arguments arguments);

    /**
     * Get instance of a common argument to keep princess quiet while executing the commands.
     * @return BooleanArgument to keep princess quiet
     */
    default BooleanArgument quietArgument() {
        return new BooleanArgument(QUIET, Messages.getString("Princess.command.quiet"), false);
    }

    /**
     * Get the arguments' representation.
     * @return String with the arguments representation
     */
    default String getArgumentsRepr() {
        StringBuilder help = new StringBuilder();
        for (Argument<?> arg : defineArguments()) {
            help.append(arg.getRepr());
        }
        return help.toString();
    }

    default String getArgumentsDescription() {
        StringBuilder help = new StringBuilder();

        for (var arg : defineArguments()) {
            help.append(arg.getName())
                .append(": ")
                .append(arg.getHelp())
                .append("    ");
        }
        return help.toString();
    }

}
