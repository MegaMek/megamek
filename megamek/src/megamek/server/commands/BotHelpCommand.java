/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.commands;

import megamek.client.bot.princess.ChatCommands;
import megamek.server.Server;

/**
 * The help command lists the other commands when run without arguments. When
 * run with another command name as an argument, it queries that command for its
 * help string and send that to the client.
 * 
 * @author Ben
 * @since March 30, 2002, 7:03 PM
 */
public class BotHelpCommand extends ServerCommand {

    /** Creates new HelpCommand */
    public BotHelpCommand(Server server) {
        super(
                server,
                "botHelp",
                "Lists all of the bot commands available, or gives help on a specific command.  Usage: /botHelp [command]");
    }

    @Override
    public void run(int connId, String[] args) {
        if (args.length == 1) {
            // no args
            server.sendServerChat(connId,
                    "Type /botHelp [command] for help on a specific bot command.  Commands available: "
                            + commandList());
        } else {
            // argument
            ChatCommands command = ChatCommands.getByValue(args[1]);
            if (command == null) {
                server.sendServerChat(connId, "Command \"" + args[1]
                        + "\" not recognized.  Commands available: "
                        + commandList());
            } else {
                server.sendServerChat(connId, command.getSyntax() + " [" + command.getDescription() + "]");
            }
        }
    }

    private String commandList() {
        StringBuilder commandList = new StringBuilder();

        for (ChatCommands cmdName : ChatCommands.values()) {
            if (commandList.length() > 0) {
                commandList.append(", ");
            }
            commandList.append(cmdName.getAbbreviation());
        }

        return commandList.toString();
    }
}
