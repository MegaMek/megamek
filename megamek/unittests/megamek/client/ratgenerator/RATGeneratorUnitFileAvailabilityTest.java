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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Drives the real Force Generator against the test data set to prove that a custom unit declaring availability in its
 * own .mtf file reaches a generated table.
 * <p>
 * The fixtures are testresources/data/forcegenerator/3050.xml, which knows only about the canon Archer, and
 * testresources/data/mekfiles/Grimjack GRM-1A.mtf, a custom unit carrying "availability:LA:5". The Grimjack appears in
 * no era file anywhere, so the only way it can reach a table is through the unit file.
 * </p>
 */
class RATGeneratorUnitFileAvailabilityTest {

    private static final int ERA = 3050;
    /** A custom unit on a chassis the era XML has never heard of. */
    private static final String CUSTOM_UNIT = "Grimjack GRM-1A";
    private static final String CUSTOM_CHASSIS_KEY = "Grimjack[Mek]";
    /** A custom variant of a canon chassis, declaring a faction the era XML never rated for that chassis. */
    private static final String CUSTOM_VARIANT = "Archer ARC-9X";
    /** A custom variant declaring a HIGHER value than canon rates that chassis for, which must not inflate it. */
    private static final String INFLATED_VARIANT = "Archer ARC-8Z";
    private static final String CANON_UNIT = "Archer ARC-2R";
    private static final String CANON_CHASSIS_KEY = "Archer[Mek]";

    private static RATGenerator ratGenerator;

    @BeforeAll
    static void loadForceGeneratorFromTestData() throws Exception {
        ratGenerator = ForceGeneratorTestFixture.loadFromTestData(ERA);
    }

    @AfterAll
    static void clearSharedSingletons() throws Exception {
        ForceGeneratorTestFixture.reset();
    }

    @Test
    void customUnitFileAvailabilityCreatesAChassisRecord() {
        ChassisRecord chassisRecord = ratGenerator.getChassisRecord(CUSTOM_CHASSIS_KEY);

        assertNotNull(chassisRecord, "The unit file should have introduced a chassis the era XML never mentions");
        assertEquals(UnitType.MEK, chassisRecord.getUnitType());
        assertEquals(3049, chassisRecord.getIntroYear());
    }

    @Test
    void customUnitFileAvailabilityReachesBothIndexes() {
        AvailabilityRating chassisRating =
              ratGenerator.findChassisAvailabilityRecord(ERA, CUSTOM_CHASSIS_KEY, "LA", ERA);
        AvailabilityRating modelRating = ratGenerator.findModelAvailabilityRecord(ERA, CUSTOM_UNIT, "LA");

        assertNotNull(chassisRating, "Chassis availability should come from the unit file");
        assertNotNull(modelRating, "Model availability should come from the unit file");
        assertEquals(5, chassisRating.getAvailability());
        assertEquals(5, modelRating.getAvailability());
    }

    @Test
    void missionRolesFromTheUnitFileAreApplied() {
        ModelRecord modelRecord = ratGenerator.getModelRecord(CUSTOM_UNIT);

        assertNotNull(modelRecord);
        assertTrue(modelRecord.getRoles().contains(MissionRole.FIRE_SUPPORT),
              "missionroles:fire_support in the unit file should reach the model record");
    }

    @Test
    void canonUnitCannotDeclareItsOwnAvailability() {
        // The Atlas AS7-D is canon. Its unit file deliberately carries "availability:LA:10", which must be ignored
        // outright: a player editing a canon unit must never be able to shift how canon forces generate
        assertNull(ratGenerator.findModelAvailabilityRecord(ERA, "Atlas AS7-D", "LA"),
              "A canon unit's own file must never feed the Force Generator");
        assertNull(ratGenerator.findChassisAvailabilityRecord(ERA, "Atlas[Mek]", "LA", ERA),
              "A canon unit's own file must not create a chassis rating either");
    }

    @Test
    void canonAvailabilityIsUntouched() {
        AvailabilityRating canonChassisRating =
              ratGenerator.findChassisAvailabilityRecord(ERA, CANON_CHASSIS_KEY, "LA", ERA);

        assertNotNull(canonChassisRating);
        assertEquals(6, canonChassisRating.getAvailability(), "The era XML value for the Archer must not change");
    }

