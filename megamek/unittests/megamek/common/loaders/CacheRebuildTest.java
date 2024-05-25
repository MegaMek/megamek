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
    @Disabled // Not everything can be built error-free at the moment
    public void testInvalidCanonUnits() {
        MechSummaryCache cache = MechSummaryCache.getInstance(true);

        boolean hasInvalidUnits = false;
        for (MechSummary ms : cache.getAllMechs()) {
            if (ms.isCanon() && ms.getInvalid()) {
                hasInvalidUnits = true;
                System.out.println("Invalid canon unit: " + ms);
            }
        }
        assertFalse(hasInvalidUnits);
    }
}
