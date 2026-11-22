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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.GunEmplacement;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.BipedMek;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.MekWarrior;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * An ejected MekWarrior sharing a hex with an enemy turret or other crewed building is captured, as they would be by
 * any other enemy unit. Buildings are never targetable as units, so the checks for other units do not see them.
 */
class MekWarriorBuildingCaptureTest {

    private static final Coords BUILDING_HEX = new Coords(5, 5);
    private static final Coords EMPTY_HEX = new Coords(10, 10);
    private static final int BUILDING_HEIGHT = 2;
    private static final int PICKED_UP_REPORT = 6420;

    private static final String BOARD_DATA = """
          size 16 17
          hex 0505 0 "" ""
          end""";

    private TWGameManager gameManager;
    private Game game;
    private Player defender;
    private Player attacker;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), anyInt());
        Mockito.doNothing().when(gameManager).sendChangedBuildings(any());
        Mockito.doNothing().when(gameManager).entityUpdate(anyInt());

        game = gameManager.getGame();
        defender = new Player(0, "Defender");
        defender.setTeam(1);
        attacker = new Player(1, "Attacker");
        attacker.setTeam(2);
        game.addPlayer(defender.getId(), defender);
        game.addPlayer(attacker.getId(), attacker);
        game.setBoard(BoardLoader.initializeBoard(BOARD_DATA));
    }

    private BuildingEntity addBuilding(CubeCoords... outerHexes) {
        Board board = game.getBoard();
        BuildingEntity building = new BuildingEntity(BuildingType.MEDIUM, IBuilding.STANDARD);
        building.getInternalBuilding().setBuildingHeight(BUILDING_HEIGHT);
        building.getInternalBuilding().addHex(CubeCoords.ZERO, 40, 0, BasementType.UNKNOWN, false);
        for (CubeCoords outerHex : outerHexes) {
            building.getInternalBuilding().addHex(outerHex, 40, 0, BasementType.UNKNOWN, false);
        }
        building.setOwner(defender);
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setId(game.getNextEntityId());
        building.getCrew().setSize(4);
        building.getCrew().setCurrentSize(4);
        game.addEntity(building);
        building.setPosition(BUILDING_HEX);
        building.updateBuildingEntityHexes(board.getBoardId(), gameManager);
        building.setDeployed(true);
        return building;
    }

    private MekWarrior addPilot(Player owner, Coords position, int elevation) {
        MekWarrior pilot = new MekWarrior();
        deploy(pilot, owner, position);
        pilot.setElevation(elevation);
        return pilot;
    }

    private void deploy(Entity entity, Player owner, Coords position) {
        entity.setOwner(owner);
        entity.setId(game.getNextEntityId());
        game.addEntity(entity);
        entity.setPosition(position);
        entity.setDeployed(true);
    }

    private void assertCapturedBy(Entity captor, MekWarrior pilot) {
        assertTrue(pilot.isCaptured());
        assertEquals(captor.getId(), pilot.getPickedUpById());
        assertEquals(captor.getExternalIdAsString(), pilot.getPickedUpByExternalIdAsString());
        assertTrue(captor.getPickedUpMekWarriors().contains(pilot.getId()));
        assertNull(pilot.getPosition(), "a captured pilot leaves the board");
        assertTrue(gameManager.getMainPhaseReport().stream()
              .anyMatch(report -> report.messageId == PICKED_UP_REPORT));
    }

    private void assertNotCaptured(MekWarrior pilot) {
        assertFalse(pilot.isCaptured());
        assertEquals(Entity.NONE, pilot.getPickedUpById());
        assertEquals(BUILDING_HEX, pilot.getPosition());
    }

    @Test
    void enemyTurretCapturesThePilot() {
        BuildingEntity turret = addBuilding();
        MekWarrior pilot = addPilot(attacker, BUILDING_HEX, 0);

        gameManager.resolveMekWarriorPickUp();

        assertCapturedBy(turret, pilot);
    }

    @Test
    void unconsciousPilotIsCaptured() {
        BuildingEntity turret = addBuilding();
        MekWarrior pilot = addPilot(attacker, BUILDING_HEX, 0);
        pilot.getCrew().setUnconscious(true);

        gameManager.resolveMekWarriorPickUp();

        assertCapturedBy(turret, pilot);
    }

    @Test
    void pilotOnTheRoofIsCaptured() {
        BuildingEntity turret = addBuilding();
        MekWarrior pilot = addPilot(attacker, BUILDING_HEX, BUILDING_HEIGHT);

        gameManager.resolveMekWarriorPickUp();

        assertCapturedBy(turret, pilot);
    }

    @Test
    void pilotAboveTheRoofIsNotCaptured() {
        addBuilding();
        MekWarrior pilot = addPilot(attacker, BUILDING_HEX, BUILDING_HEIGHT + 1);

        gameManager.resolveMekWarriorPickUp();

        assertNotCaptured(pilot);
    }

    @Test
    void pilotInAnOuterHexOfALargeBuildingIsCaptured() {
        CubeCoords outerHex = new CubeCoords(1, -1, 0);
        BuildingEntity building = addBuilding(outerHex);
        Coords outerPosition = building.getCoordsList().stream()
              .filter(coords -> !coords.equals(BUILDING_HEX))
              .findFirst()
              .orElseThrow();
        assertNotEquals(BUILDING_HEX, outerPosition);
        MekWarrior pilot = addPilot(attacker, outerPosition, 0);

        gameManager.resolveMekWarriorPickUp();

        assertCapturedBy(building, pilot);
    }

    @Test
    void pilotInAnotherHexIsNotCaptured() {
        addBuilding();
        MekWarrior pilot = addPilot(attacker, EMPTY_HEX, 0);

        gameManager.resolveMekWarriorPickUp();

        assertFalse(pilot.isCaptured());
        assertEquals(EMPTY_HEX, pilot.getPosition());
    }

    @Test
    void gunEmplacementCapturesThePilot() {
        GunEmplacement emplacement = new GunEmplacement();
        deploy(emplacement, defender, BUILDING_HEX);
        MekWarrior pilot = addPilot(attacker, BUILDING_HEX, 0);

        gameManager.resolveMekWarriorPickUp();

        assertCapturedBy(emplacement, pilot);
    }

    @Test
    void friendlyTurretDoesNotCaptureThePilot() {
        addBuilding();
        MekWarrior pilot = addPilot(defender, BUILDING_HEX, 0);

        gameManager.resolveMekWarriorPickUp();

        assertNotCaptured(pilot);
    }

    @Test
    void turretWhoseCrewIsGoneDoesNotCaptureThePilot() {
        BuildingEntity turret = addBuilding();
        turret.getCrew().setCurrentSize(0);
        MekWarrior pilot = addPilot(attacker, BUILDING_HEX, 0);

        gameManager.resolveMekWarriorPickUp();

        assertNotCaptured(pilot);
    }

    @Test
    void turretWhoseCrewIsDeadDoesNotCaptureThePilot() {
        BuildingEntity turret = addBuilding();
        turret.getCrew().setDead(true);
        MekWarrior pilot = addPilot(attacker, BUILDING_HEX, 0);

        gameManager.resolveMekWarriorPickUp();

        assertNotCaptured(pilot);
    }

    @Test
    void pilotAttackingTheTurretIsLeftToTheInfantryAction() {
        BuildingEntity turret = addBuilding();
        MekWarrior pilot = addPilot(attacker, BUILDING_HEX, 0);
        pilot.setInfantryCombatTargetId(turret.getId());

        gameManager.resolveMekWarriorPickUp();

        assertNotCaptured(pilot);
    }

    @Test
    void pilotIsPickedUpByTheirOwnMekBeforeTheTurretCapturesThem() {
        BuildingEntity turret = addBuilding();
        MekWarrior pilot = addPilot(attacker, BUILDING_HEX, 0);
        BipedMek friendlyMek = new BipedMek();
        deploy(friendlyMek, attacker, BUILDING_HEX);

        gameManager.resolveMekWarriorPickUp();

        assertFalse(pilot.isCaptured());
        assertEquals(friendlyMek.getId(), pilot.getPickedUpById());
        assertTrue(turret.getPickedUpMekWarriors().isEmpty());
    }
}
