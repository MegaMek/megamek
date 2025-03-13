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

import java.util.Iterator;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Hex;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;

/**
 * This command exists to print tile information to the chat window and is
 * primarily intended for
 * visually impaired users.
 * 
 * @author dirk
 */
public class ShowTileCommand extends ServerCommand {

    private final TWGameManager gameManager;

    public ShowTileCommand(Server server, TWGameManager gameManager) {
        super(server, "tile",
                "print the information about a tile into the chat window. Usage: /tile 01 01 whih would show the details for the hex numbered 01 01.");
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        try {
            int i = 3;
            String str = "";
            Coords coord = new Coords(Integer.parseInt(args[1]) - 1, Integer.parseInt(args[2]) - 1);
            Hex hex;

            do {
                hex = gameManager.getGame().getBoard().getHex(coord);
                if (hex != null) {
                    str = "Details for hex (" + (coord.getX() + 1) + ", "
                            + (coord.getY() + 1) + ") : " + hex;

                    // if we are not playing in double-blind mode also list the
                    // units in this tile.
                    if (!server.getGame().getOptions().booleanOption(
                            OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
                        Iterator<Entity> entList = gameManager.getGame().getEntities(coord);
                        if (entList.hasNext()) {
                            str = str + "; Contains entities: " + entList.next().getId();
                            while (entList.hasNext()) {
                                str = str + ", " + entList.next().getId();
                            }
                        }
                    }

                    server.sendServerChat(connId, str);
                } else {
                    server.sendServerChat(connId, "Hex (" + (coord.getX() + 1)
                            + ", " + (coord.getY() + 1) + ") is not on the board.");
                }

                if (i < args.length) {
                    coord = coord.translated(args[i]);
                }

                i++;
            } while (i < args.length);
        } catch (Exception ignored) {

        }
    }
}
