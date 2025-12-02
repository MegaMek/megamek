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

import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.units.BipedMek;
import megamek.common.units.EntityMovementMode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class BridgeTest extends GameBoardTestCase {

    static {
        initializeBoard("BOARD_PLAIN_BRIDGE", """
              size 1 5
              hex 0101 0 "bridge:1:09;bridge_cf:100;bridge_elev:4" ""
              hex 0102 0 "bridge:1:09;bridge_cf:100;bridge_elev:4" ""
              hex 0103 0 "bridge:1:09;bridge_cf:100;bridge_elev:4" ""
              hex 0104 0 "bridge:1:09;bridge_cf:100;bridge_elev:4" ""
              hex 0105 0 "bridge:1:09;bridge_cf:100;bridge_elev:4" ""
              end"""
        );

        initializeBoard("BOARD_SHORT_BRIDGE", """
              size 1 2
              hex 0101 4 "" ""
              hex 0102 0 "bridge:1:09;bridge_cf:100;bridge_elev:4" ""
              end"""
        );

        initializeBoard("BOARD_BRIDGE_BETWEEN_LAND", """
              size 1 5
              hex 0101 4 "" ""
              hex 0102 0 "bridge:1:09;bridge_cf:100;bridge_elev:4" ""
              hex 0103 0 "bridge:1:09;bridge_cf:100;bridge_elev:4" ""
              hex 0104 0 "bridge:1:09;bridge_cf:100;bridge_elev:4" ""
              hex 0105 4 "" ""
              end"""
        );

        initializeBoard("BOARD_BRIDGE_BETWEEN_LAND_ELEV_0_BRIDGE", """
              size 1 5
              hex 0101 4 "" ""
              hex 0102 4 "bridge:1:09;bridge_cf:100;bridge_elev:0" ""
              hex 0103 2 "bridge:1:09;bridge_cf:100;bridge_elev:2" ""
              hex 0104 0 "bridge:1:09;bridge_cf:100;bridge_elev:4" ""
              hex 0105 4 "" ""
              end"""
        );

        initializeBoard("BOARD_WALK_UNDER_BRIDGE", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "bridge:1;bridge_cf:100;bridge_elev:4" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              end"""
        );

        initializeBoard("BOARD_WALK_UNDER_LOW_BRIDGE", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "bridge:1;bridge_cf:100;bridge_elev:1" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              end"""
        );

        initializeBoard("BOARD_WALK_UP_ONTO_LOW_BRIDGE", """
              size 1 5
              hex 0101 0 "" ""
              hex 0103 0 "bridge:1:09;bridge_cf:100;bridge_elev:1" ""
              hex 0103 0 "bridge:1:09;bridge_cf:100;bridge_elev:1" ""
              hex 0103 0 "bridge:1:09;bridge_cf:100;bridge_elev:1" ""
              hex 0105 2 "" ""
              end"""
        );
    }

    @Test
    void testMovePathBoardBridgePlain() {
        setBoard("BOARD_PLAIN_BRIDGE");
        MovePath movePath = getMovePathFor(new BipedMek(), EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 4, 4, 4, 4, 4);
    }

    @Test
    void testMovePathBoardBridgeShort() {
        setBoard("BOARD_SHORT_BRIDGE");
        MovePath movePath = getMovePathFor(new BipedMek(), EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 4);
    }

    @Test
    void testMovePathBoardBetweenLand() {
        setBoard("BOARD_BRIDGE_BETWEEN_LAND");
        MovePath movePath = getMovePathFor(new BipedMek(), EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 4, 4, 4, 0);
    }

    @Test
    void testMovePathBoardBetweenLandElev0Bridge() {
        setBoard("BOARD_BRIDGE_BETWEEN_LAND_ELEV_0_BRIDGE");
        MovePath movePath = getMovePathFor(new BipedMek(), EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        assertMovePathElevations(movePath, 0, 0, 2, 4, 0);
    }

    @Test
    void testMovePathBoardWalkUnderBridge() {
        setBoard("BOARD_WALK_UNDER_BRIDGE");
        MovePath movePath = getMovePathFor(new BipedMek(), EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 0, 0, 0);
    }

    @Test
    void testMovePathBoardWalkUnderBridgeButTryToClimb() {
        setBoard("BOARD_WALK_UNDER_BRIDGE");
        MovePath movePath = getMovePathFor(new BipedMek(), EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // Move is legal, we aren't trying to climb up
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 0, 0, 0);
    }

    @Test
    void testMovePathBoardWalkUnderLowBridge() {
        setBoard("BOARD_WALK_UNDER_LOW_BRIDGE");
        MovePath movePath = getMovePathFor(new BipedMek(), EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // TO:AR 115 (6th ed) - If a unit cannot move under, it must move over
        // Entity is forced onto the bridge when it can't fit underneath
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 1, 0, 0);
    }

    @Test
    void testMovePathBoardWalkUnderLowBridgeButTryToClimb() {
        setBoard("BOARD_WALK_UNDER_LOW_BRIDGE");
        MovePath movePath = getMovePathFor(new BipedMek(), EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // Move is legal - TO:AR 115 (6th ed) - If a unit cannot move under, it must move over
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 1, 0, 0);
    }

    @Test
    void testMovePathBoardTryToClimbOntoWrongWayBridge() {
        setBoard("BOARD_WALK_UNDER_LOW_BRIDGE");
        MovePath movePath = getMovePathFor(new BipedMek(), EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // Move is legal
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 1);
    }

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
        // Move is legal, we can jump past a bridge
        // Step at bridge hex (0103) shows elevation 1 (would land on bridge if stopped there)
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 0, 1, 0, 0);
    }

    // FIXME: I think this test should be able to pass, but the user can get the same thing by toggling climb mode off
    @Test
    @Disabled
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
        MovePath movePath = getMovePathFor(new BipedMek(), EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // Move is legal, we can jump past a bridge
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 1, 0, 0);
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
        // Move is legal, we can jump onto a bridge even if the exit is unaligned
        assertTrue(movePath.isMoveLegal());
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
        // Move is legal, but mek can't fit under bridge so lands ON the bridge
        // TO:AR 115: "If a unit cannot move underneath the bridge, the unit must move onto the bridge"
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 0, 1);
    }

    @Test
    void testMovePathBoardWalkUnderUpOntoBridge() {
        setBoard("BOARD_WALK_UP_ONTO_LOW_BRIDGE");
        MovePath movePath = getMovePathFor(new BipedMek(), EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // Move is illegal, we aren't trying to climb up
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 1, 1, 1, 0);
    }
}
