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

import megamek.common.GameBoardTestCase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.SupportTank;
import megamek.common.units.Tank;
import megamek.common.units.TripodMek;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class BuildingTest extends GameBoardTestCase {

    static {
        // Single height-2 building in center hex, surrounded by clear terrain
        initializeBoard("BOARD_HEIGHT_2_BUILDING", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "bldg_elev:2;building:2;bldg_cf:100" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              end"""
        );

        // Single height-4 building in center hex, surrounded by clear terrain
        initializeBoard("BOARD_HEIGHT_4_BUILDING", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "bldg_elev:4;building:2;bldg_cf:100" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              end"""
        );

        // Single height-4 building adjacent to starting hex
        initializeBoard("BOARD_ADJACENT_HEIGHT_4_BUILDING", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "bldg_elev:4;building:2;bldg_cf:100" ""
              end"""
        );

        // Single height-8 building in center hex, surrounded by clear terrain
        initializeBoard("BOARD_HEIGHT_8_BUILDING", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "bldg_elev:8;building:2;bldg_cf:100" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              end"""
        );

        // Buildings with ascending heights (1, 2, 3) leading to a level-4 plateau
        initializeBoard("BOARD_ASCENDING_BUILDINGS_TO_PLATEAU", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "bldg_elev:1;building:2;bldg_cf:100" ""
              hex 0103 0 "bldg_elev:2;building:2;bldg_cf:100" ""
              hex 0104 0 "bldg_elev:3;building:2;bldg_cf:100" ""
              hex 0105 4 "" ""
              end"""
        );
    }

    private static Arguments createBipedMek() {
        BipedMek mek = new BipedMek();
        mek.setChassis("Biped Mek");
        mek.setWeight(100);
        return Arguments.of(mek, EntityMovementMode.BIPED);
    }

    private static Arguments createTripodMek() {
        TripodMek mek = new TripodMek();
        mek.setChassis("Tripod Mek");
        mek.setWeight(100);
        return Arguments.of(mek, EntityMovementMode.TRIPOD);
    }

    private static Arguments createTrackedTank() {
        Tank tank = new Tank();
        tank.setChassis("Tracked Tank");
        tank.setMovementMode(EntityMovementMode.TRACKED);
        return Arguments.of(tank, EntityMovementMode.TRACKED);
    }

    private static Arguments createWheeledTank() {
        Tank tank = new Tank();
        tank.setChassis("Wheeled Tank");
        tank.setMovementMode(EntityMovementMode.WHEELED);
        return Arguments.of(tank, EntityMovementMode.WHEELED);
    }

    private static Arguments createHoverTank() {
        Tank tank = new Tank();
        tank.setChassis("Hover Tank");
        tank.setMovementMode(EntityMovementMode.HOVER);
        return Arguments.of(tank, EntityMovementMode.HOVER);
    }

    private static Arguments createTrackedSupportTank() {
        SupportTank tank = new SupportTank();
        tank.setChassis("Tracked Support Tank");
        tank.setMovementMode(EntityMovementMode.TRACKED);
        return Arguments.of(tank, EntityMovementMode.TRACKED);
    }

    private static Arguments createWheeledSupportTank() {
        SupportTank tank = new SupportTank();
        tank.setChassis("Wheeled Support Tank");
        tank.setMovementMode(EntityMovementMode.WHEELED);
        return Arguments.of(tank, EntityMovementMode.WHEELED);
    }

    private static Arguments createHoverSupportTank() {
        SupportTank tank = new SupportTank();
        tank.setChassis("Hover Support Tank");
        tank.setMovementMode(EntityMovementMode.HOVER);
        return Arguments.of(tank, EntityMovementMode.HOVER);
    }

    public static Stream<Arguments> meks() {
        return Stream.of(
              createBipedMek(),
              createTripodMek()
        );
    }

    public static Stream<Arguments> tanks() {
        return Stream.of(
              createTrackedTank(),
              createWheeledTank(),
              createHoverTank(),
              createTrackedSupportTank(),
              createWheeledSupportTank(),
              createHoverSupportTank()
        );
    }

    @ParameterizedTest
    @MethodSource(value = "meks")
    void mekWalksThroughHeight2BuildingAtGroundLevel(Entity entity, EntityMovementMode entityMovementMode) {
        setBoard("BOARD_HEIGHT_2_BUILDING");
        // Walk through a building without climb mode, staying at ground level (elevation 0)
        MovePath movePath = getMovePathFor(entity, entityMovementMode,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);

        assertTrue(movePath.isMoveLegal(), "Mek can walk through height-2 building at ground level");
        // Mek enters and exits building at ground level
        assertMovePathElevations(movePath, 0, 0, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "meks")
    void mekClimbsOverHeight2Building(Entity entity, EntityMovementMode entityMovementMode) {
        setBoard("BOARD_HEIGHT_2_BUILDING");
        // Climb onto and over a 2-level building (within mek's max climb of 2)
        MovePath movePath = getMovePathFor(entity, entityMovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);

        assertTrue(movePath.isMoveLegal(), "Mek can climb onto height-2 building (within max climb of 2)");
        // Path: ground(0) -> ground(0) -> on building(2) -> ground(0) -> ground(0)
        assertMovePathElevations(movePath, 0, 0, 2, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "meks")
    void mekCannotClimbOntoHeight4BuildingFromGround(Entity entity, EntityMovementMode entityMovementMode) {
        setBoard("BOARD_HEIGHT_4_BUILDING");
        // Attempt to climb a 4-level building from ground (exceeds mek's max climb of 2)
        MovePath movePath = getMovePathFor(entity, entityMovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);

        assertFalse(movePath.isMoveLegal(), "Mek cannot climb 4 levels (exceeds max climb of 2)");
        // Elevation would be 4, but climb is illegal
        assertMovePathElevations(movePath, 0, 0, 4, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "meks")
    void mekCannotClimbOntoAdjacentHeight4Building(Entity entity, EntityMovementMode entityMovementMode) {
        setBoard("BOARD_ADJACENT_HEIGHT_4_BUILDING");
        // Attempt to climb directly onto a 4-level building from adjacent hex (exceeds max climb of 2)
        MovePath movePath = getMovePathFor(entity, entityMovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS);

        assertFalse(movePath.isMoveLegal(), "Mek cannot climb 4 levels in one step (max: 2)");
        assertMovePathElevations(movePath, 0, 4);
    }

    @ParameterizedTest
    @MethodSource(value = "meks")
    void mekWalksThroughAdjacentHeight4BuildingAtGroundLevel(Entity entity, EntityMovementMode entityMovementMode) {
        setBoard("BOARD_ADJACENT_HEIGHT_4_BUILDING");
        // Walk through building at ground level with climb mode off
        MovePath movePath = getMovePathFor(entity, entityMovementMode,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS);

        assertTrue(movePath.isMoveLegal(), "Mek can walk through buildings at ground level");
        assertMovePathElevations(movePath, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "meks")
    void mekJumpsOntoHeight4Building(Entity entity, EntityMovementMode entityMovementMode) {
        setBoard("BOARD_HEIGHT_4_BUILDING");
        entity.setOriginalJumpMP(3);
        EquipmentType equipmentType = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);
        try {
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
        } catch (Exception ignored) {}
        // Jump onto a 4-level building
        MovePath movePath = getMovePathFor(entity, entityMovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.START_JUMP,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);

        assertFalse(movePath.isMoveLegal(), "Mek cannot jump onto height-4 building with only three jump MPs");
        assertMovePathElevations(movePath, 0, 0, 0, 4);
    }

    @ParameterizedTest
    @MethodSource(value = "meks")
    void mekCannotJumpThroughHeight8Building(Entity entity, EntityMovementMode entityMovementMode) {
        setBoard("BOARD_HEIGHT_8_BUILDING");
        entity.setOriginalJumpMP(5);
        EquipmentType equipmentType = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);
        try {
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
        } catch (Exception ignored) {}
        // Jump through an 8-level building (insufficient jump MPs)
        MovePath movePath = getMovePathFor(entity, entityMovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.START_JUMP,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);

        assertFalse(movePath.isMoveLegal(), "Mek cannot jump through height-8 building with only five jump MPs");
        assertMovePathElevations(movePath, 0, 0, 0, 8, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "meks")
    void mekJumpsOntoHeight4BuildingWithSufficientJumpMP(Entity entity, EntityMovementMode entityMovementMode) {
        setBoard("BOARD_HEIGHT_4_BUILDING");
        entity.setOriginalJumpMP(4);
        EquipmentType equipmentType = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);
        try {
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
        } catch (Exception ignored) {}
        // Jump onto a 4-level building with sufficient jump MPs
        MovePath movePath = getMovePathFor(entity, entityMovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.START_JUMP,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);

        assertTrue(movePath.isMoveLegal(), "Mek can jump onto height-4 building with four jump MPs");
        assertMovePathElevations(movePath, 0, 0, 0, 4);
    }

    @ParameterizedTest
    @MethodSource(value = "meks")
    void mekJumpsThroughHeight8BuildingWithSufficientJumpMP(Entity entity, EntityMovementMode entityMovementMode) {
        setBoard("BOARD_HEIGHT_8_BUILDING");
        entity.setOriginalJumpMP(8);
        EquipmentType equipmentType = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);
        try {
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
            entity.addEquipment(equipmentType, BipedMek.LOC_CENTER_TORSO);
        } catch (Exception ignored) {}
        // Jump through an 8-level building with sufficient jump MPs
        MovePath movePath = getMovePathFor(entity, entityMovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.START_JUMP,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);

        assertTrue(movePath.isMoveLegal(), "Mek can jump through height-8 building with eight jump MPs");
        assertMovePathElevations(movePath, 0, 0, 0, 8, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "meks")
    void mekCannotClimbOntoHeight8Building(Entity entity, EntityMovementMode entityMovementMode) {
        setBoard("BOARD_HEIGHT_8_BUILDING");
        // Attempt to climb an 8-level building (far exceeds mek's max climb of 2)
        MovePath movePath = getMovePathFor(entity, entityMovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);

        assertFalse(movePath.isMoveLegal(), "Mek cannot climb 8 levels (far exceeds max climb of 2)");
        assertMovePathElevations(movePath, 0, 0, 8, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "meks")
    void mekCannotUseUpStepsInsideBuilding(Entity entity, EntityMovementMode entityMovementMode) {
        setBoard("BOARD_HEIGHT_8_BUILDING");
        // Try to use UP steps to climb inside a building (not allowed)
        MovePath movePath = getMovePathFor(entity, entityMovementMode,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.UP,
              MoveStepType.UP,
              MoveStepType.UP);

        assertFalse(movePath.isMoveLegal(), "Mek cannot use UP steps to climb inside buildings");
        assertMovePathElevations(movePath, 0, 0, 0, 1, 2, 3);
    }

    @ParameterizedTest
    @MethodSource(value = "meks")
    void mekClimbsAcrossAscendingBuildingsToPlateau(Entity entity, EntityMovementMode entityMovementMode) {
        setBoard("BOARD_ASCENDING_BUILDINGS_TO_PLATEAU");
        // Climb across buildings increasing in height by 1 each step, then step onto higher ground
        MovePath movePath = getMovePathFor(entity, entityMovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);

        assertTrue(movePath.isMoveLegal(), "Mek can climb across ascending buildings with 1-level increments");
        // Path: ground(0) -> bldg height 1 -> bldg height 2 -> bldg height 3 -> level-4 ground(0)
        assertMovePathElevations(movePath, 0, 1, 2, 3, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "tanks")
    void tankWalksThroughHeight2BuildingAtGroundLevel(Entity entity, EntityMovementMode entityMovementMode) {
        setBoard("BOARD_HEIGHT_2_BUILDING");
        // Tanks can move through buildings at ground level
        MovePath movePath = getMovePathFor(entity, entityMovementMode,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);

        assertTrue(movePath.isMoveLegal(), "Tank can move through height-2 building at ground level");
        assertMovePathElevations(movePath, 0, 0, 0, 0);
    }

    @ParameterizedTest
    @MethodSource(value = "tanks")
    void tanksCannotClimbOverHeight2Building(Entity entity, EntityMovementMode entityMovementMode) {
        setBoard("BOARD_HEIGHT_2_BUILDING");
        // Tanks cannot climb onto buildings (they can only move through at ground level) - so this should keep the
        // unit on the ground
        MovePath movePath = getMovePathFor(entity, entityMovementMode,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);

        assertMovePathElevations(movePath, 0, 0, 0, 0);
    }
}
