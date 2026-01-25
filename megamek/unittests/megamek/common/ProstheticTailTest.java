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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.EnumSet;

import megamek.common.actions.compute.ComputeAttackerToHitMods;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Infantry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests for Prosthetic Tail, Enhanced functionality (IO p.85).
 * <p>
 * TableTop Rules:
 * <ul>
 *     <li>Conventional Infantry only (not Battle Armor)</li>
 *     <li>+0.21 damage per trooper against targets in same hex</li>
 *     <li>+2 to-hit modifier (melee attack penalty)</li>
 *     <li>+0.2 BV per trooper to Offensive Battle Value</li>
 * </ul>
 */
public class ProstheticTailTest {

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
    private Infantry createInfantry(int troopers, boolean withTail) {
        Infantry infantry = new Infantry();
        infantry.setGame(game);
        infantry.setId(1);
        infantry.setChassis("Test Platoon");
        infantry.setModel(withTail ? "Tail Enhanced" : "Standard");

        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        infantry.setCrew(crew);

        if (withTail) {
            crew.getOptions().getOption(OptionsConstants.MD_PL_TAIL).setValue(true);
        }

        infantry.setOwner(game.getPlayer(0));
        infantry.autoSetInternal();
        infantry.initializeInternal(troopers, Infantry.LOC_INFANTRY);

        return infantry;
    }

    /**
     * Creates a Battle Armor unit with the specified trooper count.
     */
    private BattleArmor createBattleArmor(int troopers, boolean withTail) {
        BattleArmor battleArmor = new BattleArmor();
        battleArmor.setGame(game);
        battleArmor.setId(1);
        battleArmor.setChassis("Test BA");
        battleArmor.setModel(withTail ? "Tail Enhanced" : "Standard");
        battleArmor.setTroopers(troopers);
        battleArmor.setWeightClass(EntityWeightClass.WEIGHT_MEDIUM);

        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        battleArmor.setCrew(crew);

        if (withTail) {
            crew.getOptions().getOption(OptionsConstants.MD_PL_TAIL).setValue(true);
        }

        battleArmor.setOwner(game.getPlayer(0));

        for (int i = 1; i <= troopers; i++) {
            battleArmor.initializeArmor(4, i);
        }

        battleArmor.autoSetInternal();

        return battleArmor;
    }

    // =========================================================================
    // BV CALCULATION TESTS - Actually invoke calculateBattleValue()
    // =========================================================================

    @Nested
    @DisplayName("BV Calculation Tests")
    class BvCalculationTests {

        @Test
        @DisplayName("Infantry with tail has higher BV than without (invokes InfantryBVCalculator)")
        void infantryWithTailHasHigherBv() {
            Infantry withTail = createInfantry(21, true);
            Infantry withoutTail = createInfantry(21, false);

            // Actually invoke the BV calculator
            int bvWithTail = withTail.calculateBattleValue();
            int bvWithoutTail = withoutTail.calculateBattleValue();
            int difference = bvWithTail - bvWithoutTail;

            // Expected: 21 troopers * 0.2 = 4.2 BV bonus (before any multipliers)
            assertTrue(bvWithTail > bvWithoutTail,
                  "Infantry with prosthetic tail should have higher BV. " +
                        "With: " + bvWithTail + ", Without: " + bvWithoutTail);
            assertTrue(difference > 0,
                  "BV difference should be positive. Actual difference: " + difference);
        }

        @Test
        @DisplayName("Infantry BV difference scales with trooper count")
        void infantryBvDifferenceScalesWithTroopers() {
            // Test with 10 troopers
            Infantry with10 = createInfantry(10, true);
            Infantry without10 = createInfantry(10, false);
            int diff10 = with10.calculateBattleValue() - without10.calculateBattleValue();

            // Test with 21 troopers
            Infantry with21 = createInfantry(21, true);
            Infantry without21 = createInfantry(21, false);
            int diff21 = with21.calculateBattleValue() - without21.calculateBattleValue();

            // More troopers should mean larger BV difference
            assertTrue(diff21 > diff10,
                  "21 troopers should add more BV than 10. " +
                        "Diff for 10: " + diff10 + ", Diff for 21: " + diff21);
        }

