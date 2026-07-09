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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import megamek.common.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CacheRebuildTest {

    @BeforeEach
    void setUp() throws Exception {
        Configuration.setDataDir(new File("testresources/data"));
        resetCacheSingleton();
    }

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

        // This is here as a safety measure when this breaks.
        for (Map.Entry<String, String> entry : cache.getFailedFiles().entrySet()) {
            System.out.println("Failed to load " + entry.getKey() + ": " + entry.getValue());
        }

        // Make sure no units failed to load
        assertTrue(cache.getFailedFiles().isEmpty());

        // Sanity check to make sure the loader thread didn't fail outright
        assertTrue(cache.getAllMeks().length > 0);

        MekSummaryCache.rebuildUnitData(true);

        for (Map.Entry<String, String> entry : cache.getFailedFiles().entrySet()) {
            System.out.println("Failed to rebuild " + entry.getKey() + ": " + entry.getValue());
        }

        assertTrue(cache.getFailedFiles().isEmpty());
        assertTrue(cache.getAllMeks().length > 0);
    }

    @Test
    void testRebuildFromScratchWhenUninitializedDoesNotReadUnitCache() throws Exception {
        File cacheFile = new File(MekSummaryCache.getUnitCacheDir(), MekSummaryCache.FILENAME_UNITS_CACHE);
        if (cacheFile.exists()) {
            assertTrue(cacheFile.delete(), "Couldn't delete cache");
        }

        MekSummaryCache initialLoad = MekSummaryCache.getInstance(false);
        assertTrue(initialLoad.getAllMeks().length > 0);
        assertTrue(cacheFile.exists(), "Expected test setup to create units.cache");

        resetCacheSingleton();

        MekSummaryCache.rebuildUnitData(false);
        MekSummaryCache rebuiltCache = MekSummaryCache.getInstance(false);

        assertTrue(rebuiltCache.getAllMeks().length > 0);
        assertTrue(rebuiltCache.getFailedFiles().isEmpty());
        assertEquals(0, rebuiltCache.getCacheCount());
    }

    @Test
    void testRebuildInterruptsRunningLoadAndStartsReplacementLoad() throws Exception {
        MekSummaryCache cache = queueReplacementLoad(true);

        assertTrue(cache.getAllMeks().length > 0);
        assertTrue(cache.getFailedFiles().isEmpty());
        assertEquals(0, cache.getCacheCount());
    }

    @Test
    void testRefreshInterruptsRunningLoadAndFallsBackToReplacementLoad() throws Exception {
        MekSummaryCache cache = queueReplacementLoad(false);

        assertTrue(cache.getAllMeks().length > 0);
        assertTrue(cache.getFailedFiles().isEmpty());
        assertEquals(0, cache.getCacheCount());
    }

    private void resetCacheSingleton() throws Exception {
        setStaticField("instance", null);
        setStaticField("disposeInstance", false);
    }

    private MekSummaryCache queueReplacementLoad(boolean rebuild) throws Exception {
        MekSummaryCache cache = newCacheInstance();
        Method doneMethod = MekSummaryCache.class.getDeclaredMethod("done");
        doneMethod.setAccessible(true);
        CountDownLatch interruptedLatch = new CountDownLatch(1);

        Thread fakeLoader = new Thread(() -> {
            try {
                Thread.sleep(30_000);
            } catch (InterruptedException ex) {
                interruptedLatch.countDown();
                try {
                    doneMethod.invoke(cache);
                } catch (Exception reflectionException) {
                    throw new RuntimeException(reflectionException);
                }
            }
        }, "Test Mek Cache Loader");

        setStaticField("instance", cache);
        setInstanceField(cache, "initializing", true);
        setInstanceField(cache, "loader", fakeLoader);
        fakeLoader.start();

        if (rebuild) {
            MekSummaryCache.rebuildUnitData(true);
        } else {
            MekSummaryCache.refreshUnitData(true);
        }

        assertTrue(interruptedLatch.await(5, TimeUnit.SECONDS), "Expected the running load to be interrupted");
        return cache;
    }

    private MekSummaryCache newCacheInstance() throws Exception {
        Constructor<MekSummaryCache> constructor = MekSummaryCache.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private void setStaticField(String fieldName, Object value) throws Exception {
        Field field = MekSummaryCache.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    private void setInstanceField(MekSummaryCache cache, String fieldName, Object value) throws Exception {
        Field field = MekSummaryCache.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(cache, value);
    }
}
