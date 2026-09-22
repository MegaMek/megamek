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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.BuildingEntity;
import megamek.common.units.BuildingTarget;
import megamek.common.units.IBuilding;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * An Advanced Building placed on a fixed hex by a scenario file is written into the map when the scenario starts,
 * the same as one deployed during play.
 */
@DisplayName("Scenario-placed Advanced Buildings")
class ScenarioBuildingPlacementHandlerTest extends GameBoardTestCase {

    private static final Coords TOWER_HEX = new Coords(1, 1);
    private static final String BOARD_NAME = "SCENARIO_BUILDING_BOARD";


    private TWGameManager gameManager;
    private Game game;
    private Board board;

    @BeforeEach
    void beforeEach() {
        gameManager = spy(new TWGameManager());
        doNothing().when(gameManager).send(any(Packet.class));
        doNothing().when(gameManager).sendChangedHex(any(Coords.class), any(int.class));
        doNothing().when(gameManager).sendNewBuildings(any());
        doNothing().when(gameManager).sendRemovedBuildings(any());
        doNothing().when(gameManager).placeLobbyObjectives();
        doNothing().when(gameManager).changePhase(any(GamePhase.class));
        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Defender"));
        // a fresh board each test: a building written into one test's map must not still be there in the next
        initializeBoard(BOARD_NAME, """
              size 3 3
              hex 0101 0 "" ""
              hex 0201 0 "" ""
              hex 0301 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "" ""
              hex 0302 0 "" ""
              hex 0103 0 "" ""
              hex 0203 0 "" ""
              hex 0303 0 "" ""
              end""");
        board = getBoard(BOARD_NAME);
        game.setBoard(board);
    }

    /** A one-hex tower set up the way a scenario file's {@code at:} leaves it: deployed, positioned, nothing more. */
    private BuildingEntity towerPlacedByScenario() {
        BuildingEntity tower = new BuildingEntity(BuildingType.MEDIUM, IBuilding.STANDARD);
        tower.setGame(game);
        tower.getInternalBuilding().setBuildingHeight(1);
        tower.getInternalBuilding().addHex(CubeCoords.ZERO, 40, 0, BasementType.NONE, false);
        tower.refreshLocations();
        tower.refreshAdditionalLocations();
        tower.setOwner(game.getPlayer(0));
        tower.setId(1);
        game.addEntity(tower);
        tower.setPosition(TOWER_HEX);
        tower.setDeployed(true);
        return tower;
    }

    @Test
    @DisplayName("Before the scenario starts, a tower placed by the file is a unit on open ground")
    void scenarioPlacedTowerStartsWithoutBuildingTerrain() {
        towerPlacedByScenario();

        assertFalse(board.getHex(TOWER_HEX).containsTerrain(Terrains.BUILDING));
        assertNull(board.getBuildingAt(TOWER_HEX), "this is the state the player saw: a hex with no building in it");
    }

    @Test
    @DisplayName("Ending the scenario-start phase writes the tower into its hex")
    void scenarioStartWritesTheTowerIntoTheMap() {
        towerPlacedByScenario();
        game.setPhase(GamePhase.STARTING_SCENARIO);

        new TWPhaseEndManager(gameManager).managePhase();

        assertTrue(board.getHex(TOWER_HEX).containsTerrain(Terrains.BUILDING));
        assertNotNull(board.getBuildingAt(TOWER_HEX));
    }

    @Test
    @DisplayName("Once written in, the tower's hex can be targeted, which is what the firing arc does")
    void writtenTowerCanBeTargeted() {
        towerPlacedByScenario();
        game.setPhase(GamePhase.STARTING_SCENARIO);

        new TWPhaseEndManager(gameManager).managePhase();

        assertDoesNotThrow(() -> new BuildingTarget(TOWER_HEX, board, Targetable.TYPE_BUILDING),
              "the firing-arc display built exactly this target and got 'No building at' before the fix");
    }

    @Test
    @DisplayName("A building that has not deployed yet is left for deployment to write in")
    void undeployedBuildingIsLeftAlone() {
        BuildingEntity tower = towerPlacedByScenario();
        tower.setDeployed(false);
        tower.setPosition(null);

        new ScenarioBuildingPlacementHandler(gameManager).placePreDeployedBuildings();

        assertFalse(board.getHex(TOWER_HEX).containsTerrain(Terrains.BUILDING));
    }
}
