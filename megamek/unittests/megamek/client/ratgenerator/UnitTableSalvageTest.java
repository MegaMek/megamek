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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.loaders.MekSummary;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Salvage is rolled for after a unit is drawn, rather than sitting in the table as weighted entries (#9744).
 *
 * <p>The test data gives LA the Archer and rates it zero for MRG, so an Archer drawn for MRG can only be LA salvage,
 * and an Archer drawn for LA while LA salvages everything from MRG can only mean the salvage chained back to LA.</p>
 */
class UnitTableSalvageTest {

    /** The era the salvage data is set for. */
    private static final int ERA = 3050;
    private static final int NEXT_ERA = 3060;
    /**
     * Tables are drawn for a year in the era, five years in, because salvage is drawn from the source's table five
     * years earlier. A chained draw would reach back another five years, into the 3040 era.
     */
    private static final int TABLE_YEAR = 3055;
    private static final int DRAWS = 200;
    private static final String ARCHER = "Archer ARC-2R";

    private static RATGenerator ratGenerator;
    private static FactionRecord lyrans;
    private static FactionRecord merged;

    @BeforeAll
    static void loadForceGeneratorFromTestData() throws Exception {
        ratGenerator = ForceGeneratorTestFixture.loadFromTestData(ERA);
        // A year between eras reads the next era too
        ratGenerator.loadYear(NEXT_ERA);
        ratGenerator.loadYear(ERA - 10);
        lyrans = ratGenerator.getFaction("LA");
        merged = ratGenerator.getFaction("MRG");
        assertNotNull(lyrans, "Test data should provide faction LA");
        assertNotNull(merged, "Test data should provide faction MRG");
    }

    /** Salvage tables are looked up through the cache, so each test must build them from the salvage it sets. */
    @BeforeEach
    void startFromAnEmptyCache() {
        UnitTable.clearCache();
    }

    @AfterEach
    void clearSalvage() {
        setSalvage(lyrans, 0, null);
        setSalvage(merged, 0, null);
    }

    @AfterAll
    static void clearSharedSingletons() throws Exception {
        ForceGeneratorTestFixture.reset();
    }

    @Test
    void theTableHoldsOnlyTheFactionsOwnUnits() {
        setSalvage(merged, 50, lyrans);

        List<UnitTable.TableEntry> entries = ratGenerator.generateTable(merged, UnitType.MEK, TABLE_YEAR, null, null, 0, null,
              null, 0, null);

        assertFalse(entries.isEmpty(), "Test data should give MRG some Meks of its own");
        for (UnitTable.TableEntry entry : entries) {
            assertTrue(entry.isUnit(), "Every table entry must be a unit, not a salvage faction");
        }
        assertEquals(entries.size(), newMekTable(merged).getNumEntries(), "Salvage must not add rows to the table");
        assertFalse(ownUnits(merged).contains(ARCHER), "MRG rates the Archer zero, so it marks LA salvage");
    }

    @Test
    void theSalvageChanceComesFromTheFactionData() {
        setSalvage(merged, 50, lyrans);

        SalvageChance chance = newMekTable(merged).getSalvageChance();

        assertEquals(50.0, chance.percent(), 0.001);
        assertEquals(Set.of(lyrans), chance.factionWeights().keySet());
        assertEquals(1.0, chance.shareOf(lyrans), 0.001);
    }

    @Test
    void salvageBringsInTheOtherFactionsUnits() {
        setSalvage(merged, 100, lyrans);

        Set<String> drawn = drawNames(newMekTable(merged));

        assertTrue(drawn.contains(ARCHER), "Every unit is LA salvage, so the Archer should turn up; drew " + drawn);
    }

    @Test
    void salvageNeverChainsOnToAThirdTable() {
        // Each salvages everything from the other. A chained draw would bounce from MRG straight back to LA's Archer.
        setSalvage(lyrans, 100, merged);
        setSalvage(merged, 100, lyrans);

        Set<String> drawn = drawNames(newMekTable(lyrans));

        assertTrue(ownUnits(merged).containsAll(drawn),
              "LA salvage comes from MRG's own units only, never back to LA's Archer; drew " + drawn);
    }

    @Test
    void aZeroChanceNeverSalvages() {
        setSalvage(merged, 0, lyrans);

        UnitTable table = newMekTable(merged);

        assertFalse(table.getSalvageChance().hasSalvage());
        assertTrue(ownUnits(merged).containsAll(drawNames(table)));
    }

    @Test
    void noSalvageKeepsEveryDrawOnTheFactionsOwnUnits() {
        setSalvage(merged, 100, lyrans);
        UnitTable table = newMekTable(merged);

        Set<String> drawn = UnitTable.callWithoutSalvage(() -> drawNames(table));

        assertTrue(ownUnits(merged).containsAll(drawn), "With salvage turned off only MRG's own units may be drawn; drew "
              + drawn);
        assertFalse(UnitTable.isSalvageExcluded(), "Salvage must be back on once the generation returns");
    }

    @Test
    void aSourceWithNoUnitsIsDroppedAndTheOwnTableUsed() {
        // SUC fields nothing in the test data, so its salvage table is empty
        setSalvage(merged, 100, ratGenerator.getFaction("SUC"));
        UnitTable table = newMekTable(merged);

        assertTrue(ownUnits(merged).containsAll(drawNames(table)));
        assertFalse(table.getSalvageChance().hasSalvage(), "The empty source should have been dropped");
    }

    @Test
    void noSalvageOnTheTopForceCoversEverythingUnderIt() {
        ForceDescriptor top = new ForceDescriptor();
        ForceDescriptor lance = new ForceDescriptor();
        lance.setParent(top);
        ForceDescriptor unit = new ForceDescriptor();
        unit.setParent(lance);

        assertFalse(unit.isSalvageExcludedInTree());
        top.setSalvageExcluded(true);
        assertTrue(unit.isSalvageExcludedInTree(), "A re-rolled unit must keep the force's no-salvage setting");
    }

    @Test
    void innerSphereClanSalvageStillCountsTowardTheClanShare() {
        assertTrue(merged.isClan(), "Test data should make MRG a Clan");
        assertFalse(lyrans.isClan());
        // 10% salvage, three quarters of it from a Clan and the rest from the Inner Sphere, on a table weighing 100
        SalvageChance chance = new SalvageChance(10.0, Map.of(merged, 3.0, lyrans, 1.0));

        assertEquals(7.5, RATGenerator.clanSalvageWeight(lyrans, chance, List.of(60.0, 40.0)), 0.001,
              "As when salvage sat in the table: 10% of 100, three quarters of it Clan");
    }

    @Test
    void aClanFactionsSalvageIsNotCountedAsExtraClanTech() {
        SalvageChance chance = new SalvageChance(10.0, Map.of(lyrans, 1.0, merged, 1.0));

        assertEquals(0.0, RATGenerator.clanSalvageWeight(merged, chance, List.of(100.0)), 0.001);
    }

    @Test
    void theSalvageChanceDropsEmptySourcesAndClampsThePercent() {
        SalvageChance chance = new SalvageChance(150.0, Map.of(lyrans, 3.0, merged, 0.0));

        assertEquals(100.0, chance.percent(), 0.001);
        assertEquals(Set.of(lyrans), chance.factionWeights().keySet());
        assertEquals(1.0, chance.shareOf(lyrans), 0.001);
        assertEquals(0.0, chance.shareOf(merged), 0.001);
    }

    /**
     * Sets a faction's salvage for the test era.
     *
     * @param faction the faction to change
     * @param percent the chance a drawn unit is salvage
     * @param source  the one faction salvage comes from, or {@code null} for none
     */
    private static void setSalvage(FactionRecord faction, int percent, FactionRecord source) {
        faction.setPctSalvage(ERA, percent);
        for (String key : new ArrayList<>(faction.getSalvage(ERA).keySet())) {
            faction.removeSalvage(ERA, key);
        }
        if (source != null) {
            faction.setSalvage(ERA, source.getKey(), 1);
        }
    }

    /** Builds a Mek table outside the shared cache, so each test sees the salvage it set. */
    private static UnitTable newMekTable(FactionRecord faction) {
        return new UnitTable(new Parameters(faction, UnitType.MEK, TABLE_YEAR, null, null, 0, null, null, 0, faction));
    }

    /** @return the names of the units in a faction's own Mek table for the test year */
    private static Set<String> ownUnits(FactionRecord faction) {
        Set<String> names = new HashSet<>();
        UnitTable table = newMekTable(faction);
        for (int index = 0; index < table.getNumEntries(); index++) {
            names.add(table.getMekSummary(index).getName());
        }
        return names;
    }

    private static Set<String> drawNames(UnitTable table) {
        Set<String> names = new HashSet<>();
        List<MekSummary> units = table.generateUnits(DRAWS);
        for (MekSummary unit : units) {
            names.add(unit.getName());
        }
        return names;
    }
}
