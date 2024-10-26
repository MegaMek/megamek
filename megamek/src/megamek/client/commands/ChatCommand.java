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

/**
 * A command that exist purely for the accessibility UI so that commands can be made as the default rather than chat.
 */
public class ChatCommand extends ClientCommand {

    public ChatCommand(ClientGUI clientGUI) {
        super(clientGUI, "say", "say <message>, sends message to chat.");
    }

    @Override
    public String run(String[] args) {
        //rejoin the string but cut off the say at the beginning.
        String str = String.join(" ", args).substring(4);
        getClient().sendChat(str);

        return str;
    }
}
