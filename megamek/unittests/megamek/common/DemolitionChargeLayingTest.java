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

package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import megamek.common.actions.LayExplosivesAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.BuildingEntity;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Infantry;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests for the demolition charge laying lifecycle, TO:AUE p.152-153. Regression tests for issue #8328: laying must
 * continue across rounds while the platoon stays inside the target structure, including advanced buildings
 * (BuildingEntity).
 */
public class DemolitionChargeLayingTest extends GameBoardTestCase {

    private static final Coords CLEAR_COORDS = new Coords(0, 0);
    private static final Coords TERRAIN_BUILDING_COORDS = new Coords(0, 1);
    private static final Coords BUILDING_ENTITY_COORDS = new Coords(0, 2);
    private static final Coords BRIDGE_COORDS = new Coords(0, 3);
    private static final Coords FUEL_TANK_COORDS = new Coords(0, 4);

    static {
        // Note: board files assign hexes sequentially; the coordinates in the hex lines are ignored.
        // Hex (0,1) carries a regular terrain building, (0,3) a bridge, (0,4) a fuel tank; (0,0) and (0,2)
        // are clear. The BuildingEntity is placed at (0,2) at runtime (stamping its own building terrain).
        initializeBoard("DEMOLITION_TEST_BOARD", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "bldg_elev:2;building:2:8;bldg_cf:100" ""
              hex 0103 0 "" ""
              hex 0104 0 "bridge:1;bridge_cf:100;bridge_elev:1" ""
              hex 0105 0 "fuel_tank:1;fuel_tank_cf:100;fuel_tank_elev:1;fuel_tank_magn:100" ""
              end"""
        );
    }

    private TWGameManager gameManager;
    private Game game;
    private Board board;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        Player player = new Player(0, "Test");
        gameManager = Mockito.spy(new TWGameManager());

        // Mock methods that require Server to avoid NullPointerException
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), any(int.class));
        Mockito.doNothing().when(gameManager).entityUpdate(any(int.class));
        Mockito.doNothing().when(gameManager).sendChangedBuildings(any());

        game = gameManager.getGame();
        game.addPlayer(0, player);

        board = getBoard("DEMOLITION_TEST_BOARD");
        game.setBoard(board);
    }

    private BuildingEntity createBuildingEntityOnBoard(Coords position) {
        BuildingEntity building = new BuildingEntity(BuildingType.MEDIUM, 1);
        building.getInternalBuilding().setBuildingHeight(3);
        building.getInternalBuilding().addHex(new CubeCoords(0, 0, 0), 50, 10, BasementType.UNKNOWN, false);
        building.setOwner(game.getPlayer(0));
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setId(game.getNextEntityId());
        game.addEntity(building);
        building.setPosition(position);
        building.updateBuildingEntityHexes(board.getBoardId(), gameManager);
        return building;
    }

    private Infantry createLayingInfantry(Coords position) {
        Infantry infantry = new ConvInfantry();
        infantry.setId(game.getNextEntityId());
        infantry.setOwner(game.getPlayer(0));
        infantry.setPosition(position);
        infantry.setBoardId(board.getBoardId());
        infantry.setElevation(0);
        infantry.setDeployed(true);
        game.addEntity(infantry);
        return infantry;
    }

    @Test
    void layingContinuesInsideTerrainBuilding() {
        // Arrange - platoon started laying this turn (counter 0) inside a regular terrain building
        Infantry infantry = createLayingInfantry(TERRAIN_BUILDING_COORDS);
        infantry.turnsLayingExplosives = 0;

        // Act
        infantry.newRound(2);

        // Assert
        assertEquals(1, infantry.turnsLayingExplosives,
              "Laying must continue (counter incremented) while inside a terrain building");
    }

    @Test
    void layingContinuesInsideAdvancedBuilding() {
        // Arrange - regression test for #8328: platoon laying inside an advanced building (BuildingEntity)
        createBuildingEntityOnBoard(BUILDING_ENTITY_COORDS);
        Infantry infantry = createLayingInfantry(BUILDING_ENTITY_COORDS);
        infantry.turnsLayingExplosives = 0;

        // Act
        infantry.newRound(2);

        // Assert
        assertEquals(1, infantry.turnsLayingExplosives,
              "Laying must continue (counter incremented) while inside an advanced building (#8328)");
    }

    @Test
    void layingIsAbandonedOutsideBuilding() {
        // Arrange - platoon that left the structure gives up laying
        Infantry infantry = createLayingInfantry(CLEAR_COORDS);
        infantry.turnsLayingExplosives = 1;

        // Act
        infantry.newRound(2);

        // Assert
        assertEquals(-1, infantry.turnsLayingExplosives,
              "Laying must be abandoned when the platoon is no longer inside a building");
    }

    @Test
    void layingContinuesOnBridge() {
        // Arrange - regression test for #8330: bridges are valid demolition targets, TO:AUE p.152
        Infantry infantry = createLayingInfantry(BRIDGE_COORDS);
        infantry.turnsLayingExplosives = 0;

        // Act
        infantry.newRound(2);

        // Assert
        assertEquals(1, infantry.turnsLayingExplosives,
              "Laying must continue (counter incremented) while in a bridge hex (#8330)");
    }

    @Test
    void layingContinuesInFuelTankHex() {
        // Arrange - regression test for #687: fuel tanks are valid demolition targets
        Infantry infantry = createLayingInfantry(FUEL_TANK_COORDS);
        infantry.turnsLayingExplosives = 0;

        // Act
        infantry.newRound(2);

        // Assert
        assertEquals(1, infantry.turnsLayingExplosives,
              "Laying must continue (counter incremented) while in a fuel tank hex (#687)");
    }

    @Test
    void noDamageWhenNotLayingExplosives() {
        // Arrange - getDamageFor must not go negative for a platoon that is not laying (counter is -1)
        Infantry infantry = createLayingInfantry(CLEAR_COORDS);
        infantry.setSquadSize(7);
        infantry.autoSetInternal();
        infantry.turnsLayingExplosives = -1;

        // Sanity: the platoon would deal damage per turn if it were laying
        assertTrue(LayExplosivesAttackAction.getDamagePerTurn(infantry) > 0,
              "Test platoon must have a positive per-turn damage for this test to be meaningful");

        // Act & Assert
        assertEquals(0, LayExplosivesAttackAction.getDamageFor(infantry),
              "A platoon that is not laying explosives must have 0 charge damage, never negative");
    }

    @Test
    void layingContinuesOnBuildingRoof() {
        // Arrange - a platoon on the roof is still in the target hex, TO:AUE p.152 ("the number of turns the
        // platoon spends in the target hex"). Starting on the roof is legal, so continuing must be legal too,
        // otherwise the work is abandoned silently (#8328).
        Infantry infantry = createLayingInfantry(TERRAIN_BUILDING_COORDS);
        infantry.setElevation(2); // bldg_elev:2 - standing on top of the building
        infantry.turnsLayingExplosives = 0;

        // Act
        infantry.newRound(2);

        // Assert
        assertEquals(1, infantry.turnsLayingExplosives,
              "Laying must continue (counter incremented) while on the roof of the target building (#8328)");
    }
}
