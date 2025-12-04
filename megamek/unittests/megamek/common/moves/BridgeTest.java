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

package megamek.common.moves;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.units.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class BridgeTest extends GameBoardTestCase {

    static {
        initializeBoard("BOARD_PLAIN_BRIDGE", """
              size 1 5
              hex 0101 0 "bridge:1:09;bridge_cf:250;bridge_elev:4" ""
              hex 0102 0 "bridge:1:09;bridge_cf:250;bridge_elev:4" ""
              hex 0103 0 "bridge:1:09;bridge_cf:250;bridge_elev:4" ""
              hex 0104 0 "bridge:1:09;bridge_cf:250;bridge_elev:4" ""
              hex 0105 0 "bridge:1:09;bridge_cf:250;bridge_elev:4" ""
              end"""
        );

        initializeBoard("BOARD_SHORT_BRIDGE", """
              size 1 2
              hex 0101 4 "" ""
              hex 0102 0 "bridge:1:09;bridge_cf:250;bridge_elev:4" ""
              end"""
        );

        initializeBoard("BOARD_BRIDGE_BETWEEN_LAND", """
              size 1 5
              hex 0101 4 "" ""
              hex 0102 0 "bridge:1:09;bridge_cf:250;bridge_elev:4" ""
              hex 0103 0 "bridge:1:09;bridge_cf:250;bridge_elev:4" ""
              hex 0104 0 "bridge:1:09;bridge_cf:250;bridge_elev:4" ""
              hex 0105 4 "" ""
              end"""
        );

        initializeBoard("BOARD_BRIDGE_BETWEEN_LAND_ELEV_0_BRIDGE", """
              size 1 5
              hex 0101 4 "" ""
              hex 0102 4 "bridge:1:09;bridge_cf:250;bridge_elev:0" ""
              hex 0103 2 "bridge:1:09;bridge_cf:250;bridge_elev:2" ""
              hex 0104 0 "bridge:1:09;bridge_cf:250;bridge_elev:4" ""
              hex 0105 4 "" ""
              end"""
        );

        initializeBoard("BOARD_WALK_UNDER_BRIDGE", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "bridge:1;bridge_cf:250;bridge_elev:4" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              end"""
        );

        initializeBoard("BOARD_WALK_UNDER_LOW_BRIDGE", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "bridge:1;bridge_cf:250;bridge_elev:1" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              end"""
        );

        initializeBoard("BOARD_WALK_UP_ONTO_LOW_BRIDGE", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "bridge:1:09;bridge_cf:250;bridge_elev:1" ""
              hex 0103 0 "bridge:1:09;bridge_cf:250;bridge_elev:1" ""
              hex 0104 0 "bridge:1:09;bridge_cf:250;bridge_elev:1" ""
              hex 0105 2 "" ""
              end"""
        );

        // Water board with bridge for naval/VTOL tests
        initializeBoard("BOARD_BRIDGE_OVER_WATER", """
              size 1 5
              hex 0101 0 "water:2" ""
              hex 0102 0 "water:2" ""
              hex 0103 0 "water:2;bridge:1;bridge_cf:100;bridge_elev:2" ""
              hex 0104 0 "water:2" ""
              hex 0105 0 "water:2" ""
              end"""
        );
    }

    public static Stream<Arguments> allGroundedEntityTypes() {
        Stream<Arguments> allGroundedEntityTypes = allHeight0Units();
        allGroundedEntityTypes = Stream.concat(allGroundedEntityTypes, allHeight1Units());
        allGroundedEntityTypes = Stream.concat(allGroundedEntityTypes, allHeight2Units());
        return allGroundedEntityTypes;
    }

    public static Stream<Arguments> allHeight0Units() {
        return Stream.of(
              newTrackedTank(),
              newWheeledTank(),
              newHoverTank(),
              newTrackedSupportTank(),
              newWheeledSupportTank(),
              newHoverSupportTank(),
              newGroundedAeroSpace(),
              newGroundedConvFighter()
        );
    }

    public static Stream<Arguments> allHeight1Units() {
        return Stream.of(
              newBipedMek(),
              newTripodMek(),
              newQuadMek(),
              newWheeledSuperheavyTank(),
              newHoverSuperheavyTank(),
              newTrackedSuperheavyTank(),
              newWheeledLargeSupportTank(),
              newHoverLargeSupportTank(),
              newTrackedLargeSupportTank(),
              newSmallCraft()
        );
    }


    public static Stream<Arguments> allHeight2Units() {
        return Stream.of(
              newSuperheavyBipedMek(),
              newSuperheavyTripodMek(),
              newSuperheavyQuadMek()
        );
    }

    private static Arguments newBipedMek() {
        BipedMek bipedMek = new BipedMek();
        bipedMek.setChassis("Biped Mek");
        return Arguments.of(bipedMek, EntityMovementMode.BIPED);
    }

    private static Arguments newSuperheavyBipedMek() {
        BipedMek bipedMek = new BipedMek();
        bipedMek.setWeight(150);
        bipedMek.setChassis("Superheavy Biped Mek");
        return Arguments.of(bipedMek, EntityMovementMode.BIPED);
    }

    private static Arguments newTripodMek() {
        TripodMek tripodMek = new TripodMek();
        tripodMek.setChassis("Tripod Mek");
        return Arguments.of(tripodMek, EntityMovementMode.TRIPOD);
    }

    private static Arguments newSuperheavyTripodMek() {
        TripodMek tripodMek = new TripodMek();
        tripodMek.setWeight(150);
        tripodMek.setChassis("Superheavy Tripod Mek");
        return Arguments.of(tripodMek, EntityMovementMode.TRIPOD);
    }

    private static Arguments newQuadMek() {
        QuadMek quadMek = new QuadMek();
        quadMek.setChassis("Quad Mek");
        return Arguments.of(quadMek, EntityMovementMode.QUAD);
    }

    private static Arguments newSuperheavyQuadMek() {
        QuadMek quadMek = new QuadMek();
        quadMek.setWeight(150);
        quadMek.setChassis("Superheavy Quad Mek");
        return Arguments.of(quadMek, EntityMovementMode.QUAD);
    }

    private static Arguments newTrackedTank() {
        Tank tank = new Tank();
        tank.setChassis("Tracked Tank");
        return Arguments.of(tank, EntityMovementMode.TRACKED);
    }

    private static Arguments newWheeledTank() {
        Tank tank = new Tank();
        tank.setChassis("Wheeled Tank");
        return Arguments.of(tank, EntityMovementMode.WHEELED);
    }

    private static Arguments newHoverTank() {
        Tank tank = new Tank();
        tank.setChassis("Hover Tank");
        return Arguments.of(tank, EntityMovementMode.HOVER);
    }

    private static Arguments newTrackedSupportTank() {
        SupportTank tank = new SupportTank();
        tank.setChassis("Tracked Support Tank");
        return Arguments.of(tank, EntityMovementMode.TRACKED);
    }

    private static Arguments newWheeledSupportTank() {
        SupportTank tank = new SupportTank();
        tank.setChassis("Wheeled Support Tank");
        return Arguments.of(tank, EntityMovementMode.WHEELED);
    }

    private static Arguments newHoverSupportTank() {
        SupportTank tank = new SupportTank();
        tank.setChassis("Hover Support Tank");
        return Arguments.of(tank, EntityMovementMode.HOVER);
    }

    private static Arguments newWheeledSuperheavyTank() {
        SuperHeavyTank tank = new SuperHeavyTank();
        tank.setChassis("Wheeled Superheavy Tank");
        return Arguments.of(tank, EntityMovementMode.WHEELED);
    }

    private static Arguments newHoverSuperheavyTank() {
        SuperHeavyTank tank = new SuperHeavyTank();
        tank.setChassis("Hover Superheavy Tank");
        return Arguments.of(tank, EntityMovementMode.HOVER);
    }

    private static Arguments newTrackedSuperheavyTank() {
        SuperHeavyTank tank = new SuperHeavyTank();
        tank.setChassis("Tracked Superheavy Tank");
        return Arguments.of(tank, EntityMovementMode.TRACKED);
    }

    private static Arguments newWheeledLargeSupportTank() {
        LargeSupportTank tank = new LargeSupportTank();
        tank.setChassis("Wheeled Large Support Tank");
        return Arguments.of(tank, EntityMovementMode.WHEELED);
    }

    private static Arguments newHoverLargeSupportTank() {
        LargeSupportTank tank = new LargeSupportTank();
        tank.setChassis("Hover Large Support Tank");
        return Arguments.of(tank, EntityMovementMode.HOVER);
    }

    private static Arguments newTrackedLargeSupportTank() {
        LargeSupportTank tank = new LargeSupportTank();
        tank.setChassis("Tracked Large Support Tank");
        return Arguments.of(tank, EntityMovementMode.TRACKED);
    }

    // Grounded aeros are treated like wheeled tanks for movement
    private static Arguments newGroundedAeroSpace() {
        AeroSpaceFighter aeroSpaceFighter = new AeroSpaceFighter();
        aeroSpaceFighter.setAltitude(0);
        aeroSpaceFighter.setMovementMode(EntityMovementMode.WHEELED);
        aeroSpaceFighter.setChassis("Grounded Aerospace Fighter");
        return Arguments.of(aeroSpaceFighter, EntityMovementMode.WHEELED);
    }

    // Grounded aeros are treated like wheeled tanks for movement
    private static Arguments newGroundedConvFighter() {
        ConvFighter convFighter = new ConvFighter();
        convFighter.setAltitude(0);
        convFighter.setMovementMode(EntityMovementMode.WHEELED);
        convFighter.setChassis("Grounded Conv Fighter");
        return Arguments.of(convFighter, EntityMovementMode.WHEELED);
    }

    // Grounded aeros are treated like wheeled tanks for movement
    private static Arguments newSmallCraft() {
        SmallCraft smallCraft = new SmallCraft();
        smallCraft.setAltitude(0);
        smallCraft.setMovementMode(EntityMovementMode.WHEELED);
        smallCraft.setChassis("Grounded Small Craft");
        return Arguments.of(smallCraft, EntityMovementMode.WHEELED);
    }

    @ParameterizedTest
    @MethodSource(value = "allGroundedEntityTypes")
    void testMovePathBoardBridgePlain(Entity entity, EntityMovementMode entitymovementMode) {
        setBoard("BOARD_PLAIN_BRIDGE");
        MovePath movePath = getMovePathFor(entity, entitymovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 4, 4, 4, 4, 4);
    }

    @ParameterizedTest
    @MethodSource(value = "allGroundedEntityTypes")
    void testMovePathBoardBridgeShort(Entity entity, EntityMovementMode entitymovementMode) {
        setBoard("BOARD_SHORT_BRIDGE");
        MovePath movePath = getMovePathFor(entity, entitymovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 4);
    }

    @ParameterizedTest
    @MethodSource(value = "allGroundedEntityTypes")
    void testMovePathBoardBetweenLand(Entity entity, EntityMovementMode entitymovementMode) {
        setBoard("BOARD_BRIDGE_BETWEEN_LAND");
        MovePath movePath = getMovePathFor(entity, entitymovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 4, 4, 4, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "allGroundedEntityTypes")
    void testMovePathBoardBetweenLandElev0Bridge(Entity entity, EntityMovementMode entitymovementMode) {
        setBoard("BOARD_BRIDGE_BETWEEN_LAND_ELEV_0_BRIDGE");
        MovePath movePath = getMovePathFor(entity, entitymovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        assertMovePathElevations(movePath, 0, 0, 2, 4, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "allGroundedEntityTypes")
    void testMovePathBoardWalkUnderBridge(Entity entity, EntityMovementMode entitymovementMode) {
        setBoard("BOARD_WALK_UNDER_BRIDGE");
        MovePath movePath = getMovePathFor(entity, entitymovementMode,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 0, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "allGroundedEntityTypes")
    void testMovePathBoardWalkUnderBridgeButTryToClimb(Entity entity, EntityMovementMode entitymovementMode) {
        setBoard("BOARD_WALK_UNDER_BRIDGE");
        MovePath movePath = getMovePathFor(entity, entitymovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // Move is legal, we aren't trying to climb up
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 0, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "allHeight1Units")
    void testMovePathBoardWalkUnderLowBridge(Entity entity, EntityMovementMode entitymovementMode) {
        setBoard("BOARD_WALK_UNDER_LOW_BRIDGE");
        MovePath movePath = getMovePathFor(entity, entitymovementMode,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // TO:AR 115 (6th ed) - If a unit cannot move under, it must move over
        //assertFalse(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 1, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "allHeight1Units")
    void testMovePathBoardWalkUnderLowBridgeButTryToClimb(Entity entity, EntityMovementMode entitymovementMode) {
        setBoard("BOARD_WALK_UNDER_LOW_BRIDGE");
        MovePath movePath = getMovePathFor(entity, entitymovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal(),
              "Move should be legal - TO:AR 115 (6th ed) - If a unit cannot move under, it must move over");
        assertMovePathElevations(movePath, 0, 0, 1, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "allHeight1Units")
    void testMovePathBoardTryToClimbOntoWrongWayBridge(Entity entity, EntityMovementMode entitymovementMode) {
        setBoard("BOARD_WALK_UNDER_LOW_BRIDGE");
        MovePath movePath = getMovePathFor(entity, entitymovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal(),
              "Move should be legal - TO:AR 115 (6th ed) - If a unit cannot move under, it must move over");
        assertMovePathElevations(movePath, 0, 0, 1);
    }


    @ParameterizedTest
    @MethodSource(value = "allGroundedEntityTypes")
    void testMovePathBoardWalkUnderUpOntoBridge(Entity entity, EntityMovementMode entitymovementMode) {
        setBoard("BOARD_WALK_UP_ONTO_LOW_BRIDGE");
        MovePath movePath = getMovePathFor(entity, entitymovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal(), "Move should be legal, we can climb onto a bridge");

        assertMovePathElevations(movePath, 0, 1, 1, 1, 0);
    }

    @Nested
    public class BoardJumpTests {
        @Test
        void testMovePathBoardJumpPastLowBridge() {
            setBoard("BOARD_WALK_UNDER_LOW_BRIDGE");
            BipedMek mek = new BipedMek();
            mek.setOriginalJumpMP(5);
            EquipmentType equipmentType = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);
            try {
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            } catch (Exception ignored) {}
            MovePath movePath = getMovePathFor(mek, EntityMovementMode.BIPED,
                  MoveStepType.CLIMB_MODE_OFF,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS);
            assertTrue(movePath.isMoveLegal(), "Move should be legal, we can jump past a bridge");
            assertMovePathElevations(movePath, 0, 0, 0, 1, 0, 0);
        }

        @Test
        void testMovePathBoardJumpOverLowBridge() {
            setBoard("BOARD_WALK_UNDER_LOW_BRIDGE");
            BipedMek mek = new BipedMek();
            mek.setOriginalJumpMP(5);
            EquipmentType equipmentType = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);
            try {
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            } catch (Exception ignored) {}
            MovePath movePath = getMovePathFor(mek, EntityMovementMode.BIPED,
                  MoveStepType.CLIMB_MODE_ON,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS);
            assertTrue(movePath.isMoveLegal(), "Move should be legal, we can jump past a bridge");
            assertMovePathElevations(movePath, 0, 0, 0, 1, 0, 0);
        }

        @Test
        void testMovePathBoardJumpOntoLowBridge() {
            setBoard("BOARD_WALK_UNDER_LOW_BRIDGE");
            BipedMek mek = new BipedMek();
            mek.setOriginalJumpMP(3);
            EquipmentType equipmentType = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);
            try {
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            } catch (Exception ignored) {}
            MovePath movePath = getMovePathFor(mek, EntityMovementMode.BIPED,
                  MoveStepType.CLIMB_MODE_ON,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS);
            // M
            assertTrue(movePath.isMoveLegal(),
                  "Move should be legal, we can jump onto a bridge even if the exit is unaligned");
            assertMovePathElevations(movePath, 0, 0, 0, 1);
        }

        @Test
        void testMovePathBoardJumpUnderLowBridge() {
            setBoard("BOARD_WALK_UNDER_LOW_BRIDGE");
            BipedMek mek = new BipedMek();
            mek.setOriginalJumpMP(3);
            EquipmentType equipmentType = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);
            try {
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            } catch (Exception ignored) {}
            MovePath movePath = getMovePathFor(mek, EntityMovementMode.BIPED,
                  MoveStepType.CLIMB_MODE_OFF,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS);
            assertTrue(movePath.isMoveLegal(), "Move should be legal, we'll land on the bridge still");
            assertMovePathElevations(movePath, 0, 0, 0, 1);
        }

        @Test
        void testMovePathBoardJumpUnderBridge() {
            setBoard("BOARD_WALK_UNDER_BRIDGE");
            BipedMek mek = new BipedMek();
            mek.setOriginalJumpMP(3);
            EquipmentType equipmentType = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);
            try {
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            } catch (Exception ignored) {}
            MovePath movePath = getMovePathFor(mek, EntityMovementMode.BIPED,
                  MoveStepType.CLIMB_MODE_OFF,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS);
            assertTrue(movePath.isMoveLegal(), "Move should be legal, we'll land under the bridge");
            assertMovePathElevations(movePath, 0, 0, 0, 0);
        }


        @Test
        void testMovePathBoardTryToJumpOntoBridge() {
            setBoard("BOARD_WALK_UNDER_BRIDGE");
            BipedMek mek = new BipedMek();
            mek.setOriginalJumpMP(3);
            EquipmentType equipmentType = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);
            try {
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            } catch (Exception ignored) {}
            MovePath movePath = getMovePathFor(mek, EntityMovementMode.BIPED,
                  MoveStepType.CLIMB_MODE_ON,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS);
            assertTrue(movePath.isMoveLegal(), "Move should be legal, we'll land under the bridge");
            assertMovePathElevations(movePath, 0, 0, 0, 0);
        }

        @Test
        void testMovePathBoardJumpOntoBridgeWithEnoughJump() {
            setBoard("BOARD_WALK_UNDER_BRIDGE");
            BipedMek mek = new BipedMek();
            mek.setOriginalJumpMP(5);
            EquipmentType equipmentType = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);
            try {
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
                mek.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            } catch (Exception ignored) {}
            MovePath movePath = getMovePathFor(mek, EntityMovementMode.BIPED,
                  MoveStepType.CLIMB_MODE_ON,
                  MoveStepType.START_JUMP,
                  MoveStepType.FORWARDS,
                  MoveStepType.FORWARDS);
            assertTrue(movePath.isMoveLegal(), "Move should be legal, we'll land on the bridge");
            assertMovePathElevations(movePath, 0, 0, 0, 4);
        }
    }


    @Test
    void testNaval_PassUnderBridge() {
        setBoard("BOARD_BRIDGE_OVER_WATER");
        Tank naval = new Tank();
        naval.setMovementMode(EntityMovementMode.NAVAL);
        // Naval units at surface (elevation 0)
        MovePath movePath = getMovePathFor(naval, 0, EntityMovementMode.NAVAL,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // Naval units travel on water surface (elevation 0), bridges are above
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 0, 0, 0);
    }


    @Test
    void testHydrofoil_PassUnderBridge() {
        setBoard("BOARD_BRIDGE_OVER_WATER");
        Tank hydrofoil = new Tank();
        hydrofoil.setMovementMode(EntityMovementMode.HYDROFOIL);
        // Hydrofoils at surface (elevation 0)
        MovePath movePath = getMovePathFor(hydrofoil, 0, EntityMovementMode.HYDROFOIL,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // Hydrofoils travel on water surface, bridges are above
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 0, 0, 0);
    }


    @Test
    void testSubmarine_PassUnderBridge() {
        setBoard("BOARD_BRIDGE_OVER_WATER");
        Tank submarine = new Tank();
        submarine.setMovementMode(EntityMovementMode.SUBMARINE);
        // Submarine underwater at depth -1 (below surface)
        MovePath movePath = getMovePathFor(submarine, -1, EntityMovementMode.SUBMARINE,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // Submarines travel underwater, bridges are above water
        assertTrue(movePath.isMoveLegal());
    }
}
