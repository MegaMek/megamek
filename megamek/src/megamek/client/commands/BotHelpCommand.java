/*
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.commands;

import megamek.client.bot.princess.ChatCommands;
import megamek.client.ui.clientGUI.ClientGUI;

/**
 * @author dirk
 */
public class BotHelpCommand extends ClientCommand {

    public BotHelpCommand(ClientGUI clientGUI) {
        super(
              clientGUI,
              "botHelp",
              "Lists all of the bot commands available, or gives help on a specific command.  Usage: #botHelp [command]");
    }

    @Override
    public String run(String[] args) {
        if (args.length == 1) {
            // no args
            return "Type #botHelp [command] for help on a specific command.  Commands available: " + commandList();
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
            if (!commandList.isEmpty()) {
                commandList.append(", ");
            }
            commandList.append(cmdName.getAbbreviation());
        }

        return commandList.toString();
    }
}
