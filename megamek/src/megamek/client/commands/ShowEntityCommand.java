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
package megamek.client.commands;

import megamek.client.ui.swing.ClientGUI;
import megamek.common.Entity;
import megamek.common.options.OptionsConstants;

/**
 * This command exists to print entity information to the chat
 * window, it's primarily intended for visually impaired users.
 * @author dirk
 */
public class ShowEntityCommand extends ClientCommand {

    public ShowEntityCommand(ClientGUI clientGUI) {
        super(clientGUI, "entity",
                "Print the information about a entity into the chat window. Usage: #entity 5 which would show the details for the entity numbered 5. Also #entity 5 0 would show location 0 of entity 5.");
        // to be extended by adding /entity unit# loc# to list details on locations.
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public String run(String[] args) {
        // is this necessary to prevent cheating?
        if (getClient().getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
            return "Sorry, this command is disabled during double blind.";
        }
        if (args.length == 1) {
            String list = "List of all entities.\n";
            for (Entity ent : getClient().getEntitiesVector()) {
                list += ent.getId() + " " + ent.getOwner().getName() + "'s "
                        + ent.getDisplayName() + "\n";
            }
            return list;
        }
        try {
            int id = Integer.parseInt(args[1]);
            Entity ent = getClient().getEntity(id);

            if (ent != null) {
                if (args.length > 2) {
                    String str = "";
                    for (int i = 2; i < args.length; i++) {
                        str += ent.statusToString(args[i]);
                    }
                    return str;
                }
                return ent.statusToString();
            } else {
                return "No such entity.";
            }
        } catch (Exception ignored) {

        }

        return "Error parsing the command.";
    }
}
