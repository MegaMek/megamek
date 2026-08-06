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

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Vector;

import megamek.common.Configuration;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.universe.Factions2;

/**
 * Brings the Force Generator up against the test data set, and leaves nothing behind.
 * <p>
 * The whole test suite runs in one JVM with no forking, so every test class shares {@link Configuration}'s data
 * directory and the {@link MekSummaryCache} and {@link Factions2} singletons. A Force Generator test therefore cannot
 * assume it runs first, and cannot assume the real data directory exists.
 * </p>
 * <p>
 * Two traps this closes:
 * </p>
 * <ul>
 *     <li>{@link Factions2} loads from a hard-coded {@code data/universe/factions} path and ignores
 *     {@link Configuration#dataDir()}. That directory is a build artifact and is NOT in git, so it is absent on a
 *     clean CI checkout. Without pinning, the faction list comes back empty, every availability code is rejected as
 *     an unknown faction, and every Force Generator test fails on CI while passing locally.</li>
 *     <li>{@link MekSummaryCache} is a singleton. If another test class initialized it first, it holds units from
 *     whatever directory was current then, and the test units are missing.</li>
 * </ul>
 */
final class ForceGeneratorTestFixture {

    /**
     * The test units that count as canon. Must stay sorted, because {@code MekFileParser} looks names up with a binary
     * search. The custom units (Grimjack GRM-1A, Archer ARC-9X, Archer ARC-8Z) are deliberately absent, so that they
     * are treated as custom and their unit-file availability is used.
     */
    private static final List<String> CANON_TEST_UNITS = List.of("Archer ARC-2R", "Atlas AS7-D");

    private ForceGeneratorTestFixture() {
    }

    /**
     * Points the Force Generator at the test data set and loads it.
     *
     * @param era the era to load
     *
     * @return the loaded Force Generator
     *
     * @throws Exception if the unit cache singleton cannot be reset
     */
    static RATGenerator loadFromTestData(int era) throws Exception {
        Configuration.setDataDir(new File("testresources/data"));

        // Factions2 ignores Configuration.dataDir(), so point it at the test factions explicitly
        Factions2.setInstance(null);
        Factions2.getInstance(true);

        // Entity.isCanon() is decided by docs/OfficialUnitList.txt, another build artifact that is absent on CI.
        // Without this the list is empty, nothing counts as canon, and the canon-unit guard cannot be tested.
        // Must be set before the units are parsed, since the canon flag is stamped on during the load.
        MekFileParser.setCanonUnitNames(new Vector<>(CANON_TEST_UNITS));

        // Drop any cache another test class already built, so the test units are the ones that load
        resetMekSummaryCache();
        MekSummaryCache mekSummaryCache = MekSummaryCache.getInstance();
        while (!mekSummaryCache.isInitialized()) {
            Thread.sleep(50);
        }

        RATGenerator ratGenerator = RATGenerator.getInstance();
        ratGenerator.reloadFromDir(Configuration.forceGeneratorDir());
        ratGenerator.loadYear(era);

        return ratGenerator;
    }

    /**
     * Clears the singletons this fixture touched, so later test classes start from a clean slate.
     *
     * @throws Exception if the unit cache singleton cannot be reset
     */
    static void reset() throws Exception {
        Factions2.setInstance(null);
        resetMekSummaryCache();
        // Null restores the lazy default: the next unit load reads the real list again
        MekFileParser.setCanonUnitNames(null);
    }

    /**
     * Nulls out the {@link MekSummaryCache} singleton. It has no public reset, so this reaches in the same way
     * {@code CacheRebuildTest} does.
     *
     * @throws Exception if the fields cannot be reached
     */
    private static void resetMekSummaryCache() throws Exception {
        setStaticField("instance", null);
        setStaticField("disposeInstance", false);
    }

    private static void setStaticField(String fieldName, Object value) throws Exception {
        Field field = MekSummaryCache.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }
}
