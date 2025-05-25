/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.test;

import megamek.common.Compute;

public class TestDice {

    public static void testDice() {
        // test RNG
        long rolls = 1000000;
        long[] hits = new long[11];

        System.out.println("testing dice, " + rolls + " rolls...");

        long start = System.currentTimeMillis();
        for (long i = 0; i < rolls; i++) {
            hits[megamek.common.Compute.d6(2) - 2]++;
        }
        long end = System.currentTimeMillis();

        System.out.println("done testing dice in " + (end - start) + " ms.");
        for (int i = 0; i < hits.length; i++) {
            System.out.println("hits on " + (i + 2) + " : " + hits[i]
                    + "; probability = " + ((double) hits[i] / (double) rolls));
        }

        int[][] pairs = new int[6][6];
        System.out.println("testing streaks, " + rolls + " rolls...");

        int nLastLastRoll = 1;
        for (long i = 0; i < rolls; i++) {
            int nRoll = Compute.d6();
            pairs[nLastLastRoll - 1][nRoll - 1]++;
            nLastLastRoll = nRoll;
        }
        for (int x = 0; x < pairs.length; x++) {
            for (int y = 0; y < pairs[x].length; y++) {
                System.out
                        .println((x + 1) + "," + (y + 1) + ": " + pairs[x][y]);
            }
        }
        // odd, but necessary
        System.out.flush();
    }

    public static void main(String[] args) {
        testDice();
    }
}
