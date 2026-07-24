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
package megamek.common.loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import megamek.common.battlefieldSupport.BFSAssetType;
import megamek.common.battlefieldSupport.BFSSpecialType;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies that building a {@link MekSummary} from a Battlefield Support Asset does not throw and produces sane fields.
 * This exercises the whole {@code MekSummaryCache.getSummary} path (BV, cost, weight class, armor, movement, ...).
 */
class BFSMekSummaryTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void uninitializedSummaryIsNotAnAsset() {
        assertFalse(new MekSummary().isBattlefieldSupportAsset());
    }

    @Test
    void buildsSummaryFromBfsFileWithoutThrowing() {
        MekSummary summary = MekSummaryCache.getSummaryFromFile(
              new File("testresources/data/mekfiles/Maxim Heavy Hover Transport.bfs"));

        assertNotNull(summary, "Summary should be built for a .bfs asset file");
        assertEquals("Maxim Heavy Hover Transport", summary.getChassis());
        assertEquals(UnitType.getTypeName(UnitType.BATTLEFIELD_SUPPORT_ASSET), summary.getUnitType());
        assertTrue(summary.isBattlefieldSupportAsset());
        // BV is derived from BSP (23) * 20.
        assertEquals(23 * 20, summary.getBV());
        // The Veteran cost (BSP 27) is stored separately as Veteran BV (27 * 20).
        assertEquals(27 * 20, summary.getBfsVeteranBv());
    }

    @Test
    void regularOnlyAssetHasNoVeteranBv() {
        MekSummary summary = MekSummaryCache.getSummaryFromFile(
              new File("testresources/data/mekfiles/Mobile Long Tom LT-MOB-25.bfs"));

        assertNotNull(summary);
        assertTrue(summary.isBattlefieldSupportAsset());
        // This asset is Regular-only, so it has no Veteran BV.
        assertEquals(0, summary.getBfsVeteranBv());
    }

    @Test
    void cachesAssetStatsForAdvancedSearch() {
        MekSummary summary = MekSummaryCache.getSummaryFromFile(
              new File("testresources/data/mekfiles/Maxim Heavy Hover Transport.bfs"));

        assertNotNull(summary);
        assertEquals(BFSAssetType.VEHICLE, summary.getBfsAssetType());
        assertEquals("Maxim", summary.getBfsCardTitle());
        assertEquals("Hover Transport", summary.getBfsCardSubtitle());
        assertEquals(EntityMovementMode.HOVER, summary.getBfsMovementMode());
        assertEquals(8, summary.getBfsMp());
        assertEquals(3, summary.getBfsTmm());
        assertEquals(6, summary.getBfsSkill());
        // Damage is 5x4, so the cached total is 20.
        assertNotNull(summary.getBfsDamage());
        assertEquals(20, summary.getBfsDamage().total());
        assertEquals(7, summary.getBfsDestroyCheck());
        assertEquals(5, summary.getBfsThreshold());
        assertEquals(23, summary.getBfsBsp());
        // Range 3/6/9 is a numeric range with long band 9.
        assertNotNull(summary.getBfsRange());
        assertFalse(summary.getBfsRange().isKeyword());
        assertEquals(9, summary.getBfsRange().longRange());
        // APC1 and IF2 resolve to the APC and Indirect Fire Specials.
        assertTrue(summary.getBfsSpecials().contains(BFSSpecialType.APC));
        assertTrue(summary.getBfsSpecials().contains(BFSSpecialType.INDIRECT_FIRE));
    }

    @Test
    void cachesKeywordRangeAndArtillerySpecial() {
        MekSummary summary = MekSummaryCache.getSummaryFromFile(
              new File("testresources/data/mekfiles/Mobile Long Tom LT-MOB-25.bfs"));

        assertNotNull(summary);
        assertEquals(EntityMovementMode.TRACKED, summary.getBfsMovementMode());
        assertEquals(0, summary.getBfsTmm());
        // No attack, so the cached damage total is 0.
        assertNotNull(summary.getBfsDamage());
        assertEquals(0, summary.getBfsDamage().total());
        assertEquals(66, summary.getBfsBsp());
        // Range is a keyword range (all bands -1).
        assertNotNull(summary.getBfsRange());
        assertTrue(summary.getBfsRange().isKeyword());
        assertTrue(summary.getBfsSpecials().contains(BFSSpecialType.ARTILLERY));
        assertTrue(summary.getBfsSpecials().contains(BFSSpecialType.NO_TURRET));
    }
}
