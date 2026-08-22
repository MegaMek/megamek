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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrains;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests the gamemaster's edits to a building: that the construction factor is written to the building, that a hex
 * without a building is refused, and that setting the factor to zero brings the building down rather than leaving it
 * standing on nothing.
 */
class BuildingEditHandlerTest {

    /**
     * Rebuilt for every test, because each test changes the building on it and a shared board would carry one test's
     * change into the next.
     */
    private static final String BOARD_DATA = """
          size 3 3
          hex 0101 0 "bldg_elev:2;building:2;bldg_class:1;bldg_cf:40" ""
          hex 0102 0 "" ""
          hex 0103 0 "" ""
          end""";

    private static final String GAMEMASTER = "Referee";
    private static final Coords BUILDING_HEX = new Coords(0, 0);
    private static final Coords EMPTY_HEX = new Coords(1, 0);

    private TWGameManager gameManager;
    private Game game;
    private Board board;
    private BuildingEditHandler buildingEditHandler;

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), anyInt());
        Mockito.doNothing().when(gameManager).sendChangedBuildings(any());

        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Test"));
        board = BoardLoader.initializeBoard(BOARD_DATA);
        game.setBoard(board);

        buildingEditHandler = new BuildingEditHandler(gameManager);
    }

    /** @return an edit that sets only the construction factor and leaves everything else alone */
    private static BuildingEditHandler.BuildingEdit cfOnly(int constructionFactor) {
        return new BuildingEditHandler.BuildingEdit(constructionFactor, null, null, null);
    }

    @Test
    void theBoardStartsWithABuildingToEdit() {
        IBuilding building = board.getBuildingAt(BUILDING_HEX);

        assertNotNull(building, "the test board should hold a building to edit");
        assertEquals(40, building.getCurrentCF(BUILDING_HEX), "the building should start at its board factor");
    }

    @Test
    void weakeningABuildingSetsItsConstructionFactor() {
        String refusal = buildingEditHandler.applyEdit(BUILDING_HEX, cfOnly(15), GAMEMASTER);

        assertNull(refusal, "weakening a building should be allowed");
        assertEquals(15, board.getBuildingAt(BUILDING_HEX).getCurrentCF(BUILDING_HEX),
              "the building should be left at the factor it was set to");
    }

    @Test
    void thePhaseFactorMovesWithIt() {
        buildingEditHandler.applyEdit(BUILDING_HEX, cfOnly(15), GAMEMASTER);

        assertEquals(15, board.getBuildingAt(BUILDING_HEX).getPhaseCF(BUILDING_HEX),
              "leaving the phase factor behind would measure this phase's damage against the old value");
    }

    @Test
    void aHexWithNoBuildingIsRefused() {
        String refusal = buildingEditHandler.applyEdit(EMPTY_HEX, cfOnly(20), GAMEMASTER);

        assertNotNull(refusal, "there is nothing to change in a hex with no building");
    }

    @Test
    void aFactorOfZeroBringsTheBuildingDown() {
        String refusal = buildingEditHandler.applyEdit(BUILDING_HEX,
              cfOnly(BuildingEditHandler.COLLAPSING_CONSTRUCTION_FACTOR),
              GAMEMASTER);

        assertNull(refusal, "collapsing a building should be allowed");
        assertNull(board.getBuildingAt(BUILDING_HEX),
              "a collapsed building is gone from the hex, not merely left standing at zero");
    }

    @Test
    void theHeightIsWrittenToTheBuildingAndTheHex() {
        String refusal = buildingEditHandler.applyEdit(BUILDING_HEX,
              new BuildingEditHandler.BuildingEdit(null, null, 5, null), GAMEMASTER);

        assertNull(refusal, "raising a building should be allowed");
        assertEquals(5, board.getBuildingAt(BUILDING_HEX).getHeight(BUILDING_HEX),
              "the building should stand at its new height");
        assertEquals(5, board.getHex(BUILDING_HEX).terrainLevel(Terrains.BLDG_ELEV),
              "the hex is what the board is drawn from, so it has to agree with the building");
    }

    @Test
    void theArmorIsWrittenToTheBuilding() {
        String refusal = buildingEditHandler.applyEdit(BUILDING_HEX,
              new BuildingEditHandler.BuildingEdit(null, 12, null, null), GAMEMASTER);

        assertNull(refusal, "armouring a building should be allowed");
        assertEquals(12, board.getBuildingAt(BUILDING_HEX).getArmor(BUILDING_HEX),
              "the building should carry the armor it was given");
    }

    @Test
    void valuesLeftAloneAreNotTouched() {
        buildingEditHandler.applyEdit(BUILDING_HEX,
              new BuildingEditHandler.BuildingEdit(null, 12, null, null), GAMEMASTER);

        assertEquals(40, board.getBuildingAt(BUILDING_HEX).getCurrentCF(BUILDING_HEX),
              "only the armor was set, so the construction factor should be untouched");
    }
}
