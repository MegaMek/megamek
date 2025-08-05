/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 * @since 9/3/14 1:44 PM
 */
class BoardUtilitiesTest {

    @Test
    void testCraterProfile() {
        int craterRadius = 8;
        int maxDepth = 4;

        // Start at the center;
        int distanceFromCenter = 0;
        int expected = -4;
        assertEquals(expected, BoardUtilities.craterProfile(distanceFromCenter, craterRadius, maxDepth));

        // One hex from center;
        distanceFromCenter = 1;
        expected = -4;
        assertEquals(expected, BoardUtilities.craterProfile(distanceFromCenter, craterRadius, maxDepth));

        // Three hexes from center;
        distanceFromCenter = 3;
        expected = -4;
        assertEquals(expected, BoardUtilities.craterProfile(distanceFromCenter, craterRadius, maxDepth));

        // Four hexes from center;
        distanceFromCenter = 4;
        expected = -4;
        assertEquals(expected, BoardUtilities.craterProfile(distanceFromCenter, craterRadius, maxDepth));

        // Five hexes from center;
        distanceFromCenter = 5;
        expected = -3;
        assertEquals(expected, BoardUtilities.craterProfile(distanceFromCenter, craterRadius, maxDepth));

        // Six hexes from center;
        distanceFromCenter = 6;
        expected = -3;
        assertEquals(expected, BoardUtilities.craterProfile(distanceFromCenter, craterRadius, maxDepth));

        // Seven hexes from center;
        distanceFromCenter = 7;
        expected = -2;
        assertEquals(expected, BoardUtilities.craterProfile(distanceFromCenter, craterRadius, maxDepth));

        // Eight hexes from center;
        distanceFromCenter = 8;
        expected = 0;
        assertEquals(expected, BoardUtilities.craterProfile(distanceFromCenter, craterRadius, maxDepth));
    }
}
