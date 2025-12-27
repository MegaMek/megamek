/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.utils.EntityLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Obsolete quirk functionality including multiple obsolete/reintroduction cycles.
 */
class ObsoleteQuirkTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Nested
    @DisplayName("Obsolete Quirk Parsing Tests")
    class ParsingTests {

        @Test
        @DisplayName("Single obsolete year parses correctly")
        void singleObsoleteYear() {
            Entity entity = EntityLoader.loadFromFile("Exterminator EXT-4A.mtf");
            IOption option = entity.getQuirks().getOption(OptionsConstants.QUIRK_NEG_OBSOLETE);
            option.setValue("2950");

            assertEquals("2950", entity.getObsoleteQuirkValue());
            assertEquals(List.of(2950), entity.getObsoleteYears());
            assertEquals(2950, entity.getObsoleteYear());
            assertFalse(entity.isObsoleteInYear(2949));
            assertTrue(entity.isObsoleteInYear(2950));
            assertTrue(entity.isObsoleteInYear(3000));
        }

        @Test
        @DisplayName("Obsolete with reintroduction parses correctly")
        void obsoleteWithReintroduction() {
            Entity entity = EntityLoader.loadFromFile("Exterminator EXT-4A.mtf");
            IOption option = entity.getQuirks().getOption(OptionsConstants.QUIRK_NEG_OBSOLETE);
            option.setValue("2950,3146");

            assertEquals("2950,3146", entity.getObsoleteQuirkValue());
            assertEquals(List.of(2950, 3146), entity.getObsoleteYears());

            assertFalse(entity.isObsoleteInYear(2949), "Should not be obsolete before first obsolete year");
            assertTrue(entity.isObsoleteInYear(2950), "Should be obsolete at first obsolete year");
            assertTrue(entity.isObsoleteInYear(3000), "Should be obsolete during obsolete period");
            assertTrue(entity.isObsoleteInYear(3145), "Should be obsolete just before reintroduction");
            assertFalse(entity.isObsoleteInYear(3146), "Should not be obsolete at reintroduction year");
            assertFalse(entity.isObsoleteInYear(3200), "Should not be obsolete after reintroduction");
        }

        @Test
        @DisplayName("Multiple obsolete/reintroduction cycles parse correctly")
        void multipleCycles() {
            Entity entity = EntityLoader.loadFromFile("Exterminator EXT-4A.mtf");
            IOption option = entity.getQuirks().getOption(OptionsConstants.QUIRK_NEG_OBSOLETE);
            option.setValue("2950,3146,3200");

            assertEquals("2950,3146,3200", entity.getObsoleteQuirkValue());
            assertEquals(List.of(2950, 3146, 3200), entity.getObsoleteYears());

            assertFalse(entity.isObsoleteInYear(2949), "Should not be obsolete before first obsolete year");
            assertTrue(entity.isObsoleteInYear(2950), "Should be obsolete at first obsolete year");
            assertTrue(entity.isObsoleteInYear(3000), "Should be obsolete during first obsolete period");
            assertFalse(entity.isObsoleteInYear(3146), "Should not be obsolete at reintroduction");
            assertFalse(entity.isObsoleteInYear(3199), "Should not be obsolete before second obsolete year");
            assertTrue(entity.isObsoleteInYear(3200), "Should be obsolete at second obsolete year");
            assertTrue(entity.isObsoleteInYear(3300), "Should be obsolete indefinitely after final obsolete year");
        }

        @Test
        @DisplayName("Empty obsolete value returns empty list")
        void emptyValue() {
            Entity entity = EntityLoader.loadFromFile("Exterminator EXT-4A.mtf");
            IOption option = entity.getQuirks().getOption(OptionsConstants.QUIRK_NEG_OBSOLETE);
            option.setValue("");

            assertEquals("", entity.getObsoleteQuirkValue());
            assertTrue(entity.getObsoleteYears().isEmpty());
            assertEquals(0, entity.getObsoleteYear());
            assertFalse(entity.isObsoleteInYear(3000));
            assertFalse(entity.hasObsoleteQuirk(), "Empty value means no obsolete quirk");
        }

        @Test
        @DisplayName("Unknown marker returns empty year list but hasObsoleteQuirk is true")
        void unknownMarker() {
            Entity entity = EntityLoader.loadFromFile("Exterminator EXT-4A.mtf");
            IOption option = entity.getQuirks().getOption(OptionsConstants.QUIRK_NEG_OBSOLETE);
            option.setValue("unknown");

            assertEquals("unknown", entity.getObsoleteQuirkValue());
            assertTrue(entity.getObsoleteYears().isEmpty(), "Unknown marker should return empty year list");
            assertEquals(0, entity.getObsoleteYear());
            assertFalse(entity.isObsoleteInYear(3000), "Unknown year should not trigger obsolete status");
            assertTrue(entity.hasObsoleteQuirk(), "Unknown marker should still mark quirk as active");
        }
    }

    @Nested
    @DisplayName("Obsolete Repair Modifier Tests")
    class RepairModifierTests {

        @Test
        @DisplayName("Repair modifier increases over time")
        void repairModifierIncreasesOverTime() {
            Entity entity = EntityLoader.loadFromFile("Exterminator EXT-4A.mtf");
            IOption option = entity.getQuirks().getOption(OptionsConstants.QUIRK_NEG_OBSOLETE);
            option.setValue("3000");

            assertEquals(0, entity.getObsoleteRepairModifier(3000), "No modifier at obsolete year");
            assertEquals(0, entity.getObsoleteRepairModifier(3014), "No modifier at 14 years");
            assertEquals(1, entity.getObsoleteRepairModifier(3015), "+1 at 15 years");
            assertEquals(2, entity.getObsoleteRepairModifier(3030), "+2 at 30 years");
            assertEquals(5, entity.getObsoleteRepairModifier(3075), "+5 at 75 years (max)");
            assertEquals(5, entity.getObsoleteRepairModifier(3100), "+5 at 100 years (capped at max)");
        }

        @Test
        @DisplayName("Repair modifier resets after reintroduction")
        void repairModifierResetsAfterReintroduction() {
            Entity entity = EntityLoader.loadFromFile("Exterminator EXT-4A.mtf");
            IOption option = entity.getQuirks().getOption(OptionsConstants.QUIRK_NEG_OBSOLETE);
            option.setValue("3000,3050");

            assertEquals(2, entity.getObsoleteRepairModifier(3030), "+2 during obsolete period");
            assertEquals(0, entity.getObsoleteRepairModifier(3050), "No modifier after reintroduction");
            assertEquals(0, entity.getObsoleteRepairModifier(3100), "No modifier remains after reintroduction");
        }
    }

    @Nested
    @DisplayName("Obsolete Resale Modifier Tests")
    class ResaleModifierTests {

        @Test
        @DisplayName("Resale modifier decreases over time")
        void resaleModifierDecreasesOverTime() {
            Entity entity = EntityLoader.loadFromFile("Exterminator EXT-4A.mtf");
            IOption option = entity.getQuirks().getOption(OptionsConstants.QUIRK_NEG_OBSOLETE);
            option.setValue("3000");

            assertEquals(1.0, entity.getObsoleteResaleModifier(3000), 0.001, "100% at obsolete year");
            assertEquals(1.0, entity.getObsoleteResaleModifier(3019), 0.001, "100% at 19 years");
            assertEquals(0.9, entity.getObsoleteResaleModifier(3020), 0.001, "90% at 20 years");
            assertEquals(0.8, entity.getObsoleteResaleModifier(3040), 0.001, "80% at 40 years");
            assertEquals(0.5, entity.getObsoleteResaleModifier(3100), 0.001, "50% at 100 years (min)");
            assertEquals(0.5, entity.getObsoleteResaleModifier(3200), 0.001, "50% at 200 years (capped at min)");
        }
    }

    @Nested
    @DisplayName("CompositeTechLevel Extinction Range Tests")
    class CompositeTechLevelTests {

        @Test
        @DisplayName("Single obsolete year creates open-ended extinction range")
        void singleObsoleteYearCreatesOpenEndedRange() {
            List<Integer> years = List.of(2950);

            CompositeTechLevel ctl = createTestCompositeTechLevel();
            ctl.setObsoleteYears(years);

            String extinctionRange = ctl.getExtinctionRange();
            assertTrue(extinctionRange.contains("2950"), "Should contain obsolete year");
            assertTrue(extinctionRange.contains("+"), "Should be open-ended");
        }

        @Test
        @DisplayName("Obsolete with reintroduction creates bounded extinction range")
        void obsoleteWithReintroductionCreatesBoundedRange() {
            List<Integer> years = List.of(2950, 3146);

            CompositeTechLevel ctl = createTestCompositeTechLevel();
            ctl.setObsoleteYears(years);

            String extinctionRange = ctl.getExtinctionRange();
            assertTrue(extinctionRange.contains("2950"), "Should contain obsolete year");
            // The reintro year is 3146, so extinction ends at 3145
            assertTrue(extinctionRange.contains("3145"), "Should show end of obsolete period (year before reintro)");
            assertFalse(extinctionRange.contains("3146"), "Should not include reintroduction year in extinction range");
        }

        @Test
        @DisplayName("Multiple cycles create multiple extinction ranges")
        void multipleCyclesCreateMultipleRanges() {
            List<Integer> years = List.of(2950, 3146, 3200);

            CompositeTechLevel ctl = createTestCompositeTechLevel();
            ctl.setObsoleteYears(years);

            String extinctionRange = ctl.getExtinctionRange();
            // Should contain both ranges separated by comma
            assertTrue(extinctionRange.contains("2950"), "Should contain first obsolete year");
            assertTrue(extinctionRange.contains("3200"), "Should contain second obsolete year");
        }

        private CompositeTechLevel createTestCompositeTechLevel() {
            TechAdvancement ta = new TechAdvancement();
            ta.setTechRating(megamek.common.enums.TechRating.C);
            return new CompositeTechLevel(ta, false, false, 2750,
                  megamek.common.enums.Faction.IS);
        }
    }
}
