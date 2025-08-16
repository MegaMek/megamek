/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;

import megamek.common.MekSummaryCache;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CacheRebuildTest {

    /**
     * Tests that every single unit can load successfully.
     */
    @Test
    @Disabled("Invalidated by the data changes in 50.07")
    void testCacheRebuild() {
        File cacheFile = new File(MekSummaryCache.getUnitCacheDir(), MekSummaryCache.FILENAME_UNITS_CACHE);
        if (cacheFile.exists()) {
            assertTrue(cacheFile.delete(), "Couldn't delete cache");
        }

        MekSummaryCache cache = MekSummaryCache.getInstance(true);
        MekSummaryCache.refreshUnitData(true);

        // This is here as a safety measure when this breaks.
        for (Map.Entry<String, String> entry : cache.getFailedFiles().entrySet()) {
            System.out.println("Failed to load " + entry.getKey() + ": " + entry.getValue());
        }

        // Make sure no units failed to load
        assertTrue(cache.getFailedFiles().isEmpty());

        // Sanity check to make sure the loader thread didn't fail outright
        assertTrue(cache.getAllMeks().length > 0);
    }
}
