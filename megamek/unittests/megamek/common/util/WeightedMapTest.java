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

import megamek.common.util.weightedMap.WeightedDoubleMap;
import megamek.common.util.weightedMap.WeightedIntMap;
import org.junit.Test;

import static org.junit.Assert.*;

public class WeightedMapTest {

    @Test
    public void testWeightedIntMap() {
        WeightedIntMap<Integer> weightedIntMap = new WeightedIntMap<>();
        int total = 0;
        for (int i = 0; i < 6; i++) {
            weightedIntMap.put(i, i);
            total += i;
        }

        assertEquals(weightedIntMap.size(), 5);
        assertEquals(weightedIntMap.lastKey().intValue(), total);
        assertEquals(1, weightedIntMap.ceilingEntry(1).getValue().intValue());
        assertEquals(2, weightedIntMap.ceilingEntry(2).getValue().intValue());
        assertEquals(3, weightedIntMap.ceilingEntry(10).getValue().intValue());
        assertEquals(4, weightedIntMap.ceilingEntry(11).getValue().intValue());
        assertEquals(5, weightedIntMap.ceilingEntry(15).getValue().intValue());
    }

    @Test
    public void testWeightedDoubleMap() {
        WeightedDoubleMap<Integer> weightedDoubleMap = new WeightedDoubleMap<>();
        weightedDoubleMap.add(0.0, 0);
        weightedDoubleMap.add(1.1d, 1);
        weightedDoubleMap.add(2.2d, 2);
        weightedDoubleMap.add(3.3d, 3);
        weightedDoubleMap.add(4.4d, 4);
        weightedDoubleMap.add(5.5d, 5);

        assertEquals(weightedDoubleMap.size(), 5);
        assertEquals(weightedDoubleMap.firstKey(), 1.1d, 0.1d);
        assertEquals(weightedDoubleMap.lastKey(), 16.5d, 0.1d);
        assertEquals(1, weightedDoubleMap.ceilingEntry(0.0d).getValue().intValue());
        assertEquals(1, weightedDoubleMap.ceilingEntry(0.6d).getValue().intValue());
        assertEquals(1, weightedDoubleMap.ceilingEntry(1.1d).getValue().intValue());
        assertEquals(2, weightedDoubleMap.ceilingEntry(3.3d).getValue().intValue());
        assertEquals(3, weightedDoubleMap.ceilingEntry(6.6d).getValue().intValue());
        assertEquals(4, weightedDoubleMap.ceilingEntry(11.0d).getValue().intValue());
        assertEquals(5, weightedDoubleMap.ceilingEntry(11.1d).getValue().intValue());
        assertEquals(5, weightedDoubleMap.ceilingEntry(16.5d).getValue().intValue());
    }
}
