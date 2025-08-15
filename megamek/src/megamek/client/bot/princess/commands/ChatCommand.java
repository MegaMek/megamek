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

import megamek.client.bot.Messages;
import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.BooleanArgument;

/**
 * Interface for chat commands for the bot.
 *
 * @author Luana Coppio
 */
public interface ChatCommand {
    String QUIET = "quiet";

    /**
     * Define the arguments for the command.
     *
     * @return List of arguments
     */
    default List<Argument<?>> defineArguments() {
        return List.of();
    }

    /**
     * Execute the command.
     *
     * @param princess  The bot
     * @param arguments The arguments for the command
     */
    void execute(Princess princess, Arguments arguments);

    /**
     * Get instance of a common argument to keep princess quiet while executing the commands.
     *
     * @return BooleanArgument to keep princess quiet
     */
    default BooleanArgument quietArgument() {
        return new BooleanArgument(QUIET, Messages.getString("Princess.command.quiet"), false);
    }

    /**
     * Get the arguments' representation.
     *
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
