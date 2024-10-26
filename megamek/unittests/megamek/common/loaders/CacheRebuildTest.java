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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import megamek.common.MekSummaryCache;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CacheRebuildTest {

    /**
     * Tests that every single unit can load successfully.
     */
    @Test
    void testCacheRebuild() {
        File cacheFile = new File(MekSummaryCache.getUnitCacheDir(), MekSummaryCache.FILENAME_UNITS_CACHE);
        if (cacheFile.exists()) {
            assertTrue(cacheFile.delete(), "Couldn't delete cache");
        }

        MekSummaryCache cache = MekSummaryCache.getInstance(true);

        MekSummaryCache.refreshUnitData(true);

        // Make sure no units failed loading
        assertTrue(cache.getFailedFiles().isEmpty());
        // Sanity check to make sure the loader thread didn't fail outright
        assertTrue(cache.getAllMeks().length > 0);
    }
}
