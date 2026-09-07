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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;

import java.util.Vector;

import megamek.common.GameBoardTestCase;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.BipedMek;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.IBuilding;
import megamek.common.units.LargeSupportTank;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Issue #8904: a Large Support Vehicle entering a building hex inflicts double the standard one point per ten tons
 * of damage on the building (TW p. 168, Large Support Vehicles).
 */
class LargeSupportVehicleBuildingDamageTest extends GameBoardTestCase {

    private static final Coords OUTSIDE_HEX = new Coords(5, 4);
    private static final Coords BUILDING_HEX = new Coords(5, 5);
    private static final int STARTING_CF = 90;
    private static final double UNIT_WEIGHT = 150;
    private static final int STANDARD_DAMAGE = 15;

    static {
        initializeBoard("LARGE_SUPPORT_BUILDING_BOARD", """
              size 16 17
              hex 0504 0 "" ""
              hex 0505 0 "" ""
              end"""
        );
    }

    private TWGameManager gameManager;
    private Game game;
    private BuildingEntity building;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        Player player = new Player(0, "Test");
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), any(int.class));
        Mockito.doNothing().when(gameManager).entityUpdate(any(int.class));
        Mockito.doNothing().when(gameManager).sendChangedBuildings(any());
        game = gameManager.getGame();
        game.addPlayer(0, player);

        Board board = getBoard("LARGE_SUPPORT_BUILDING_BOARD");
        game.setBoard(board);

        // A standard-class medium building applies no damage scaling, so the CF drop is the raw damage.
        building = new BuildingEntity(BuildingType.MEDIUM, IBuilding.STANDARD);
        building.getInternalBuilding().setBuildingHeight(1);
        building.getInternalBuilding().addHex(CubeCoords.ZERO, STARTING_CF, 0, BasementType.UNKNOWN, false);
        building.setOwner(game.getPlayer(0));
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setId(0);
        game.addEntity(building);
        building.setPosition(BUILDING_HEX);
        building.updateBuildingEntityHexes(board.getBoardId(), gameManager);

        // The Driving Skill Roll and the damage to the moving unit are not under test.
        doReturn(0).when(gameManager).doSkillCheckWhileMoving(any(Entity.class), anyInt(), any(Coords.class),
              any(Coords.class), any(), anyBoolean(), any());
        doReturn(new Vector<Report>()).when(gameManager)
              .damageEntity(any(Entity.class), any(HitData.class), anyInt());
    }

    private <T extends Entity> T addUnit(T unit) {
        unit.setOwner(game.getPlayer(0));
        unit.setWeight(UNIT_WEIGHT);
        unit.setId(game.getNextEntityId());
        game.addEntity(unit);
        unit.setPosition(OUTSIDE_HEX);
        return unit;
    }

    private void enterBuilding(Entity unit) {
        gameManager.passBuildingWall(unit, building, OUTSIDE_HEX, BUILDING_HEX, 1, "", false,
              EntityMovementType.MOVE_WALK, true, new Vector<>());
    }

    @Test
    void largeSupportVehicleInflictsDoubleDamageOnTheBuilding() {
        LargeSupportTank largeSupportVehicle = addUnit(new LargeSupportTank());

        enterBuilding(largeSupportVehicle);

        assertEquals(STARTING_CF - (2 * STANDARD_DAMAGE), building.getCurrentCF(BUILDING_HEX));
    }

    @Test
    void ordinaryVehicleInflictsStandardDamageOnTheBuilding() {
        Tank tank = addUnit(new Tank());

        enterBuilding(tank);

        assertEquals(STARTING_CF - STANDARD_DAMAGE, building.getCurrentCF(BUILDING_HEX));
    }

    @Test
    void mekInflictsStandardDamageOnTheBuilding() {
        BipedMek mek = addUnit(new BipedMek());

        enterBuilding(mek);

        assertEquals(STARTING_CF - STANDARD_DAMAGE, building.getCurrentCF(BUILDING_HEX));
    }
}
