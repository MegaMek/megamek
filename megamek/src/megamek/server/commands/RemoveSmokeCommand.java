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
import megamek.server.commands.arguments.Argument;
import megamek.server.totalwarfare.TWGameManager;

import java.util.List;
import java.util.Map;

/**
 * @author Luana Scoppio
 */
public class RemoveSmokeCommand extends GamemasterServerCommand {

    /** Creates new KillCommand */
    public RemoveSmokeCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "nosmoke", "GM removes all smoke cloud hexes. Usage: /nosmoke");
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of();
    }

    @Override
    protected void runAsGM(int connId, Map<String, Argument<?>> args) {
        gameManager.getSmokeCloudList().forEach(gameManager::removeSmokeTerrain);
        server.sendServerChat(connId, "GM cleared the smoke clouds.");
    }
}
