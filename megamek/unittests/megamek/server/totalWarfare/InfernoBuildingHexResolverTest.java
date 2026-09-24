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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Vector;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.rolls.Roll;
import megamek.common.units.BuildingEntity;
import megamek.common.units.BuildingTarget;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Issue #8907: inferno missiles on a building hex. Every unit inside rolls per missile and is struck on 5 or 6, but a
 * conventional infantry unit that is struck only takes the building type's share of the struck missiles, rounded
 * normally (TW p. 141 errata, Infantry Damage in Buildings Table p. 172). Battle armor takes every struck missile.
 */
class InfernoBuildingHexResolverTest extends GameBoardTestCase {

    private static final Coords ATTACKER_HEX = new Coords(5, 3);
    private static final Coords BUILDING_HEX = new Coords(5, 5);
    private static final int CALLED_NONE = 0;

    static {
        initializeBoard("INFERNO_BUILDING_BOARD", """
              size 16 17
              hex 0503 0 "" ""
              hex 0505 0 "" ""
              end"""
        );
    }

    private TWGameManager gameManager;
    private Game game;
    private Board board;
    private ConvInfantry attacker;

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

        board = getBoard("INFERNO_BUILDING_BOARD");
        game.setBoard(board);

        attacker = addPlatoon(ATTACKER_HEX);
    }

    private void placeBuilding(BuildingType buildingType) {
        BuildingEntity building = new BuildingEntity(buildingType, IBuilding.STANDARD);
        building.getInternalBuilding().setBuildingHeight(2);
        building.getInternalBuilding().addHex(CubeCoords.ZERO, 100, 0, BasementType.UNKNOWN, false);
        building.setOwner(game.getPlayer(0));
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setId(game.getNextEntityId());
        game.addEntity(building);
        building.setPosition(BUILDING_HEX);
        building.updateBuildingEntityHexes(board.getBoardId(), gameManager);
    }

    private ConvInfantry addPlatoonOnLevel(Coords position, int level) {
        ConvInfantry platoon = addPlatoon(position);
        platoon.setElevation(level);
        return platoon;
    }

    private <T extends Infantry> T addInfantry(T infantry, Coords position, int squadSize, int squadCount) {
        infantry.setOwner(game.getPlayer(0));
        infantry.setId(game.getNextEntityId());
        infantry.setSquadSize(squadSize);
        infantry.setSquadCount(squadCount);
        infantry.autoSetInternal();
        game.addEntity(infantry);
        infantry.setDeployed(true);
        infantry.setPosition(position);
        infantry.setElevation(0);
        return infantry;
    }

    private ConvInfantry addPlatoon(Coords position) {
        return addInfantry(new ConvInfantry(), position, 7, 4);
    }

    private BattleArmor addSquad(Coords position) {
        return addInfantry(new BattleArmor(), position, 5, 1);
    }

    /** Puts the unit in the hex with the missile delivery stubbed out, so only the call counts are observed. */
    private void stubDelivery(Entity unit) {
        doReturn(new Vector<Report>()).when(gameManager)
              .deliverInfernoMissiles(any(Entity.class), eq(unit), anyInt(), anyInt());
    }

    private void fireInfernosAtBuilding(int missiles, int dieResult) {
        Roll roll = mock(Roll.class);
        when(roll.getIntValue()).thenReturn(dieResult);
        when(roll.getReport()).thenReturn(String.valueOf(dieResult));
        BuildingTarget target = new BuildingTarget(BUILDING_HEX, board, false);
        try (MockedStatic<Compute> mockedCompute = Mockito.mockStatic(Compute.class, Mockito.CALLS_REAL_METHODS)) {
            mockedCompute.when(() -> Compute.rollD6(1)).thenReturn(roll);
            gameManager.deliverInfernoMissiles(attacker, target, missiles, CALLED_NONE);
        }
    }

    @Test
    void hardenedBuildingShieldsConventionalInfantryFromEveryStruckMissile() {
        placeBuilding(BuildingType.HARDENED);
        ConvInfantry platoon = addPlatoon(BUILDING_HEX);
        stubDelivery(platoon);

        fireInfernosAtBuilding(6, 6);

        verify(gameManager, never()).deliverInfernoMissiles(any(Entity.class), eq(platoon), anyInt(), anyInt());
    }

    @Test
    void lightBuildingPassesThreeQuartersOfTheStruckMissiles() {
        placeBuilding(BuildingType.LIGHT);
        ConvInfantry platoon = addPlatoon(BUILDING_HEX);
        stubDelivery(platoon);

        fireInfernosAtBuilding(4, 6);

        verify(gameManager).deliverInfernoMissiles(any(Entity.class), eq(platoon), eq(3), eq(CALLED_NONE));
    }

    @Test
    void mediumBuildingRoundsHalfAMissileUp() {
        placeBuilding(BuildingType.MEDIUM);
        ConvInfantry platoon = addPlatoon(BUILDING_HEX);
        stubDelivery(platoon);

        fireInfernosAtBuilding(3, 6);

        verify(gameManager).deliverInfernoMissiles(any(Entity.class), eq(platoon), eq(2), eq(CALLED_NONE));
    }

    @Test
    void heavyBuildingRoundsASingleStruckMissileAway() {
        placeBuilding(BuildingType.HEAVY);
        ConvInfantry platoon = addPlatoon(BUILDING_HEX);
        stubDelivery(platoon);

        fireInfernosAtBuilding(1, 6);

        verify(gameManager, never()).deliverInfernoMissiles(any(Entity.class), eq(platoon), anyInt(), anyInt());
    }

    @Test
    void missilesThatMissTheRollNeverReachTheInfantry() {
        placeBuilding(BuildingType.LIGHT);
        ConvInfantry platoon = addPlatoon(BUILDING_HEX);
        stubDelivery(platoon);

        fireInfernosAtBuilding(4, 4);

        verify(gameManager, never()).deliverInfernoMissiles(any(Entity.class), eq(platoon), anyInt(), anyInt());
    }

    /** TW p. 141: only units inside on the level that was struck are affected; the roof is not inside at all. */
    @Test
    void unitsOnTheRoofOrAnotherLevelAreNotStruck() {
        placeBuilding(BuildingType.LIGHT);
        ConvInfantry groundFloor = addPlatoonOnLevel(BUILDING_HEX, 0);
        ConvInfantry upstairs = addPlatoonOnLevel(BUILDING_HEX, 1);
        ConvInfantry onTheRoof = addPlatoonOnLevel(BUILDING_HEX, 2);
        stubDelivery(groundFloor);
        stubDelivery(upstairs);
        stubDelivery(onTheRoof);

        fireInfernosAtBuilding(4, 6);

        verify(gameManager).deliverInfernoMissiles(any(Entity.class), eq(groundFloor), eq(3), eq(CALLED_NONE));
        verify(gameManager, never()).deliverInfernoMissiles(any(Entity.class), eq(upstairs), anyInt(), anyInt());
        verify(gameManager, never()).deliverInfernoMissiles(any(Entity.class), eq(onTheRoof), anyInt(), anyInt());
    }

    @Test
    void battleArmorTakesEveryStruckMissileEvenInAHardenedBuilding() {
        placeBuilding(BuildingType.HARDENED);
        BattleArmor squad = addSquad(BUILDING_HEX);
        stubDelivery(squad);

        fireInfernosAtBuilding(2, 6);

        verify(gameManager, times(2)).deliverInfernoMissiles(any(Entity.class), eq(squad), eq(1), eq(CALLED_NONE));
    }
}
