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
package megamek.utilities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BoardsTagger}.
 */
class BoardsTaggerTest {

    private Board buildEmptyBoard() {
        Board board = new Board(2, 2);
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                board.setHex(x, y, new Hex());
            }
        }
        return board;
    }

    @Test
    void industrialElevatorHexTagsBoardAsIndustrialElevator() {
        Board board = buildEmptyBoard();
        Hex elevatorHex = new Hex();
        // level 0 shaft bottom, exits encode shaft top 4 with 100-ton capacity
        elevatorHex.addTerrain(new Terrain(Terrains.INDUSTRIAL_ELEVATOR, 0, true, (4 << 8) | 10));
        board.setHex(0, 0, elevatorHex);

        List<String> tags = BoardsTagger.tagsFor(board);

        assertTrue(tags.contains(BoardsTagger.Tags.TAG_INDUSTRIAL_ELEVATOR.getName()),
              "A board with an industrial elevator hex should get the IndustrialElevator tag");
        assertFalse(tags.contains(BoardsTagger.Tags.TAG_ELEVATOR.getName()),
              "An industrial elevator should not trigger the Solaris Elevator tag");
    }

    @Test
    void boardWithoutElevatorGetsNoElevatorTags() {
        Board board = buildEmptyBoard();

        List<String> tags = BoardsTagger.tagsFor(board);

        assertFalse(tags.contains(BoardsTagger.Tags.TAG_INDUSTRIAL_ELEVATOR.getName()));
        assertFalse(tags.contains(BoardsTagger.Tags.TAG_ELEVATOR.getName()));
    }
}
