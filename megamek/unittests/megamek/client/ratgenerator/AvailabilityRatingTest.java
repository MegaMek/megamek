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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AvailabilityRatingTest {

    private static final int ERA = 3050;
    private static final String UNIT = "Archer ARC-2R";

    @Test
    void makeCopyKeepsTheStartYear() {
        // "FS:5:3055" means the Fed Suns do not get this unit until 3055, five years into the 3050 era
        AvailabilityRating original = new AvailabilityRating(UNIT, ERA, "FS:5:3055");
        assertEquals(3055, original.getStartYear());

        AvailabilityRating copy = original.makeCopy("LA");

        assertEquals(3055, copy.getStartYear(),
              "A copy that drops the start year makes the unit available from the start of the era instead");
        assertEquals(5, copy.getAvailability());
        assertEquals("LA", copy.getFaction());
    }

    @Test
    void makeCopyKeepsAnUngatedStartYear() {
        AvailabilityRating original = new AvailabilityRating(UNIT, ERA, "FS:5");
        assertEquals(ERA, original.getStartYear());

        AvailabilityRating copy = original.makeCopy("LA");

        assertEquals(ERA, copy.getStartYear());
    }

    @Test
    void makeCopyKeepsTheRatingAdjustment() {
        AvailabilityRating original = new AvailabilityRating(UNIT, ERA, "FS:5+");
        AvailabilityRating copy = original.makeCopy("LA");

        // The +/- adjustment slides availability across equipment ratings, so a copy must not lose it
        assertEquals(original.adjustForRating(0, 5), copy.adjustForRating(0, 5));
        assertEquals(original.adjustForRating(4, 5), copy.adjustForRating(4, 5));
    }

    @Test
    void makeCopyKeepsPerRatingValues() {
        AvailabilityRating original = new AvailabilityRating(UNIT, ERA, "CLAN!Solahma:1!Second Line:4");
        assertTrue(original.hasMultipleRatings());

        AvailabilityRating copy = original.makeCopy("CJF");

        assertTrue(copy.hasMultipleRatings());
        assertEquals(1, copy.getAvailability("Solahma"));
        assertEquals(4, copy.getAvailability("Second Line"));
    }

    @Test
    void startYearRoundTripsThroughToString() {
        AvailabilityRating original = new AvailabilityRating(UNIT, ERA, "FS:5:3055");

        assertEquals("FS:5:3055", original.toString());

        AvailabilityRating reparsed = new AvailabilityRating(UNIT, ERA, original.toString());
        assertEquals(3055, reparsed.getStartYear());
    }

    @Test
    void ungatedRatingDoesNotWriteAStartYear() {
        AvailabilityRating original = new AvailabilityRating(UNIT, ERA, "FS:5");

        assertEquals("FS:5", original.toString());
        assertFalse(original.toString().endsWith(":" + ERA));
    }
}
