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
import megamek.client.bot.princess.ChatCommands;
import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;

import java.util.List;

/**
 * Command to list all available commands.
 */
public class ListCommands implements ChatCommand {
    @Override
    public void execute(Princess princess, Arguments arguments) {
        princess.sendChat(Messages.getString("Princess.chat.commands"));
        for (ChatCommands cmd : ChatCommands.values()) {
            princess.sendChat("# " + cmd.getSyntax() + " :: " + cmd.getDescription());
        }
    }
}
