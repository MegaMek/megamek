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
package megamek.common.moves;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;

import megamek.common.CriticalSlot;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscMounted;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ClimbingHelper} utility class (TO:AR p.20).
 */
class ClimbingHelperTest {

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
    }

    /**
     * Creates a BipedMek with all arm actuators functional.
     */
    private Mek createFullyFunctionalMek(double tonnage) {
        Mek mek = spy(new BipedMek());
        mek.setGame(game);
        mek.setId(game.getNextEntityId());
        mek.setChassis("Test");
        mek.setModel("Mek");
        mek.setWeight(tonnage);
        mek.autoSetInternal();

        Crew crew = new Crew(CrewType.SINGLE);
        mek.setCrew(crew);

        // Set up arm actuators for both arms
        setupArmActuators(mek, Mek.LOC_LEFT_ARM);
        setupArmActuators(mek, Mek.LOC_RIGHT_ARM);

        // Default: no carried objects, no clubs
        when(mek.getCarriedObjects()).thenReturn(new HashMap<>());
        when(mek.getClubs()).thenReturn(new ArrayList<>());

        return mek;
    }

    /**
     * Sets up all four arm actuators in the given location.
     */
    private void setupArmActuators(Mek mek, int location) {
        mek.setCritical(location, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_SHOULDER));
        mek.setCritical(location, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_UPPER_ARM));
        mek.setCritical(location, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_LOWER_ARM));
        mek.setCritical(location, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HAND));
    }

    /**
     * Destroys a specific actuator in the given location.
     */
    private void destroyActuator(Mek mek, int location, int actuatorType) {
        for (int i = 0; i < mek.getNumberOfCriticalSlots(location); i++) {
            CriticalSlot slot = mek.getCritical(location, i);
            if ((slot != null) && (slot.getType() == CriticalSlot.TYPE_SYSTEM)
                  && (slot.getIndex() == actuatorType)) {
                slot.setDestroyed(true);
                return;
            }
        }
    }

    @Nested
    @DisplayName("Arm Climb Capability Tests")
    class ArmClimbCapabilityTests {

        @Test
        @DisplayName("Arm with all four actuators functional is climb capable")
        void fullyFunctionalArmIsClimbCapable() {
            Mek mek = createFullyFunctionalMek(50);

            assertTrue(ClimbingHelper.isArmClimbCapable(mek, Mek.LOC_LEFT_ARM),
                  "Left arm with all actuators should be climb capable");
            assertTrue(ClimbingHelper.isArmClimbCapable(mek, Mek.LOC_RIGHT_ARM),
                  "Right arm with all actuators should be climb capable");
        }

        @Test
        @DisplayName("Arm with destroyed hand actuator is NOT climb capable")
        void destroyedHandBlocksClimbing() {
            Mek mek = createFullyFunctionalMek(50);
            destroyActuator(mek, Mek.LOC_LEFT_ARM, Mek.ACTUATOR_HAND);

            assertFalse(ClimbingHelper.isArmClimbCapable(mek, Mek.LOC_LEFT_ARM),
                  "Arm with destroyed hand should not be climb capable");
            assertTrue(ClimbingHelper.isArmClimbCapable(mek, Mek.LOC_RIGHT_ARM),
                  "Undamaged arm should still be climb capable");
        }

        @Test
        @DisplayName("Arm with destroyed shoulder actuator is NOT climb capable")
        void destroyedShoulderBlocksClimbing() {
            Mek mek = createFullyFunctionalMek(50);
            destroyActuator(mek, Mek.LOC_RIGHT_ARM, Mek.ACTUATOR_SHOULDER);

            assertFalse(ClimbingHelper.isArmClimbCapable(mek, Mek.LOC_RIGHT_ARM),
                  "Arm with destroyed shoulder should not be climb capable");
        }

        @Test
        @DisplayName("Arm with destroyed lower arm actuator is NOT climb capable")
        void destroyedLowerArmBlocksClimbing() {
            Mek mek = createFullyFunctionalMek(50);
            destroyActuator(mek, Mek.LOC_LEFT_ARM, Mek.ACTUATOR_LOWER_ARM);

            assertFalse(ClimbingHelper.isArmClimbCapable(mek, Mek.LOC_LEFT_ARM),
                  "Arm with destroyed lower arm actuator should not be climb capable");
        }

        @Test
        @DisplayName("Non-arm locations are never climb capable")
        void nonArmLocationsNotClimbCapable() {
            Mek mek = createFullyFunctionalMek(50);

            assertFalse(ClimbingHelper.isArmClimbCapable(mek, Mek.LOC_LEFT_LEG),
                  "Leg should not be climb capable");
            assertFalse(ClimbingHelper.isArmClimbCapable(mek, Mek.LOC_CENTER_TORSO),
                  "Torso should not be climb capable");
            assertFalse(ClimbingHelper.isArmClimbCapable(mek, Mek.LOC_HEAD),
                  "Head should not be climb capable");
        }
    }

    @Nested
    @DisplayName("Count Climbable Arms Tests")
    class CountClimbableArmsTests {

        @Test
        @DisplayName("Mek with both arms functional has 2 climbable arms")
        void bothArmsFunctional() {
            Mek mek = createFullyFunctionalMek(50);

            assertEquals(2, ClimbingHelper.countClimbableArms(mek),
                  "Mek with both arms functional should have 2 climbable arms");
        }

        @Test
        @DisplayName("Mek with one damaged hand has 1 climbable arm")
        void oneDamagedHand() {
            Mek mek = createFullyFunctionalMek(50);
            destroyActuator(mek, Mek.LOC_RIGHT_ARM, Mek.ACTUATOR_HAND);

            assertEquals(1, ClimbingHelper.countClimbableArms(mek),
                  "Mek with one damaged hand should have 1 climbable arm");
        }

        @Test
        @DisplayName("Mek with both hands damaged has 0 climbable arms")
        void bothHandsDamaged() {
            Mek mek = createFullyFunctionalMek(50);
            destroyActuator(mek, Mek.LOC_LEFT_ARM, Mek.ACTUATOR_HAND);
            destroyActuator(mek, Mek.LOC_RIGHT_ARM, Mek.ACTUATOR_HAND);

            assertEquals(0, ClimbingHelper.countClimbableArms(mek),
                  "Mek with both hands damaged should have 0 climbable arms");
        }
    }

    @Nested
    @DisplayName("Can Climb Tests")
    class CanClimbTests {

        @Test
        @DisplayName("Functional Mek can climb")
        void functionalMekCanClimb() {
            Mek mek = createFullyFunctionalMek(50);

            assertTrue(ClimbingHelper.canClimb(mek),
                  "Functional Mek should be able to climb");
        }

        @Test
        @DisplayName("Prone Mek cannot climb")
        void proneMekCannotClimb() {
            Mek mek = createFullyFunctionalMek(50);
            mek.setProne(true);

            assertFalse(ClimbingHelper.canClimb(mek),
                  "Prone Mek should not be able to climb");
        }

        @Test
        @DisplayName("Shut down Mek cannot climb")
        void shutDownMekCannotClimb() {
            Mek mek = createFullyFunctionalMek(50);
            mek.setShutDown(true);

            assertFalse(ClimbingHelper.canClimb(mek),
                  "Shut down Mek should not be able to climb");
        }

        @Test
        @DisplayName("Mek with no climbable arms cannot climb")
        void noClimbableArmsCannotClimb() {
            Mek mek = createFullyFunctionalMek(50);
            destroyActuator(mek, Mek.LOC_LEFT_ARM, Mek.ACTUATOR_HAND);
            destroyActuator(mek, Mek.LOC_RIGHT_ARM, Mek.ACTUATOR_HAND);

            assertFalse(ClimbingHelper.canClimb(mek),
                  "Mek with no climbable arms should not be able to climb");
        }

        @Test
        @DisplayName("Infantry cannot climb using Mek climbing rules")
        void infantryCannotClimb() {
            Infantry infantry = new Infantry();
            infantry.setGame(game);
            infantry.setId(game.getNextEntityId());

            assertFalse(ClimbingHelper.canClimb(infantry),
                  "Infantry should not be able to use Mek climbing rules");
        }
    }

    @Nested
    @DisplayName("MP Cost Per Level Tests")
    class MPCostTests {

        @Test
        @DisplayName("Two functional hands costs 2 MP per level")
        void twoHandsCost2MP() {
            Mek mek = createFullyFunctionalMek(50);

            assertEquals(ClimbingHelper.MP_COST_TWO_HANDS,
                  ClimbingHelper.getClimbingMPCostPerLevel(mek),
                  "Two functional hands should cost 2 MP per level");
        }

        @Test
        @DisplayName("One functional hand costs 3 MP per level")
        void oneHandCost3MP() {
            Mek mek = createFullyFunctionalMek(50);
            destroyActuator(mek, Mek.LOC_RIGHT_ARM, Mek.ACTUATOR_HAND);

            assertEquals(ClimbingHelper.MP_COST_ONE_HAND,
                  ClimbingHelper.getClimbingMPCostPerLevel(mek),
                  "One functional hand should cost 3 MP per level");
        }
    }

    @Nested
    @DisplayName("Climbing Impossible Reason Tests")
    class ClimbingImpossibleReasonTests {

        @Test
        @DisplayName("Functional Mek has no impossible reason")
        void functionalMekNoReason() {
            Mek mek = createFullyFunctionalMek(50);

            assertNull(ClimbingHelper.getClimbingImpossibleReason(mek),
                  "Functional Mek should have no climbing impossible reason");
        }

        @Test
        @DisplayName("Prone Mek returns prone reason")
        void proneMekReturnsReason() {
            Mek mek = createFullyFunctionalMek(50);
            mek.setProne(true);

            String reason = ClimbingHelper.getClimbingImpossibleReason(mek);
            assertNotNull(reason, "Prone Mek should have a reason");
            assertTrue(reason.contains("prone"),
                  "Reason should mention prone");
        }

        @Test
        @DisplayName("Shut down Mek returns shut down reason")
        void shutDownMekReturnsReason() {
            Mek mek = createFullyFunctionalMek(50);
            mek.setShutDown(true);

            String reason = ClimbingHelper.getClimbingImpossibleReason(mek);
            assertNotNull(reason, "Shut down Mek should have a reason");
            assertTrue(reason.contains("shut down"),
                  "Reason should mention shut down");
        }

        @Test
        @DisplayName("Mek with no arms returns arm reason")
        void noArmsReturnsReason() {
            Mek mek = createFullyFunctionalMek(50);
            destroyActuator(mek, Mek.LOC_LEFT_ARM, Mek.ACTUATOR_HAND);
            destroyActuator(mek, Mek.LOC_RIGHT_ARM, Mek.ACTUATOR_SHOULDER);

            String reason = ClimbingHelper.getClimbingImpossibleReason(mek);
            assertNotNull(reason, "Mek with no functional arms should have a reason");
            assertTrue(reason.contains("arm"),
                  "Reason should mention arm requirements");
        }

        @Test
        @DisplayName("Infantry returns non-Mek reason")
        void infantryReturnsReason() {
            Infantry infantry = new Infantry();
            infantry.setGame(game);

            String reason = ClimbingHelper.getClimbingImpossibleReason(infantry);
            assertNotNull(reason, "Infantry should have a reason");
            assertTrue(reason.contains("Mek"),
                  "Reason should mention only Meks can climb");
        }
    }

    @Nested
    @DisplayName("Climbing Constants Tests")
    class ConstantsTests {

        @Test
        @DisplayName("PSR modifier constants are correct per TO:AR p.20")
        void psrModifiersCorrect() {
            assertEquals(1, ClimbingHelper.CLIMBING_PSR_MODIFIER,
                  "Climbing PSR modifier should be +1");
            assertEquals(2, ClimbingHelper.ONE_ARM_PSR_MODIFIER,
                  "One arm PSR modifier should be +2");
        }

        @Test
        @DisplayName("Target modifier is -2 per TO:AR p.20")
        void targetModifierCorrect() {
            assertEquals(-2, ClimbingHelper.TARGET_CLIMBING_MODIFIER,
                  "Target climbing modifier should be -2");
        }

        @Test
        @DisplayName("MP costs are correct per TO:AR p.20")
        void mpCostsCorrect() {
            assertEquals(2, ClimbingHelper.MP_COST_TWO_HANDS,
                  "Two hands MP cost should be 2");
            assertEquals(3, ClimbingHelper.MP_COST_ONE_HAND,
                  "One hand MP cost should be 3");
        }
    }
}
