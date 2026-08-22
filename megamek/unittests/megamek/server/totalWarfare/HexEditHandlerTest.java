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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import megamek.common.Player;
import megamek.common.board.Board;
import megamek.utils.BoardLoader;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.Entity;
import megamek.common.units.BipedMek;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests the gamemaster's single-hex terrain edits: that a legal change is written to the board, that a change which
 * would leave the hex invalid is refused, and that the water depth under a unit is left alone.
 */
class HexEditHandlerTest {

    /**
     * Rebuilt for every test. The board must not be shared between tests, because each test edits it and a shared
     * board would carry one test's edits into the next.
     */
    private static final String BOARD_DATA = """
          size 4 4
          hex 0101 0 "" ""
          hex 0102 0 "woods:1" ""
          hex 0103 0 "water:2" ""
          hex 0104 0 "bldg_elev:2;building:2;bldg_class:1;bldg_cf:40" ""
          end""";

    private static final String GAMEMASTER = "Referee";
    // hex 0101 is the first column of the first row, and the numbering runs along the row, so 0102 and 0103 are
    // its neighbours to the east rather than hexes below it
    private static final Coords BARE_HEX = new Coords(0, 0);
    private static final Coords WOODS_HEX = new Coords(1, 0);
    private static final Coords WATER_HEX = new Coords(2, 0);
    private static final Coords BUILDING_HEX = new Coords(3, 0);

