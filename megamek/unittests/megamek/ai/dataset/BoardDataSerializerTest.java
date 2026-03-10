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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import megamek.common.Hex;
import megamek.common.board.Board;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BoardDataSerializer class.
 */
class BoardDataSerializerTest {

    @Test
    void testGetHeaderLine() {
        BoardDataSerializer serializer = new BoardDataSerializer();
        String header = serializer.getHeaderLine();
        assertEquals("VERSION\tBOARD_NAME\tWIDTH\tHEIGHT", header);
    }

    @Test
    void testSerializeStandardBoard() {
        int width = 2;
        int height = 2;
        String boardName = "TestBoard";

        Hex[] hexData = new Hex[width * height];
        for (int i = 0; i < hexData.length; i++) {
            hexData[i] = new Hex();
        }
        Board board = new Board(width, height, hexData);
        board.setMapName(boardName);

        BoardData data = BoardData.fromBoard(board);
        BoardDataSerializer serializer = new BoardDataSerializer();

        String serialized = serializer.serialize(data);
        assertNotNull(serialized);

        String[] lines = serialized.split("\n");
        // Header, Main Data, Col Header, Row 0, Row 1
        assertEquals(5, lines.length);

        assertEquals("VERSION\tBOARD_NAME\tWIDTH\tHEIGHT", lines[0]);
        assertEquals("BoardData.31052025\tTestBoard\t2\t2", lines[1]);
        assertEquals("VERSION\tCOL_0\tCOL_1", lines[2]);

        // Rows should start with a versioned class name, followed by ROW_X, then hex strings
        String expectedRow0Start = "BoardData.31052025\tROW_0\t";
        assertTrue(lines[3].startsWith(expectedRow0Start));

        String expectedRow1Start = "BoardData.31052025\tROW_1\t";
        assertTrue(lines[4].startsWith(expectedRow1Start));

        // Verify hex strings (empty board has default hexes)
        String hexStr = new Hex().toString();
        assertEquals(expectedRow0Start + hexStr + "\t" + hexStr, lines[3]);
        assertEquals(expectedRow1Start + hexStr + "\t" + hexStr, lines[4]);
    }

    @Test
    void testSerializeEmptyBoard() {
        Board board = new Board(0, 0);
        board.setMapName("Empty");

        BoardData data = BoardData.fromBoard(board);
        BoardDataSerializer serializer = new BoardDataSerializer();

        String serialized = serializer.serialize(data);
        String[] lines = serialized.split("\n");

        // Header, Main Data. No hex data lines because height is 0.
        assertEquals(2, lines.length);
        assertEquals("VERSION\tBOARD_NAME\tWIDTH\tHEIGHT", lines[0]);
        assertEquals("BoardData.31052025\tEmpty\t0\t0", lines[1]);
    }

    @Test
    void testSerializeBoardWithNullName() {
        Board board = new Board(1, 1);
        board.setMapName(null);

        BoardData data = BoardData.fromBoard(board);
        BoardDataSerializer serializer = new BoardDataSerializer();

        String serialized = serializer.serialize(data);
        String[] lines = serialized.split("\n");

        // "null" because String.valueOf(null) is "null" in BoardDataSerializer
        assertEquals("BoardData.31052025\tnull\t1\t1", lines[1]);
    }

    @Test
    void testSerializeWithNullHexRows() {
        BoardData data = new BoardData();
        data.put(BoardData.Field.BOARD_NAME, "NullRows")
              .put(BoardData.Field.WIDTH, 1)
              .put(BoardData.Field.HEIGHT, 1);
        // HEX_DATA is not put, so getHexRows() returns null

        BoardDataSerializer serializer = new BoardDataSerializer();
        String serialized = serializer.serialize(data);
        String[] lines = serialized.split("\n");

        // Should only have 2 lines: Header and Main Data
        assertEquals(2, lines.length);
        assertEquals("VERSION\tBOARD_NAME\tWIDTH\tHEIGHT", lines[0]);
        assertEquals("BoardData.31052025\tNullRows\t1\t1", lines[1]);
    }

    @Test
    void testSerializeWithEmptyHexRows() {
        BoardData data = new BoardData();
        data.put(BoardData.Field.BOARD_NAME, "EmptyRows")
              .put(BoardData.Field.WIDTH, 1)
              .put(BoardData.Field.HEIGHT, 1);
        data.put(BoardData.Field.HEX_DATA, new java.util.ArrayList<>());

        BoardDataSerializer serializer = new BoardDataSerializer();
        String serialized = serializer.serialize(data);
        String[] lines = serialized.split("\n");

        assertEquals(2, lines.length);
    }

    @Test
    void testSerializeWithNullHex() {
        BoardData data = new BoardData();
        data.put(BoardData.Field.BOARD_NAME, "NullHexTest")
              .put(BoardData.Field.WIDTH, 1)
              .put(BoardData.Field.HEIGHT, 1);

        // Manually create a HexRow with a null hex
        BoardData.HexRow row = new BoardData.HexRow(0, Collections.singletonList(null));
        data.put(BoardData.Field.HEX_DATA, List.of(row));

        BoardDataSerializer serializer = new BoardDataSerializer();
        String serialized = serializer.serialize(data);
        String[] lines = serialized.split("\n");

        // Row 0 line: ClassName TAB ROW_0 TAB (empty string for null hex)
        assertEquals("BoardData.31052025\tROW_0\t", lines[3]);
    }
}
