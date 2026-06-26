/*
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.server.commands;

import java.util.Iterator;

import megamek.common.Hex;
import megamek.common.board.Coords;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;

/**
 * This command exists to print tile information to the chat window and is primarily intended for visually impaired
 * users.
 *
 * @author dirk
 */
public class ShowTileCommand extends ServerCommand {

    private final TWGameManager gameManager;

    public ShowTileCommand(Server server, TWGameManager gameManager) {
        super(server,
              "tile",
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
            StringBuilder str;
            Coords coord = new Coords(Integer.parseInt(args[1]) - 1, Integer.parseInt(args[2]) - 1);
            Hex hex;

            do {
                hex = gameManager.getGame().getBoard().getHex(coord);
                if (hex != null) {
                    str = new StringBuilder("Details for hex (" + (coord.getX() + 1) + ", "
                          + (coord.getY() + 1) + ") : " + hex);

                    // if we are not playing in double-blind mode also list the
                    // units in this tile.
                    if (!server.getGame().getOptions().booleanOption(
                          OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
                        Iterator<Entity> entList = gameManager.getGame().getEntities(coord);
                        if (entList.hasNext()) {
                            str.append("; Contains entities: ").append(entList.next().getId());
                            while (entList.hasNext()) {
                                str.append(", ").append(entList.next().getId());
                            }
                        }
                    }

                    server.sendServerChat(connId, str.toString());
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
