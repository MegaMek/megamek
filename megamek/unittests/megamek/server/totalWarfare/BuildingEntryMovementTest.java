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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Issue #8903: a vehicle that fails its Driving Skill Roll while entering a building hex must make one immediate roll
 * on the Motive System Damage Table (TW p. 168, Vehicles), on top of the damage from the wall.
 */
class BuildingEntryMovementTest extends GameBoardTestCase {

    private static final Coords OUTSIDE_HEX = new Coords(5, 4);
    private static final Coords BUILDING_HEX = new Coords(5, 5);
    private static final int STARTING_CF = 40;
    private static final int MOTIVE_DAMAGE_SENTINEL_REPORT = 99999;

    static {
        initializeBoard("BUILDING_ENTRY_BOARD", """
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

        Board board = getBoard("BUILDING_ENTRY_BOARD");
        game.setBoard(board);

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

        // The wall damage to the moving unit is not under test; keep the unit intact.
        doReturn(new Vector<Report>()).when(gameManager)
              .damageEntity(any(Entity.class), any(HitData.class), anyInt());
        Vector<Report> motiveDamageReports = new Vector<>();
        motiveDamageReports.add(new Report(MOTIVE_DAMAGE_SENTINEL_REPORT));
        doReturn(motiveDamageReports).when(gameManager).vehicleMotiveDamage(any(Tank.class), anyInt());
    }

    private void drivingSkillRollFails() {
        doReturn(1).when(gameManager).doSkillCheckWhileMoving(any(Entity.class), anyInt(), any(Coords.class),
              any(Coords.class), any(), anyBoolean(), any());
    }

    private void drivingSkillRollPasses() {
        doReturn(0).when(gameManager).doSkillCheckWhileMoving(any(Entity.class), anyInt(), any(Coords.class),
              any(Coords.class), any(), anyBoolean(), any());
    }

    private <T extends Entity> T addUnit(T unit, double weight) {
        unit.setOwner(game.getPlayer(0));
        unit.setWeight(weight);
        unit.setId(game.getNextEntityId());
        game.addEntity(unit);
        unit.setPosition(OUTSIDE_HEX);
        return unit;
    }

    private Vector<Report> enterBuilding(Entity unit) {
        Vector<Report> reports = new Vector<>();
        gameManager.passBuildingWall(unit, building, OUTSIDE_HEX, BUILDING_HEX, 1, "", false,
              EntityMovementType.MOVE_WALK, true, reports);
        return reports;
    }

    private static boolean containsReport(Vector<Report> reports, int messageId) {
        return reports.stream().anyMatch(report -> report.messageId == messageId);
    }

    @Test
    void vehicleFailingTheDrivingSkillRollRollsMotiveSystemDamage() {
        Tank tank = addUnit(new Tank(), 50);
        drivingSkillRollFails();

        Vector<Report> reports = enterBuilding(tank);

        verify(gameManager).vehicleMotiveDamage(eq(tank), eq(0));
        assertTrue(containsReport(reports, MOTIVE_DAMAGE_SENTINEL_REPORT),
              "the motive damage roll must be reported with the building entry");
    }

    @Test
    void vehiclePassingTheDrivingSkillRollTakesNoMotiveDamage() {
        Tank tank = addUnit(new Tank(), 50);
        drivingSkillRollPasses();

        Vector<Report> reports = enterBuilding(tank);

        verify(gameManager, never()).vehicleMotiveDamage(any(Tank.class), anyInt());
        assertFalse(containsReport(reports, MOTIVE_DAMAGE_SENTINEL_REPORT));
    }

    @Test
    void mekFailingThePilotingSkillRollTakesNoMotiveDamage() {
        BipedMek mek = addUnit(new BipedMek(), 50);
        drivingSkillRollFails();

        enterBuilding(mek);

        verify(gameManager, never()).vehicleMotiveDamage(any(Tank.class), anyInt());
    }
}
