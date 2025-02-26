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
import megamek.common.Coords;
import megamek.common.Hex;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.totalwarfare.TWGameManager;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * The Server Command "/nofires" removes all fires on the board.
 *
 * @author Luana Coppio
 */
public class NoFiresCommand extends GamemasterServerCommand {

    private final String reason;

    public NoFiresCommand(Server server, TWGameManager gameManager) {
        super(server,
            gameManager,
            "nofires",
            Messages.getString("Gamemaster.cmd.nofire.help"),
            Messages.getString("Gamemaster.cmd.nofire.longName"));
        this.reason = Messages.getString("Gamemaster.cmd.firefight.reason");
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see ServerCommand#run(int, String[])
     */
    @Override
    protected void runCommand(int connId, Arguments args) {
        try {
            getAllCoords().forEach(this::firefight);
        } catch (Exception e) {
            logger.error(Messages.getString("Gamemaster.cmd.fire.failed"), e);
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.fire.failed"));
        }
    }

    private HashSet<Coords> getAllCoords() {
        var boardHeight = gameManager.getGame().getBoard().getHeight();
        var boardWidth = gameManager.getGame().getBoard().getWidth();
        var coordsSet = new HashSet<Coords>();
        for (int x = 0; x < boardWidth; x++) {
            for (int y = 0; y < boardHeight; y++) {
                coordsSet.add(new Coords(x, y));
            }
        }
        return coordsSet;
    }

    private void firefight(Coords coords) {
        Hex hex = gameManager.getGame().getBoard().getHex(coords);
        if (null == hex) {
            // Just ignore null hexes...
            // they should not happen, but I don't want to crash the command
            return;
        }
        gameManager.removeFire(coords, reason);
    }
}
