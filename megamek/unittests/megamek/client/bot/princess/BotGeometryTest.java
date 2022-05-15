/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import megamek.common.Coords;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BotGeometryTest {

    /**
     * Carries out a test of the BotGeometry donut functionality. 
     */
    @Test
    public void testDonut() {
        Coords testCoords = new Coords(0, 0);
        
        List<Coords> resultingCoords = testCoords.allAtDistance(0);
        assertEquals(1, resultingCoords.size());
        assertTrue(resultingCoords.contains(testCoords));
        
        // for a radius 1 donut, we expect to see 6 hexes.
        resultingCoords = testCoords.allAtDistance(1);
        
        List<Coords> expectedCoords = new ArrayList<>();
        expectedCoords.add(new Coords(1, -1));
        expectedCoords.add(new Coords(1, 0));
        expectedCoords.add(new Coords(0, -1));
        expectedCoords.add(new Coords(0, 1));
        expectedCoords.add(new Coords(-1, 0));
        expectedCoords.add(new Coords(-1, -1));
        
        assertEquals(6, resultingCoords.size());
        for (Coords expectedCoord : expectedCoords) {
            assertTrue(resultingCoords.contains(expectedCoord));
        }
        
        // for a radius 2 donut we expect to see 12 hexes.
        resultingCoords = testCoords.allAtDistance(2);
        
        expectedCoords = new ArrayList<>();
        expectedCoords.add(new Coords(-2, 0));
        expectedCoords.add(new Coords(0, -2));
        expectedCoords.add(new Coords(1, 1));
        expectedCoords.add(new Coords(-2, 1));
        expectedCoords.add(new Coords(1, -2));
        expectedCoords.add(new Coords(-2, -1));
        expectedCoords.add(new Coords(2, 1));
        expectedCoords.add(new Coords(-1, -2));
        expectedCoords.add(new Coords(2, -1));
        expectedCoords.add(new Coords(2, 0));
        expectedCoords.add(new Coords(0, 2));
        expectedCoords.add(new Coords(-1, 1));
        assertEquals(12, resultingCoords.size());
        for (Coords expectedCoord : expectedCoords) {
            assertTrue(resultingCoords.contains(expectedCoord));
        }
    }
}
