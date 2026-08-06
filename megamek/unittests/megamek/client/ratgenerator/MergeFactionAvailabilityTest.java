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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Covers {@link RATGenerator#mergeFactionAvailability(String, List)}, which builds a rating for a faction with several
 * parents (for example the Federated Commonwealth, whose parents are the Federated Suns and the Lyran Commonwealth) by
 * averaging what the parents have.
 */
class MergeFactionAvailabilityTest {

    private static final int ERA = 3050;
    private static final String UNIT = "Archer ARC-2R";
    /** The Lyran rating system is F, D, C, B, A, so C sits at index 2 and A at index 4. */
    private static final int RATING_INDEX_C = 2;
    private static final int RATING_INDEX_A = 4;

    private static RATGenerator ratGenerator;

    @BeforeAll
    static void loadForceGeneratorFromTestData() throws Exception {
        ratGenerator = ForceGeneratorTestFixture.loadFromTestData(ERA);
    }

    @AfterAll
    static void clearSharedSingletons() throws Exception {
        ForceGeneratorTestFixture.reset();
    }

    @Test
    void mergedRatingKeepsTheStartYear() {
        // A parent that only gets the unit from 3055 must not hand its children the unit from 3050
        AvailabilityRating gatedParent = new AvailabilityRating(UNIT, ERA, "IS:5:3055");

        AvailabilityRating merged = ratGenerator.mergeFactionAvailability("LA", List.of(gatedParent));

        assertNotNull(merged);
        assertEquals(3055, merged.getStartYear(),
              "Losing the start year makes the unit available five years early for every multi-parent faction");
    }

    @Test
    void mergedRatingResolvesPerRatingValuesForTheNewFaction() {
        // Per-rating codes index their values against a faction's own equipment rating system, so the merged rating
        // has to resolve them again for the faction it was merged for. Otherwise every lookup returns zero and the
        // unit quietly drops out of the table
        AvailabilityRating perRatingParent = new AvailabilityRating(UNIT, ERA, "IS!C:6!A:8");

        AvailabilityRating merged = ratGenerator.mergeFactionAvailability("LA", List.of(perRatingParent));

        assertNotNull(merged);
        assertEquals(6, merged.getAvailability(RATING_INDEX_C),
              "A merged per-rating value must not read back as 0");
        assertEquals(8, merged.getAvailability(RATING_INDEX_A));
    }

    @Test
    void mergedRatingAveragesTheParents() {
        AvailabilityRating firstParent = new AvailabilityRating(UNIT, ERA, "IS:4");
        AvailabilityRating secondParent = new AvailabilityRating(UNIT, ERA, "IS:8");

        AvailabilityRating merged = ratGenerator.mergeFactionAvailability("LA", List.of(firstParent, secondParent));

        assertNotNull(merged);
        // Averaged on weight (2^(av/2)), not on the raw value, so the result leans toward the more common parent
        assertEquals(6, merged.getAvailability());
    }

    @Test
    void mergingNothingReturnsNothing() {
        assertEquals(null, ratGenerator.mergeFactionAvailability("LA", List.of()));
    }
}
