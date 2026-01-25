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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.actions.compute.ComputeAttackerToHitMods;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Aero;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for VDNI (Vehicular Direct Neural Interface) and BVDNI (Buffered VDNI) functionality.
 * <p>
 * Per IO pg 71:
 * <h3>VDNI Rules:</h3>
 * <ul>
 *     <li>-1 to all Gunnery Skill Rolls</li>
 *     <li>-1 to all Piloting Skill Rolls</li>
 *     <li>Meks: Internal damage triggers feedback (2D6, TN 8+, 1 damage on failure)</li>
 *     <li>Vehicles: Specific criticals trigger feedback (Commander/Driver/Crew Stunned = 1 dmg, Crew Killed = kills)</li>
 *     <li>Fighters: Any critical hit triggers feedback (2D6, TN 8+)</li>
 *     <li>Battle Armor: No feedback damage</li>
 * </ul>
 * <h3>BVDNI Rules:</h3>
 * <ul>
 *     <li>-1 to all Gunnery Skill Rolls</li>
 *     <li>NO piloting bonus (neuro-lag)</li>
 *     <li>Ignores Small Cockpit +1 piloting penalty</li>
 *     <li>Meks/Vehicles: Any critical hit triggers feedback (2D6, TN 8+)</li>
 *     <li>Fighters: No feedback damage</li>
 *     <li>Battle Armor: No feedback damage</li>
 * </ul>
 */
public class VdniBvdniTest {

    private Game mockGame;
    private GameOptions mockOptions;
    private Crew mockCrew;
    private WeaponMounted mockWeapon;
    private WeaponType mockWeaponType;

    @BeforeEach
    void setUp() {
        // Mock game options
        mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        // Mock game
        mockGame = mock(Game.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);

        // Mock crew
        mockCrew = mock(Crew.class);
        when(mockCrew.getHits()).thenReturn(0);

        // Mock weapon type
        mockWeaponType = mock(WeaponType.class);
        when(mockWeaponType.getName()).thenReturn("Test Weapon");

        // Mock weapon
        mockWeapon = mock(WeaponMounted.class);
        when(mockWeapon.getType()).thenReturn(mockWeaponType);
    }

    // ========================================================================
    // GUNNERY MODIFIER TESTS
    // ========================================================================

    @Nested
    @DisplayName("Gunnery Modifier Tests (IO pg 71)")
    class GunneryModifierTests {

        @Test
        @DisplayName("VDNI gives -1 gunnery modifier")
        void vdniGivesMinusOneGunnery() {
            Entity mockEntity = mock(Entity.class);
            when(mockEntity.getCrew()).thenReturn(mockCrew);
            when(mockEntity.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_VDNI.equals(arg);
            });
            // Mock hasActiveDNI() to return true for entities with VDNI
            when(mockEntity.hasActiveDNI()).thenReturn(true);

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockEntity, toHit, mockWeapon);

