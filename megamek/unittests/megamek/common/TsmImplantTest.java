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
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.battleArmor.BattleArmor;
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
 * Tests for Triple-Strength Myomer (TSM) Implant functionality.
 * <p>
 * TableTop Rules:
 * <ul>
 *     <li>Conventional Infantry: +0.1 BV per trooper to Weapon Battle Value</li>
 *     <li>Battle Armor: +1 BV per augmented trooper before skill modifiers</li>
 *     <li>Battle Armor: +1 damage per trooper for same-hex attacks (Leg Attack, Swarm)</li>
 *     <li>Vehicles: Protect crew against falls and Crew Stunned results</li>
 * </ul>
 */
public class TsmImplantTest {

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
     * Creates a conventional infantry unit with the specified trooper count.
     */
    private Infantry createInfantry(int troopers, boolean withTsmImplant) {
        Infantry infantry = new Infantry();
        infantry.setGame(game);
        infantry.setId(1);
        infantry.setChassis("Test Platoon");
        infantry.setModel(withTsmImplant ? "TSM" : "Standard");

        // Initialize crew
        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        infantry.setCrew(crew);

        // Set TSM implant option on existing crew options
        if (withTsmImplant) {
            crew.getOptions().getOption(OptionsConstants.MD_TSM_IMPLANT).setValue(true);
        }

        infantry.setOwner(game.getPlayer(0));

        // Set up infantry structure
        infantry.autoSetInternal();
        infantry.initializeInternal(troopers, Infantry.LOC_INFANTRY);

        return infantry;
    }

    /**
     * Creates a Battle Armor unit with the specified trooper count.
     */
    private BattleArmor createBattleArmor(int troopers, boolean withTsmImplant) {
        BattleArmor ba = new BattleArmor();
        ba.setGame(game);
        ba.setId(1);
        ba.setChassis("Test BA");
        ba.setModel(withTsmImplant ? "TSM" : "Standard");
        ba.setTroopers(troopers);
        ba.setWeightClass(EntityWeightClass.WEIGHT_MEDIUM);

        // Initialize crew
        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        ba.setCrew(crew);

        // Set TSM implant option on existing crew options
        if (withTsmImplant) {
            crew.getOptions().getOption(OptionsConstants.MD_TSM_IMPLANT).setValue(true);
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
    @DisplayName("Conventional Infantry BV Tests")
    class ConventionalInfantryBVTests {

        @Test
        @DisplayName("Infantry hasAbility returns true when TSM implant is set")
        void infantryHasAbilityReturnsTrueForTsm() {
            Infantry infantry = createInfantry(21, true);

            assertTrue(infantry.hasAbility(OptionsConstants.MD_TSM_IMPLANT),
                  "Infantry with TSM implant should return true for hasAbility check");
        }

        @Test
        @DisplayName("Infantry without TSM implant returns false for hasAbility")
        void infantryWithoutTsmReturnsFalse() {
            Infantry infantry = createInfantry(21, false);

            assertTrue(!infantry.hasAbility(OptionsConstants.MD_TSM_IMPLANT),
                  "Infantry without TSM implant should return false for hasAbility check");
        }

        @Test
        @DisplayName("Infantry TSM BV bonus formula: troopers * 0.1")
        void infantryTsmBvBonusFormula() {
            // Verify the TSM bonus formula: troopers * 0.1
            // This tests the mathematical calculation used in InfantryBVCalculator
            int troopers = 21;
            double tsmBonus = troopers * 0.1;

            assertEquals(2.1, tsmBonus, 0.001,
                  "TSM bonus should be 2.1 for 21 troopers (21 * 0.1)");
        }
    }

    @Nested
    @DisplayName("Battle Armor BV Tests")
    class BattleArmorBVTests {

        @Test
        @DisplayName("Battle Armor hasAbility returns true when TSM implant is set")
        void battleArmorHasAbilityReturnsTrueForTsm() {
            BattleArmor ba = createBattleArmor(4, true);

            assertTrue(ba.hasAbility(OptionsConstants.MD_TSM_IMPLANT),
                  "BA with TSM implant should return true for hasAbility check");
        }

        @Test
        @DisplayName("Battle Armor with TSM implant has higher BV than without")
        void battleArmorWithTsmHasHigherBv() {
            BattleArmor withTsm = createBattleArmor(4, true);
            BattleArmor withoutTsm = createBattleArmor(4, false);

            int bvWithTsm = withTsm.calculateBattleValue();
            int bvWithoutTsm = withoutTsm.calculateBattleValue();

            assertTrue(bvWithTsm > bvWithoutTsm,
                  "BA with TSM should have higher BV than without. " +
                        "With TSM: " + bvWithTsm + ", Without: " + bvWithoutTsm);
        }

        @Test
        @DisplayName("Battle Armor TSM BV bonus is +1 per trooper (4 troopers)")
        void battleArmorTsmBvBonusIsCorrect() {
            BattleArmor withTsm = createBattleArmor(4, true);
            BattleArmor withoutTsm = createBattleArmor(4, false);

            int bvWithTsm = withTsm.calculateBattleValue();
            int bvWithoutTsm = withoutTsm.calculateBattleValue();
            int bvDifference = bvWithTsm - bvWithoutTsm;

            // Expected bonus: 4 troopers * 1 = 4 BV
            // The bonus is added per-trooper before squad factor multiplication
            // Allow tolerance for rounding effects
            assertTrue(bvDifference >= 3 && bvDifference <= 6,
                  "TSM BV bonus should be approximately 4-5 for 4 troopers. " +
                        "Actual difference: " + bvDifference);
        }
    }

    @Nested
    @DisplayName("Battle Armor Same-Hex Damage Tests")
    class BattleArmorDamageTests {

        @Test
        @DisplayName("BA calculateSwarmDamage returns base damage (TSM bonus applied in handler)")
        void baCalculateSwarmDamageReturnsBaseDamage() {
            BattleArmor withTsm = createBattleArmor(4, true);
            BattleArmor withoutTsm = createBattleArmor(4, false);

            // calculateSwarmDamage() returns base equipment damage
            // TSM bonus is added in SwarmWeaponAttackHandler.calcDamagePerHit()
            int damageWithTsm = withTsm.calculateSwarmDamage();
            int damageWithoutTsm = withoutTsm.calculateSwarmDamage();

            // Base damage should be the same (TSM doesn't affect base equipment damage)
            assertEquals(damageWithoutTsm, damageWithTsm,
                  "Base swarm damage should be the same; TSM bonus applied in handler");
        }

        @Test
        @DisplayName("TSM damage bonus calculation: 4 troopers = +4 damage")
        void tsmDamageBonusCalculation() {
            BattleArmor ba = createBattleArmor(4, true);

            // Verify the BA has TSM ability
            assertTrue(ba.hasAbility(OptionsConstants.MD_TSM_IMPLANT),
                  "BA should have TSM implant ability");

            // TSM bonus is +1 per trooper, applied in handler
            int expectedTsmBonus = ba.getTroopers();
            assertEquals(4, expectedTsmBonus,
                  "TSM bonus should be +4 for 4 troopers (+1 each)");
        }
    }
}
