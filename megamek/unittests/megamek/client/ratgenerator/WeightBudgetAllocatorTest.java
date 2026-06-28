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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import megamek.common.units.EntityWeightClass;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeightBudgetAllocator#integerBudget}, the deterministic core of the per-cluster-type weight
 * budget. Tests use spread 0 so the largest-remainder math is reproducible.
 */
class WeightBudgetAllocatorTest {

    private static final int L = EntityWeightClass.WEIGHT_LIGHT;
    private static final int M = EntityWeightClass.WEIGHT_MEDIUM;
    private static final int H = EntityWeightClass.WEIGHT_HEAVY;
    private static final int A = EntityWeightClass.WEIGHT_ASSAULT;

    private static Map<Integer, Double> pct(double l, double m, double h, double a) {
        Map<Integer, Double> p = new HashMap<>();
        p.put(L, l);
        p.put(M, m);
        p.put(H, h);
        p.put(A, a);
        return p;
    }

    private static int sum(Map<Integer, Integer> budget) {
        return budget.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Test
    void budgetAlwaysSumsToN() {
        for (int n = 0; n <= 60; n++) {
            Map<Integer, Integer> budget = WeightBudgetAllocator.integerBudget(pct(25, 40, 25, 10), 0, n);
            assertEquals(n, sum(budget), "budget must sum to n=" + n);
        }
    }

    @Test
    void allInOneClassWhenTargetIsExclusive() {
        Map<Integer, Integer> budget = WeightBudgetAllocator.integerBudget(pct(0, 0, 0, 100), 0, 10);
        assertEquals(10, budget.get(A));
        assertEquals(0, budget.getOrDefault(L, 0));
        assertEquals(0, budget.getOrDefault(M, 0));
        assertEquals(0, budget.getOrDefault(H, 0));
    }

    @Test
    void evenSplitDividesCleanly() {
        Map<Integer, Integer> budget = WeightBudgetAllocator.integerBudget(pct(25, 25, 25, 25), 0, 40);
        assertEquals(10, budget.get(L));
        assertEquals(10, budget.get(M));
        assertEquals(10, budget.get(H));
        assertEquals(10, budget.get(A));
    }

    @Test
    void largestRemainderStaysWithinFloorCeil() {
        // 10 slots at an even 25/25/25/25 -> 2.5 each; floors to 2, two leftover slots distributed.
        Map<Integer, Integer> budget = WeightBudgetAllocator.integerBudget(pct(25, 25, 25, 25), 0, 10);
        assertEquals(10, sum(budget));
        for (int wc : new int[] { L, M, H, A }) {
            int c = budget.getOrDefault(wc, 0);
            assertTrue((c == 2) || (c == 3), "each class should be floor(2) or ceil(3), got " + c);
        }
    }

    @Test
    void emptyTargetStillSumsToN() {
        // All-zero target: renormalize guard kicks in, leftover distributed; must not throw or lose slots.
        Map<Integer, Integer> budget = WeightBudgetAllocator.integerBudget(new HashMap<>(), 0, 15);
        assertEquals(15, sum(budget));
    }

    @Test
    void heavyTargetWeightsHeavy() {
        Map<Integer, Integer> budget = WeightBudgetAllocator.integerBudget(pct(0, 8, 42, 50), 0, 50);
        assertEquals(50, sum(budget));
        assertTrue(budget.get(A) > budget.get(H), "assault share should exceed heavy for this target");
        assertTrue(budget.get(H) > budget.getOrDefault(M, 0), "heavy share should exceed medium");
        assertEquals(0, budget.getOrDefault(L, 0), "no light slots for a 0% light target at spread 0");
    }
}
