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
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
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
            Infantry infantry = new ConvInfantry();
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
            Infantry infantry = new ConvInfantry();
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

        @Test
        @DisplayName("Dangle constants are correct per TO:AR p.20")
        void dangleConstantsCorrect() {
            assertEquals(2, ClimbingHelper.DANGLE_LEVELS_PER_TURN,
                  "Dangle levels per turn should be 2");
            assertEquals(4, ClimbingHelper.DROP_MP_COST,
                  "Drop MP cost should be 4");
            assertEquals(3, ClimbingHelper.MIN_CLIMBING_LEVELS,
                  "Minimum climbing levels should be 3");
        }
    }

    @Nested
    @DisplayName("Can Dangle Tests")
    class CanDangleTests {

        @Test
        @DisplayName("Mek with two functional arms can dangle")
        void twoArmsMekCanDangle() {
            Mek mek = createFullyFunctionalMek(50);

            assertTrue(ClimbingHelper.canDangle(mek),
                  "Mek with two functional arms should be able to dangle");
        }

        @Test
        @DisplayName("Mek with one arm cannot dangle")
        void oneArmMekCannotDangle() {
            Mek mek = createFullyFunctionalMek(50);
            destroyActuator(mek, Mek.LOC_RIGHT_ARM, Mek.ACTUATOR_HAND);

            assertFalse(ClimbingHelper.canDangle(mek),
                  "Mek with one arm should not be able to dangle");
        }

        @Test
        @DisplayName("Prone Mek cannot dangle")
        void proneMekCannotDangle() {
            Mek mek = createFullyFunctionalMek(50);
            mek.setProne(true);

            assertFalse(ClimbingHelper.canDangle(mek),
                  "Prone Mek should not be able to dangle");
        }

        @Test
        @DisplayName("Shut down Mek cannot dangle")
        void shutDownMekCannotDangle() {
            Mek mek = createFullyFunctionalMek(50);
            mek.setShutDown(true);

            assertFalse(ClimbingHelper.canDangle(mek),
                  "Shut down Mek should not be able to dangle");
        }

        @Test
        @DisplayName("Infantry cannot dangle")
        void infantryCannotDangle() {
            Infantry infantry = new ConvInfantry();
            infantry.setGame(game);

            assertFalse(ClimbingHelper.canDangle(infantry),
                  "Infantry should not be able to dangle");
        }
    }

    @Nested
    @DisplayName("Superheavy Mek Restriction Tests")
    class SuperheavyTests {

        @Test
        @DisplayName("Superheavy Mek cannot climb")
        void superheavyCannotClimb() {
            Mek mek = createFullyFunctionalMek(150);
            when(mek.isSuperHeavy()).thenReturn(true);

            assertFalse(ClimbingHelper.canClimb(mek),
                  "Superheavy Mek should not be able to climb");
        }

        @Test
        @DisplayName("Superheavy Mek cannot dangle")
        void superheavyCannotDangle() {
            Mek mek = createFullyFunctionalMek(150);
            when(mek.isSuperHeavy()).thenReturn(true);

            assertFalse(ClimbingHelper.canDangle(mek),
                  "Superheavy Mek should not be able to dangle");
        }

        @Test
        @DisplayName("Superheavy climb impossible reason mentions superheavy")
        void superheavyClimbReason() {
            Mek mek = createFullyFunctionalMek(150);
            when(mek.isSuperHeavy()).thenReturn(true);

            String reason = ClimbingHelper.getClimbingImpossibleReason(mek);
            assertNotNull(reason, "Superheavy should have a reason");
            assertTrue(reason.toLowerCase().contains("superheavy"),
                  "Reason should mention superheavy");
        }

        @Test
        @DisplayName("Superheavy dangle impossible reason mentions superheavy")
        void superheavyDangleReason() {
            Mek mek = createFullyFunctionalMek(150);
            when(mek.isSuperHeavy()).thenReturn(true);

            String reason = ClimbingHelper.getDangleImpossibleReason(mek);
            assertNotNull(reason, "Superheavy should have a reason");
            assertTrue(reason.toLowerCase().contains("superheavy"),
                  "Reason should mention superheavy");
        }
    }

    @Nested
    @DisplayName("Entity Climbing State Tests")
    class EntityStateTests {

        @Test
        @DisplayName("isClimbing returns true when climbing")
        void isClimbingWhenClimbing() {
            Mek mek = createFullyFunctionalMek(50);
            mek.setClimbing(true);

            assertTrue(mek.isClimbing(),
                  "isClimbing should return true when climbing");
        }

        @Test
        @DisplayName("isClimbing returns true when dangling")
        void isClimbingWhenDangling() {
            Mek mek = createFullyFunctionalMek(50);
            mek.setDangling(true);

            assertTrue(mek.isClimbing(),
                  "isClimbing should return true when dangling (same combat restrictions)");
        }

        @Test
        @DisplayName("isDangling returns false when only climbing")
        void isDanglingFalseWhenClimbing() {
            Mek mek = createFullyFunctionalMek(50);
            mek.setClimbing(true);

            assertFalse(mek.isDangling(),
                  "isDangling should return false when climbing (not dangling)");
        }

        @Test
        @DisplayName("isDangling returns true when dangling")
        void isDanglingTrueWhenDangling() {
            Mek mek = createFullyFunctionalMek(50);
            mek.setDangling(true);

            assertTrue(mek.isDangling(),
                  "isDangling should return true when dangling");
        }
    }

    @Nested
    @DisplayName("Dangle Impossible Reason Tests")
    class DangleImpossibleReasonTests {

        @Test
        @DisplayName("Mek with two arms has no dangle impossible reason")
        void twoArmsNoReason() {
            Mek mek = createFullyFunctionalMek(50);

            assertNull(ClimbingHelper.getDangleImpossibleReason(mek),
                  "Mek with two functional arms should have no dangle impossible reason");
        }

        @Test
        @DisplayName("Mek with one arm returns arm reason")
        void oneArmReturnsReason() {
            Mek mek = createFullyFunctionalMek(50);
            destroyActuator(mek, Mek.LOC_LEFT_ARM, Mek.ACTUATOR_HAND);

            String reason = ClimbingHelper.getDangleImpossibleReason(mek);
            assertNotNull(reason, "Mek with one arm should have a dangle reason");
            assertTrue(reason.contains("arm"),
                  "Reason should mention arm requirements");
        }

        @Test
        @DisplayName("Infantry returns non-Mek reason")
        void infantryReturnsReason() {
            Infantry infantry = new ConvInfantry();
            infantry.setGame(game);

            String reason = ClimbingHelper.getDangleImpossibleReason(infantry);
            assertNotNull(reason, "Infantry should have a dangle reason");
        }
    }

    @Nested
    @DisplayName("getClimbDestinationLevel - top of climbable feature in target hex")
    class GetClimbDestinationLevelTests {

        @Test
        @DisplayName("Bare hex returns the hex level")
        void bareHex() {
            Hex hex = new Hex(3, "", "");
            assertEquals(3, ClimbingHelper.getClimbDestinationLevel(hex),
                  "Plain terrain has no climbable feature; destination is the hex level itself.");
        }

        @Test
        @DisplayName("Building hex returns roof absolute level")
        void buildingHex() {
            Hex hex = new Hex(0, "bldg_elev:5;building:2:80;bldg_cf:80", "");
            assertEquals(5, ClimbingHelper.getClimbDestinationLevel(hex),
                  "A building roof at floor 5 sits at absolute level 5; the dialog measures to it.");
        }

        @Test
        @DisplayName("Building hex on elevated ground stacks bldg_elev on top of level")
        void buildingHexElevated() {
            Hex hex = new Hex(2, "bldg_elev:3;building:1:80;bldg_cf:80", "");
            assertEquals(5, ClimbingHelper.getClimbDestinationLevel(hex),
                  "Building sits on top of the hex level: 2 (ground) + 3 (3 floors) = 5.");
        }

        @Test
        @DisplayName("Bridge hex returns bridge surface absolute level — regression for "
              + "continuation-climb dialog skipping bridges")
        void bridgeHexAddsBridgeElev() {
            // This is the exact in-game scenario: bridge_elev:2 over water:2. Before the fix the
            // dialog ignored the BRIDGE branch and computed destination = hex.getLevel() = 0, so
            // a Mek clinging at water surface (elev 0) saw "0 levels remaining" and the
            // continue-climbing dialog never appeared.
            Hex hex = new Hex(0, "water:2;bridge:1;bridge_cf:100;bridge_elev:2", "");
            assertEquals(2, ClimbingHelper.getClimbDestinationLevel(hex),
                  "Bridge surface at bridge_elev 2 sits at absolute level 2; the continuation "
                        + "dialog must measure to it, not to the bare water hex.");
        }

        @Test
        @DisplayName("Building takes precedence when a hex has both BUILDING and BRIDGE")
        void buildingTakesPrecedenceOverBridge() {
            // Pathological hex with both terrains (rare but theoretically possible if a hex
            // contains both a building footprint and a bridge fragment). We pick BUILDING to
            // match the historical behavior of the dialog code that this helper replaces.
            Hex hex = new Hex(0, "bldg_elev:5;building:2:80;bldg_cf:80;bridge:1;bridge_elev:2", "");
            assertEquals(5, ClimbingHelper.getClimbDestinationLevel(hex),
                  "When both BUILDING and BRIDGE are present, the building roof wins.");
        }
    }

    @Nested
    @DisplayName("getEdgeDropHeight - distance from edge to landing surface")
    class GetEdgeDropHeightTests {

        /**
         * Builds a 1x2 board so a Mek at (0,0) is adjacent to a target hex at (0,1), places the
         * Mek on the {@code entityHex}, and returns it ready for getEdgeDropHeight tests.
         */
        private Mek setupEdgeTest(Hex entityHex, int entityElevation, Hex targetHex) {
            Board board = new Board(1, 2);
            board.setHex(0, 0, entityHex);
            board.setHex(0, 1, targetHex);
            game.setBoard(board);
            Mek mek = createFullyFunctionalMek(50);
            mek.setPosition(new Coords(0, 0));
            mek.setElevation(entityElevation);
            return mek;
        }

        @Test
        @DisplayName("Drop into adjacent water counts to the water FLOOR, not the surface — "
              + "regression for bridge-top-to-water edge-descent dialog skipping")
        void bridgeTopToAdjacentWaterCountsToWaterFloor() {
            // Mek on top of a bridge over water:2 (entity hex at level 0, bridge_elev:2 →
            // entity absolute alt +2). Adjacent target is a plain water:2 hex (level 0, floor
            // -2). The "edge" the Mek is stepping off measures from +2 down to the water floor
            // -2 = 4 levels — comfortably above the 3-level threshold that triggers the
            // edge-descent dialog. Before the fix only hex.getLevel() was used, so the drop
            // looked like 2 levels and the dialog never fired.
            Hex bridgeHex = new Hex(0, "water:2;bridge:1;bridge_cf:100;bridge_elev:2", "");
            Hex waterHex = new Hex(0, "water:2", "");
            Mek mek = setupEdgeTest(bridgeHex, 2, waterHex);

            int drop = ClimbingHelper.getEdgeDropHeight(mek, new Coords(0, 1), game);
            assertEquals(4, drop,
                  "Drop from bridge top (+2) into adjacent water:2 (floor at -2) must be 4 levels "
                        + "so the edge-descent dialog fires.");
            assertTrue(ClimbingHelper.isAtEdge(mek, new Coords(0, 1), game),
                  "4-level drop is at or above the 3-level edge threshold — must be 'at edge'.");
        }

        @Test
        @DisplayName("Adjacent BUILDING roof intercepts the drop")
        void dropOntoBuildingRoof() {
            // Mek at absolute +5 stepping onto an adjacent 2-floor building. The Mek lands on
            // the roof (level 0 + 2 = 2), so the drop measures down to the roof, not past it.
            Hex cliffHex = new Hex(5, "", "");
            Hex buildingHex = new Hex(0, "bldg_elev:2;building:2:80;bldg_cf:80", "");
            Mek mek = setupEdgeTest(cliffHex, 0, buildingHex);

            int drop = ClimbingHelper.getEdgeDropHeight(mek, new Coords(0, 1), game);
            assertEquals(3, drop,
                  "Cliff top (+5) to building roof (+2) is a 3-level drop, not 5 — the Mek lands "
                        + "on the roof, not at the building's base.");
        }

        @Test
        @DisplayName("Adjacent BRIDGE surface intercepts the drop")
        void dropOntoBridgeSurface() {
            // Mek on a +5 cliff stepping onto an adjacent bridge over water (bridge_elev:2 →
            // absolute +2). Lands on the bridge surface, drop = 5 - 2 = 3.
            Hex cliffHex = new Hex(5, "", "");
            Hex bridgeHex = new Hex(0, "water:2;bridge:1;bridge_cf:100;bridge_elev:2", "");
            Mek mek = setupEdgeTest(cliffHex, 0, bridgeHex);

            int drop = ClimbingHelper.getEdgeDropHeight(mek, new Coords(0, 1), game);
            assertEquals(3, drop,
                  "Drop into a bridge hex stops on the bridge surface — not down to the water "
                        + "floor below. The Mek climb-mode-on lands on the bridge.");
        }

        @Test
        @DisplayName("Dry-to-dry drop equals the simple level difference")
        void dropOntoDryGround() {
            Hex cliffHex = new Hex(5, "", "");
            Hex groundHex = new Hex(0, "", "");
            Mek mek = setupEdgeTest(cliffHex, 0, groundHex);

            int drop = ClimbingHelper.getEdgeDropHeight(mek, new Coords(0, 1), game);
            assertEquals(5, drop,
                  "Dry hex → dry hex: drop is just absolute altitude difference.");
        }
    }
}
