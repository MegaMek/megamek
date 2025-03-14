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
package megamek.client.commands;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import megamek.client.ui.swing.ClientGUI;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Hex;
import megamek.common.options.OptionsConstants;

/**
 * This command exists to print tile information to the chat window, it's primarily intended for
 * visually impaired users.
 * @author dirk
 */
public class ShowTileCommand extends ClientCommand {
    public final static Set<String> directions = new HashSet<>();
    static {
        directions.add("N");
        directions.add("NW");
        directions.add("NE");
        directions.add("S");
        directions.add("SW");
        directions.add("SE");
    }

    public ShowTileCommand(ClientGUI clientGUI) {
        super(
                clientGUI,
                "tile",
                "print the information about a tile into the chat window. " +
                        "Usage: #tile 01 01 [dir1 ...] which would show the details for the hex numbered 01 01. " +
                        "The command can be followed with any number of directions (N,NE,SE,S,SW,NW) to list " +
                        "the tiles following those directions. Updates Current Hex. " +
                        "Can also list just directions to look from current tile.");
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public String run(String[] args) {
        try {
            int i = 3;
            String str, report = "";
            Coords coord;
            if ((args.length >= 1) && directions.contains(args[0].toUpperCase())) {
                i = 1;
                coord = getClientGUI().getCurrentHex().translated(args[0]);
            } else if ((args.length > 1) && directions.contains(args[1].toUpperCase()) ) {
                i = 2;
                coord = getClientGUI().getCurrentHex().translated(args[1]);
            } else {
                coord = new Coords(Integer.parseInt(args[1]) - 1, Integer
                        .parseInt(args[2]) - 1);
            }
            Hex hex;

            do {
                hex = getClient().getGame().getBoard().getHex(coord);
                getClientGUI().setCurrentHex(hex);
                if (hex != null) {
                    str = "Details for hex (" + (coord.getX() + 1) + ", "
                          + (coord.getY() + 1) + ") : " + hex;

                    // if we are not playing in double-blind mode also list the
                    // units in this tile.
                    if (!getClient().getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
                        Iterator<Entity> entList = getClient().getGame().getEntities(coord);
                        if (entList.hasNext()) {
                            str = str + "; Contains entities: " + entList.next().getId();
                            while (entList.hasNext()) {
                                str = str + ", " + entList.next().getId();
                            }
                        }
                    }

                    report = report + str + "\n";
                } else {
                    report = report + "Hex (" + (coord.getX() + 1) + ", "
                             + (coord.getY() + 1) + ") is not on the board.\n";
                }

                if (i < args.length) {
                    coord = coord.translated(args[i]);
                }

                i++;
            } while (i <= args.length);

            return report;
        } catch (Exception ignored) {

        }

        return "Error parsing the command.";
    }
}
