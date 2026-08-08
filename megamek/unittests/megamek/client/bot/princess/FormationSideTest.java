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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the rule that a force split by deep water forms up on its own bank.
 *
 * <p>Built on a real {@link Board} rather than mocks, because the behaviour under test is entirely about what the
 * terrain says: a mocked board would only assert that the test agrees with itself.</p>
 */
class FormationSideTest {

    private static final int WIDTH = 20;
    private static final int HEIGHT = 10;

    /** A north-south river down column 10, deep enough to separate a formation. */
    private static final int RIVER_COLUMN = 10;

    private Board board;

    /** A board of clear hexes with a north-south river of the given depth down {@link #RIVER_COLUMN}. */
    private static Board boardWithRiver(int waterDepth) {
        Hex[] hexes = new Hex[WIDTH * HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                Hex hex = new Hex();
                if (x == RIVER_COLUMN) {
                    hex.addTerrain(new Terrain(Terrains.WATER, waterDepth));
                }
                hexes[(y * WIDTH) + x] = hex;
            }
        }
        return new Board(WIDTH, HEIGHT, hexes);
    }

    @BeforeEach
    void setUp() {
        board = boardWithRiver(FormationSide.SEPARATING_DEPTH);
    }

    @Test
    void twoUnitsOnTheSameBankFormUpTogether() {
        assertTrue(FormationSide.sameSide(board, new Coords(2, 4), new Coords(6, 5)),
              "nothing between them, so they are one formation");
    }

    @Test
    void aRiverBetweenThemSplitsTheFormation() {
        assertFalse(FormationSide.sameSide(board, new Coords(4, 5), new Coords(16, 5)),
              "the river runs between them, so they cannot form up as one body");
    }

    @Test
    void aUnitIsAlwaysOnItsOwnSide() {
        Coords position = new Coords(3, 3);
        assertTrue(FormationSide.sameSide(board, position, position));
    }

    @Test
    void shallowWaterIsAFordAndDoesNotSplitAForce() {
        Board ford = boardWithRiver(FormationSide.SEPARATING_DEPTH - 1);
        assertTrue(FormationSide.sameSide(ford, new Coords(4, 5), new Coords(16, 5)),
              "a unit wades a ford and the force stays a force; only deep water breaks it up");
    }

    @Test
    void missingBoardOrPositionsNeverSplitsAForce() {
        assertTrue(FormationSide.sameSide(null, new Coords(1, 1), new Coords(2, 2)));
        assertTrue(FormationSide.sameSide(board, null, new Coords(2, 2)));
        assertTrue(FormationSide.sameSide(board, new Coords(1, 1), null));
    }

    @Test
    void isForceSplitDetectsAnyFriendCutOff() {
        Coords mover = new Coords(4, 5);
        assertFalse(FormationSide.isForceSplit(board, mover, List.of(new Coords(5, 5), new Coords(7, 6))),
              "a force entirely on one bank is not split");
        assertTrue(FormationSide.isForceSplit(board, mover, List.of(new Coords(5, 5), new Coords(16, 5))),
              "one friend across the river is enough to make the force split");
    }
}
