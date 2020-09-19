/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class WeightedMapTest {

    @Test
    public void testWeightedMap() {
        WeightedMap<Integer> map = new WeightedMap<>();
        int total = 0;
        for (int i = 0; i < 6; i++) {
            map.add(i, i);
            total += i;
        }

        assertEquals(map.size(), 5);
        assertEquals(map.lastKey().intValue(), total);
        assertEquals(map.ceilingEntry(1).getValue().intValue(), 1);
        assertEquals(map.ceilingEntry(2).getValue().intValue(), 2);
        assertEquals(map.ceilingEntry(10).getValue().intValue(), 4);
        assertEquals(map.ceilingEntry(11).getValue().intValue(), 5);
        assertEquals(map.ceilingEntry(15).getValue().intValue(), 5);
    }
}