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
package megamek.common.units;

import static megamek.common.units.ForceGeneratorAvailability.UNSPECIFIED_YEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ForceGeneratorAvailabilityTest {

    private static final int GRIMJACK_INTRO_YEAR = 3049;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void parsesCodesWithoutYearRange() {
        ForceGeneratorAvailability availability = ForceGeneratorAvailability.parse("FS:5,LA:3");

        assertEquals(UNSPECIFIED_YEAR, availability.startYear());
        assertEquals(UNSPECIFIED_YEAR, availability.endYear());
        assertEquals("FS:5,LA:3", availability.availabilityCodes());
    }

    @Test
    void parsesClosedYearRange() {
        ForceGeneratorAvailability availability = ForceGeneratorAvailability.parse("3067-3085 FS:7,LA:6");

        assertEquals(3067, availability.startYear());
        assertEquals(3085, availability.endYear());
        assertEquals("FS:7,LA:6", availability.availabilityCodes());
    }

    @Test
    void parsesOpenEndedYearRange() {
        ForceGeneratorAvailability availability = ForceGeneratorAvailability.parse("3067- FS:7");

        assertEquals(3067, availability.startYear());
        assertEquals(UNSPECIFIED_YEAR, availability.endYear());
        assertEquals("FS:7", availability.availabilityCodes());
        assertEquals(Integer.MAX_VALUE, availability.effectiveEndYear());
    }

    @Test
    void acceptsAColonAfterTheYearRange() {
        // The codes are full of colons (ZN:8), so a player naturally writes the range with a colon too. QA hit this:
        // a colon separator was not recognised, the range was lost, and the codes read as undated across every era.
        ForceGeneratorAvailability availability = ForceGeneratorAvailability.parse("3056-3061:ZN:8,FS:4");

        assertEquals(3056, availability.startYear());
        assertEquals(3061, availability.endYear());
        assertEquals("ZN:8,FS:4", availability.availabilityCodes());
    }

    @Test
    void parsesOpenStartYearRange() {
        // QA hit this: "-3068 FS:2" means from the unit's own start through 3068. The start year was required, so the
        // range was not recognised and the codes read as undated.
        ForceGeneratorAvailability availability = ForceGeneratorAvailability.parse("-3068 FS:2");

        assertEquals(UNSPECIFIED_YEAR, availability.startYear());
        assertEquals(3068, availability.endYear());
        assertEquals("FS:2", availability.availabilityCodes());
        assertEquals(3049, availability.effectiveStartYear(3049));
    }

    @Test
    void parsesOpenStartRangeWithAColon() {
        ForceGeneratorAvailability availability = ForceGeneratorAvailability.parse("-3068:FS:2,LA:1");

        assertEquals(UNSPECIFIED_YEAR, availability.startYear());
        assertEquals(3068, availability.endYear());
        assertEquals("FS:2,LA:1", availability.availabilityCodes());
    }

    @Test
    void acceptsAColonAfterAnOpenEndedRange() {
        ForceGeneratorAvailability availability = ForceGeneratorAvailability.parse("3068-:PIR:3,Periphery:2");

        assertEquals(3068, availability.startYear());
        assertEquals(UNSPECIFIED_YEAR, availability.endYear());
        assertEquals("PIR:3,Periphery:2", availability.availabilityCodes());
    }

    @Test
    void handlesAFileThatMixesSpaceAndColonSeparators() {
        // The exact QA block: line 1 uses a space, the rest use a colon. Every line must parse the same way.
        List<ForceGeneratorAvailability> entries = ForceGeneratorAvailability.parseAll(List.of(
              "3015-3055 ZN:8",
              "3056-3061:ZN:8,FS:4",
              "3062-3067:ZN:6,FS:6,PIR:3,Periphery:1",
              "3068-3090:ZN:4,FS:4,PIR:3,Periphery:2"), "Republic of Zeon test unit");

        assertEquals(4, entries.size());
        assertEquals(3015, entries.get(0).startYear());
        assertEquals("ZN:8", entries.get(0).availabilityCodes());
        assertEquals(3056, entries.get(1).startYear());
        assertEquals(3061, entries.get(1).endYear());
        assertEquals("ZN:8,FS:4", entries.get(1).availabilityCodes());
        assertEquals(3062, entries.get(2).startYear());
        assertEquals("ZN:6,FS:6,PIR:3,Periphery:1", entries.get(2).availabilityCodes());
        assertEquals(3068, entries.get(3).startYear());
        assertEquals(3090, entries.get(3).endYear());
    }

    @Test
    void parseKeepsRatingAndYearSuffixesIntact() {
        // The codes are handed to AvailabilityRating unchanged, so the +/-, !rating and :year forms must survive
        ForceGeneratorAvailability availability =
              ForceGeneratorAvailability.parse("CJF:5+,CSA!Second Line:2,FS:3:3055");

        assertEquals("CJF:5+,CSA!Second Line:2,FS:3:3055", availability.availabilityCodes());
    }

    @Test
    void parseRejectsBlankLine() {
        assertThrows(IllegalArgumentException.class, () -> ForceGeneratorAvailability.parse("   "));
    }

    @Test
    void constructorRejectsEndYearBeforeStartYear() {
        assertThrows(IllegalArgumentException.class,
              () -> new ForceGeneratorAvailability(3085, 3067, "FS:5"));
    }

    @Test
    void parseAllSkipsMalformedLinesInsteadOfFailing() {
        // A typo must not make the whole unit unloadable
        List<ForceGeneratorAvailability> entries =
              ForceGeneratorAvailability.parseAll(List.of("FS:5", "", "3085-3067 LA:4"), "Grimjack GRM-1A");

        assertEquals(1, entries.size());
        assertEquals("FS:5", entries.getFirst().availabilityCodes());
    }

    @Test
    void unspecifiedStartYearFallsBackToIntroductionYear() {
        ForceGeneratorAvailability availability = ForceGeneratorAvailability.parse("FS:5");

        assertEquals(GRIMJACK_INTRO_YEAR, availability.effectiveStartYear(GRIMJACK_INTRO_YEAR));
    }

    @Test
    void explicitStartYearOverridesIntroductionYear() {
        ForceGeneratorAvailability availability = ForceGeneratorAvailability.parse("3067- FS:5");

        assertEquals(3067, availability.effectiveStartYear(GRIMJACK_INTRO_YEAR));
    }

    @Test
    void undatedEntryAppliesFromIntroductionYearOnward() {
        ForceGeneratorAvailability availability = ForceGeneratorAvailability.parse("FS:5");

        // The 3039 era runs 3039-3048, and the Grimjack does not exist yet
        assertFalse(availability.appliesToEra(3039, 3048, GRIMJACK_INTRO_YEAR));
        // The 3049 era covers its introduction
        assertTrue(availability.appliesToEra(3049, 3054, GRIMJACK_INTRO_YEAR));
        // And it never drops out
        assertTrue(availability.appliesToEra(3145, Integer.MAX_VALUE, GRIMJACK_INTRO_YEAR));
    }

    @Test
    void closedRangeAppliesOnlyToOverlappingEras() {
        ForceGeneratorAvailability availability = ForceGeneratorAvailability.parse("3067-3085 FS:7");

        assertFalse(availability.appliesToEra(3049, 3054, GRIMJACK_INTRO_YEAR));
        // Partial overlap at the start of the range still counts
        assertTrue(availability.appliesToEra(3060, 3067, GRIMJACK_INTRO_YEAR));
        assertTrue(availability.appliesToEra(3075, 3077, GRIMJACK_INTRO_YEAR));
        // Partial overlap at the end of the range still counts
        assertTrue(availability.appliesToEra(3085, 3099, GRIMJACK_INTRO_YEAR));
        assertFalse(availability.appliesToEra(3100, 3130, GRIMJACK_INTRO_YEAR));
    }

    @Test
    void survivesJavaSerialization() throws Exception {
        // Regression guard: this type rides along on Entity, which is serialized into the network packet when a unit
        // is added to the lobby, and into units.cache via MekSummary. A non-serializable field here would surface as
        // NotSerializableException and "cannot add the unit to the lobby"
        ForceGeneratorAvailability original = ForceGeneratorAvailability.parse("3067-3085 FS:7,LA:6");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutput = new ObjectOutputStream(bytes)) {
            objectOutput.writeObject(original);
        }

        ForceGeneratorAvailability restored;
        try (ObjectInputStream objectInput = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (ForceGeneratorAvailability) objectInput.readObject();
        }

        assertEquals(original, restored);
        assertEquals(3067, restored.startYear());
        assertEquals(3085, restored.endYear());
        assertEquals("FS:7,LA:6", restored.availabilityCodes());
    }

    @Test
    void entityCarryingAvailabilitySurvivesJavaSerialization() throws Exception {
        // The lobby case: the whole Entity goes over the wire
        Mek mek = new BipedMek();
        mek.setForceGeneratorAvailability(List.of(ForceGeneratorAvailability.parse("FS:5,LA:3")));
        mek.setMissionRoles("fire_support");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutput = new ObjectOutputStream(bytes)) {
            objectOutput.writeObject(mek);
        }

        Entity restored;
        try (ObjectInputStream objectInput = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (Entity) objectInput.readObject();
        }

        assertEquals(1, restored.getForceGeneratorAvailability().size());
        assertEquals("FS:5,LA:3", restored.getForceGeneratorAvailability().getFirst().availabilityCodes());
        assertEquals("fire_support", restored.getMissionRoles());
    }

    @Test
    void toFileFormatRoundTripsEveryForm() {
        assertRoundTrip("FS:5,LA:3");
        assertRoundTrip("3067-3085 FS:7,LA:6");
        assertRoundTrip("3067- FS:7");
        // Open start: a row that begins at the unit's intro year but ends on a specific year. Dropping the end here is
        // what made the "never stops" box re-check itself on reload.
        assertRoundTrip("-3060 FS:4");
    }

    @Test
    void anOpenStartRangeKeepsItsEndYearThroughTheFile() {
        ForceGeneratorAvailability availability =
              new ForceGeneratorAvailability(UNSPECIFIED_YEAR, 3060, "FS:4");

        assertEquals("-3060 FS:4", availability.toFileFormat(),
              "The end year must survive being written out, or the range reads back as never-ending");
        assertEquals(3060, ForceGeneratorAvailability.parse(availability.toFileFormat()).endYear());
    }

    private static void assertRoundTrip(String line) {
        ForceGeneratorAvailability availability = ForceGeneratorAvailability.parse(line);

        assertEquals(line, availability.toFileFormat());
        assertEquals(availability, ForceGeneratorAvailability.parse(availability.toFileFormat()));
    }
}
