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
package megamek.utilities.ratdata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import megamek.client.ratgenerator.AvailabilityRating;
import org.junit.jupiter.api.Test;

/**
 * Checks the availability code format the website reads: {@code FACTION=CODE}, with {@code @YEAR} when the code starts
 * after its era.
 */
class RatInputExporterTest {

    @Test
    void plainCodeIsFactionEqualsValue() {
        assertEquals("FS=7", RatInputExporter.encodeRating(new AvailabilityRating("Wasp[Mek]", 3039, "FS:7")));
    }

    @Test
    void ratingDynamicSignIsKept() {
        assertEquals("FS=7+", RatInputExporter.encodeRating(new AvailabilityRating("Wasp[Mek]", 3039, "FS:7+")));
        assertEquals("TC=4-", RatInputExporter.encodeRating(new AvailabilityRating("Wasp[Mek]", 3039, "TC:4-")));
    }

    @Test
    void laterStartYearIsAppended() {
        assertEquals("FWL=4@3057",
              RatInputExporter.encodeRating(new AvailabilityRating("Albatross[Mek]", 3055, "FWL:4:3057")));
    }

    @Test
    void perRatingCodesAreWrittenAsTheEraFileHasThem() {
        assertEquals("LA=!A:5!B:3",
              RatInputExporter.encodeRating(new AvailabilityRating("Albatross[Mek]", 3049, "LA!A:5!B:3")));
    }

    @Test
    void ratingsAreSortedByFaction() {
        List<AvailabilityRating> ratings = List.of(new AvailabilityRating("Wasp[Mek]", 3039, "LA:6"),
              new AvailabilityRating("Wasp[Mek]", 3039, "DC:7"), new AvailabilityRating("Wasp[Mek]", 3039, "FS:8"));

        assertEquals(List.of("DC=7", "FS=8", "LA=6"), RatInputExporter.encodeRatings(ratings));
    }
}
