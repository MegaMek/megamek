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

import megamek.client.ui.swing.ClientGUI;
import megamek.common.Coords;
import megamek.common.Hex;

import java.util.ArrayList;
import java.util.List;

public class LookCommand extends ClientCommand {
    private final static List<String> directions = new ArrayList<>();
    {
        directions.add("N");
        directions.add("NE");
        directions.add("SE");
        directions.add("S");
        directions.add("SW");
        directions.add("NW");
    }

    public LookCommand(ClientGUI clientGUI) {
        super(clientGUI, "look", "Look around the current hex.");
    }

    @Override
    public String run(String[] args) {
        Coords pos = getClientGUI().getCurrentHex();
        Hex hex = getClient().getGame().getBoard().getHex(pos);
        String str;
        if (hex != null) {
            str = "Looking around hex (" + (pos.getX() + 1) + ", "
                    + (pos.getY() + 1) + ")\n";
            for (String dir : directions) {
                Coords coord = pos.translated(dir);
                str += "To the " + dir  + " (" + (coord.getX() + 1) + ", "
                        + (coord.getY() + 1) + "): ";
                hex = getClient().getGame().getBoard().getHex(coord);
                if (hex != null) {
                    str += hex.toString();
                } else {
                    str += "off map.\n";
                }
            }
        } else {
            str = "No current hex, or current hex off board.";
        }
        return str;
    }
}