        @Test
        @DisplayName("Battle Armor with tail ability has same BV (tail only for conventional)")
        void battleArmorTailDoesNotAffectBv() {
            BattleArmor withTail = createBattleArmor(4, true);
            BattleArmor withoutTail = createBattleArmor(4, false);

            // Invoke actual BV calculation
            int bvWithTail = withTail.calculateBattleValue();
            int bvWithoutTail = withoutTail.calculateBattleValue();

            assertEquals(bvWithoutTail, bvWithTail,
                  "Battle Armor BV should be unaffected by tail ability " +
                        "(conventional infantry only). With: " + bvWithTail + ", Without: " + bvWithoutTail);
        }
    }

    // =========================================================================
    // TO-HIT MODIFIER TESTS - Actually invoke ComputeAttackerToHitMods
    // =========================================================================

    @Nested
    @DisplayName("To-Hit Modifier Tests")
    class ToHitModifierTests {

        private Game mockGame;
        private GameOptions mockOptions;
        private LosEffects mockLos;
        private WeaponMounted mockWeapon;
        private WeaponType mockWeaponType;

        @BeforeEach
        void setUpMocks() {
            mockOptions = mock(GameOptions.class);
            when(mockOptions.booleanOption(anyString())).thenReturn(false);

            mockGame = mock(Game.class);
            when(mockGame.getOptions()).thenReturn(mockOptions);

            mockLos = mock(LosEffects.class);

            mockWeaponType = mock(WeaponType.class);
            when(mockWeaponType.getName()).thenReturn("Test Weapon");

            mockWeapon = mock(WeaponMounted.class);
            when(mockWeapon.getType()).thenReturn(mockWeaponType);
            when(mockWeapon.getLocation()).thenReturn(0);
        }

        private ToHitData callCompileAttackerToHitMods(Entity attacker, Entity target) {
            try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
                mockedCompute.when(() -> Compute.getAttackerMovementModifier(any(Game.class), anyInt()))
                      .thenReturn(new ToHitData());
                mockedCompute.when(() -> Compute.getProneMods(any(Game.class), any(Entity.class), anyInt()))
                      .thenReturn(new ToHitData());
                mockedCompute.when(() -> Compute.getSecondaryTargetMod(any(Game.class), any(Entity.class), any()))
                      .thenReturn(new ToHitData());

                ToHitData toHit = new ToHitData();
                return ComputeAttackerToHitMods.compileAttackerToHitMods(
                      mockGame,
                      attacker,
                      target,
                      mockLos,
                      toHit,
                      -1,
                      AimingMode.NONE,
                      mockWeaponType,
                      mockWeapon,
                      1,
                      null,
                      EnumSet.noneOf(AmmoType.Munitions.class),
                      false,
                      false,
                      false,
                      false,
                      false
                );
            }
        }

        @Test
        @DisplayName("Infantry with tail in same hex gets +2 to-hit penalty")
        void infantryWithTailInSameHexGetsPlusTwoModifier() {
            // Create infantry with tail at position (5,5)
            Infantry withTail = createInfantry(21, true);
            withTail.setPosition(new Coords(5, 5));

            // Create infantry without tail at same position
            Infantry withoutTail = createInfantry(21, false);
            withoutTail.setPosition(new Coords(5, 5));

            // Create mock target at same position (range 0)
            Entity mockTarget = mock(Entity.class);
            when(mockTarget.getPosition()).thenReturn(new Coords(5, 5));

            // Invoke actual to-hit calculation
            ToHitData resultWithTail = callCompileAttackerToHitMods(withTail, mockTarget);
            ToHitData resultWithoutTail = callCompileAttackerToHitMods(withoutTail, mockTarget);

            int difference = resultWithTail.getValue() - resultWithoutTail.getValue();

            assertEquals(2, difference,
                  "Infantry with tail should have +2 modifier compared to without. " +
                        "With tail: " + resultWithTail.getValue() +
                        ", Without: " + resultWithoutTail.getValue());
        }

