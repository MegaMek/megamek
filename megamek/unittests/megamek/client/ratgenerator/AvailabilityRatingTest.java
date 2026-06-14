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

import org.junit.jupiter.api.Test;

class AvailabilityRatingTest {

    @Test
    void parsesSimpleAvailabilityWithoutAdjustment() {
        AvailabilityRating availabilityRating = new AvailabilityRating("Test Unit", 3025, "FS:5");

        assertEquals("FS", availabilityRating.getFactionCode());
        assertEquals(5, availabilityRating.getAvailability());
        assertEquals(0, availabilityRating.getRatingAdjustment());
        assertEquals(3025, availabilityRating.getStartYear());
        assertEquals("5", availabilityRating.getAvailabilityCode());
    }

    @Test
    void plusAvailabilityDropsAsEquipmentRatingFalls() {
        AvailabilityRating availabilityRating = new AvailabilityRating("Test Unit", 3025, "FS:5+");

        assertEquals(5, availabilityRating.adjustForRating(4, 5));
        assertEquals(4, availabilityRating.adjustForRating(3, 5));
        assertEquals(3, availabilityRating.adjustForRating(2, 5));
        assertEquals(2, availabilityRating.adjustForRating(1, 5));
        assertEquals(1, availabilityRating.adjustForRating(0, 5));
        assertEquals("5+", availabilityRating.getAvailabilityCode());
    }

    @Test
    void minusAvailabilityDropsAsEquipmentRatingImproves() {
        AvailabilityRating availabilityRating = new AvailabilityRating("Test Unit", 3025, "FS:5-");

        assertEquals(5, availabilityRating.adjustForRating(0, 5));
        assertEquals(4, availabilityRating.adjustForRating(1, 5));
        assertEquals(3, availabilityRating.adjustForRating(2, 5));
        assertEquals(2, availabilityRating.adjustForRating(3, 5));
        assertEquals(1, availabilityRating.adjustForRating(4, 5));
        assertEquals("5-", availabilityRating.getAvailabilityCode());
    }

    @Test
    void discreteRatingsAreMappedToConfiguredNumericLevels() {
        AvailabilityRating availabilityRating = new AvailabilityRating("Test Unit", 3025,
              "FS!F:4!D:5!C:6!B:7!A:8");
        FactionRecord factionRecord = new FactionRecord("FS", "Federated Suns");
        factionRecord.setRatings("F,D,C,B,A");

        availabilityRating.setRatingByNumericLevel(factionRecord);

        assertTrue(availabilityRating.hasMultipleRatings());
        assertEquals(8, availabilityRating.getAvailability());
        assertEquals(4, availabilityRating.getAvailability(0));
        assertEquals(5, availabilityRating.getAvailability(1));
        assertEquals(6, availabilityRating.getAvailability(2));
        assertEquals(7, availabilityRating.getAvailability(3));
        assertEquals(8, availabilityRating.getAvailability(4));
        assertEquals(8, availabilityRating.getAvailability("A"));
        assertEquals(4, availabilityRating.getAvailability("F"));
    }

    @Test
    void calcAvailabilityAppliesPrototypeAndIntroYearPenalties() {
        AbstractUnitRecord unitRecord = new AbstractUnitRecord("Test Unit");
        unitRecord.introYear = 3025;
        AvailabilityRating availabilityRating = new AvailabilityRating("Test Unit", 3025, "FS:5");

        assertEquals(3, unitRecord.calcAvailability(availabilityRating, 2, 5, 3024));
        assertEquals(4, unitRecord.calcAvailability(availabilityRating, 2, 5, 3025));
        assertEquals(5, unitRecord.calcAvailability(availabilityRating, 2, 5, 3026));
    }

    @Test
    void calcAvailabilityUsesDiscreteRatingsBeforeIntroPenalty() {
        AbstractUnitRecord unitRecord = new AbstractUnitRecord("Test Unit");
        unitRecord.introYear = 3025;
        AvailabilityRating availabilityRating = new AvailabilityRating("Test Unit", 3025,
              "FS!F:4!D:5!C:6!B:7!A:8");
        FactionRecord factionRecord = new FactionRecord("FS", "Federated Suns");
        factionRecord.setRatings("F,D,C,B,A");
        availabilityRating.setRatingByNumericLevel(factionRecord);

        assertEquals(7, unitRecord.calcAvailability(availabilityRating, 4, 5, 3025));
        assertEquals(8, unitRecord.calcAvailability(availabilityRating, 4, 5, 3026));
        assertEquals(2, unitRecord.calcAvailability(availabilityRating, 0, 5, 3024));
    }

    @Test
    void parsesStartYearModifier() {
        AvailabilityRating availabilityRating = new AvailabilityRating("Test Unit", 3025, "FS:5:3030");

        assertEquals(3030, availabilityRating.getStartYear());
        assertEquals("FS:5:3030", availabilityRating.toString());
    }
}