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
import static org.junit.jupiter.api.Assertions.assertSame;
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
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.BuildingEntity;
import megamek.common.units.BuildingTarget;
import megamek.common.units.IBuilding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * An Advanced Building (such as a migrated gun emplacement) deployed onto a hex that already holds a map building
 * replaces that building on the server as well as on the clients. Issue #9374: the server kept the old building in its
 * building list, and once the emplacement collapsed the end-of-phase physical attack check built a target on a hex with
 * no building in it and threw, freezing the game.
 */
@DisplayName("Advanced Building deployed onto a map building")
class BuildingEntityReplacesMapBuildingTest extends GameBoardTestCase {

    private static final Coords EMPLACEMENT_HEX = new Coords(1, 1);
    private static final Coords FAR_BUILDING_HEX = new Coords(4, 2);
    private static final String BOARD_NAME = "BUILDING_REPLACED_BY_EMPLACEMENT_BOARD";

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
        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Defender"));
        // a fresh board each test: hex 0202 holds the map building the emplacement lands on, 0503 one far away
        initializeBoard(BOARD_NAME, """
              size 5 3
              hex 0101 0 "" ""
              hex 0201 0 "" ""
              hex 0301 0 "" ""
              hex 0401 0 "" ""
              hex 0501 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "bldg_elev:2;building:4;bldg_class:0;bldg_cf:90" ""
              hex 0302 0 "" ""
              hex 0402 0 "" ""
              hex 0502 0 "" ""
              hex 0103 0 "" ""
              hex 0203 0 "" ""
              hex 0303 0 "" ""
              hex 0403 0 "" ""
              hex 0503 0 "bldg_elev:1;building:3;bldg_class:0;bldg_cf:60" ""
              end""");
        board = getBoard(BOARD_NAME);
        game.setBoard(board);
    }

    /** A one-hex emplacement deployed onto the map building at {@link #EMPLACEMENT_HEX}. */
    private BuildingEntity deployEmplacementOntoMapBuilding() {
        BuildingEntity emplacement = new BuildingEntity(BuildingType.MEDIUM, IBuilding.STANDARD);
        emplacement.setGame(game);
        emplacement.getInternalBuilding().setBuildingHeight(1);
        emplacement.getInternalBuilding().addHex(CubeCoords.ZERO, 40, 0, BasementType.NONE, false);
        emplacement.refreshLocations();
        emplacement.refreshAdditionalLocations();
        emplacement.setOwner(game.getPlayer(0));
        emplacement.setId(1);
        game.addEntity(emplacement);
        emplacement.setPosition(EMPLACEMENT_HEX);
        emplacement.setDeployed(true);
        emplacement.updateBuildingEntityHexes(board.getBoardId(), gameManager);
        return emplacement;
    }

    @Test
    @DisplayName("The setup really has a map building under the emplacement's hex")
    void mapBuildingStandsOnTheHexBeforeDeployment() {
        assertNotNull(board.getBuildingAt(EMPLACEMENT_HEX));
        assertNotNull(board.getBuildingAt(FAR_BUILDING_HEX));
    }

    @Test
    @DisplayName("Deploying onto a map building takes the old building out of the server's building list")
    void replacedBuildingLeavesTheBuildingList() {
        IBuilding mapBuilding = board.getBuildingAt(EMPLACEMENT_HEX);

        BuildingEntity emplacement = deployEmplacementOntoMapBuilding();

        assertFalse(board.getBuildingsVector().contains(mapBuilding),
              "the clients drop this building on the remove packet; the server kept it before the fix");
        assertSame(emplacement, board.getBuildingAt(EMPLACEMENT_HEX));
    }

    @Test
    @DisplayName("A map building the emplacement does not touch is kept")
    void untouchedMapBuildingIsKept() {
        IBuilding farBuilding = board.getBuildingAt(FAR_BUILDING_HEX);

        deployEmplacementOntoMapBuilding();

        assertTrue(board.getBuildingsVector().contains(farBuilding));
        assertSame(farBuilding, board.getBuildingAt(FAR_BUILDING_HEX));
    }

    @Test
    @DisplayName("After the emplacement collapses, every hex a listed building claims can still be targeted")
    void everyListedBuildingHexCanBeTargetedAfterTheEmplacementCollapses() {
        deployEmplacementOntoMapBuilding();

        // the same call BuildingCollapseHandler makes when the emplacement's hex collapses
        board.collapseBuilding(EMPLACEMENT_HEX);
        assertNull(board.getBuildingAt(EMPLACEMENT_HEX));

        // the walk Entity.isEligibleForPhysical does over the board's buildings at the start of each phase
        for (IBuilding building : board.getBuildingsVector()) {
            for (Coords buildingHex : building.getCoordsList()) {
                assertDoesNotThrow(() -> new BuildingTarget(buildingHex, board, false),
                      "issue #9374 threw 'No building at' here and the phase never advanced");
            }
        }
    }
}
