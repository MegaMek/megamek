/*
 * Copyright (C) 2024 Luana Scoppio (luana.coppio@gmail.com)
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

import megamek.common.planetaryconditions.Fog;
import megamek.common.planetaryconditions.Wind;
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Luana Scoppio
 */
public class RemoveSmokeCommand extends ServerCommand {

    private final TWGameManager gameManager;

    /** Creates new KillCommand */
    public RemoveSmokeCommand(Server server, TWGameManager gameManager) {
        super(server, "nosmoke", "GM removes all smoke cloud hexes. Usage: /nosmoke");
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {
        if (!server.getPlayer(connId).getGameMaster()) {
            server.sendServerChat(connId, "You are not a Game Master.");
            return;
        }

        // Check argument integrity.
        if (args.length == 1) {
            // Check command
            gameManager.getSmokeCloudList().forEach(gameManager::removeSmokeTerrain);
            server.sendServerChat(connId, "GM cleared the smoke clouds.");
        } else {
            // Error out; it's not a valid call.
            server.sendServerChat(connId, "nosmoke command failed (1).");
        }
    }
}
