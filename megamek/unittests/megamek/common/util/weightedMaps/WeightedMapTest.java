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
package megamek.common.util.weightedMaps;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class WeightedMapTest {

    @Test
    public void testWeightedIntMap() {
        WeightedIntMap<Integer> weightedIntMap = new WeightedIntMap<>();
        int total = 0;
        for (int i = 0; i < 6; i++) {
            weightedIntMap.add(i, i);
            total += i;
        }

        assertEquals(5, weightedIntMap.size());
        assertEquals(total, weightedIntMap.lastKey().intValue());
        assertEquals(1, weightedIntMap.randomItem(1).intValue());
        assertEquals(2, weightedIntMap.randomItem(2).intValue());
        assertEquals(2, weightedIntMap.randomItem(3).intValue());
        assertEquals(3, weightedIntMap.randomItem(4).intValue());
        assertEquals(3, weightedIntMap.randomItem(6).intValue());
        assertEquals(4, weightedIntMap.randomItem(7).intValue());
        assertEquals(4, weightedIntMap.randomItem(10).intValue());
        assertEquals(5, weightedIntMap.randomItem(11).intValue());
        assertEquals(5, weightedIntMap.randomItem(15).intValue());
        assertNull(weightedIntMap.randomItem(16));
    }

    @Test
    public void testWeightedDoubleMap() {
        WeightedDoubleMap<Integer> weightedDoubleMap = new WeightedDoubleMap<>();
        // Totals add up to 16.5, so that's the maximum number that can be handled
        weightedDoubleMap.add(0.0d, 0);
        weightedDoubleMap.add(1.1d, 1);
        weightedDoubleMap.add(2.2d, 2);
        weightedDoubleMap.add(3.3d, 3);
        weightedDoubleMap.add(4.4d, 4);
        weightedDoubleMap.add(5.5d, 5);

        assertEquals(5, weightedDoubleMap.size());
        assertEquals(1.1d, weightedDoubleMap.firstKey(), 0.1d);
        assertEquals(16.5d, weightedDoubleMap.lastKey(), 0.1d);
        assertEquals(1, weightedDoubleMap.randomItem(0.0d).intValue());
        assertEquals(1, weightedDoubleMap.randomItem(0.6d).intValue());
        assertEquals(1, weightedDoubleMap.randomItem(1.1d).intValue());
        assertEquals(2, weightedDoubleMap.randomItem(3.3d).intValue());
        assertEquals(3, weightedDoubleMap.randomItem(6.6d).intValue());
        assertEquals(4, weightedDoubleMap.randomItem(11.0d).intValue());
        assertEquals(5, weightedDoubleMap.randomItem(11.1d).intValue());
        assertEquals(5, weightedDoubleMap.randomItem(16.5d).intValue());
        assertNull(weightedDoubleMap.randomItem(16.51d));
    }
}
