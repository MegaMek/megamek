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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;

import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the {@link MissionRole#CAVALRY} fallback for 'Meks that carry no cavalry tag.
 *
 * <p>A call for cavalry also takes untagged medium 'Meks with walk 6 or more, heavy 'Meks with walk 5 or more and
 * light 'Meks with walk 5 to 7, each at the same reduced rating. The medium branch subtracted the reduced rating
 * from the rating instead of assigning it, so a fast medium 'Mek got a flat rating whatever its availability, and
 * was dropped entirely when the Force Generator relaxed the role to strictness zero.</p>
 */
class CavalryRoleTest {
    private static final int YEAR = 3067;

    @Test
    @DisplayName("A fast medium 'Mek is cut by the same amount as a fast heavy 'Mek")
    void fastMediumIsReducedLikeFastHeavy() {
        ModelRecord fastMedium = untaggedMek(EntityWeightClass.WEIGHT_MEDIUM, 6);
        ModelRecord fastHeavy = untaggedMek(EntityWeightClass.WEIGHT_HEAVY, 5);

        // At strictness 3 the reduction is 4, so a rating of 6 should become 2 for both.
        Double medium = adjustForCavalry(fastMedium, 6.0, 3);
        Double heavy = adjustForCavalry(fastHeavy, 6.0, 3);

        assertNotNull(medium, "an untagged fast medium 'Mek must answer a call for cavalry");
        assertEquals(2.0, heavy, 1e-9, "the heavy branch is the reference for the reduced rating");
        assertEquals(heavy, medium, 1e-9, "medium and heavy fallbacks must apply the same reduction");
    }

    @Test
    @DisplayName("A fast medium 'Mek keeps its rating at strictness zero")
    void fastMediumSurvivesStrictnessZero() {
        ModelRecord fastMedium = untaggedMek(EntityWeightClass.WEIGHT_MEDIUM, 6);

        Double medium = adjustForCavalry(fastMedium, 6.0, 0);

        assertNotNull(medium, "an untagged fast medium 'Mek must answer a call for cavalry");
        assertTrue(medium > 0, "a rating of zero or less drops the unit from the table");
        assertEquals(6.0, medium, 1e-9, "strictness zero applies no reduction");
    }

    private static Double adjustForCavalry(ModelRecord model, double rating, int strictness) {
        return MissionRole.adjustAvailabilityByRole(rating, EnumSet.of(MissionRole.CAVALRY), model, YEAR,
              strictness);
    }

    /** A 'Mek with no role tags, as most fast 'Meks in the year files are. */
    private static ModelRecord untaggedMek(int weightClass, int walk) {
        ModelRecord model = mock(ModelRecord.class);
        when(model.getRoles()).thenReturn(EnumSet.noneOf(MissionRole.class));
        when(model.getUnitType()).thenReturn(UnitType.MEK);
        when(model.getWeightClass()).thenReturn(weightClass);
        when(model.getSpeed()).thenReturn(walk);
        when(model.getMovementMode()).thenReturn(EntityMovementMode.BIPED);
        return model;
    }
}
