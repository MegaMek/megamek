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
package megamek.common.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.client.ui.clientGUI.calculationReport.DummyCalculationReport;
import megamek.common.Player;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.enums.MDAugmentationType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Infantry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for MD Augmentation cost calculations in Infantry and Battle Armor units. Verifies that the per-trooper costs
 * from IO are correctly applied.
 */
public class MDAugmentationCostTest {

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
    }

    /**
     * Creates a conventional infantry unit with the specified trooper count and augmentations.
     */
    private Infantry createInfantry(int troopers, String... augmentations) {
        Infantry infantry = new Infantry();
        infantry.setGame(game);
        infantry.setId(1);
        infantry.setChassis("Test Platoon");
        infantry.setModel("Augmented");

        // Initialize crew
        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        infantry.setCrew(crew);

        // Set augmentation options
        for (String augmentation : augmentations) {
            crew.getOptions().getOption(augmentation).setValue(true);
        }

        infantry.setOwner(game.getPlayer(0));

        // Set up infantry structure
        infantry.autoSetInternal();
        infantry.initializeInternal(troopers, Infantry.LOC_INFANTRY);

        return infantry;
    }

    /**
     * Creates a Battle Armor unit with the specified trooper count and augmentations.
     */
    private BattleArmor createBattleArmor(int troopers, String... augmentations) {
        BattleArmor ba = new BattleArmor();
        ba.setGame(game);
        ba.setId(1);
        ba.setChassis("Test BA");
        ba.setModel("Augmented");
        ba.setTroopers(troopers);
        ba.setWeightClass(EntityWeightClass.WEIGHT_MEDIUM);

        // Initialize crew
        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        ba.setCrew(crew);

        // Set augmentation options
        for (String augmentation : augmentations) {
            crew.getOptions().getOption(augmentation).setValue(true);
        }

        ba.setOwner(game.getPlayer(0));

        // Set armor values for each trooper
        for (int i = 1; i <= troopers; i++) {
            ba.initializeArmor(4, i);
        }

        ba.autoSetInternal();

        return ba;
    }

    @Nested
    @DisplayName("MDAugmentationType Enum Tests")
    class MDAugmentationTypeTests {

        @Test
        @DisplayName("Pain Shunt costs 500,000 C-bills")
        void painShuntCost() {
            MDAugmentationType aug = MDAugmentationType.getByOptionName(OptionsConstants.MD_PAIN_SHUNT);
            assertEquals(500000, aug.getCost(), "Pain Shunt should cost 500,000 C-bills");
        }

        @Test
        @DisplayName("TSM Implant costs 2,500,000 C-bills")
        void tsmImplantCost() {
            MDAugmentationType aug = MDAugmentationType.getByOptionName(OptionsConstants.MD_TSM_IMPLANT);
            assertEquals(2500000, aug.getCost(), "TSM Implant should cost 2,500,000 C-bills");
        }

        @Test
        @DisplayName("Suicide Implant costs 250 C-bills")
        void suicideImplantCost() {
            MDAugmentationType aug = MDAugmentationType.getByOptionName(OptionsConstants.MD_SUICIDE_IMPLANTS);
            assertEquals(250, aug.getCost(), "Suicide Implant should cost 250 C-bills");
        }

        @Test
        @DisplayName("All augmentation types can be looked up by option name")
        void allAugmentationsHaveValidLookup() {
            String[] optionNames = {
                  OptionsConstants.MD_PAIN_SHUNT,
                  OptionsConstants.MD_COMM_IMPLANT,
                  OptionsConstants.MD_BOOST_COMM_IMPLANT,
                  OptionsConstants.MD_DERMAL_ARMOR,
                  OptionsConstants.MD_TSM_IMPLANT,
                  OptionsConstants.MD_SUICIDE_IMPLANTS,
                  OptionsConstants.MD_PL_MASC,
                  OptionsConstants.MD_PL_ENHANCED,
                  OptionsConstants.MD_PL_FLIGHT
            };

            for (String optionName : optionNames) {
                MDAugmentationType aug = MDAugmentationType.getByOptionName(optionName);
                assertTrue(aug != null, "Should find augmentation for " + optionName);
                assertTrue(aug.getCost() >= 0, "Cost should be non-negative for " + optionName);
            }
        }
    }

    @Nested
    @DisplayName("Conventional Infantry Cost Tests")
    class InfantryCostTests {

        @Test
        @DisplayName("Infantry with no augmentations has base cost only")
        void infantryNoCostWithoutAugmentations() {
            Infantry withAug = createInfantry(21, OptionsConstants.MD_PAIN_SHUNT);
            Infantry withoutAug = createInfantry(21);

            double costWith = InfantryCostCalculator.calculateCost(withAug, new DummyCalculationReport(), true);
            double costWithout = InfantryCostCalculator.calculateCost(withoutAug, new DummyCalculationReport(), true);

            assertTrue(costWith > costWithout,
                  "Infantry with augmentations should cost more. With: " + costWith + ", Without: " + costWithout);
        }

        @Test
        @DisplayName("Infantry augmentation cost scales with trooper count")
        void infantryAugmentationCostScalesWithTroopers() {
            // Pain Shunt = 500,000 per trooper
            Infantry infantry21 = createInfantry(21, OptionsConstants.MD_PAIN_SHUNT);
            Infantry infantry7 = createInfantry(7, OptionsConstants.MD_PAIN_SHUNT);

            double cost21 = InfantryCostCalculator.calculateCost(infantry21, new DummyCalculationReport(), true);
            double cost7 = InfantryCostCalculator.calculateCost(infantry7, new DummyCalculationReport(), true);

            // The difference should be approximately 14 troopers * 500,000 = 7,000,000
            // Allow for multiplier effects
            double costDifference = cost21 - cost7;
            assertTrue(costDifference > 5000000,
                  "Cost difference should be significant. 21 trooper cost: " + cost21 + ", 7 trooper cost: " + cost7);
        }

        @Test
        @DisplayName("Multiple augmentations stack costs")
        void multipleAugmentationsStackCosts() {
            Infantry singleAug = createInfantry(10, OptionsConstants.MD_PAIN_SHUNT);
            Infantry doubleAug = createInfantry(10, OptionsConstants.MD_PAIN_SHUNT, OptionsConstants.MD_COMM_IMPLANT);

            double costSingle = InfantryCostCalculator.calculateCost(singleAug, new DummyCalculationReport(), true);
            double costDouble = InfantryCostCalculator.calculateCost(doubleAug, new DummyCalculationReport(), true);

            assertTrue(costDouble > costSingle,
                  "Infantry with two augmentations should cost more than one. " +
                        "Single: " + costSingle + ", Double: " + costDouble);
        }
    }

    @Nested
    @DisplayName("Battle Armor Cost Tests")
    class BattleArmorCostTests {

        @Test
        @DisplayName("Battle Armor with augmentations costs more than without")
        void battleArmorWithAugmentationsCostsMore() {
            BattleArmor withAug = createBattleArmor(4, OptionsConstants.MD_PAIN_SHUNT);
            BattleArmor withoutAug = createBattleArmor(4);

            double costWith = BattleArmorCostCalculator.calculateCost(withAug,
                  new DummyCalculationReport(),
                  true,
                  true);
            double costWithout = BattleArmorCostCalculator.calculateCost(withoutAug,
                  new DummyCalculationReport(),
                  true,
                  true);

            assertTrue(costWith > costWithout,
                  "BA with augmentations should cost more. With: " + costWith + ", Without: " + costWithout);
        }

        @Test
        @DisplayName("Battle Armor augmentation cost scales with squad size")
        void battleArmorAugmentationCostScalesWithSquadSize() {
            BattleArmor ba6 = createBattleArmor(6, OptionsConstants.MD_PAIN_SHUNT);
            BattleArmor ba4 = createBattleArmor(4, OptionsConstants.MD_PAIN_SHUNT);

            double cost6 = BattleArmorCostCalculator.calculateCost(ba6, new DummyCalculationReport(), true, true);
            double cost4 = BattleArmorCostCalculator.calculateCost(ba4, new DummyCalculationReport(), true, true);

            assertTrue(cost6 > cost4,
                  "6-trooper BA should cost more than 4-trooper. 6: " + cost6 + ", 4: " + cost4);
        }
    }

    @Nested
    @DisplayName("Tech Availability Tests")
    class TechAvailabilityTests {

        @Test
        @DisplayName("Pain Shunt (TechBase.ALL) is available for both IS and Clan")
        void painShuntAvailableForBothFactions() {
            MDAugmentationType aug = MDAugmentationType.getByOptionName(OptionsConstants.MD_PAIN_SHUNT);
            assertNotNull(aug);

            // Pain Shunt has DATE_ES intro, should be available in any year
            assertTrue(aug.isAvailableIn(3000, false), "Pain Shunt should be available for IS in 3000");
            assertTrue(aug.isAvailableIn(3000, true), "Pain Shunt should be available for Clan in 3000");
            assertTrue(aug.isAvailableIn(2500, false), "Pain Shunt should be available for IS in 2500");
        }

        @Test
        @DisplayName("TSM Implant (TechBase.IS, 3060) is available for IS after intro date")
        void tsmImplantAvailabilityByYear() {
            MDAugmentationType aug = MDAugmentationType.getByOptionName(OptionsConstants.MD_TSM_IMPLANT);
            assertNotNull(aug);

            // TSM Implant intro date is 3060 for IS
            assertFalse(aug.isAvailableIn(3059, false), "TSM should not be available for IS before 3060");
            assertTrue(aug.isAvailableIn(3060, false), "TSM should be available for IS in 3060");
            assertTrue(aug.isAvailableIn(3065, false), "TSM should be available for IS in 3065");
        }

        @Test
        @DisplayName("Prosthetic Glider Wings (3069) not available before intro date")
        void gliderWingsNotAvailableBeforeIntro() {
            MDAugmentationType aug = MDAugmentationType.getByOptionName(OptionsConstants.MD_PL_GLIDER);
            assertNotNull(aug);

            // Glider Wings intro date is 3069
            assertFalse(aug.isAvailableIn(3065, false), "Glider Wings should not be available in 3065");
            assertFalse(aug.isAvailableIn(3068, false), "Glider Wings should not be available in 3068");
            assertTrue(aug.isAvailableIn(3069, false), "Glider Wings should be available in 3069");
            assertTrue(aug.isAvailableIn(3070, false), "Glider Wings should be available in 3070");
        }

        @Test
        @DisplayName("Extraneous Limbs (3068) not available before intro date")
        void extraneousLimbsNotAvailableBeforeIntro() {
            MDAugmentationType aug = MDAugmentationType.getByOptionName(OptionsConstants.MD_PL_EXTRA_LIMBS);
            assertNotNull(aug);

            // Extraneous Limbs intro date is 3068
            assertFalse(aug.isAvailableIn(3065, false), "Extraneous Limbs should not be available in 3065");
            assertFalse(aug.isAvailableIn(3067, false), "Extraneous Limbs should not be available in 3067");
            assertTrue(aug.isAvailableIn(3068, false), "Extraneous Limbs should be available in 3068");
        }

        @Test
        @DisplayName("Improved Enhanced Prosthetics (TechBase.ALL, 2650) available early")
        void improvedEnhancedAvailableEarly() {
            MDAugmentationType aug = MDAugmentationType.getByOptionName(OptionsConstants.MD_PL_I_ENHANCED);
            assertNotNull(aug);

            // Improved Enhanced intro date is 2650
            assertFalse(aug.isAvailableIn(2649, false), "Should not be available before 2650");
            assertTrue(aug.isAvailableIn(2650, false), "Should be available in 2650");
            assertTrue(aug.isAvailableIn(3065, false), "Should be available in 3065");
        }

        @Test
        @DisplayName("All augmentations have valid tech data")
        void allAugmentationsHaveValidTechData() {
            for (MDAugmentationType aug : MDAugmentationType.values()) {
                assertNotNull(aug.getTechAdvancement(),
                      aug.getDisplayName() + " should have tech advancement data");
                assertNotNull(aug.getTechBase(),
                      aug.getDisplayName() + " should have tech base");
                assertTrue(aug.getCost() >= 0,
                      aug.getDisplayName() + " should have non-negative cost");
            }
        }
    }
}