    @Test
    void customVariantGrantsACanonChassisToAFactionCanonNeverRated() {
        // The era XML rates the Archer chassis for LA only. The custom ARC-9X declares CBS:5, and that is the only
        // way the Clan Blood Spirit can end up fielding an Archer at all
        AvailabilityRating clanRating =
              ratGenerator.findChassisAvailabilityRecord(ERA, CANON_CHASSIS_KEY, "CBS", ERA);

        assertNotNull(clanRating, "The unit file should have opened the canon chassis to a new faction");
        assertEquals(5, clanRating.getAvailability());
    }

    @Test
    void customVariantCannotInflateACanonChassisRating() {
        // The ARC-8Z deliberately declares LA:10 against a canon Archer chassis the era XML rates LA:6. If the unit
        // file were allowed to win, Archers would suddenly flood Lyran tables. Canon must hold the line at 6
        AvailabilityRating lyranRating =
              ratGenerator.findChassisAvailabilityRecord(ERA, CANON_CHASSIS_KEY, "LA", ERA);

        assertNotNull(lyranRating);
        assertEquals(6, lyranRating.getAvailability(),
              "A custom variant must never change how common a canon chassis is for a faction canon already rates");
    }

    @Test
    void inflatedCustomVariantShiftsTheVariantSplitButNotTheChassisShare() {
        // The ARC-8Z's LA:10 is not ignored, it just applies where it should: at the model level, where it decides
        // which Archer you get rather than how often an Archer turns up at all
        AvailabilityRating inflatedModelRating =
              ratGenerator.findModelAvailabilityRecord(ERA, INFLATED_VARIANT, "LA");
        AvailabilityRating canonModelRating = ratGenerator.findModelAvailabilityRecord(ERA, CANON_UNIT, "LA");

        assertNotNull(inflatedModelRating, "The custom variant should still be offered to the Lyrans");
        assertNotNull(canonModelRating, "The canon variant should still be offered to the Lyrans");
        assertEquals(10, inflatedModelRating.getAvailability(),
              "The declared value belongs on the model, where it only competes against sibling variants");

        // Model weight is 2^(availability/2), so a 10 outweighs the canon 6 and dominates the split
        assertTrue(inflatedModelRating.getWeight() > canonModelRating.getWeight(),
              "The custom variant should dominate the canon variant within the chassis");
    }

    @Test
    void customVariantIsOnlyOfferedToTheFactionsItNames() {
        // The ARC-9X names CBS and nothing else, so a Lyran Archer must never come out as an ARC-9X
        assertNotNull(ratGenerator.findModelAvailabilityRecord(ERA, CUSTOM_VARIANT, "CBS"));
        assertNull(ratGenerator.findModelAvailabilityRecord(ERA, CUSTOM_VARIANT, "LA"),
              "The custom variant declared CBS only, so it must not leak into Lyran tables");
    }

    @Test
    void customVariantOfACanonChassisIsGeneratedForItsNewFaction() {
        FactionRecord bloodSpirit = ratGenerator.getFaction("CBS");
        assertNotNull(bloodSpirit);

        List<UnitTable.TableEntry> tableEntries = generateMekTable(bloodSpirit);

        assertTrue(containsUnit(tableEntries, CUSTOM_VARIANT),
              "The custom Archer variant should reach a faction that canon never gave the Archer to");
    }

    @Test
    void customUnitIsGeneratedInATable() {
        FactionRecord lyranCommonwealth = ratGenerator.getFaction("LA");
        assertNotNull(lyranCommonwealth, "The test faction data should provide the Lyran Commonwealth");

        List<UnitTable.TableEntry> tableEntries = generateMekTable(lyranCommonwealth);

        assertTrue(containsUnit(tableEntries, CUSTOM_UNIT),
              "The custom Grimjack should appear in a Lyran Mek table, drawn purely from its unit file");
        assertTrue(containsUnit(tableEntries, CANON_UNIT),
              "The canon Archer should still appear alongside it");
    }

    private static List<UnitTable.TableEntry> generateMekTable(FactionRecord factionRecord) {
        return ratGenerator.generateTable(factionRecord,
              UnitType.MEK,
              ERA,
              null,
              null,
              0,
              List.of(),
              List.of(),
              0,
              factionRecord);
    }

    private static boolean containsUnit(List<UnitTable.TableEntry> tableEntries, String unitName) {
        return tableEntries.stream()
              .filter(UnitTable.TableEntry::isUnit)
              .anyMatch(entry -> unitName.equals(entry.getUnitEntry().getName()));
    }
}
