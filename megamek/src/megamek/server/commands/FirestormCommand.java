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

/**
 * The Server Command "/firestorm" that starts a blazing inferno on the board.
 *
 * @author Luana Coppio
 */
public class FirestormCommand extends GamemasterServerCommand {

    private static final String FIRESTORM = "firestorm";
    private static final String TYPE = "type";
    private static final String PERCENT = "percent";

    public FirestormCommand(Server server, TWGameManager gameManager) {
        super(server,
            gameManager,
            FIRESTORM,
            Messages.getString("Gamemaster.cmd.firestorm.help"),
            Messages.getString("Gamemaster.cmd.firestorm.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new IntegerArgument(TYPE, Messages.getString("Gamemaster.cmd.fire.type"), 1, 4, 1),
            new IntegerArgument(PERCENT, Messages.getString("Gamemaster.cmd.fire.percent"), 1, 100, 25)
        );
    }


    /**
     * Run this command with the arguments supplied
     *
     * @see ServerCommand#run(int, String[])
     */
    @Override
    protected void runCommand(int connId, Arguments args) {
        if (getGameManager().getGame().getBoard().isSpace()) {
            server.sendServerChat(connId, "Can't start a firestorm in space");
        }
        try {
            var fireType = (int) args.get(TYPE).getValue();
            var percent = (int) args.get(PERCENT).getValue();
            var coords = getRandomCoords(numberOfCoordsFromPercent(percent));
            coords.forEach(c -> igniteHex(c, fireType));
        } catch (Exception e) {
            logger.error(Messages.getString("Gamemaster.cmd.fire.failed"), e);
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.fire.failed"));
        }
    }

    private int numberOfCoordsFromPercent(int percent) {
        var boardHeight = gameManager.getGame().getBoard().getHeight();
        var boardWidth = gameManager.getGame().getBoard().getWidth();
        return Math.max((boardWidth * boardHeight * percent / 100), 1);
    }

    private HashSet<Coords> getRandomCoords(int size) {
        var boardHeight = gameManager.getGame().getBoard().getHeight();
        var boardWidth = gameManager.getGame().getBoard().getWidth();
        var coordsSet = new HashSet<Coords>();
        var maxTries = size * 10;
        while (coordsSet.size() < size && maxTries > 0) {
            var x = (int) (Math.random() * boardWidth);
            var y = (int) (Math.random() * boardHeight);
            coordsSet.add(new Coords(x, y));
            maxTries--;
        }

        return coordsSet;
    }

    private void igniteHex(Coords coords, int fireType) {
        Hex hex = gameManager.getGame().getBoard().getHex(coords);
        if (null == hex) {
            // Just ignore null hexes...
            // they should not happen, but I don't want to crash the command
            return;
        }
        gameManager.ignite(coords, fireType, gameManager.getMainPhaseReport());
    }
}
