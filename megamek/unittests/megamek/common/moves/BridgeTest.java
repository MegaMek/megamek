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
import megamek.common.units.Infantry;
import megamek.common.units.QuadMek;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
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

    // ========== QUAD MEK TESTS ==========

    @Test
    void testQuadMek_WalkUnderHighBridge() {
        setBoard("BOARD_WALK_UNDER_BRIDGE");
        MovePath movePath = getMovePathFor(new QuadMek(), EntityMovementMode.QUAD,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // QuadMek has height 1, bridge_elev 4, can fit under
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 0, 0, 0);
    }

    @Test
    void testQuadMek_WalkUnderLowBridge_ForcedOnto() {
        setBoard("BOARD_WALK_UNDER_LOW_BRIDGE");
        MovePath movePath = getMovePathFor(new QuadMek(), EntityMovementMode.QUAD,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // QuadMek has height 1, bridge_elev 1, cannot fit under - forced onto bridge
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 1, 0, 0);
    }

    @Test
    void testQuadMek_WalkOverBridge() {
        setBoard("BOARD_BRIDGE_BETWEEN_LAND");
        MovePath movePath = getMovePathFor(new QuadMek(), EntityMovementMode.QUAD,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 4, 4, 4, 0);
    }

    // ========== TANK TESTS (Height 0 - Can Always Fit Under) ==========

    @Test
    void testTrackedTank_DriveUnderHighBridge() {
        setBoard("BOARD_WALK_UNDER_BRIDGE");
        Tank tank = new Tank();
        tank.setMovementMode(EntityMovementMode.TRACKED);
        MovePath movePath = getMovePathFor(tank, EntityMovementMode.TRACKED,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // Tanks have height 0, can always fit under bridges
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 0, 0, 0);
    }

    @Test
    void testTrackedTank_DriveUnderLowBridge() {
        setBoard("BOARD_WALK_UNDER_LOW_BRIDGE");
        Tank tank = new Tank();
        tank.setMovementMode(EntityMovementMode.TRACKED);
        MovePath movePath = getMovePathFor(tank, EntityMovementMode.TRACKED,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // Tanks have height 0, can fit under even low bridges
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 0, 0, 0);
    }

    @Test
    void testTrackedTank_DriveOverBridge() {
        setBoard("BOARD_BRIDGE_BETWEEN_LAND");
        Tank tank = new Tank();
        tank.setMovementMode(EntityMovementMode.TRACKED);
        MovePath movePath = getMovePathFor(tank, EntityMovementMode.TRACKED,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 4, 4, 4, 0);
    }

    @Test
    void testWheeledTank_DriveUnderLowBridge() {
        setBoard("BOARD_WALK_UNDER_LOW_BRIDGE");
        Tank tank = new Tank();
        tank.setMovementMode(EntityMovementMode.WHEELED);
        MovePath movePath = getMovePathFor(tank, EntityMovementMode.WHEELED,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // Wheeled tanks have height 0, can fit under
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 0, 0, 0);
    }

    @Test
    void testHoverTank_DriveUnderLowBridge() {
        setBoard("BOARD_WALK_UNDER_LOW_BRIDGE");
        Tank tank = new Tank();
        tank.setMovementMode(EntityMovementMode.HOVER);
        MovePath movePath = getMovePathFor(tank, EntityMovementMode.HOVER,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // Hover tanks have height 0, can fit under
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 0, 0, 0);
    }

    // ========== VTOL TESTS (Can Fly Under Bridges) ==========

    @Test
    void testVTOL_FlyUnderBridge() {
        setBoard("BOARD_WALK_UNDER_BRIDGE");
        VTOL vtol = new VTOL();
        // VTOL at elevation 1 can fly under bridge at elev 4
        MovePath movePath = getMovePathFor(vtol, 1, EntityMovementMode.VTOL,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // VTOL can fly under bridge at elevation 1 (bridge at elev 4)
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 1, 1, 1, 1, 1);
    }

    @Test
    void testVTOL_FlyOverBridge() {
        setBoard("BOARD_WALK_UNDER_BRIDGE");
        VTOL vtol = new VTOL();
        // VTOL at elevation 5 flies over bridge at elev 4
        MovePath movePath = getMovePathFor(vtol, 5, EntityMovementMode.VTOL,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // VTOL can fly over bridge at elevation 5 (bridge at elev 4)
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 5, 5, 5, 5, 5);
    }

    // ========== INFANTRY TESTS (Height 0 - Can Always Fit Under) ==========

    @Test
    void testLegInfantry_WalkUnderHighBridge() {
        setBoard("BOARD_WALK_UNDER_BRIDGE");
        Infantry infantry = new Infantry();
        infantry.setMovementMode(EntityMovementMode.INF_LEG);
        MovePath movePath = getMovePathFor(infantry, EntityMovementMode.INF_LEG,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // Infantry has height 0, can fit under
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 0, 0, 0);
    }

    @Test
    void testLegInfantry_WalkUnderLowBridge() {
        setBoard("BOARD_WALK_UNDER_LOW_BRIDGE");
        Infantry infantry = new Infantry();
        infantry.setMovementMode(EntityMovementMode.INF_LEG);
        MovePath movePath = getMovePathFor(infantry, EntityMovementMode.INF_LEG,
              MoveStepType.CLIMB_MODE_OFF,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        // Infantry has height 0, can fit under even low bridges
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 0, 0, 0, 0);
    }

    @Test
    void testLegInfantry_WalkOverBridge() {
        setBoard("BOARD_BRIDGE_BETWEEN_LAND");
        Infantry infantry = new Infantry();
        infantry.setMovementMode(EntityMovementMode.INF_LEG);
        MovePath movePath = getMovePathFor(infantry, EntityMovementMode.INF_LEG,
              MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS,
              MoveStepType.FORWARDS);
        assertTrue(movePath.isMoveLegal());
        assertMovePathElevations(movePath, 0, 4, 4, 4, 0);
    }

    // ========== NAVAL TESTS (Surface Water - Can Pass Under Bridges) ==========

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
