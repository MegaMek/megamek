/*
 * MegaMek - Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2018-2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.commands;

import megamek.client.ui.IClientCommandHandler;
import megamek.client.ui.swing.ClientGUI;

/**
 * @author dirk
 */
public class HelpCommand extends ClientCommand {

    private IClientCommandHandler cmdHandler;

    public HelpCommand(ClientGUI clientGUI) {
        super(
                clientGUI,
                "help",
                "Lists all of the commands available, or gives help on a specific command.  Usage: #help [command]");
        cmdHandler = clientGUI;
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