        @Test
        @DisplayName("Infantry with tail at different hex gets no penalty")
        void infantryWithTailAtDifferentHexGetsNoModifier() {
            // Create infantry with tail at position (5,5)
            Infantry withTail = createInfantry(21, true);
            withTail.setPosition(new Coords(5, 5));

            // Create infantry without tail at same position
            Infantry withoutTail = createInfantry(21, false);
            withoutTail.setPosition(new Coords(5, 5));

            // Create mock target at DIFFERENT position (not range 0)
            Entity mockTarget = mock(Entity.class);
            when(mockTarget.getPosition()).thenReturn(new Coords(6, 6));

            // Invoke actual to-hit calculation
            ToHitData resultWithTail = callCompileAttackerToHitMods(withTail, mockTarget);
            ToHitData resultWithoutTail = callCompileAttackerToHitMods(withoutTail, mockTarget);

            int difference = resultWithTail.getValue() - resultWithoutTail.getValue();

            assertEquals(0, difference,
                  "Infantry with tail at different hex should have no additional modifier. " +
                        "With tail: " + resultWithTail.getValue() +
                        ", Without: " + resultWithoutTail.getValue());
        }

        @Test
        @DisplayName("Battle Armor with tail ability gets no to-hit penalty")
        void battleArmorWithTailGetsNoModifier() {
            // Create BA with tail at position (5,5)
            BattleArmor withTail = createBattleArmor(4, true);
            withTail.setPosition(new Coords(5, 5));

            // Create BA without tail at same position
            BattleArmor withoutTail = createBattleArmor(4, false);
            withoutTail.setPosition(new Coords(5, 5));

            // Create mock target at same position (range 0)
            Entity mockTarget = mock(Entity.class);
            when(mockTarget.getPosition()).thenReturn(new Coords(5, 5));

            // Invoke actual to-hit calculation
            ToHitData resultWithTail = callCompileAttackerToHitMods(withTail, mockTarget);
            ToHitData resultWithoutTail = callCompileAttackerToHitMods(withoutTail, mockTarget);

            int difference = resultWithTail.getValue() - resultWithoutTail.getValue();

            assertEquals(0, difference,
                  "Battle Armor should not get tail to-hit penalty (conventional infantry only). " +
                        "With tail: " + resultWithTail.getValue() +
                        ", Without: " + resultWithoutTail.getValue());
        }
    }

    // =========================================================================
    // CONVENTIONAL INFANTRY RESTRICTION TESTS
    // =========================================================================

    @Nested
    @DisplayName("Conventional Infantry Restriction Tests")
    class ConventionalInfantryRestrictionTests {

        @Test
        @DisplayName("Conventional infantry is correctly identified")
        void conventionalInfantryIsIdentified() {
            Infantry infantry = createInfantry(21, true);

            assertTrue(infantry.isConventionalInfantry(),
                  "Infantry unit should be identified as conventional infantry");
        }

        @Test
        @DisplayName("Battle Armor is not conventional infantry")
        void battleArmorIsNotConventionalInfantry() {
            BattleArmor battleArmor = createBattleArmor(4, true);

            assertFalse(battleArmor.isConventionalInfantry(),
                  "Battle Armor should not be identified as conventional infantry");
        }

        @Test
        @DisplayName("Conventional infantry can use prosthetic tail")
        void conventionalInfantryCanUseTail() {
            Infantry infantry = createInfantry(21, true);

            boolean canUseTail = infantry.isConventionalInfantry()
                  && infantry.hasAbility(OptionsConstants.MD_PL_TAIL);

            assertTrue(canUseTail,
                  "Conventional infantry with tail ability should be able to use it");
        }

        @Test
        @DisplayName("Battle Armor cannot use prosthetic tail")
        void battleArmorCannotUseTail() {
            BattleArmor battleArmor = createBattleArmor(4, true);

            // BA has the ability set, but isConventionalInfantry() returns false
            boolean canUseTail = battleArmor.isConventionalInfantry()
                  && battleArmor.hasAbility(OptionsConstants.MD_PL_TAIL);

            assertFalse(canUseTail,
                  "Battle Armor should not be able to use prosthetic tail");
        }
    }
}
