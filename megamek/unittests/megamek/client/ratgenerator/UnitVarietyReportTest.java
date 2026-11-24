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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BooleanSupplier;

import megamek.common.Configuration;
import megamek.common.MMRandom;
import megamek.common.compute.Compute;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.UnitType;
import megamek.common.universe.Factions2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Measures how often generated forces repeat a unit, with and without {@link UnitVariety}, against the real data set.
 *
 * <p>Not part of the normal run. It needs the full staged data directory, which CI does not have, and it points the
 * Force Generator's singletons at that data, which would leak into the other ratgenerator tests sharing the JVM. Run
 * it on its own:</p>
 *
 * <pre>./gradlew :megamek:test --tests megamek.client.ratgenerator.UnitVarietyReportTest
 *     -Dmegamek.forcegen.varietyReport=true</pre>
 *
 * <p>For each case it rolls the same number of forces with variety off and on, each from the same seed, and prints
 * units, distinct chassis and how many units repeat a chassis already in their own formation. It then checks that
 * variety lowered that repeat count overall.</p>
 */
@EnabledIfSystemProperty(named = "megamek.forcegen.varietyReport", matches = "true")
class UnitVarietyReportTest {

    private static final long SEED = 10_475L;
    private static final int ROLLS_PER_CASE = 20;
    private static final long LOAD_TIMEOUT_MILLIS = 300_000;

    /**
     * One kind of force to roll.
     *
     * @param faction  the faction key
     * @param year     the year
     * @param unitType the unit type
     * @param echelon  the echelon, after constant substitution
     */
    private record Case(String faction, int year, int unitType, int echelon) {
        @Override
        public String toString() {
            return faction + " " + year + " " + UnitType.getTypeName(unitType) + " echelon " + echelon;
        }
    }

    private static final List<Case> CASES = List.of(
          new Case("FS", 3025, UnitType.MEK, 4),
          new Case("DC", 3050, UnitType.MEK, 4),
          new Case("LA", 3067, UnitType.MEK, 5),
          new Case("CW", 3060, UnitType.MEK, 5),
          new Case("MERC", 3151, UnitType.MEK, 4),
          new Case("FS", 3050, UnitType.TANK, 4));

    /** What a set of rolls came to. */
    private static final class Tally {
        int units;
        int distinctChassis;
        int repeatsInFormation;

        @Override
        public String toString() {
            return String.format("%4d units, %4d distinct chassis (%.0f%%), %3d repeat a chassis in their formation"
                        + " (%.1f per 12 units)", units, distinctChassis,
                  (units == 0) ? 0.0 : (100.0 * distinctChassis / units), repeatsInFormation,
                  (units == 0) ? 0.0 : (12.0 * repeatsInFormation / units));
        }
    }

    @Test
    void varietyLowersRepeatUnits() throws Exception {
        File dataDir = new File("data");
        assumeTrue(new File(dataDir, "forcegenerator/faction_rules").isDirectory()
              && new File(dataDir, "mekfiles").isDirectory(), "the staged data directory is needed for this report");
        loadProductionData(dataDir);

        Tally allOff = new Tally();
        Tally allOn = new Tally();
        try {
            for (Case force : CASES) {
                RATGenerator.getInstance().loadYear(force.year());
                Tally off = roll(force, false);
                Tally on = roll(force, true);
                add(allOff, off);
                add(allOn, on);
                System.out.printf("%-30s off: %s%n%-30s on:  %s%n", force, off, "", on);
            }
        } finally {
            Compute.setRNG(MMRandom.R_DEFAULT);
        }
        System.out.printf("%-30s off: %s%n%-30s on:  %s%n", "ALL", allOff, "", allOn);

        assertTrue(allOn.units > 0, "the cases generated nothing; check the data directory");
        double offRate = (double) allOff.repeatsInFormation / Math.max(1, allOff.units);
        double onRate = (double) allOn.repeatsInFormation / Math.max(1, allOn.units);
        assertTrue(onRate < offRate, "variety should lower repeats within formations: off " + offRate + ", on " + onRate);
    }

    private static Tally roll(Case force, boolean variety) {
        Compute.setRNG(new SeededRandom(SEED));
        Tally tally = new Tally();
        for (int roll = 0; roll < ROLLS_PER_CASE; roll++) {
            ForceDescriptor descriptor = new ForceDescriptor();
            descriptor.setTopLevel(true);
            descriptor.setFaction(force.faction());
            descriptor.setYear(force.year());
            descriptor.setUnitType(force.unitType());
            descriptor.setEchelon(force.echelon());
            descriptor.setExperience(CrewDescriptor.randomExperienceLevel());
            descriptor.setUnitVarietyBias(variety);
            Ruleset ruleset = Ruleset.findRuleset(descriptor);
            if (ruleset == null) {
                continue;
            }
            ruleset.processRoot(descriptor, null);
            count(descriptor, tally);
        }
        return tally;
    }

    private static void count(ForceDescriptor force, Tally tally) {
        Map<String, Integer> chassisCounts = new HashMap<>();
        Map<ForceDescriptor, Map<String, Integer>> byFormation = new IdentityHashMap<>();
        UnitVariety.forEachElement(force, element -> {
            ModelRecord unit = RATGenerator.getInstance().getModelRecord(element.getModelName());
            String chassis = (unit == null) ? element.getModelName() : unit.getChassis();
            tally.units++;
            chassisCounts.merge(chassis, 1, Integer::sum);
            byFormation.computeIfAbsent(element.getParent(), key -> new HashMap<>()).merge(chassis, 1, Integer::sum);
        });
        tally.distinctChassis += chassisCounts.size();
        for (Map<String, Integer> formation : byFormation.values()) {
            for (int count : formation.values()) {
                tally.repeatsInFormation += count - 1;
            }
        }
    }

    private static void add(Tally total, Tally part) {
        total.units += part.units;
        total.distinctChassis += part.distinctChassis;
        total.repeatsInFormation += part.repeatsInFormation;
    }

    private static void loadProductionData(File dataDir) throws Exception {
        Configuration.setDataDir(dataDir);
        Factions2.setInstance(null);
        Factions2.getInstance(true);
        // Another test class may have built the unit cache from the test data set; drop it so the real units load.
        Field instance = MekSummaryCache.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        Field disposeInstance = MekSummaryCache.class.getDeclaredField("disposeInstance");
        disposeInstance.setAccessible(true);
        disposeInstance.set(null, false);
        MekSummaryCache cache = MekSummaryCache.getInstance();
        waitFor("unit cache", cache::isInitialized);

        RATGenerator ratGenerator = RATGenerator.getInstance();
        waitFor("Force Generator", ratGenerator::isInitialized);
        ratGenerator.reloadFromDir(Configuration.forceGeneratorDir());
        Ruleset.loadData();
    }

    private static void waitFor(String what, BooleanSupplier loaded) throws InterruptedException {
        long deadline = System.currentTimeMillis() + LOAD_TIMEOUT_MILLIS;
        while (!loaded.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException(what + " did not finish loading");
            }
            Thread.sleep(100);
        }
    }

    /** Rolls from a fixed seed, so off and on start from the same dice. */
    private static final class SeededRandom extends MMRandom {
        private final Random random;

        SeededRandom(long seed) {
            random = new Random(seed);
        }

        @Override
        public int randomInt(int maxValue) {
            return random.nextInt(maxValue);
        }

        @Override
        public float randomFloat() {
            return random.nextFloat();
        }
    }
}