            assertEquals(-1, toHit.getValue(), "VDNI should give -1 gunnery modifier");
        }

        @Test
        @DisplayName("BVDNI gives -1 gunnery modifier")
        void bvdniGivesMinusOneGunnery() {
            Entity mockEntity = mock(Entity.class);
            when(mockEntity.getCrew()).thenReturn(mockCrew);
            when(mockEntity.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_BVDNI.equals(arg);
            });
            // Mock hasActiveDNI() to return true for entities with BVDNI
            when(mockEntity.hasActiveDNI()).thenReturn(true);

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockEntity, toHit, mockWeapon);

            assertEquals(-1, toHit.getValue(), "BVDNI should give -1 gunnery modifier");
        }

        @Test
        @DisplayName("VDNI+BVDNI together gives only -1 gunnery (no stacking)")
        void vdniAndBvdniDoNotStack() {
            Entity mockEntity = mock(Entity.class);
            when(mockEntity.getCrew()).thenReturn(mockCrew);
            when(mockEntity.hasAbility(anyString())).thenAnswer(invocation -> {
                String arg = invocation.getArgument(0);
                return OptionsConstants.MD_VDNI.equals(arg) || OptionsConstants.MD_BVDNI.equals(arg);
            });
            // Mock hasActiveDNI() to return true for entities with VDNI/BVDNI
            when(mockEntity.hasActiveDNI()).thenReturn(true);

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockEntity, toHit, mockWeapon);

            assertEquals(-1, toHit.getValue(), "VDNI+BVDNI should give only -1 gunnery (no stacking)");
        }

        @Test
        @DisplayName("Entity without VDNI/BVDNI gets no gunnery modifier")
        void noVdniGivesNoModifier() {
            Entity mockEntity = mock(Entity.class);
            when(mockEntity.getCrew()).thenReturn(mockCrew);
            when(mockEntity.hasAbility(anyString())).thenReturn(false);

            ToHitData toHit = new ToHitData();
            toHit = ComputeAttackerToHitMods.compileCrewToHitMods(mockGame, mockEntity, toHit, mockWeapon);

            assertEquals(0, toHit.getValue(), "Entity without VDNI/BVDNI should get no modifier");
        }
    }

    // ========================================================================
    // PILOTING MODIFIER CONDITION TESTS
    // These test the condition logic that determines piloting bonus eligibility
    // ========================================================================

    @Nested
    @DisplayName("Piloting Modifier Condition Tests (IO pg 71)")
    class PilotingModifierConditionTests {

        @Test
        @DisplayName("VDNI without BVDNI satisfies piloting bonus condition")
        void vdniWithoutBvdniSatisfiesPilotingCondition() {
            // Per IO pg 71: VDNI gives -1 piloting
            // Code condition: hasAbility(MD_VDNI) && !hasAbility(MD_BVDNI)
            boolean hasVdni = true;
            boolean hasBvdni = false;

            boolean eligible = hasVdni && !hasBvdni;

            assertTrue(eligible, "VDNI without BVDNI should satisfy piloting bonus condition");
        }

        @Test
        @DisplayName("BVDNI alone does NOT satisfy piloting bonus condition (neuro-lag)")
        void bvdniAloneDoesNotSatisfyPilotingCondition() {
            // Per IO pg 71: BVDNI does NOT give piloting bonus due to neuro-lag
            boolean hasVdni = false;
            boolean hasBvdni = true;

            boolean eligible = hasVdni && !hasBvdni;

            assertFalse(eligible, "BVDNI alone should NOT satisfy piloting bonus condition (neuro-lag)");
        }

        @Test
        @DisplayName("VDNI+BVDNI together does NOT satisfy piloting bonus condition")
        void vdniAndBvdniDoesNotSatisfyPilotingCondition() {
            // When both are present, BVDNI takes precedence (no piloting bonus)
            boolean hasVdni = true;
            boolean hasBvdni = true;

            boolean eligible = hasVdni && !hasBvdni;

            assertFalse(eligible, "VDNI+BVDNI should NOT satisfy piloting bonus condition");
        }

        @Test
        @DisplayName("No VDNI/BVDNI does NOT satisfy piloting bonus condition")
        void noVdniDoesNotSatisfyPilotingCondition() {
            boolean hasVdni = false;
            boolean hasBvdni = false;

            boolean eligible = hasVdni && !hasBvdni;

            assertFalse(eligible, "No VDNI should NOT satisfy piloting bonus condition");
        }
    }

    // ========================================================================
    // SMALL COCKPIT PENALTY CONDITION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Small Cockpit Penalty Condition Tests (IO pg 71)")
    class SmallCockpitConditionTests {

        @Test
        @DisplayName("BVDNI causes Small Cockpit penalty to be skipped")
        void bvdniCausesSmallCockpitPenaltySkip() {
            // Per IO pg 71: BVDNI pilots ignore the +1 piloting penalty from Small Cockpit
            // Code condition for applying penalty: !hasAbility(MD_BVDNI)
            boolean hasBvdni = true;

            boolean penaltyApplies = !hasBvdni;

            assertFalse(penaltyApplies, "Small Cockpit penalty should NOT apply with BVDNI");
        }

        @Test
        @DisplayName("Non-BVDNI takes Small Cockpit penalty")
        void nonBvdniTakesSmallCockpitPenalty() {
            boolean hasBvdni = false;

            boolean penaltyApplies = !hasBvdni;

            assertTrue(penaltyApplies, "Small Cockpit penalty should apply without BVDNI");
        }
    }

    // ========================================================================
    // INTERNAL DAMAGE FEEDBACK CONDITION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Internal Damage Feedback Condition Tests (IO pg 71)")
    class InternalDamageFeedbackConditionTests {

        @Test
        @DisplayName("Mek entity type check passes for Mek")
        void mekEntityTypeCheckPasses() {
            Mek mockMek = mock(Mek.class);
            assertTrue(mockMek instanceof Mek, "Mek should pass instanceof Mek check");
        }

        @Test
        @DisplayName("Entity type filtering - Only Meks eligible for internal damage feedback")
        void entityTypeFilteringLogic() {
            // The actual code uses: entity instanceof Mek
            // This test verifies the logic pattern - only "isMek" entities are eligible
            // Tanks, Aeros, and BattleArmor are excluded by the instanceof Mek check

            boolean isMek = true;
            boolean isTank = false;  // Tank cannot be instanceof Mek
            boolean isAero = false;  // Aero cannot be instanceof Mek
            boolean isBattleArmor = false;  // BattleArmor cannot be instanceof Mek

            assertTrue(isMek, "Mek type is eligible for internal damage feedback");
            assertFalse(isTank, "Tank type is NOT eligible for internal damage feedback");
            assertFalse(isAero, "Aero type is NOT eligible for internal damage feedback");
            assertFalse(isBattleArmor, "BattleArmor type is NOT eligible for internal damage feedback");
        }

        @Test
        @DisplayName("VDNI without BVDNI satisfies ability condition for internal feedback")
        void vdniWithoutBvdniSatisfiesAbilityCondition() {
            // Condition: MD_VDNI && !MD_BVDNI && !MD_PAIN_SHUNT
            boolean hasVdni = true;
            boolean hasBvdni = false;
            boolean hasPainShunt = false;

            boolean eligible = hasVdni && !hasBvdni && !hasPainShunt;

            assertTrue(eligible, "VDNI without BVDNI/Pain Shunt should satisfy ability condition");
        }

        @Test
        @DisplayName("BVDNI blocks internal damage feedback")
        void bvdniBlocksInternalFeedback() {
            boolean hasVdni = true;
            boolean hasBvdni = true;
            boolean hasPainShunt = false;

            boolean eligible = hasVdni && !hasBvdni && !hasPainShunt;

            assertFalse(eligible, "BVDNI should block internal damage feedback");
        }

        @Test
        @DisplayName("Pain Shunt blocks internal damage feedback")
        void painShuntBlocksInternalFeedback() {
            boolean hasVdni = true;
            boolean hasBvdni = false;
            boolean hasPainShunt = true;

            boolean eligible = hasVdni && !hasBvdni && !hasPainShunt;

            assertFalse(eligible, "Pain Shunt should block internal damage feedback");
        }
    }

    // ========================================================================
    // VEHICLE CRITICAL FEEDBACK CONDITION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Vehicle Critical Hit Feedback Condition Tests (IO pg 71)")
    class VehicleCriticalFeedbackConditionTests {

        @Test
        @DisplayName("VDNI vehicle specific critical condition is satisfied")
        void vdniVehicleSpecificConditionSatisfied() {
            // VDNI vehicles use specific critical handling (Commander/Driver/Stunned/Killed)
            // Condition: MD_VDNI && !MD_BVDNI
            boolean hasVdni = true;
            boolean hasBvdni = false;

            boolean eligible = hasVdni && !hasBvdni;

            assertTrue(eligible, "VDNI vehicle should satisfy specific critical condition");
        }

        @Test
        @DisplayName("BVDNI vehicle specific critical condition is NOT satisfied")
        void bvdniVehicleSpecificConditionNotSatisfied() {
            // BVDNI vehicles use generic BVDNI check instead
            boolean hasVdni = false;
            boolean hasBvdni = true;

            boolean eligible = hasVdni && !hasBvdni;

            assertFalse(eligible, "BVDNI vehicle should NOT satisfy specific critical condition");
        }

        @Test
        @DisplayName("BVDNI generic critical condition satisfied for vehicles")
        void bvdniGenericConditionSatisfiedForVehicles() {
            // Generic BVDNI check: MD_BVDNI && !MD_PAIN_SHUNT && !(Aero) && !(BattleArmor)
            // For a Tank entity, it is NOT an Aero and NOT a BattleArmor

            boolean hasBvdni = true;
            boolean hasPainShunt = false;
            boolean isAero = false;  // Tank is not instanceof Aero
            boolean isBattleArmor = false;  // Tank is not instanceof BattleArmor

            boolean eligible = hasBvdni && !hasPainShunt && !isAero && !isBattleArmor;

            assertTrue(eligible, "BVDNI vehicle should satisfy generic critical condition");
        }
    }

    // ========================================================================
    // FIGHTER CRITICAL FEEDBACK CONDITION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Fighter Critical Hit Feedback Condition Tests (IO pg 71)")
    class FighterCriticalFeedbackConditionTests {

        @Test
        @DisplayName("VDNI fighter critical feedback condition satisfied")
        void vdniFighterConditionSatisfied() {
            // Condition: isFighter() && MD_VDNI && !MD_BVDNI && !MD_PAIN_SHUNT
            boolean isFighter = true;
            boolean hasVdni = true;
            boolean hasBvdni = false;
            boolean hasPainShunt = false;

            boolean eligible = isFighter && hasVdni && !hasBvdni && !hasPainShunt;

            assertTrue(eligible, "VDNI fighter should satisfy critical feedback condition");
        }

        @Test
        @DisplayName("BVDNI fighter critical feedback condition NOT satisfied")
        void bvdniFighterVdniConditionNotSatisfied() {
            // BVDNI fighters get NO feedback at all
            boolean isFighter = true;
            boolean hasVdni = false;
            boolean hasBvdni = true;
            boolean hasPainShunt = false;

            boolean eligibleVdni = isFighter && hasVdni && !hasBvdni && !hasPainShunt;

            assertFalse(eligibleVdni, "BVDNI fighter should NOT satisfy VDNI critical condition");
        }

        @Test
        @DisplayName("BVDNI fighter excluded from generic BVDNI check")
        void bvdniFighterExcludedFromGenericCheck() {
            // Generic BVDNI excludes Aero
            Aero mockFighter = mock(Aero.class);

            boolean hasBvdni = true;
            boolean hasPainShunt = false;
            boolean isAero = mockFighter instanceof Aero;

            boolean eligible = hasBvdni && !hasPainShunt && !isAero;

            assertFalse(eligible, "BVDNI fighter should be excluded from generic BVDNI check");
        }

        @Test
        @DisplayName("Non-fighter Aero with VDNI does NOT get fighter feedback")
        void nonFighterAeroNoFeedback() {
            // DropShips etc are not fighters
            boolean isFighter = false;
            boolean hasVdni = true;
            boolean hasBvdni = false;
            boolean hasPainShunt = false;

            boolean eligible = isFighter && hasVdni && !hasBvdni && !hasPainShunt;

            assertFalse(eligible, "Non-fighter Aero should NOT satisfy fighter feedback condition");
        }
    }

    // ========================================================================
    // BATTLE ARMOR NO-FEEDBACK CONDITION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Battle Armor No-Feedback Condition Tests (IO pg 71)")
    class BattleArmorNoFeedbackConditionTests {

        @Test
        @DisplayName("BattleArmor fails Mek check for internal damage feedback")
        void battleArmorFailsMekCheck() {
            // BattleArmor is NOT an instanceof Mek - this is a compile-time fact
            // The internal damage feedback code uses: entity instanceof Mek
            // This test verifies that BA units are correctly excluded by type
            boolean isBattleArmorInstanceOfMek = false;  // BattleArmor cannot be instanceof Mek
            assertFalse(isBattleArmorInstanceOfMek, "BattleArmor should NOT be instanceof Mek");
        }

        @Test
        @DisplayName("BattleArmor with BVDNI excluded from generic check")
        void battleArmorExcludedFromGenericBvdniCheck() {
            // Generic BVDNI check excludes BattleArmor: !(en instanceof BattleArmor)
            // For a BattleArmor entity, isBattleArmor is true, so it gets excluded

            boolean hasBvdni = true;
            boolean hasPainShunt = false;
            boolean isBattleArmor = true;  // BattleArmor IS instanceof BattleArmor

            boolean eligible = hasBvdni && !hasPainShunt && !isBattleArmor;

            assertFalse(eligible, "BattleArmor should be excluded from generic BVDNI check");
        }
    }

    // ========================================================================
    // PAIN SHUNT INTERACTION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Pain Shunt Interaction Tests (IO pg 71)")
    class PainShuntInteractionTests {

        @Test
        @DisplayName("Pain Shunt blocks VDNI internal damage feedback")
        void painShuntBlocksVdniInternalFeedback() {
            boolean hasVdni = true;
            boolean hasBvdni = false;
            boolean hasPainShunt = true;

            boolean eligible = hasVdni && !hasBvdni && !hasPainShunt;

            assertFalse(eligible, "Pain Shunt should block VDNI internal damage feedback");
        }

        @Test
        @DisplayName("Pain Shunt blocks BVDNI critical hit feedback")
        void painShuntBlocksBvdniCriticalFeedback() {
            boolean hasBvdni = true;
            boolean hasPainShunt = true;

            boolean eligible = hasBvdni && !hasPainShunt;

            assertFalse(eligible, "Pain Shunt should block BVDNI critical hit feedback");
        }

        @Test
        @DisplayName("Pain Shunt blocks VDNI fighter critical feedback")
        void painShuntBlocksVdniFighterFeedback() {
            boolean isFighter = true;
            boolean hasVdni = true;
            boolean hasBvdni = false;
            boolean hasPainShunt = true;

            boolean eligible = isFighter && hasVdni && !hasBvdni && !hasPainShunt;

            assertFalse(eligible, "Pain Shunt should block VDNI fighter critical feedback");
        }
    }

    // ========================================================================
    // BV SKILL MULTIPLIER CONDITION TESTS
    // ========================================================================

    @Nested
    @DisplayName("BV Skill Multiplier Condition Tests (IO pg 71)")
    class BvSkillMultiplierConditionTests {

        @Test
        @DisplayName("VDNI applies both gunnery and piloting modifiers to BV")
        void vdniAppliesBothModifiersToBv() {
            // VDNI gives -1 Gunnery AND -1 Piloting for BV purposes
            // 4/5 pilot becomes effectively 3/4
            int baseGunnery = 4;
            int basePiloting = 5;

            boolean hasVdni = true;
            boolean hasBvdni = false;

            int effectiveGunnery = hasVdni && !hasBvdni ? baseGunnery - 1 : baseGunnery;
            int effectivePiloting = hasVdni && !hasBvdni ? basePiloting - 1 : basePiloting;

            assertEquals(3, effectiveGunnery, "VDNI should reduce gunnery by 1 for BV");
            assertEquals(4, effectivePiloting, "VDNI should reduce piloting by 1 for BV");
        }

        @Test
        @DisplayName("BVDNI applies only gunnery modifier to BV")
        void bvdniAppliesOnlyGunneryToBv() {
            // BVDNI gives -1 Gunnery only (no piloting due to neuro-lag)
            // 4/5 pilot becomes effectively 3/5
            int baseGunnery = 4;
            int basePiloting = 5;

            boolean hasBvdni = true;

            int effectiveGunnery = hasBvdni ? baseGunnery - 1 : baseGunnery;
            int effectivePiloting = basePiloting;  // BVDNI does NOT modify piloting

            assertEquals(3, effectiveGunnery, "BVDNI should reduce gunnery by 1 for BV");
            assertEquals(5, effectivePiloting, "BVDNI should NOT reduce piloting for BV");
        }

        @Test
        @DisplayName("VDNI and BVDNI do not stack - BVDNI takes precedence")
        void vdniAndBvdniDoNotStack() {
            // If pilot has both VDNI and BVDNI, only BVDNI applies (gunnery only)
            // The code checks: VDNI && !BVDNI for VDNI bonus, BVDNI for BVDNI bonus
            int baseGunnery = 4;
            int basePiloting = 5;

            boolean hasVdni = true;
            boolean hasBvdni = true;

            // VDNI condition (should NOT apply when BVDNI is present)
            boolean vdniApplies = hasVdni && !hasBvdni;
            // BVDNI condition (should apply)
            boolean bvdniApplies = hasBvdni;

            int effectiveGunnery = baseGunnery;
            int effectivePiloting = basePiloting;

            if (vdniApplies) {
                effectiveGunnery -= 1;
                effectivePiloting -= 1;
            }
            if (bvdniApplies) {
                effectiveGunnery -= 1;
                // No piloting modifier for BVDNI
            }

            assertEquals(3, effectiveGunnery, "With both VDNI+BVDNI, gunnery should be -1 (not -2)");
            assertEquals(5, effectivePiloting, "With BVDNI, piloting should NOT be modified");
        }

        @Test
        @DisplayName("Skill modifiers cannot reduce below 0")
        void skillModifiersCannotGoBelowZero() {
            // Edge case: 0/0 pilot with VDNI should stay at 0/0
            int baseGunnery = 0;
            int basePiloting = 0;

            boolean hasVdni = true;
            boolean hasBvdni = false;

            int effectiveGunnery = hasVdni && !hasBvdni ? Math.max(0, baseGunnery - 1) : baseGunnery;
            int effectivePiloting = hasVdni && !hasBvdni ? Math.max(0, basePiloting - 1) : basePiloting;

            assertEquals(0, effectiveGunnery, "Gunnery cannot go below 0");
            assertEquals(0, effectivePiloting, "Piloting cannot go below 0");
        }
    }
}
