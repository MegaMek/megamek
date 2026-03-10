/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.ai.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import megamek.common.Hex;
import megamek.common.board.Board;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BoardData class.
 */
class BoardDataTest {

    @Test
    void testEmptyConstructor() {
        BoardData data = new BoardData();
        assertNull(data.get(BoardData.Field.BOARD_NAME));
        assertNull(data.get(BoardData.Field.WIDTH));
        assertNull(data.get(BoardData.Field.HEIGHT));
        assertNull(data.get(BoardData.Field.HEX_DATA));
        assertEquals(BoardData.Field.class, data.getFieldEnumClass());
    }

    @Test
    void testFromBoard() {
        int width = 2;
        int height = 3;
        String boardName = "Test Board";
        Board board = new Board(width, height);
        board.setMapName(boardName);

        BoardData data = BoardData.fromBoard(board);

        assertEquals(boardName, data.get(BoardData.Field.BOARD_NAME));
        assertEquals(width, data.get(BoardData.Field.WIDTH));
        assertEquals(height, data.get(BoardData.Field.HEIGHT));

        List<BoardData.HexRow> hexRows = data.getHexRows();
        assertNotNull(hexRows);
        assertEquals(height, hexRows.size());

        for (int y = 0; y < height; y++) {
            BoardData.HexRow row = hexRows.get(y);
            assertEquals(y, row.rowIndex());
            assertEquals(width, row.hexes().size());
            for (int x = 0; x < width; x++) {
                assertEquals(board.getHex(x, y), row.hexes().get(x));
            }
        }
    }

    @Test
    void testFromBoardEmpty() {
        Board board = new Board(0, 0);
        board.setMapName("");

        BoardData data = BoardData.fromBoard(board);

        assertEquals("", data.get(BoardData.Field.BOARD_NAME));
        assertEquals(0, data.get(BoardData.Field.WIDTH));
        assertEquals(0, data.get(BoardData.Field.HEIGHT));
        assertTrue(data.getHexRows().isEmpty());
    }

    @Test
    void testHexRowImmutability() {
        List<Hex> hexes = new ArrayList<>(Arrays.asList(new Hex(), new Hex()));
        BoardData.HexRow row = new BoardData.HexRow(1, hexes);

        // Modify an original list
        hexes.clear();
        assertEquals(2, row.hexes().size(), "HexRow should have its own copy of the list");

        // Try to modify list returned by hexes()
        List<Hex> returnedHexes = row.hexes();
        returnedHexes.clear();
        assertEquals(2, row.hexes().size(), "hexes() should return a copy of the list");
    }

    @Test
    void testManualPutAndGet() {
        BoardData data = new BoardData();
        String name = "Manual Name";
        data.put(BoardData.Field.BOARD_NAME, name);
        assertEquals(name, data.get(BoardData.Field.BOARD_NAME));
        assertEquals(name, data.get(BoardData.Field.BOARD_NAME, String.class));
    }

    @Test
    void testGetWithWrongType() {
        BoardData data = new BoardData();
        data.put(BoardData.Field.WIDTH, 10);
        assertNull(data.get(BoardData.Field.WIDTH, String.class));
    }

    @Test
    void testGetFieldOrder() {
        BoardData data = new BoardData();
        data.put(BoardData.Field.BOARD_NAME, "Test");
        data.put(BoardData.Field.WIDTH, 16);
        List<BoardData.Field> order = data.getFieldOrder();
        assertEquals(2, order.size());
        assertEquals(BoardData.Field.BOARD_NAME, order.get(0));
        assertEquals(BoardData.Field.WIDTH, order.get(1));
    }

    @Test
    void testGetAllFields() {
        BoardData data = new BoardData();
        data.put(BoardData.Field.BOARD_NAME, "Test");
        assertEquals(1, data.getAllFields().size());
        assertEquals("Test", data.getAllFields().get(BoardData.Field.BOARD_NAME));
    }

    @Test
    void testGetVersionedClassName() {
        BoardData data = new BoardData();
        assertEquals("BoardData.31052025", data.getVersionedClassName());
    }

    @Test
    void testFromBoardWithNullName() {
        Board board = new Board(1, 1);
        // Depending on Board implementation, setMapName(null) might work or throw
        board.setMapName(null);

        BoardData data = BoardData.fromBoard(board);
        assertNull(data.get(BoardData.Field.BOARD_NAME));
    }

    @Test
    void testFromBoardNull() {
        assertThrows(NullPointerException.class, () -> BoardData.fromBoard(null));
    }
}
