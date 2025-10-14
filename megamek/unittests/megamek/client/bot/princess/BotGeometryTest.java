/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import megamek.common.board.Coords;
import org.junit.jupiter.api.Test;

class BotGeometryTest {

    /**
     * Carries out a test of the BotGeometry donut functionality.
     */
    @Test
    void testDonut() {
        //========================================================
        // Note, this is for the underlying 0,0 coordinate system.
        // To convert to map number, add [+1 +1] to Coords:
        //
        //         Internal Rep.                  Map Rep.
        //           _____                         _____
        //          /     \                       /     \
        //    _____/ 0,-1  \_____           _____/  0100 \_____
        //   /     \       /     \         /     \       /     \
        //  / -1,-1 \_____/  1,-1 \       /  0000 \_____/  0200 \
        //  \       /     \       /       \       /     \       /
        //   \_____/  0,0  \_____/  ___\   \_____/  0101 \_____/
        //   /     \       /     \     /   /     \       /     \
        //  / -1,0  \_____/  1,0  \       /  0001 \_____/  0201 \
        //  \       /     \       /       \       /     \       /
        //   \_____/  0,1  \_____/         \_____/  0102 \_____/
        //         \       /                     \       /
        //          \_____/                       \_____/
        //
        // Conversely, add [-1 -1] to a real map hex to get its
        // internal representation coordinates.
        //========================================================

        Coords testCoords = new Coords(0, 0);

        List<Coords> resultingCoords = testCoords.allAtDistance(0);
        assertEquals(1, resultingCoords.size());
        assertTrue(resultingCoords.contains(testCoords));

        // for a radius 1 donut, we expect to see 6 hexes.
        resultingCoords = testCoords.allAtDistance(1);

        List<Coords> expectedCoords = new ArrayList<>();
        // Clockwise around center from dir 0
        expectedCoords.add(new Coords(0, -1));
        expectedCoords.add(new Coords(1, -1));
        expectedCoords.add(new Coords(1, 0));
        expectedCoords.add(new Coords(0, 1));
        expectedCoords.add(new Coords(-1, 0));
        expectedCoords.add(new Coords(-1, -1));

        assertEquals(6, resultingCoords.size());
        for (Coords expectedCoord : expectedCoords) {
            assertTrue(resultingCoords.contains(expectedCoord), expectedCoord.toString());
        }

        // for a radius 2 donut we expect to see 12 hexes.
        resultingCoords = testCoords.allAtDistance(2);

        expectedCoords = new ArrayList<>();
        // Clockwise around center from dir 0
        expectedCoords.add(new Coords(0, -2));
        expectedCoords.add(new Coords(1, -2));
        expectedCoords.add(new Coords(2, -1));
        expectedCoords.add(new Coords(2, 0));
        expectedCoords.add(new Coords(2, 1));
        expectedCoords.add(new Coords(1, 1));
        expectedCoords.add(new Coords(0, 2));
        expectedCoords.add(new Coords(-1, 1));
        expectedCoords.add(new Coords(-2, 1));
        expectedCoords.add(new Coords(-2, 0));
        expectedCoords.add(new Coords(-2, -1));
        expectedCoords.add(new Coords(-1, -2));
        assertEquals(12, resultingCoords.size());
        for (Coords expectedCoord : expectedCoords) {
            assertTrue(resultingCoords.contains(expectedCoord), expectedCoord.toString());
        }
    }
}
