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

import java.util.Enumeration;

import megamek.client.Client;
import megamek.client.ui.IClientCommandHandler;

/**
 * @author dirk
 */
public class HelpCommand extends ClientCommand {

    private IClientCommandHandler cmdHandler;

    /** Creates new HelpCommand */
    public HelpCommand(Client client) {
        super(
                client,
                "help",
                "Lists all of the commands available, or gives help on a specific command.  Usage: #help [command]");
        cmdHandler = client;
    }

    @Override
    public String run(String[] args) {
        if (args.length == 1) {
            // no args
            return "Type #help [command] for help on a specific command.  Commands available: "
                    + commandList();
        }
        // argument
        ClientCommand command = cmdHandler.getCommand(args[1]);
        if (command == null) {
            return "Command \"" + args[1]
                    + "\" not recognized.  Commands available: "
                    + commandList();
        }
        return "#" + command.getName() + " : " + command.getHelp();
    }

    private String commandList() {
        return String.join(", ", cmdHandler.getAllCommandNames());
    }
}
