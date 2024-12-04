/*
 * MegaMek - Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.server.commands;

import megamek.client.ui.Messages;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.totalwarfare.TWGameManager;

import java.util.List;
import java.util.Map;

/**
 * @author Luana Coppio
 */
public class RemoveSmokeCommand extends GamemasterServerCommand {

    /** Creates new KillCommand */
    public RemoveSmokeCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "nosmoke", Messages.getString("Gamemaster.cmd.removesmoke.help"),
            Messages.getString("Gamemaster.cmd.removesmoke.longName"));
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        gameManager.getSmokeCloudList().forEach(gameManager::removeSmokeTerrain);
        server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.removesmoke.success"));
    }
}
