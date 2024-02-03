/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.commands;

import megamek.client.Client;
import megamek.client.bot.princess.ChatCommands;
import megamek.client.ui.IClientCommandHandler;

/**
 * @author dirk
 */
public class BotHelpCommand extends ClientCommand {

    private IClientCommandHandler cmdHandler;

    /** Creates new HelpCommand */
    public BotHelpCommand(Client client) {
        super(
                client,
                "botHelp",
                "Lists all of the bot commands available, or gives help on a specific command.  Usage: #botHelp [command]");
        cmdHandler = client;
    }

    @Override
    public String run(String[] args) {
        if (args.length == 1) {
            // no args
            return "Type #botHelp [command] for help on a specific command.  Commands available: "
                    + commandList();
        }
        // argument
        ChatCommands command = ChatCommands.getByValue(args[1]);
        if (command == null) {
            return "Command \"" + args[1]
                    + "\" not recognized.  Commands available: "
                    + commandList();
        }
        return "#" + command.getSyntax() + " [" + command.getDescription() + "]";
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
