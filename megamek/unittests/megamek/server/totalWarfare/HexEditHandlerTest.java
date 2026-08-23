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

import java.util.Arrays;
import java.util.List;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.HexEditSpec;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.util.SerializationHelper;
import megamek.utils.BoardLoader;
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

    /** @return an edit that paints every given hex with water at the given depth */
    private HexEditSpec floodOf(int depth, Coords... hexes) {
        HexEditSpec spec = new HexEditSpec(board.getBoardId());
        for (Coords hex : hexes) {
            HexEditSpec.HexPaint paint = new HexEditSpec.HexPaint();
            paint.setTerrain(Terrains.WATER, depth);
            spec.paint(hex, paint);
        }
        return spec;
    }

    /** @return a paint holding the given terrain at the given level, and nothing else */
    private static HexEditSpec.HexPaint paintOf(int terrainType, int level) {
        HexEditSpec.HexPaint paint = new HexEditSpec.HexPaint();
        paint.setTerrain(terrainType, level);
        return paint;
    }

    @Test
    void oneEditFloodsEveryHexItNames() {
        String refusal = hexEditHandler.applyHexEdit(floodOf(2, BARE_HEX, WOODS_HEX), GAMEMASTER);

        assertNull(refusal, "flooding two clear hexes should be allowed");
        assertEquals(2, board.getHex(BARE_HEX).depth(), "the first hex should be flooded");
        assertEquals(2, board.getHex(WOODS_HEX).depth(), "and so should the second");
        assertFalse(board.getHex(WOODS_HEX).containsTerrain(Terrains.WOODS),
              "the edit says what the hex ends up as, so the woods it used to hold are gone");
    }

    @Test
    void oneBadHexRefusesTheWholeEdit() {
        String refusal = hexEditHandler.applyHexEdit(floodOf(2, BARE_HEX, BUILDING_HEX), GAMEMASTER);

        assertNotNull(refusal, "the building hex cannot be flooded, so the edit should be refused");
        assertEquals(0, board.getHex(BARE_HEX).depth(),
              "and the hex that could have been flooded must be left alone, not half-applied");
    }

    @Test
    void theRefusalNamesTheHexThatCausedIt() {
        String refusal = hexEditHandler.applyHexEdit(floodOf(2, BARE_HEX, BUILDING_HEX), GAMEMASTER);

        assertNotNull(refusal);
        assertTrue(refusal.contains(String.valueOf(BUILDING_HEX.getBoardNum())),
              "a gamemaster needs to know which hex stopped the edit, not just that one did");
    }

    @Test
    void anEditWithNoTerrainClearsTheHexes() {
        HexEditSpec spec = new HexEditSpec(board.getBoardId());
        spec.paint(WOODS_HEX, new HexEditSpec.HexPaint());

        String refusal = hexEditHandler.applyHexEdit(spec, GAMEMASTER);

        assertNull(refusal, "clearing a hex should be allowed");
        assertFalse(board.getHex(WOODS_HEX).containsTerrain(Terrains.WOODS), "the woods should be gone");
    }

    @Test
    void anEditNamingNoHexesIsRefused() {
        String refusal = hexEditHandler.applyHexEdit(new HexEditSpec(board.getBoardId()), GAMEMASTER);

        assertNotNull(refusal, "an edit with no hexes chosen has nothing to do");
    }

    @Test
    void aTerrainEditLeavesAStructureStanding() {
        HexEditSpec spec = new HexEditSpec(board.getBoardId());
        spec.paint(BUILDING_HEX, paintOf(Terrains.ROUGH, 1));

        String refusal = hexEditHandler.applyHexEdit(spec, GAMEMASTER);

        assertNull(refusal, "roughening the ground around a building should be allowed");
        assertTrue(board.getHex(BUILDING_HEX).containsTerrain(Terrains.BUILDING),
              "a terrain edit describes the ground, so the building on it must survive");
        assertTrue(board.getHex(BUILDING_HEX).containsTerrain(Terrains.ROUGH), "and the new ground should be there");
    }

    @Test
    void undoPutsTheHexesBackAsTheyWere() {
        hexEditHandler.applyHexEdit(floodOf(2, WOODS_HEX), GAMEMASTER);
        assertEquals(2, board.getHex(WOODS_HEX).depth(), "the hex should have been flooded first");

        HexEditSpec undo = new HexEditSpec(board.getBoardId());
        undo.setUndoingLastEdit(true);
        String refusal = hexEditHandler.applyHexEdit(undo, GAMEMASTER);

        assertNull(refusal, "there was an edit to take back");
        assertEquals(0, board.getHex(WOODS_HEX).depth(), "the water should be gone again");
        assertTrue(board.getHex(WOODS_HEX).containsTerrain(Terrains.WOODS),
              "and the woods the hex used to hold should be back");
    }

    @Test
    void undoWithNothingToUndoIsRefused() {
        HexEditSpec undo = new HexEditSpec(board.getBoardId());
        undo.setUndoingLastEdit(true);

        assertNotNull(hexEditHandler.applyHexEdit(undo, GAMEMASTER), "there is no edit to take back");
    }

    @Test
    void onlyTheMostRecentEditCanBeTakenBack() {
        hexEditHandler.applyHexEdit(floodOf(2, WOODS_HEX), GAMEMASTER);
        hexEditHandler.applyHexEdit(floodOf(3, BARE_HEX), GAMEMASTER);

        HexEditSpec undo = new HexEditSpec(board.getBoardId());
        undo.setUndoingLastEdit(true);
        hexEditHandler.applyHexEdit(undo, GAMEMASTER);

        assertEquals(0, board.getHex(BARE_HEX).depth(), "the most recent edit should be taken back");
        assertEquals(2, board.getHex(WOODS_HEX).depth(), "the one before it should stand");
    }

    @Test
    void anUndoCannotBeUndoneTwice() {
        hexEditHandler.applyHexEdit(floodOf(2, WOODS_HEX), GAMEMASTER);
        HexEditSpec undo = new HexEditSpec(board.getBoardId());
        undo.setUndoingLastEdit(true);
        hexEditHandler.applyHexEdit(undo, GAMEMASTER);

        assertNotNull(hexEditHandler.applyHexEdit(undo, GAMEMASTER),
              "the remembered hexes are used up by the first undo");
    }

    @Test
    void aReservoirNeedsBothALevelAndADepth() {
        // the shape of a dam board hex: "hex 0101 5 water:8" - ground at level 5 holding water 8 deep
        HexEditSpec.HexPaint reservoir = paintOf(Terrains.WATER, 8);
        reservoir.setLevel(5);
        HexEditSpec spec = new HexEditSpec(board.getBoardId());
        spec.paint(BARE_HEX, reservoir);

        String refusal = hexEditHandler.applyHexEdit(spec, GAMEMASTER);

        assertNull(refusal, "a reservoir above the surrounding ground should be allowed");
        assertEquals(5, board.getHex(BARE_HEX).getLevel(), "the ground should have been raised");
        assertEquals(8, board.getHex(BARE_HEX).depth(), "and the water should be eight deep on top of it");
    }

    @Test
    void anEditWithNoLevelLeavesTheGroundWhereItIs() {
        board.getHex(BARE_HEX).setLevel(3);

        hexEditHandler.applyHexEdit(floodOf(2, BARE_HEX), GAMEMASTER);

        assertEquals(3, board.getHex(BARE_HEX).getLevel(),
              "an edit that says nothing about the level must not flatten the hex");
    }

    @Test
    void undoPutsTheGroundLevelBackToo() {
        board.getHex(BARE_HEX).setLevel(2);
        HexEditSpec.HexPaint raised = new HexEditSpec.HexPaint();
        raised.setLevel(7);
        HexEditSpec spec = new HexEditSpec(board.getBoardId());
        spec.paint(BARE_HEX, raised);
        hexEditHandler.applyHexEdit(spec, GAMEMASTER);

        HexEditSpec undo = new HexEditSpec(board.getBoardId());
        undo.setUndoingLastEdit(true);
        hexEditHandler.applyHexEdit(undo, GAMEMASTER);

        assertEquals(2, board.getHex(BARE_HEX).getLevel(), "the ground should be back where it was");
    }

    @Test
    void differentHexesCanBePaintedDifferently() {
        HexEditSpec spec = new HexEditSpec(board.getBoardId());
        spec.paint(BARE_HEX, paintOf(Terrains.WATER, 2));
        spec.paint(WOODS_HEX, paintOf(Terrains.ROUGH, 1));

        String refusal = hexEditHandler.applyHexEdit(spec, GAMEMASTER);

        assertNull(refusal, "painting a channel and its bank in one action should be allowed");
        assertEquals(2, board.getHex(BARE_HEX).depth(), "the channel hex should hold water");
        assertTrue(board.getHex(WOODS_HEX).containsTerrain(Terrains.ROUGH), "and the bank hex should hold rough");
        assertEquals(0, board.getHex(WOODS_HEX).depth(), "the bank must not have been flooded too");
    }

    /** @return a paint holding rough ground and nothing else */
    private static HexEditSpec.HexPaint paintOfRough() {
        HexEditSpec.HexPaint paint = new HexEditSpec.HexPaint();
        paint.setTerrain(Terrains.ROUGH, 1);
        return paint;
    }

    @Test
    void aTerrainEditKeepsTheHexesSpecialImage() {
        board.getHex(WOODS_HEX).addTerrain(new Terrain(Terrains.GROUND_FLUFF, 100));

        hexEditHandler.applyHexEdit(floodOf(1, WOODS_HEX), GAMEMASTER);

        assertEquals(100, board.getHex(WOODS_HEX).terrainLevel(Terrains.GROUND_FLUFF),
              "fluff terrain chooses how a hex is drawn, so repainting the ground must not throw the artwork away");
    }

    @Test
    void aTerrainEditKeepsABuildingsSpecialImage() {
        board.getHex(BUILDING_HEX).addTerrain(new Terrain(Terrains.BLDG_FLUFF, 100));

        HexEditSpec spec = new HexEditSpec(board.getBoardId());
        spec.paint(BUILDING_HEX, paintOfRough());
        hexEditHandler.applyHexEdit(spec, GAMEMASTER);

        assertEquals(100, board.getHex(BUILDING_HEX).terrainLevel(Terrains.BLDG_FLUFF),
              "a drawn building must not become a generic box because the ground around it was repainted");
    }

    @Test
    void groundFloodedByAGamemasterCanBeMadeBareAgain() {
        hexEditHandler.applyHexEdit(floodOf(1, BARE_HEX), GAMEMASTER);
        assertEquals(1, board.getHex(BARE_HEX).depth(), "the hex should be flooded first");

        HexEditSpec bareGround = new HexEditSpec(board.getBoardId());
        bareGround.paint(BARE_HEX, new HexEditSpec.HexPaint());
        String refusal = hexEditHandler.applyHexEdit(bareGround, GAMEMASTER);

        assertNull(refusal, "draining a hex nobody is standing in should be allowed");
        assertEquals(0, board.getHex(BARE_HEX).depth(), "the water should be gone");
        assertFalse(board.getHex(BARE_HEX).containsTerrain(Terrains.WATER),
              "and no water terrain should be left behind at depth zero");
    }

    @Test
    void groundFloodedBeforeASaveCanBeMadeBareAfterTheLoad() {
        // A gamemaster floods hexes, saves, and comes back to it later. The board goes through XStream on the way
        // out and back, so the hex the second session edits is a rebuilt one - this checks it can still be drained.
        hexEditHandler.applyHexEdit(floodOf(1, BARE_HEX), GAMEMASTER);

        Game reloaded = (Game) SerializationHelper.getLoadSaveGameXStream()
              .fromXML(SerializationHelper.getSaveGameXStream().toXML(game));
        TWGameManager reloadedManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(reloadedManager).send(any(Packet.class));
        Mockito.doNothing().when(reloadedManager).sendChangedHex(any(Coords.class), anyInt());
        reloadedManager.setGame(reloaded);
        Board reloadedBoard = reloaded.getBoard(board.getBoardId());
        assertEquals(1, reloadedBoard.getHex(BARE_HEX).depth(), "the water should have survived the save");

        HexEditSpec bareGround = new HexEditSpec(reloadedBoard.getBoardId());
        bareGround.paint(BARE_HEX, new HexEditSpec.HexPaint());
        String refusal = new HexEditHandler(reloadedManager).applyHexEdit(bareGround, GAMEMASTER);

        assertNull(refusal, "a reloaded game must still let the gamemaster drain what they flooded");
        assertEquals(0, reloadedBoard.getHex(BARE_HEX).depth(), "the water should be gone after the reload too");
    }

    @Test
    void anEditAskingForWhatTheHexAlreadyHoldsIsAppliedAndChangesNothing() {
        // the shape of the bug behind "I picked bare ground and nothing happened": the dialog sent the hex's own
        // terrain straight back, and the server applied it, reported it and left the board looking untouched
        HexEditSpec sameAgain = new HexEditSpec(board.getBoardId());
        sameAgain.paint(WATER_HEX, paintOf(Terrains.WATER, 2));

        String refusal = hexEditHandler.applyHexEdit(sameAgain, GAMEMASTER);

        assertNull(refusal, "asking a hex for what it already holds is legal, just pointless");
        assertEquals(2, board.getHex(WATER_HEX).depth(), "and the hex is left exactly as it was");
    }

    @Test
    void aWideBrushEditPaintsEveryHexItCovers() {
        // the seven-hex brush sends the hex clicked and the ring around it as one edit; the two on this board that
        // are on it stand in for that ring
        String refusal = hexEditHandler.applyHexEdit(floodOf(1, BARE_HEX, WOODS_HEX, WATER_HEX), GAMEMASTER);

        assertNull(refusal, "a wide stroke over clear ground should be allowed");
        assertEquals(1, board.getHex(BARE_HEX).depth(), "the hex clicked should be flooded");
        assertEquals(1, board.getHex(WOODS_HEX).depth(), "and so should its neighbours");
        assertEquals(1, board.getHex(WATER_HEX).depth(), "including one that already held deeper water");
    }

    @Test
    void anEditNamingAHexOffTheBoardIsRefused() {
        // the dialog clips a wide brush at the board edge, and this is the guard behind that: a stroke that ran off
        // the map must be refused by name rather than dropping the server into a null hex
        HexEditSpec spec = floodOf(1, BARE_HEX);
        spec.paint(new Coords(99, 99), paintOf(Terrains.WATER, 1));

        String refusal = hexEditHandler.applyHexEdit(spec, GAMEMASTER);

        assertNotNull(refusal, "a hex that is not on the board cannot be painted");
        assertEquals(0, board.getHex(BARE_HEX).depth(),
              "and the hex that could have been painted must be left alone, not half-applied");
    }

    /**
     * @return the terrains a board author would have written in the hex, leaving out the ones the board adds for
     *       itself - the inclines and cliff edges it works out from how the hex sits against its neighbours
     */
    private static List<Integer> nonAutomaticTerrainsIn(Hex hex) {
        return Arrays.stream(hex.getTerrainTypes())
              .boxed()
              .filter(terrainType -> !Terrains.AUTOMATIC.contains(terrainType))
              .toList();
    }

    /** @return an edit that leaves one hex as bare ground sitting at the given level */
    private HexEditSpec bareGroundAt(int hexLevel, Coords hex) {
        HexEditSpec.HexPaint paint = new HexEditSpec.HexPaint();
        paint.setLevel(hexLevel);
        HexEditSpec spec = new HexEditSpec(board.getBoardId());
        spec.paint(hex, paint);
        return spec;
    }

    @Test
    void bareGroundAtALevelRaisesABareHill() {
        // the ground level and the terrain are two separate things to set, so bare ground with a level is how a hill
        // with nothing growing on it is made
        String refusal = hexEditHandler.applyHexEdit(bareGroundAt(3, WOODS_HEX), GAMEMASTER);

        assertNull(refusal, "raising bare ground should be allowed");
        assertEquals(3, board.getHex(WOODS_HEX).getLevel(), "the ground should have been raised");
        assertFalse(board.getHex(WOODS_HEX).containsTerrain(Terrains.WOODS),
              "and the woods that used to grow there should be gone");
        assertTrue(nonAutomaticTerrainsIn(board.getHex(WOODS_HEX)).isEmpty(),
              "leaving nothing on the hill but what the board works out for itself");
    }

    @Test
    void bareGroundAtANegativeLevelDigsAHole() {
        String refusal = hexEditHandler.applyHexEdit(bareGroundAt(-3, BARE_HEX), GAMEMASTER);

        assertNull(refusal, "digging bare ground down should be allowed");
        assertEquals(-3, board.getHex(BARE_HEX).getLevel(), "the ground should have been lowered");
        assertEquals(0, board.getHex(BARE_HEX).depth(), "a hole is not a pond, so no water should appear in it");
    }

    @Test
    void raisingTheGroundUnderAWaterHexDrainsItAsWell() {
        // bare ground says the hex ends up holding nothing, and the water is terrain like any other, so it goes
        String refusal = hexEditHandler.applyHexEdit(bareGroundAt(2, WATER_HEX), GAMEMASTER);

        assertNull(refusal, "turning a pond into a hill should be allowed when nobody is standing in it");
        assertEquals(2, board.getHex(WATER_HEX).getLevel(), "the ground should have been raised");
        assertEquals(0, board.getHex(WATER_HEX).depth(), "and the water should be gone");
    }

    /** @return an edit that paints one hex with a terrain held at a particular terrain factor */
    private HexEditSpec paintWithFactor(Coords hex, int terrainType, int level, int terrainFactor) {
        HexEditSpec.HexPaint paint = new HexEditSpec.HexPaint();
        paint.setTerrain(terrainType, level);
        paint.setTerrainFactor(terrainType, terrainFactor);
        HexEditSpec spec = new HexEditSpec(board.getBoardId());
        spec.paint(hex, paint);
        return spec;
    }

    @Test
    void aPaintedTerrainCanBeGivenATerrainFactorOfItsOwn() {
        // what the Modify Terrain command used to do, now reachable from the brush: heavy woods already shelled
        // most of the way down, rather than a fresh stand at the full 90
        String refusal = hexEditHandler.applyHexEdit(paintWithFactor(BARE_HEX, Terrains.WOODS, 2, 20), GAMEMASTER);

        assertNull(refusal, "painting weakened woods should be allowed");
        assertEquals(2, board.getHex(BARE_HEX).terrainLevel(Terrains.WOODS), "the woods should be heavy woods");
        assertEquals(20, board.getHex(BARE_HEX).getTerrain(Terrains.WOODS).getTerrainFactor(),
              "and should stand at the factor that was asked for, not the one the rules give new woods");
    }

    @Test
    void aPaintedTerrainWithNoFactorGivenStartsAtTheBookValue() {
        HexEditSpec spec = new HexEditSpec(board.getBoardId());
        spec.paint(BARE_HEX, paintOf(Terrains.WOODS, 2));

        hexEditHandler.applyHexEdit(spec, GAMEMASTER);

        assertEquals(Terrains.getTerrainFactor(Terrains.WOODS, 2),
              board.getHex(BARE_HEX).getTerrain(Terrains.WOODS).getTerrainFactor(),
              "an edit that says nothing about the factor must leave fresh terrain as the rules make it");
    }

    @Test
    void theFactorFollowsTheTerrainItWasGivenFor() {
        // the factor is keyed by terrain, so one meant for woods must not land on the water beside it
        HexEditSpec spec = paintWithFactor(BARE_HEX, Terrains.WOODS, 2, 20);
        spec.getPaintedHexes().get(BARE_HEX).setTerrain(Terrains.WATER, 1);

        String refusal = hexEditHandler.applyHexEdit(spec, GAMEMASTER);

        assertNull(refusal, "woods standing in shallow water is a legal hex");
        assertEquals(20, board.getHex(BARE_HEX).getTerrain(Terrains.WOODS).getTerrainFactor(),
              "the woods should hold the factor it was given");
        assertEquals(Terrains.getTerrainFactor(Terrains.WATER, 1),
              board.getHex(BARE_HEX).getTerrain(Terrains.WATER).getTerrainFactor(),
              "and the water should be left as the rules make it");
    }

    @Test
    void aTerrainCannotBeMadeStrongerThanTheRulesAllow() {
        // TacOps p.63 gives heavy woods a terrain factor of 90 and damage only takes it down. Woods at 300 would
        // simply never burn, so the edit is refused rather than quietly built.
        int aboveTheTable = Terrains.getTerrainFactor(Terrains.WOODS, 2) + 10;

        String refusal = hexEditHandler.applyHexEdit(
              paintWithFactor(BARE_HEX, Terrains.WOODS, 2, aboveTheTable), GAMEMASTER);

        assertNotNull(refusal, "woods stronger than the rules allow should be refused");
        assertFalse(board.getHex(BARE_HEX).containsTerrain(Terrains.WOODS), "and the hex should be left alone");
    }

    @Test
    void aTerrainCanBeSetToExactlyWhatTheRulesAllow() {
        int fromTheTable = Terrains.getTerrainFactor(Terrains.WOODS, 2);

        String refusal = hexEditHandler.applyHexEdit(
              paintWithFactor(BARE_HEX, Terrains.WOODS, 2, fromTheTable), GAMEMASTER);

        assertNull(refusal, "the value in the table is allowed, it is only going above it that is not");
        assertEquals(fromTheTable, board.getHex(BARE_HEX).getTerrain(Terrains.WOODS).getTerrainFactor());
    }

    @Test
    void aHexAlreadyStrongerThanTheTableCanStillBeEdited() {
        // a board may say what it likes. A hex already above the table must not become uneditable, so what it has
        // is allowed - the gamemaster may lower it, just not raise it further
        board.getHex(WOODS_HEX).getTerrain(Terrains.WOODS).setTerrainFactor(400);

        String keepingIt = hexEditHandler.applyHexEdit(
              paintWithFactor(WOODS_HEX, Terrains.WOODS, 1, 400), GAMEMASTER);
        assertNull(keepingIt, "leaving an over-strong hex as it is must not be refused");

        board.getHex(WOODS_HEX).getTerrain(Terrains.WOODS).setTerrainFactor(400);
        String raisingIt = hexEditHandler.applyHexEdit(
              paintWithFactor(WOODS_HEX, Terrains.WOODS, 1, 500), GAMEMASTER);
        assertNotNull(raisingIt, "but making it stronger still should be refused");
    }

    /** Sends a hex edit the way a client does, so the packet handler and its Game Master guard are exercised. */
    private void sendAsPacket(HexEditSpec spec, boolean senderIsGameMaster) {
        Player sender = game.getPlayer(0);
        sender.setGameMaster(senderIsGameMaster);
        gameManager.handlePacket(0, new Packet(PacketCommand.HEX_EDIT, spec));
    }

    @Test
    void aGameMasterCanChangeTheTerrainThroughThePacket() {
        sendAsPacket(floodOf(2, BARE_HEX), true);

        assertEquals(2, board.getHex(BARE_HEX).depth(),
              "the packet path should reach the handler and flood the hex");
    }

    @Test
    void aPlayerWhoIsNotGameMasterCannotChangeTheTerrain() {
        // the positive test above is what keeps this one honest: without it, a broken packet path would leave the
        // hex dry here too and this test would pass while proving nothing
        sendAsPacket(floodOf(2, BARE_HEX), false);

        assertEquals(0, board.getHex(BARE_HEX).depth(),
              "only a gamemaster may change the board, whatever a client sends");
    }

    @Test
    void aPlayerWhoIsNotGameMasterCannotUndoATerrainChange() {
        hexEditHandler.applyHexEdit(floodOf(2, BARE_HEX), GAMEMASTER);

        HexEditSpec undo = new HexEditSpec(board.getBoardId());
        undo.setUndoingLastEdit(true);
        sendAsPacket(undo, false);

        assertEquals(2, board.getHex(BARE_HEX).depth(),
              "an undo is a board change like any other, so it needs the same guard");
    }
}
