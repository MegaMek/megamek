/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
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
 */
package megamek.ai.dataset;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Board;
import megamek.common.Hex;

/**
 * Flexible container for board data using a map-based approach with enum keys.
 * @author Luana Coppio
 */
public class BoardData extends EntityDataMap<BoardData.Field> {

    /**
     * Enum defining all available board data fields.
     */
    public enum Field {
        BOARD_NAME,
        WIDTH,
        HEIGHT,
        HEX_DATA; // Special field that contains all hex data
    }

    /**
     * Nested class to represent a single row of hex data
     */
    public static class HexRow {
        private final int rowIndex;
        private final List<Hex> hexes;

        public HexRow(int rowIndex, List<Hex> hexes) {
            this.rowIndex = rowIndex;
            this.hexes = new ArrayList<>(hexes);
        }

        public int getRowIndex() {
            return rowIndex;
        }

        public List<Hex> getHexes() {
            return new ArrayList<>(hexes);
        }
    }

    /**
     * Creates an empty BoardData.
     */
    public BoardData() {
        super(Field.class);
    }

    /**
     * Creates a BoardData from a Board.
     * @param board The board to extract data from
     * @return A populated BoardData
     */
    public static BoardData fromBoard(Board board) {
        BoardData data = new BoardData();

        // Basic board information
        data.put(Field.BOARD_NAME, board.getBoardName())
              .put(Field.WIDTH, board.getWidth())
              .put(Field.HEIGHT, board.getHeight());

        // Extract hex data by row
        List<HexRow> hexRows = new ArrayList<>();
        for (int y = 0; y < board.getHeight(); y++) {
            List<Hex> rowHexes = new ArrayList<>();
            for (int x = 0; x < board.getWidth(); x++) {
                rowHexes.add(board.getHex(x, y));
            }
            hexRows.add(new HexRow(y, rowHexes));
        }

        data.put(Field.HEX_DATA, hexRows);

        return data;
    }

    /**
     * Gets the board's hex rows.
     * @return List of HexRow objects containing the board's hexes
     */
    @SuppressWarnings("unchecked")
    public List<HexRow> getHexRows() {
        return (List<HexRow>) get(Field.HEX_DATA);
    }
}
