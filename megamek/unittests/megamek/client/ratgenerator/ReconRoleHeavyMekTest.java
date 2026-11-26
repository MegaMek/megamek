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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Set;

import megamek.common.loaders.MekSummary;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers which untagged heavy Meks answer a call for the {@link MissionRole#RECON} role.
 *
 * <p>A recon lance limited to heavy and assault weights used to draw only Meks tagged for recon. For a Mercenary
 * force in 3000 that is the Charger CGR-1A1 alone, so every slot rolled one (MegaMek/mekhq#11219). Fast heavy Meks
 * now fill those slots at the same reduced weight fast light and medium Meks already get.</p>
 */
class ReconRoleHeavyMekTest {
    /** Availability and strictness are arbitrary here; only survival and relative weight are under test. */
    private static final double AVAILABILITY = 5.0;
    private static final int YEAR = 3000;
    private static final int STRICTNESS = 3;

    @Test
    @DisplayName("An untagged heavy Mek with Walk 5 answers a call for recon")
    void untaggedFastHeavyMekIsIncluded() {
        // Shaped like a Dragon DRG-1N: 60 tons, Walk 5, no recon tag in the year files.
        ModelRecord fastHeavy = mek(EntityWeightClass.WEIGHT_HEAVY, 5, 5);

        assertNotNull(adjustForRecon(fastHeavy), "a Walk 5 heavy Mek must be able to fill a recon slot");
    }

    @Test
    @DisplayName("An untagged heavy Mek with Walk 4 is still left out")
    void untaggedSlowHeavyMekIsExcluded() {
        // Shaped like a Warhammer WHM-6R: Walk 4, no jump.
        ModelRecord slowHeavy = mek(EntityWeightClass.WEIGHT_HEAVY, 4, 4);

        assertNull(adjustForRecon(slowHeavy), "a Walk 4 heavy Mek is not a scout");
    }

    @Test
    @DisplayName("Jumping does not make a Walk 4 heavy Mek a scout")
    void jumpingDoesNotCountTowardTheWalkThreshold() {
        // Shaped like a Grasshopper GHR-5H: Walk 4, Jump 4. The generator's speed figure counts it as 5, but the
        // Recon Lance asks for Walk 5+, so it must stay out.
        ModelRecord jumpingHeavy = mek(EntityWeightClass.WEIGHT_HEAVY, 4, 5);

        assertNull(adjustForRecon(jumpingHeavy), "the threshold is walking MP, not the jump-adjusted speed");
    }

    @Test
    @DisplayName("Untagged assault Meks are unchanged and still left out")
    void untaggedFastAssaultMekIsExcluded() {
        ModelRecord fastAssault = mek(EntityWeightClass.WEIGHT_ASSAULT, 5, 5);

        assertNull(adjustForRecon(fastAssault), "only heavy Meks were widened; assault Meks need the recon tag");
    }

    @Test
    @DisplayName("A tagged recon heavy Mek still outranks an untagged fast one")
    void taggedReconMekOutranksUntaggedFastHeavy() {
        ModelRecord taggedRecon = mek(EntityWeightClass.WEIGHT_HEAVY, 5, 5, MissionRole.RECON);
        ModelRecord untagged = mek(EntityWeightClass.WEIGHT_HEAVY, 5, 5);

        Double taggedRating = adjustForRecon(taggedRecon);
        Double untaggedRating = adjustForRecon(untagged);
        assertNotNull(taggedRating);
        assertNotNull(untaggedRating);
        assertTrue(taggedRating > untaggedRating,
              "a Mek built for recon must stay more likely than one that only qualifies on speed");
    }

    private static Double adjustForRecon(ModelRecord model) {
        Set<MissionRole> roles = EnumSet.of(MissionRole.RECON);
        return MissionRole.adjustAvailabilityByRole(AVAILABILITY, roles, model, YEAR, STRICTNESS);
    }

    /**
     * A Mek model record with the given weight and movement.
     *
     * @param weightClass the weight class
     * @param walkMp      walking MP
     * @param speed       the generator's speed figure, which counts jumping
     * @param roles       roles the year files would have tagged it with
     */
    private static ModelRecord mek(int weightClass, int walkMp, int speed, MissionRole... roles) {
        MekSummary mekSummary = mock(MekSummary.class);
        when(mekSummary.getWalkMp()).thenReturn(walkMp);

        Set<MissionRole> roleSet = EnumSet.noneOf(MissionRole.class);
        for (MissionRole role : roles) {
            roleSet.add(role);
        }

        ModelRecord model = mock(ModelRecord.class);
        when(model.getUnitType()).thenReturn(UnitType.MEK);
        when(model.getWeightClass()).thenReturn(weightClass);
        when(model.getSpeed()).thenReturn(speed);
        when(model.getMekSummary()).thenReturn(mekSummary);
        when(model.getRoles()).thenReturn(roleSet);
        return model;
    }
}
