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

import megamek.common.Coords;
import megamek.common.Hex;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.totalwarfare.TWGameManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * The Server Command "/firestorm" that starts a blazing inferno on the board.
 *
 * @author Luana Scoppio
 */
public class FirestormCommand extends GamemasterServerCommand {

    public FirestormCommand(Server server, TWGameManager gameManager) {
        super(server,
            gameManager,
            "firestorm",
            "Starts fire in the entire board. "
                + "Usage: /firestorm [<intensity>] [<percent>]"
                + "The intensity can be 1=Normal, 2=Inferno, 3=Inferno Bomb or 4=Inferno IV, default is 1. "
                + "The size can be a percent of the board, from 1 to 100, default is 25.");
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(new IntegerArgument("intensity", 1, 4, 1),
            new IntegerArgument("percent", 1, 100, 25));
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see ServerCommand#run(int, String[])
     */
    @Override
    protected void runAsGM(int connId, Map<String, Argument<?>> args) {
        try {
            var fireType = (int) args.get("intensity").getValue();
            var percent = (int) args.get("percent").getValue();
            var coords = getRandomCoords(numberOfCoordsFromPercent(percent));
            coords.forEach(c -> igniteHex(c, fireType));
        } catch (Exception e) {
            logger.error("Failed to ignite fire.", e);
            server.sendServerChat(connId, "Failed to ignite fire.");
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
            // Just ignore null hexes... they should not happen, but I don't want to crash the command
            return;
        }
        gameManager.ignite(coords, fireType, gameManager.getvPhaseReport());
    }
}
