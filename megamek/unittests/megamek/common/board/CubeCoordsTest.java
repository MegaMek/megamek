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

package megamek.common.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Round-trip tests for {@link CubeCoords#toOffset()}. {@code toOffset()} must be the exact inverse of
 * {@link Coords#toCube()} under the odd-q offset convention, so a cube coordinate must survive a
 * cube -> offset -> cube round-trip unchanged. These tests use the real {@link Coords} on the offset side
 * (no mocks), because the contract being verified is precisely how the two real classes agree.
 */
class CubeCoordsTest {

    /** Valid cube coordinates spanning odd and even, positive and negative columns. */
    static Stream<CubeCoords> cubeCoordinates() {
        List<CubeCoords> coordinates = new ArrayList<>();
        for (int column = -4; column <= 4; column++) {
            for (int rowValue = -4; rowValue <= 4; rowValue++) {
                coordinates.add(new Coords(column, rowValue).toCube());
            }
        }
        return coordinates.stream();
    }

    @ParameterizedTest
    @MethodSource("cubeCoordinates")
    void cubeToOffsetToCubeReturnsOriginalCube(CubeCoords cube) {
        assertEquals(cube, cube.toOffset().toCube(),
              "cube -> offset -> cube must return the same hex for " + cube);
    }

    @Test
    void negativeRowHexConvertsToCorrectOffset() {
        // Regression: the old truncating formula turned (1,-1,0) into offset (1,0), a different hex.
        assertEquals(new Coords(1, -1), new CubeCoords(1, -1, 0).toOffset());
    }

    @Test
    void adjacentEastHexesKeepDistinctOffsets() {
        // Regression: the old formula collapsed both of these onto offset (1,0).
        Coords first = new CubeCoords(1, -1, 0).toOffset();
        Coords second = new CubeCoords(1, 0, -1).toOffset();
        assertNotEquals(first, second, "distinct cube hexes must map to distinct offset hexes");
    }
}