    private TWGameManager gameManager;
    private Game game;
    private Board board;
    private HexEditHandler hexEditHandler;

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), anyInt());

        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Test"));
        board = BoardLoader.initializeBoard(BOARD_DATA);
        game.setBoard(board);

        hexEditHandler = new HexEditHandler(gameManager);
    }

    /** Puts a Mek in the given hex so the occupied-water rule has something to find. */
    private void placeUnitIn(Coords coords) {
        Entity mek = new BipedMek();
        mek.setOwner(game.getPlayer(0));
        mek.setId(1);
        game.addEntity(mek);
        mek.setPosition(coords);
        mek.setDeployed(true);
    }

    @Test
    void floodingAnEmptyHexSetsTheDepth() {
        String refusal = hexEditHandler.setTerrain(BARE_HEX, board.getBoardId(), Terrains.WATER, 2, GAMEMASTER);

        assertNull(refusal, "flooding an empty hex should be allowed");
        assertEquals(2, board.getHex(BARE_HEX).depth(), "the hex should now be two levels deep");
    }

    @Test
    void aLevelOfZeroRemovesTheTerrain() {
        String refusal = hexEditHandler.setTerrain(WOODS_HEX,
              board.getBoardId(),
              Terrains.WOODS,
              HexEditHandler.REMOVE_TERRAIN_LEVEL,
              GAMEMASTER);

        assertNull(refusal, "removing woods should be allowed");
        assertFalse(board.getHex(WOODS_HEX).containsTerrain(Terrains.WOODS), "the woods should be gone");
    }

    @Test
    void rapidsOutsideWaterAreRefused() {
        String refusal = hexEditHandler.setTerrain(BARE_HEX, board.getBoardId(), Terrains.RAPIDS, 1, GAMEMASTER);

        assertNotNull(refusal, "rapids need water to sit in, so this should be refused");
        assertFalse(board.getHex(BARE_HEX).containsTerrain(Terrains.RAPIDS),
              "a refused edit must leave the hex untouched");
    }

    @Test
    void rapidsInWaterAreAllowed() {
        String refusal = hexEditHandler.setTerrain(WATER_HEX, board.getBoardId(), Terrains.RAPIDS, 2, GAMEMASTER);

        assertNull(refusal, "a torrent in depth 2 water should be allowed");
        assertEquals(2, board.getHex(WATER_HEX).terrainLevel(Terrains.RAPIDS), "the hex should hold a torrent");
    }

    @Test
    void changingWaterDepthUnderAUnitIsRefused() {
        placeUnitIn(WATER_HEX);

        String refusal = hexEditHandler.setTerrain(WATER_HEX, board.getBoardId(), Terrains.WATER, 4, GAMEMASTER);

        assertNotNull(refusal, "there is no rule for flooding the ground under a unit, so this should be refused");
        assertEquals(2, board.getHex(WATER_HEX).depth(), "the depth must be left as it was");
    }

    @Test
    void changingOtherTerrainUnderAUnitIsAllowed() {
        placeUnitIn(WOODS_HEX);

        String refusal = hexEditHandler.setTerrain(WOODS_HEX, board.getBoardId(), Terrains.WOODS, 2, GAMEMASTER);

        assertNull(refusal, "only water depth is held back by an occupying unit");
        assertEquals(2, board.getHex(WOODS_HEX).terrainLevel(Terrains.WOODS), "the woods should have grown");
    }

    @Test
    void clearingAnOccupiedWaterHexIsRefused() {
        placeUnitIn(WATER_HEX);

        String refusal = hexEditHandler.clearHex(WATER_HEX, board.getBoardId(), GAMEMASTER);

        assertNotNull(refusal, "clearing water from under a unit drains it, which is the same problem");
        assertTrue(board.getHex(WATER_HEX).containsTerrain(Terrains.WATER), "the water must be left as it was");
    }

    @Test
    void clearingAnEmptyHexRemovesEverything() {
        String refusal = hexEditHandler.clearHex(WOODS_HEX, board.getBoardId(), GAMEMASTER);

        assertNull(refusal, "clearing an unoccupied hex should be allowed");
        assertFalse(board.getHex(WOODS_HEX).containsTerrain(Terrains.WOODS), "the woods should be gone");
    }

    @Test
    void growingUltraWoodsRaisesTheFoliageWithIt() {
        String refusal = hexEditHandler.setTerrain(WOODS_HEX, board.getBoardId(), Terrains.WOODS, 3, GAMEMASTER);

        assertNull(refusal, "growing the woods to ultra should be allowed");
        assertEquals(3, board.getHex(WOODS_HEX).terrainLevel(Terrains.FOLIAGE_ELEV),
              "ultra woods stand higher, so the foliage elevation must rise with them");
    }

    @Test
    void removingWoodsTakesTheFoliageElevationWithIt() {
        hexEditHandler.setTerrain(WOODS_HEX,
              board.getBoardId(),
              Terrains.WOODS,
              HexEditHandler.REMOVE_TERRAIN_LEVEL,
              GAMEMASTER);

        assertFalse(board.getHex(WOODS_HEX).containsTerrain(Terrains.FOLIAGE_ELEV),
              "foliage elevation left behind with nothing growing in it would make the hex invalid");
    }

    @Test
    void modifyingTerrainSetsItsFactorWithoutChangingIt() {
        String refusal = hexEditHandler.setTerrainFactor(WOODS_HEX, board.getBoardId(), Terrains.WOODS, 20,
              GAMEMASTER);

        assertNull(refusal, "weakening the woods should be allowed");
        assertEquals(20, board.getHex(WOODS_HEX).getTerrain(Terrains.WOODS).getTerrainFactor(),
              "the woods should be left with less of it standing");
        assertEquals(1, board.getHex(WOODS_HEX).terrainLevel(Terrains.WOODS),
              "modifying the terrain must not change what the terrain is");
    }

    @Test
    void woodsStartAtTheirBookTerrainFactor() {
        assertEquals(50, board.getHex(WOODS_HEX).getTerrain(Terrains.WOODS).getTerrainFactor(),
              "light woods start at a terrain factor of 50");
    }

    @Test
    void modifyingATerrainThatIsNotThereIsRefused() {
        String refusal = hexEditHandler.setTerrainFactor(BARE_HEX, board.getBoardId(), Terrains.WOODS, 20,
              GAMEMASTER);

        assertNotNull(refusal, "there is no woods in a bare hex to modify");
    }

    @Test
    void floodingAHexWithABuildingInItIsRefused() {
        String refusal = hexEditHandler.setTerrain(BUILDING_HEX, board.getBoardId(), Terrains.WATER, 2, GAMEMASTER);

        assertNotNull(refusal, "a building cannot stand in water, so flooding its hex should be refused");
        assertEquals(0, board.getHex(BUILDING_HEX).depth(), "the hex must be left dry");
        assertTrue(board.getHex(BUILDING_HEX).containsTerrain(Terrains.BUILDING),
              "and the building must still be standing");
    }

    @Test
    void aBuildingHexCanStillBeChangedInOtherWays() {
        String refusal = hexEditHandler.setTerrain(BUILDING_HEX, board.getBoardId(), Terrains.ROUGH, 1, GAMEMASTER);

        assertNull(refusal, "only water is held back by a building; rough ground around it is fine");
    }
}
