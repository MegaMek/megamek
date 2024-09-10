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

import megamek.common.Entity;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;

/**
 * This command exists to print entity information to the chat window.
 * It is primarily intended for visually impaired users.
 *
 * @author dirk
 */
public class ShowEntityCommand extends ServerCommand {

    private final TWGameManager gameManager;

    public ShowEntityCommand(Server server, TWGameManager gameManager) {
        super(server, "entity",
                "Print the information about a entity into the chat window. Usage: /entity # which would show the details for the entity numbered #.");
        // to be extended by adding /entity unit# loc# to list details on locations.
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String... args) {
        if (server.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
            server.sendServerChat(connId, "Sorry, this command is disabled during double blind.");
            return;
        }
        try {
            int id = Integer.parseInt(args[1]);
            Entity ent = gameManager.getGame().getEntity(id);

            if (ent != null) {
                server.sendServerChat(connId, ent.statusToString());
            } else {
                server.sendServerChat(connId, "No such entity.");
            }
        } catch (Exception ignored) {

        }
    }
}
