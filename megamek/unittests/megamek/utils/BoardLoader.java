/*
  Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.utils;

import java.util.ArrayList;
import java.util.List;

import megamek.common.board.Board;

/**
 * Utility class to load a board from a string representation.
 *
 * @author Luana Coppio
 */
public class BoardLoader {

    private BoardLoader() {
        // Prevent instantiation
    }

    /**
     * Load a board from a string
     *
     * @param data the board as a string
     */
    public static Board initializeBoard(String data) {
        int[] size = parseBoardSize(data);
        Board board = new Board(size[0], size[1]);
        List<String> errors = new ArrayList<>();
        board.load(data, errors);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Errors loading board, errors: " + errors);
        }
        return board;
    }

    /**
     * Parse the board size from the data string
     *
     * @param data the data string
     *
     * @return an array with the width and height of the board
     */
    private static int[] parseBoardSize(String data) {
        try {
            String[] lines = data.split("\n");
            String[] size = lines[0].split(" ");
            int width = Integer.parseInt(size[1]);
            int height = Integer.parseInt(size[2]);
            return new int[] { width, height };
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid board size in data: " + data, e);
        }
    }
}
