/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.loaders;

import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CacheRebuildTest {

    /**
     * Tests that every single unit can load successfully.
     */
    @Test
    @Order(1)
    public void testCacheRebuild() {
        File cacheFile = new File(MechSummaryCache.getUnitCacheDir(), MechSummaryCache.FILENAME_UNITS_CACHE);
        if (cacheFile.exists()) {
            assertTrue(cacheFile.delete(), "Couldn't delete cache");
        }

        MechSummaryCache cache = MechSummaryCache.getInstance(true);

        MechSummaryCache.refreshUnitData(true);

        // Make sure no units failed loading
        assertTrue(cache.getFailedFiles().isEmpty());
        // Sanity check to make sure the loader thread didn't fail outright
        assertTrue(cache.getAllMechs().length > 9000);
    }

    /**
     * Tests that all canon units are valid.
     */
    @Test
    @Order(2)
    @Disabled("Behaves unpredictably and detects several units as being invalid when they show as valid in the full program.")
    public void testInvalidCanonUnits() {
        MechSummaryCache cache = MechSummaryCache.getInstance(true);

        boolean hasInvalidUnits = false;
        for (MechSummary ms : cache.getAllMechs()) {
            // Ideally we would have no invalid canon units, but since we do, skip those units.
            // This allows the test to prevent any *new* invalid units from being added.
            // If any of these units are corrected to become valid, they should be removed from the KNOWN_INVALID_UNITS list.
            if (KNOWN_INVALID_UNITS.contains(ms.getMulId())) {
                if (!ms.getInvalid()) {
                    System.out.println("Unit #" + ms.getMulId() + " " + ms + " expected to be invalid but was not.");
                }
                continue;
            }
            if (ms.isCanon() && ms.getInvalid()) {

                hasInvalidUnits = true;
                System.out.println("Invalid canon unit: #" + ms.getMulId() + " " + ms);
            }
        }
        assertFalse(hasInvalidUnits);
    }

    /*
     * MUL IDs of every unit expected to be invalid.
     */
    private static final List<Integer> KNOWN_INVALID_UNITS = List.of(
        6729, // Gray Death Strike Suit (HarJel)(Sqd4)
        6729, // Gray Death Strike Suit (HarJel)(Sqd4)
        6729, // Gray Death Strike Suit (HarJel)(Sqd4)
              // GDLSS HarJel Sqd4 really does fail thrice for some reason, and is triplicated here to note this fact.
        7387, // Clan Interface Armor (Sqd1)
        5779, // Tsuru VIP Aircraft
        7147, // Drone M-3
        4698, // Nekohono'o (HQ)
        5626, // Czar Dropship
        2849, // Scytha A
        3924, // Centurion CNT-1A
        6812, // Morgenstern MR-1SE
        6811, // Tatsu MIK-OF
        3713, // Aquilla Transport Jumpship
        766,  // Cudgel CDG-1B
        6631, // Zeus-X ZEU-X
        5391, // Wasp LAM WSP-110
        8101, // Uni ATAE-70 ArtilleryMech
        8035, // Svartalfa 3
        4134, // Escape Pod
        4510, // Life Boat
        7195, // Intrepid Assault Craft (2331)
        7196, // Intrepid Assault Craft (2478)
        3684, // Air Car
        4251, // Ground Car
        4395, // Jeep
        1531, // Hi-Scout Drone (PathTrak)
        226,  // Bandit (C) Hovercraft G
        5740, // Ajax Assault Tank C
        5729, // Glaive Medium Tank (MFB)
        3657, // Zugvogel Omni Support Aircraft C
        3658, // Zugvogel Omni Support Aircraft D 'Raubvogel'
        4662, // Moray Heavy Attack Submarine (Original)
        6807, // Zugvogel Omni Support Aircraft F
        4285, // Hector Road Train Tractor
        3864, // Brunel Dump Truck (AC)
        3865, // Brunel Dump Truck (LRM)
        4408, // Jonah Submarine JN-002
        5063, // Silverfin Coastal Cutter
        5556, // Fury Command Tank CX-17
        5554, // Lightning Attack Hovercraft CX-3
        3112, // SturmFeur 'Kalki' Cruise Missile Launcher
        7146, // Capital Drone M-5 'Caspar'
        7148, // Capital Drone M-5C 'Caspar'
        4692, // Naga Destroyer (Caspar II Control Ship)
        3759, // Comet Airliner ACL-800
        7630  // Debbie 'The Warcrime Wagon'
    );
}
