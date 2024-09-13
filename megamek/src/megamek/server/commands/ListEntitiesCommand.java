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
import megamek.common.Player;
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Jay Lawson (Taharqa)
 */
public class ListEntitiesCommand extends ServerCommand {

    private final TWGameManager gameManager;

    public ListEntitiesCommand(Server server, TWGameManager gameManager) {
        super(server, "listEntities",
                "Show the ids of all entities owned by this player. Usage: /listEntities");
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        Player p = server.getGame().getPlayer(connId);
        if (null == p) {
            return;
        }

        for (Entity ent : gameManager.getGame().getEntitiesVector()) {
            try {
                if (ent.getOwnerId() == connId) {
                    server.sendServerChat(connId, ent.getId() + " - " + ent.getDisplayName());
                }
            } catch (Exception ignored) {

            }
        }
    }
}
