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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import megamek.common.GameBoardTestCase;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.BridgeConstruction;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.game.IGame;
import megamek.common.moves.MovePath;
import megamek.common.net.packets.Packet;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.IBuilding;
import megamek.common.units.Tank;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests for Bridge-Building Engineers, TO:AUE: the 6-turn bridge build lifecycle on ConvInfantry, the shared site
 * validation and bridge placement in BridgeConstruction (TM p.242 placement rules), and the END phase processing in
 * TWGameManager.
 */
public class BridgeBuildingTest extends GameBoardTestCase {

    private static final Coords ENGINEER_COORDS = new Coords(1, 1);
    /** Water (depth 2) with land banks to its north and south; a valid site whose bridge CF is doubled. */
    private static final Coords WATER_TARGET_COORDS = new Coords(1, 2);
    /** Clear gap between a level 1 bank to the north and a level 2 bank to the south; a valid site. */
    private static final Coords GAP_TARGET_COORDS = new Coords(0, 2);
    /** Clear hex at level 0 with a level 0 bank to the north and a level 4 rim to the south; one high rim anchors. */
    private static final Coords HIGH_BANK_TARGET_COORDS = new Coords(2, 2);
    /** Water hex whose north and south banks are also water; no land connection along that axis. */
    private static final Coords ISOLATED_WATER_COORDS = new Coords(3, 2);
    /** Water hex whose north bank holds another bridge; valid through the bridge connection. */
    private static final Coords BRIDGE_CONNECTED_WATER_COORDS = new Coords(4, 2);
    /** A hex already holding a building; no second structure may be built there. */
    private static final Coords BUILDING_COORDS = new Coords(1, 4);
    /** A hex on the north board edge; its north bank is off the board. */
    private static final Coords EDGE_COORDS = new Coords(0, 0);

    /** Exits bitmask connecting the north (0) and south (3) hexsides. */
    private static final int EXITS_NORTH_SOUTH = (1 << 0) | (1 << 3);

    private static final String BOARD_NAME = "BRIDGE_TEST_BOARD";

    private TWGameManager gameManager;
    private Game game;
    private Board board;

    /** A 1x3 column for crossing tests: level 1 bank, level 0 depth 1 river, level 1 bank. */
    private static final String CROSSING_BOARD = "BRIDGE_CROSSING_BOARD";

    /** The same column with a map-authored bridge over the river, as a control for the crossing tests. */
    private static final String CROSSING_BOARD_AUTHORED = "BRIDGE_CROSSING_BOARD_AUTHORED";

    private static void loadBoard() {
        initializeBoard(CROSSING_BOARD, """
              size 1 3
              hex 0101 1 "" ""
              hex 0102 0 "water:1" ""
              hex 0103 1 "" ""
              end"""
        );
        initializeBoard(CROSSING_BOARD_AUTHORED, """
              size 1 3
              hex 0101 1 "" ""
              hex 0102 0 "water:1;bridge:1:09;bridge_cf:30;bridge_elev:1" ""
              hex 0103 1 "" ""
              end"""
        );
        // Note: board files assign hexes sequentially; the coordinates in the hex lines are ignored.
        initializeBoard(BOARD_NAME, """
              size 5 6
              hex 0101 0 "" ""
              hex 0201 0 "" ""
              hex 0301 0 "" ""
              hex 0401 0 "" ""
              hex 0501 0 "" ""
              hex 0102 1 "" ""
              hex 0202 0 "" ""
              hex 0302 0 "" ""
              hex 0402 0 "water:1" ""
              hex 0502 0 "water:1;bridge:1;bridge_cf:30;bridge_elev:0" ""
              hex 0103 0 "" ""
              hex 0203 0 "water:2" ""
              hex 0303 0 "" ""
              hex 0403 0 "water:1" ""
              hex 0503 0 "water:1" ""
              hex 0104 2 "" ""
              hex 0204 0 "" ""
              hex 0304 4 "" ""
              hex 0404 0 "water:1" ""
              hex 0504 0 "water:1" ""
              hex 0105 0 "" ""
              hex 0205 0 "bldg_elev:1;building:1:8;bldg_cf:15" ""
              hex 0305 0 "" ""
              hex 0405 0 "" ""
              hex 0505 0 "" ""
              hex 0106 0 "" ""
              hex 0206 0 "" ""
              hex 0306 0 "" ""
              hex 0406 0 "" ""
              hex 0506 0 "" ""
              end"""
        );
    }

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        // Reload the board for every test: bridge placement mutates hexes and registers structures
        loadBoard();

        Player player = new Player(0, "Test");
        gameManager = Mockito.spy(new TWGameManager());

        // Mock methods that require Server to avoid NullPointerException
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), any(int.class));
        Mockito.doNothing().when(gameManager).entityUpdate(any(int.class));
        Mockito.doNothing().when(gameManager).sendChangedBuildings(any());

        game = gameManager.getGame();
        game.addPlayer(0, player);

        board = getBoard(BOARD_NAME);
        game.setBoard(board);
    }

    private ConvInfantry createEngineerPlatoon(Coords position) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setId(game.getNextEntityId());
        infantry.setOwner(game.getPlayer(0));
        infantry.setPosition(position);
        infantry.setBoardId(board.getBoardId());
        infantry.setElevation(0);
        infantry.setDeployed(true);
        infantry.setSquadSize(7);
        infantry.autoSetInternal();
        infantry.setSpecializations(ConvInfantry.BRIDGE_ENGINEERS);
        game.addEntity(infantry);
        return infantry;
    }

    // region BridgeConstruction site validation

    @Test
    void anyTwoDistinctHexsidesAreValidExits() {
        // Opposite sides give a straight bridge
        assertTrue(BridgeConstruction.isValidBridgeExits((1 << 0) | (1 << 3)),
              "Opposite hexsides must be a valid (straight) bridge orientation");
        // Non-opposite sides give a curved bridge, e.g. bridge_17.gif connects sides 0 and 4
        assertTrue(BridgeConstruction.isValidBridgeExits((1 << 0) | (1 << 4)),
              "Non-opposite hexsides must be a valid (curved) bridge orientation");
        assertTrue(BridgeConstruction.isValidBridgeExits((1 << 0) | (1 << 1)),
              "Adjacent hexsides must be a valid (sharp curve) bridge orientation");
        assertFalse(BridgeConstruction.isValidBridgeExits(1 << 2), "A single hexside is not a valid orientation");
        assertFalse(BridgeConstruction.isValidBridgeExits((1 << 0) | (1 << 2) | (1 << 3)),
              "Three hexsides are not a valid orientation");
        assertFalse(BridgeConstruction.isValidBridgeExits(0), "No hexsides is not a valid orientation");
    }

    @Test
    void exitsForMatchesTheTilesetImageIndex() {
        // bridge_17.gif is the curved bridge connecting sides 0 and 4; exitsFor must produce 17 so the sprite
        // and terrain pick that image
        assertEquals(17, BridgeConstruction.exitsFor(0, 4), "Sides 0 and 4 must give exits bitmask 17");
        assertEquals(9, BridgeConstruction.exitsFor(0, 3), "Opposite sides 0 and 3 must give exits bitmask 9");
    }

    @Test
    void curvedBridgeIsValidAndPlacedWithTheMatchingExits() {
        // A water hex surrounded by land: a bridge connecting two non-opposite sides (a curve) is a valid site,
        // and the placed terrain carries the exact exits bitmask so the tileset picks the matching curved image
        initializeBoard("BRIDGE_CURVED_BOARD", """
              size 3 3
              hex 0101 0 "" ""
              hex 0201 0 "" ""
              hex 0301 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "water:1" ""
              hex 0302 0 "" ""
              hex 0103 0 "" ""
              hex 0203 0 "" ""
              hex 0303 0 "" ""
              end"""
        );
        Board curvedBoard = getBoard("BRIDGE_CURVED_BOARD");
        Coords middle = new Coords(1, 1);
        int curvedExits = BridgeConstruction.exitsFor(0, 2);

        assertTrue(BridgeConstruction.isValidBridgeSite(curvedBoard, middle, curvedExits),
              "A curved bridge between two land banks of a water hex must be a valid site");
        BridgeConstruction.placeBridge(curvedBoard, middle, curvedExits, ConvInfantry.BRIDGE_TYPE_LIGHT, 30);
        assertEquals(curvedExits, curvedBoard.getHex(middle).getTerrain(Terrains.BRIDGE).getExits(),
              "The placed curved bridge must store the exact exits bitmask that selects its tileset image");
    }

    @Test
    void waterBridgeFromElevatedLandTowardWaterIsValidAndSitsAtLandLevel() {
        // First span of a wide-river crossing: from a level-2 land bank, across a water hex, pointing at the next
        // water hex (which a later span will bridge). Valid because one bank is land; the far side may be water.
        // The deck sits at the land bank's level so units cross from the land at grade and the chain stays level.
        initializeBoard("BRIDGE_ELEVATED_WATER_BOARD", """
              size 1 3
              hex 0101 2 "" ""
              hex 0102 0 "water:1" ""
              hex 0103 0 "water:1" ""
              end"""
        );
        Board elevatedBoard = getBoard("BRIDGE_ELEVATED_WATER_BOARD");
        Coords section = new Coords(0, 1);
        int exits = BridgeConstruction.exitsFor(section.direction(new Coords(0, 0)),
              section.direction(new Coords(0, 2)));

        assertTrue(BridgeConstruction.isValidBridgeSite(elevatedBoard, section, exits),
              "A water span from a level-2 land bank toward more water must be valid (one bank is land)");
        BridgeConstruction.placeBridge(elevatedBoard, section, exits, ConvInfantry.BRIDGE_TYPE_LIGHT, 30);
        assertEquals(2, elevatedBoard.getHex(section).terrainLevel(Terrains.BRIDGE_ELEV),
              "The deck must sit at the level-2 land bank, not dip to the water surface");
    }

    @Test
    void waterSiteWithLandBanksIsValid() {
        assertTrue(BridgeConstruction.isValidBridgeSite(board, WATER_TARGET_COORDS, EXITS_NORTH_SOUTH),
              "Water hex with land banks north and south must be a valid bridge site");
    }

    @Test
    void isolatedWaterSiteIsInvalid() {
        assertFalse(BridgeConstruction.isValidBridgeSite(board, ISOLATED_WATER_COORDS, EXITS_NORTH_SOUTH),
              "Water hex connecting only to water must not be a valid bridge site (TM p.242)");
    }

    @Test
    void waterSiteConnectedToBridgeIsValid() {
        assertTrue(BridgeConstruction.isValidBridgeSite(board, BRIDGE_CONNECTED_WATER_COORDS, EXITS_NORTH_SOUTH),
              "Water hex connecting to another bridge must be a valid bridge site (TM p.242)");
    }

    @Test
    void siteAnchoredToOneHighRimWithALowerFarSideIsValid() {
        // A canyon-floor/cliff site below one tall rim (the other side at floor level) is buildable: the span
        // anchors to the rim, the far side may be deeper/equal canyon to be reached by a further span. Engineers
        // can bridge any canyon regardless of depth (TO:AuE p.152 clarification).
        assertTrue(BridgeConstruction.isValidBridgeSite(board, HIGH_BANK_TARGET_COORDS, EXITS_NORTH_SOUTH),
              "A site below a single high rim must be a valid bridge site");
    }

    @Test
    void gapWithOneLevelBankDifferenceIsValid() {
        assertTrue(BridgeConstruction.isValidBridgeSite(board, GAP_TARGET_COORDS, EXITS_NORTH_SOUTH),
              "Gap between banks 1 level apart must be a valid bridge site (TM p.242)");
    }

    @Test
    void deepCanyonWithRimsWithinOneLevelIsValid() {
        // A single span may cross a canyon of any DEPTH (floor far below the rims), as long as its two rims are
        // within one level of each other - a single-hex span changes at most one level (TO:AuE p.152). Here the
        // floor is at -4 but the rims are level 3 and 2 (within 1), so the span is valid.
        initializeBoard("BRIDGE_CANYON_BOARD", """
              size 1 3
              hex 0101 3 "" ""
              hex 0102 -4 "" ""
              hex 0103 2 "" ""
              end"""
        );
        Board canyonBoard = getBoard("BRIDGE_CANYON_BOARD");
        Coords canyonFloor = new Coords(0, 1);
        assertTrue(BridgeConstruction.isValidBridgeSite(canyonBoard, canyonFloor, EXITS_NORTH_SOUTH),
              "A deep canyon whose rims are within one level must be a valid bridge site at any depth");
    }

    @Test
    void bridgeSiteIssueReportsTheSpecificReason() {
        // The validator exposes WHY a site is rejected so the UI can explain it to the player
        assertEquals(BridgeConstruction.BridgeSiteIssue.VALID,
              BridgeConstruction.bridgeSiteIssue(board, WATER_TARGET_COORDS, EXITS_NORTH_SOUTH),
              "A water hex with land banks is valid");
        assertEquals(BridgeConstruction.BridgeSiteIssue.OCCUPIED,
              BridgeConstruction.bridgeSiteIssue(board, BUILDING_COORDS, EXITS_NORTH_SOUTH),
              "A hex with a structure reports OCCUPIED");
        assertEquals(BridgeConstruction.BridgeSiteIssue.NO_ANCHOR,
              BridgeConstruction.bridgeSiteIssue(board, ISOLATED_WATER_COORDS, EXITS_NORTH_SOUTH),
              "Water touching only deeper water reports NO_ANCHOR");
        assertEquals(BridgeConstruction.BridgeSiteIssue.OFF_BOARD,
              BridgeConstruction.bridgeSiteIssue(board, EDGE_COORDS, EXITS_NORTH_SOUTH),
              "A site whose bank lies off the board reports OFF_BOARD");

        initializeBoard("BRIDGE_STEEP_ISSUE_BOARD", """
              size 1 3
              hex 0101 2 "" ""
              hex 0102 -2 "" ""
              hex 0103 4 "" ""
              end"""
        );
        assertEquals(BridgeConstruction.BridgeSiteIssue.RIMS_TOO_STEEP,
              BridgeConstruction.bridgeSiteIssue(getBoard("BRIDGE_STEEP_ISSUE_BOARD"), new Coords(0, 1),
                    EXITS_NORTH_SOUTH),
              "Two rims more than one level apart report RIMS_TOO_STEEP");
    }

    @Test
    void singleSpanBetweenRimsOverTwoLevelsApartIsInvalid() {
        // Rims 2 levels apart cannot be a single span (max 1 level of change per hex); the climb is made by chaining
        // spans up terrain that steps one level per hex (TO:AuE p.152 multi-level bridge rule).
        initializeBoard("BRIDGE_STEEP_RIMS_BOARD", """
              size 1 3
              hex 0101 2 "" ""
              hex 0102 -2 "" ""
              hex 0103 4 "" ""
              end"""
        );
        Board steepBoard = getBoard("BRIDGE_STEEP_RIMS_BOARD");
        assertFalse(BridgeConstruction.isValidBridgeSite(steepBoard, new Coords(0, 1), EXITS_NORTH_SOUTH),
              "A single span between rims 2 levels apart must be invalid (more than 1 level of change in one hex)");
    }

    @Test
    void wideCanyonCanBeChainedSpanBySpan() {
        // A two-hex-wide canyon (two floor hexes between two rims): span 1 anchors to the rim and points at the
        // second floor hex (not a rim); the engineers then stand on span 1 to bridge the second floor hex to the
        // far rim. Span 1's far side being canyon floor (not a rim) must be allowed, exactly like a wide river.
        initializeBoard("BRIDGE_WIDE_CANYON_BOARD", """
              size 1 4
              hex 0101 2 "" ""
              hex 0102 -2 "" ""
              hex 0103 -2 "" ""
              hex 0104 2 "" ""
              end"""
        );
        Board wideCanyonBoard = getBoard("BRIDGE_WIDE_CANYON_BOARD");
        Coords nearRim = new Coords(0, 0);
        Coords floorOne = new Coords(0, 1);
        Coords floorTwo = new Coords(0, 2);
        Coords farRim = new Coords(0, 3);

        // Span 1: near rim (level 2) over the first floor hex, pointing at the second floor hex (also -2)
        int spanOneExits = BridgeConstruction.exitsFor(floorOne.direction(nearRim), floorOne.direction(floorTwo));
        assertTrue(BridgeConstruction.isValidBridgeSite(wideCanyonBoard, floorOne, spanOneExits),
              "Span 1 over the first canyon floor (anchored to the rim) must be valid even though the far side "
                    + "is more canyon floor");
        BridgeConstruction.placeBridge(wideCanyonBoard, floorOne, spanOneExits, ConvInfantry.BRIDGE_TYPE_LIGHT, 15);
        Hex spanOneHex = wideCanyonBoard.getHex(floorOne);
        // BRIDGE_ELEV is relative to the hex level; the absolute deck level is hex level + BRIDGE_ELEV
        assertEquals(2, spanOneHex.getLevel() + spanOneHex.terrainLevel(Terrains.BRIDGE_ELEV),
              "Span 1 deck must sit at the rim's absolute level (2), spanning the canyon below");

        // Span 2: the first span (a bridge, deck at 2) over the second floor hex to the far rim
        int spanTwoExits = BridgeConstruction.exitsFor(floorTwo.direction(floorOne), floorTwo.direction(farRim));
        assertTrue(BridgeConstruction.isValidBridgeSite(wideCanyonBoard, floorTwo, spanTwoExits),
              "Span 2 must be valid, anchored to span 1 (a bridge) and the far rim");
    }

    @Test
    void flatDryGroundIsNotAValidBridgeSite() {
        // A dry hex level with its banks is not a gap; a bridge there would span nothing (TM p.242)
        initializeBoard("BRIDGE_FLAT_BOARD", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              end"""
        );
        Board flatBoard = getBoard("BRIDGE_FLAT_BOARD");
        assertFalse(BridgeConstruction.isValidBridgeSite(flatBoard, new Coords(0, 1), EXITS_NORTH_SOUTH),
              "Flat open ground level with its banks must not be a valid bridge site");
    }

    @Test
    void occupiedHexIsInvalid() {
        assertFalse(BridgeConstruction.isValidBridgeSite(board, BUILDING_COORDS, EXITS_NORTH_SOUTH),
              "A hex already holding a building must not be a valid bridge site");
    }

    @Test
    void offBoardBankIsInvalid() {
        assertFalse(BridgeConstruction.isValidBridgeSite(board, EDGE_COORDS, EXITS_NORTH_SOUTH),
              "A site whose bank lies off the board must not be valid");
    }

    // endregion

    // region BridgeConstruction placement

    @Test
    void placedBridgeOverWaterSetsTerrainAndRegistersStructure() {
        BridgeConstruction.placeBridge(board, WATER_TARGET_COORDS, EXITS_NORTH_SOUTH,
              ConvInfantry.BRIDGE_TYPE_LIGHT, 30);

        Hex hex = board.getHex(WATER_TARGET_COORDS);
        assertEquals(ConvInfantry.BRIDGE_TYPE_LIGHT, hex.terrainLevel(Terrains.BRIDGE),
              "The placed bridge must be a light bridge");
        assertEquals(30, hex.terrainLevel(Terrains.BRIDGE_CF), "The placed bridge must keep the supplied CF");
        assertEquals(0, hex.terrainLevel(Terrains.BRIDGE_ELEV),
              "A bridge between level 0 banks over water must sit at the water surface");
        assertTrue(hex.containsTerrainExit(Terrains.BRIDGE, 0), "The bridge must connect its north hexside");
        assertTrue(hex.containsTerrainExit(Terrains.BRIDGE, 3), "The bridge must connect its south hexside");
        assertNotNull(board.getBuildingAt(WATER_TARGET_COORDS),
              "The placed bridge must be registered as a board structure so it can take damage and collapse");
    }

    @Test
    void placedBridgeSitsAtTheLowerRim() {
        BridgeConstruction.placeBridge(board, GAP_TARGET_COORDS, EXITS_NORTH_SOUTH,
              ConvInfantry.BRIDGE_TYPE_MEDIUM, 40);

        Hex hex = board.getHex(GAP_TARGET_COORDS);
        assertEquals(1, hex.terrainLevel(Terrains.BRIDGE_ELEV),
              "A bridge between level 1 and level 2 rims sits at the lower rim; both are within 1 level so the deck "
                    + "is reachable from each (a single-hex span changes at most one level)");
    }

    // endregion

    // region ConvInfantry build lifecycle

    @Test
    void buildingPlatoonIsMovementOnlyEligibleSoItCanContinueOrCancel() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);

        assertTrue(engineers.isBuildingBridge(), "The platoon must be building after the build is declared");
        // The platoon stays selectable in the movement phase so the player can keep building or cancel to dismantle,
        // but it may take no firing or physical action while building. TO:AUE p.152.
        assertTrue(engineers.isEligibleFor(GamePhase.MOVEMENT),
              "A building platoon stays movement-eligible so it can continue or cancel the build");
        assertFalse(engineers.isEligibleFor(GamePhase.FIRING), "A building platoon may take no other action (firing)");
        assertFalse(engineers.isEligibleFor(GamePhase.PHYSICAL),
              "A building platoon may take no other action (physical)");
    }

    @Test
    void dismantlingPlatoonIsMovementOnlyEligible() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);
        engineers.startBridgeDismantle();

        assertTrue(engineers.isDismantlingBridge(), "The platoon must be dismantling after cancelling");
        assertTrue(engineers.isEligibleFor(GamePhase.MOVEMENT),
              "A dismantling platoon stays movement-eligible");
        assertFalse(engineers.isEligibleFor(GamePhase.FIRING),
              "A dismantling platoon may take no other action (firing)");
        assertFalse(engineers.isEligibleFor(GamePhase.PHYSICAL),
              "A dismantling platoon may take no other action (physical)");
    }

    @Test
    void buildProgressesEachRoundWhileAdjacent() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);

        // Turns of work are banked once per round in the END phase, not at newRound
        gameManager.checkBuildBridges();
        engineers.newRound(2);
        gameManager.checkBuildBridges();

        assertEquals(2, engineers.getBridgeBuildTurns(),
              "Each round's END phase adjacent to the site must bank a completed turn of work");
    }

    @Test
    void buildIsAbandonedWhenThePlatoonIsDisplaced() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);

        engineers.setPosition(new Coords(4, 5));
        engineers.newRound(2);

        assertFalse(engineers.isBuildingBridge(),
              "A platoon no longer adjacent to its site must abandon the build");
    }

    @Test
    void casualtiesExtendTheBuildOncePerTurn() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);
        assertEquals(ConvInfantry.BRIDGE_BUILD_TURNS, engineers.getBridgeBuildRequiredTurns(),
              "A fresh build must require the base number of turns");

        engineers.setInternal(5, ConvInfantry.LOC_INFANTRY);
        assertTrue(engineers.updateBridgeBuildCasualties(),
              "Trooper losses since the last check must extend the build");
        assertEquals(ConvInfantry.BRIDGE_BUILD_TURNS + 1, engineers.getBridgeBuildRequiredTurns(),
              "A turn with casualties must extend the build by exactly 1 turn");

        assertFalse(engineers.updateBridgeBuildCasualties(),
              "A check without further losses must not extend the build again");
        assertEquals(ConvInfantry.BRIDGE_BUILD_TURNS + 1, engineers.getBridgeBuildRequiredTurns(),
              "The required turns must be unchanged without further losses");
    }

    @Test
    void budgetAllowsTwoLightOrOneMediumBridge() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        assertTrue(engineers.canAffordBridge(ConvInfantry.BRIDGE_TYPE_MEDIUM),
              "The full budget must cover one medium bridge");

        engineers.spendBridgeBuildPoints(ConvInfantry.BRIDGE_TYPE_LIGHT);
        assertFalse(engineers.canAffordBridge(ConvInfantry.BRIDGE_TYPE_MEDIUM),
              "After one light bridge the budget must no longer cover a medium bridge");
        assertTrue(engineers.canAffordBridge(ConvInfantry.BRIDGE_TYPE_LIGHT),
              "After one light bridge the budget must still cover a second light bridge");

        engineers.spendBridgeBuildPoints(ConvInfantry.BRIDGE_TYPE_LIGHT);
        assertFalse(engineers.canStartBridgeBuild(),
              "With the budget spent the platoon must not be able to start another build");
    }

    @Test
    void builtBridgeCFMatchesTypeAndDoublesOverWater() {
        assertEquals(15, ConvInfantry.getBuiltBridgeCF(ConvInfantry.BRIDGE_TYPE_LIGHT, false),
              "Light bridge CF must be 15 (TO:AUE)");
        assertEquals(40, ConvInfantry.getBuiltBridgeCF(ConvInfantry.BRIDGE_TYPE_MEDIUM, false),
              "Medium bridge CF must be 40 (TO:AUE)");
        assertEquals(30, ConvInfantry.getBuiltBridgeCF(ConvInfantry.BRIDGE_TYPE_LIGHT, true),
              "Light bridge CF must double to 30 over water (TO:AUE)");
        assertEquals(80, ConvInfantry.getBuiltBridgeCF(ConvInfantry.BRIDGE_TYPE_MEDIUM, true),
              "Medium bridge CF must double to 80 over water (TO:AUE)");
    }

    @Test
    void bridgeEngineerSpecializationEquipsTheBridgeKit() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        assertTrue(engineers.hasBridgeKit(),
              "Setting the Bridge-Building Engineers specialization must equip the Infantry Bridge Kit");
        assertTrue(engineers.canStartBridgeBuild(), "A fresh engineer platoon must be able to start a build");
    }

    // endregion

    // region END phase processing

    @Test
    void endPhaseCompletesTheBridgeAfterSixFullTurns() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);

        // Each round's END phase banks one turn; the first five leave the build in progress
        for (int round = 1; round <= 5; round++) {
            gameManager.checkBuildBridges();
            assertTrue(engineers.isBuildingBridge(),
                  "The build must still be in progress after " + round + " of 6 turns");
            assertFalse(board.getHex(WATER_TARGET_COORDS).containsTerrain(Terrains.BRIDGE),
                  "No bridge may appear before the work is done");
            engineers.newRound(round + 1);
        }
        // The sixth END phase completes the build
        gameManager.checkBuildBridges();

        Hex hex = board.getHex(WATER_TARGET_COORDS);
        assertTrue(hex.containsTerrain(Terrains.BRIDGE), "The bridge must be placed after 6 full turns of work");
        assertEquals(30, hex.terrainLevel(Terrains.BRIDGE_CF),
              "A light bridge built over water must have its CF doubled to 30 (TO:AUE)");
        assertNotNull(board.getBuildingAt(WATER_TARGET_COORDS),
              "The completed bridge must be registered as a board structure");
        assertFalse(engineers.isBuildingBridge(), "The build must end when the bridge is completed");
    }

    @Test
    void endPhaseAbandonsTheBuildWhenThePlatoonWasDisplaced() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);

        engineers.setPosition(new Coords(4, 5));
        gameManager.checkBuildBridges();

        assertFalse(engineers.isBuildingBridge(), "A displaced platoon must abandon its build in the END phase");
        assertFalse(board.getHex(WATER_TARGET_COORDS).containsTerrain(Terrains.BRIDGE),
              "No bridge may appear from an abandoned build");
    }

    @Test
    void destroyedPlatoonDoesNotCompleteItsBridgeOnTheFinalTurn() {
        // TO:AUE: destroyed before completing the task means no bridge. A platoon wiped out during the combat of
        // its final turn must not still finish the bridge in the END phase completion check.
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);
        // Bank five turns of work, leaving the build one turn from completion
        for (int round = 1; round <= 5; round++) {
            gameManager.checkBuildBridges();
            engineers.newRound(round + 1);
        }
        engineers.setDestroyed(true);

        gameManager.checkBuildBridges();

        assertFalse(board.getHex(WATER_TARGET_COORDS).containsTerrain(Terrains.BRIDGE),
              "A platoon destroyed before the completion check must not finish its bridge (TO:AUE)");
        assertFalse(engineers.isBuildingBridge(), "The destroyed platoon's build must be abandoned");
    }

    @Test
    void casualtiesDelayCompletionByOneTurn() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);

        // Bank five turns; the sixth END phase would normally finish the build...
        for (int round = 1; round <= 5; round++) {
            gameManager.checkBuildBridges();
            engineers.newRound(round + 1);
        }
        // ...but the platoon takes casualties during the final turn
        engineers.setInternal(5, ConvInfantry.LOC_INFANTRY);
        gameManager.checkBuildBridges();

        assertTrue(engineers.isBuildingBridge(), "Casualties in the final turn must delay completion (TO:AUE)");
        assertFalse(board.getHex(WATER_TARGET_COORDS).containsTerrain(Terrains.BRIDGE),
              "No bridge may appear while the extended build is unfinished");

        // One more uneventful turn finishes the extended build
        engineers.newRound(7);
        gameManager.checkBuildBridges();
        assertTrue(board.getHex(WATER_TARGET_COORDS).containsTerrain(Terrains.BRIDGE),
              "The extended build must complete one turn later");
    }

    @Test
    void cancelledBridgeDismantlesOverTheTurnsSpentAndRefundsTheBudget() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);
        engineers.spendBridgeBuildPoints(ConvInfantry.BRIDGE_TYPE_LIGHT);
        assertEquals(ConvInfantry.BRIDGE_BUILD_POINTS - ConvInfantry.BRIDGE_TYPE_LIGHT,
              engineers.getBridgeBuildPoints(), "A light bridge must spend one point when the build starts");

        // Bank three full turns of work (one per round's END phase), then cancel
        gameManager.checkBuildBridges();
        engineers.newRound(2);
        gameManager.checkBuildBridges();
        engineers.newRound(3);
        gameManager.checkBuildBridges();
        assertEquals(3, engineers.getBridgeBuildTurns(), "Three turns of work must be banked before cancelling");

        engineers.startBridgeDismantle();
        assertFalse(engineers.isBuildingBridge(), "Cancelling must stop the build");
        assertTrue(engineers.isDismantlingBridge(), "Cancelling must start dismantling");
        assertEquals(3, engineers.getBridgeDismantleRequiredTurns(),
              "Dismantling must take as many turns as were banked building (3)");

        // Dismantling turn 1 of 3
        engineers.newRound(4);
        gameManager.checkBuildBridges();
        assertTrue(engineers.isDismantlingBridge(), "Dismantling must still be in progress after 1 of 3 turns");
        assertEquals(ConvInfantry.BRIDGE_BUILD_POINTS - ConvInfantry.BRIDGE_TYPE_LIGHT,
              engineers.getBridgeBuildPoints(), "The budget must not be refunded until dismantling finishes");

        // Turn 2 of 3
        engineers.newRound(5);
        gameManager.checkBuildBridges();
        assertTrue(engineers.isDismantlingBridge(), "Dismantling must still be in progress after 2 of 3 turns");

        // Turn 3 of 3 finishes the dismantling
        engineers.newRound(6);
        gameManager.checkBuildBridges();
        assertFalse(engineers.isDismantlingBridge(), "Dismantling must finish after 3 turns");
        assertFalse(engineers.isBuildingBridge(), "The platoon must be free after dismantling");
        assertEquals(ConvInfantry.BRIDGE_BUILD_POINTS, engineers.getBridgeBuildPoints(),
              "The spent point must be refunded once dismantling finishes");
        assertFalse(board.getHex(WATER_TARGET_COORDS).containsTerrain(Terrains.BRIDGE),
              "A cancelled build must never place a bridge");
    }

    @Test
    void dismantlingCountsTheStandingStructureBackDownOnTheBuildScale() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);

        // Bank four turns of work (display would read 4/6), then cancel
        for (int round = 1; round <= 4; round++) {
            gameManager.checkBuildBridges();
            engineers.newRound(round + 1);
        }
        assertEquals(4, engineers.getBridgeBuildTurns());

        engineers.startBridgeDismantle();
        // Dismantling starts from the last build turn (4 of 6 still standing), not a fresh 1
        assertEquals(4, engineers.getBridgeDismantleRemaining(),
              "Dismantling must start from the structure that was built (4), not restart at 1");
        assertEquals(ConvInfantry.BRIDGE_BUILD_TURNS, engineers.getBridgeBuildRequiredTurns(),
              "The build's required-turn denominator must be preserved as the countback scale");

        // Each dismantling turn removes one turn of structure: 4 -> 3 -> 2 -> 1 still standing
        int round = 6;
        for (int expectedRemaining : new int[] { 3, 2, 1 }) {
            gameManager.checkBuildBridges();
            assertTrue(engineers.isDismantlingBridge(), "Dismantling must still be in progress");
            assertEquals(expectedRemaining, engineers.getBridgeDismantleRemaining(),
                  "The standing structure must count back down one per round");
            engineers.newRound(round++);
        }
        // The fourth dismantling turn removes the last of the structure and frees the platoon
        gameManager.checkBuildBridges();
        assertFalse(engineers.isDismantlingBridge(), "Dismantling must finish when the structure reaches 0");
    }

    @Test
    void resumeReversesDismantlingBackToBuildingFromTheStandingStructure() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);

        // Bank four turns of work, then cancel and dismantle one turn (4 -> 3 still standing)
        for (int round = 1; round <= 4; round++) {
            gameManager.checkBuildBridges();
            engineers.newRound(round + 1);
        }
        engineers.startBridgeDismantle();
        gameManager.checkBuildBridges();
        assertEquals(3, engineers.getBridgeDismantleRemaining(), "One dismantling turn must leave 3 standing");

        // Resume: building continues from the structure still standing, not from scratch
        engineers.resumeBridgeBuild();
        assertTrue(engineers.isBuildingBridge(), "Resuming must put the platoon back into building");
        assertFalse(engineers.isDismantlingBridge(), "Resuming must clear the dismantling state");
        assertEquals(3, engineers.getBridgeBuildTurns(),
              "Building must resume from the structure still standing (3 of 6)");
        assertEquals(ConvInfantry.BRIDGE_BUILD_TURNS, engineers.getBridgeBuildRequiredTurns(),
              "The build's required turns must be preserved across the cancel/resume");

        // Banking the three remaining turns completes the bridge (3 -> 6)
        int round = 7;
        for (int i = 0; i < 3; i++) {
            engineers.newRound(round++);
            gameManager.checkBuildBridges();
        }
        assertTrue(board.getHex(WATER_TARGET_COORDS).containsTerrain(Terrains.BRIDGE),
              "A resumed build must complete once the remaining turns are banked");
        assertFalse(engineers.isBuildingBridge(), "The build must end when the resumed bridge completes");
    }

    @Test
    void anInProgressBridgeReservesItsHexAgainstASecondBuild() {
        ConvInfantry first = createEngineerPlatoon(ENGINEER_COORDS);
        first.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);
        ConvInfantry second = createEngineerPlatoon(new Coords(2, 2));

        // Another platoon must see the target hex as claimed; the building platoon excludes itself
        assertTrue(ConvInfantry.isBridgeTargetClaimed(game, board.getBoardId(), WATER_TARGET_COORDS, second),
              "An in-progress bridge must reserve its hex against a second platoon");
        assertFalse(ConvInfantry.isBridgeTargetClaimed(game, board.getBoardId(), WATER_TARGET_COORDS, first),
              "The building platoon must not be blocked by its own build");

        // The reservation persists while paused (the partial bridge places no terrain to mark the hex)
        first.pauseBridgeBuild();
        assertTrue(ConvInfantry.isBridgeTargetClaimed(game, board.getBoardId(), WATER_TARGET_COORDS, second),
              "A paused bridge must still reserve its hex");

        // A hex with no in-progress bridge is free
        assertFalse(ConvInfantry.isBridgeTargetClaimed(game, board.getBoardId(), ENGINEER_COORDS, second),
              "A hex with no in-progress bridge is not claimed");
    }

    @Test
    void abandonIsInstantAndForfeitsThePoints() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);
        engineers.spendBridgeBuildPoints(ConvInfantry.BRIDGE_TYPE_LIGHT);
        gameManager.checkBuildBridges();

        engineers.abandonBridge();

        assertFalse(engineers.hasBridgeInProgress(), "Abandon clears all bridge work immediately");
        assertFalse(engineers.isBuildingBridge(), "Abandon stops building at once");
        assertEquals(ConvInfantry.BRIDGE_BUILD_POINTS - ConvInfantry.BRIDGE_TYPE_LIGHT,
              engineers.getBridgeBuildPoints(), "Abandon must NOT refund the spent point");
        assertFalse(board.getHex(WATER_TARGET_COORDS).containsTerrain(Terrains.BRIDGE),
              "An abandoned build never places a bridge");
    }

    @Test
    void pausedPlatoonIsFreedHoldsProgressAndDoesNotAdvance() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);
        // Bank three turns, then pause
        gameManager.checkBuildBridges();
        engineers.newRound(2);
        gameManager.checkBuildBridges();
        engineers.newRound(3);
        gameManager.checkBuildBridges();
        assertEquals(3, engineers.getBridgeBuildTurns());

        engineers.pauseBridgeBuild();
        assertTrue(engineers.isBridgePaused(), "Pausing must enter the paused state");
        assertFalse(engineers.isBuildingBridge(), "A paused platoon is not actively building");
        assertFalse(engineers.isBusyWithBridge(), "A paused platoon is freed (not busy with the bridge)");
        assertTrue(engineers.hasBridgeInProgress(), "A paused platoon still has bridge work in progress");
        assertTrue(engineers.isEligibleFor(GamePhase.MOVEMENT), "A paused platoon is eligible to act");

        // A paused build does not advance at END
        gameManager.checkBuildBridges();
        assertEquals(3, engineers.getBridgeBuildTurns(), "A paused build must not advance");
        assertTrue(engineers.isBridgePaused());

        // A paused platoon may move away without losing the work
        engineers.setPosition(new Coords(4, 5));
        engineers.newRound(4);
        assertTrue(engineers.isBridgePaused(), "Moving away must not abandon a paused build");
        assertEquals(3, engineers.getBridgeBuildTurns(), "The held progress persists while paused");
    }

    @Test
    void destroyedPausedPlatoonClearsItsBridgeState() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);
        gameManager.checkBuildBridges();
        engineers.pauseBridgeBuild();
        assertTrue(engineers.hasBridgeInProgress(), "Sanity: the paused platoon has bridge work in progress");

        engineers.setDestroyed(true);
        gameManager.checkBuildBridges();

        assertFalse(engineers.hasBridgeInProgress(),
              "A destroyed paused platoon must have its bridge state cleared, leaving no stale reservation/indicator");
    }

    @Test
    void pausedBridgeResumesFromHeldProgressAndCompletes() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);
        // Bank three turns, pause, wander off and back
        for (int round = 1; round <= 3; round++) {
            gameManager.checkBuildBridges();
            engineers.newRound(round + 1);
        }
        engineers.pauseBridgeBuild();
        engineers.setPosition(new Coords(4, 5));
        engineers.newRound(5);
        // Return adjacent and resume
        engineers.setPosition(ENGINEER_COORDS);
        engineers.resumeBridgeBuild();
        assertTrue(engineers.isBuildingBridge(), "Resuming a pause returns to active building");
        assertFalse(engineers.isBridgePaused());
        assertEquals(3, engineers.getBridgeBuildTurns(), "Building resumes from the held 3 turns");

        // Bank the remaining three turns to finish
        for (int round = 6; round <= 8; round++) {
            engineers.newRound(round);
            gameManager.checkBuildBridges();
        }
        assertTrue(board.getHex(WATER_TARGET_COORDS).containsTerrain(Terrains.BRIDGE),
              "A resumed paused build completes once the remaining turns are banked");
    }

    @Test
    void cannotStartANewBridgeWhilePausedOrBuilding() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);
        assertFalse(engineers.canStartBridgeBuild(), "Cannot start a second bridge while actively building");
        engineers.pauseBridgeBuild();
        assertFalse(engineers.canStartBridgeBuild(), "Cannot start a second bridge while one is paused");
    }

    @Test
    void pausedBridgeStateSurvivesSerialization() throws Exception {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_MEDIUM);
        gameManager.checkBuildBridges();
        engineers.newRound(2);
        gameManager.checkBuildBridges();
        engineers.pauseBridgeBuild();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOut = new ObjectOutputStream(buffer)) {
            objectOut.writeObject(engineers);
        }
        ConvInfantry restored;
        try (ObjectInputStream objectIn = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            restored = (ConvInfantry) objectIn.readObject();
        }

        assertTrue(restored.isBridgePaused(), "The restored platoon must still be paused");
        assertFalse(restored.isBuildingBridge(), "A restored paused platoon is not actively building");
        assertEquals(engineers.getBridgeBuildTurns(), restored.getBridgeBuildTurns(),
              "The held progress must survive a save game round trip");
        assertEquals(WATER_TARGET_COORDS, restored.getBridgeTargetCoords(),
              "The paused bridge site must survive a save game round trip");
    }

    @Test
    void displacedDismantlingPlatoonLosesItsWorkWithoutRefund() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);
        engineers.spendBridgeBuildPoints(ConvInfantry.BRIDGE_TYPE_LIGHT);
        gameManager.checkBuildBridges();
        engineers.newRound(2);
        engineers.startBridgeDismantle();

        // The platoon is displaced before the dismantling completes
        engineers.setPosition(new Coords(4, 5));
        engineers.newRound(3);

        assertFalse(engineers.isDismantlingBridge(), "A displaced dismantling platoon must abandon the work");
        assertFalse(engineers.isBuildingBridge(), "A displaced dismantling platoon must not be building");
        assertEquals(ConvInfantry.BRIDGE_BUILD_POINTS - ConvInfantry.BRIDGE_TYPE_LIGHT,
              engineers.getBridgeBuildPoints(), "An abandoned dismantling must not refund the budget");
    }

    @Test
    void dismantlingStateSurvivesSerialization() throws Exception {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);
        gameManager.checkBuildBridges();
        engineers.newRound(2);
        gameManager.checkBuildBridges();
        engineers.startBridgeDismantle();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOut = new ObjectOutputStream(buffer)) {
            objectOut.writeObject(engineers);
        }
        ConvInfantry restored;
        try (ObjectInputStream objectIn = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            restored = (ConvInfantry) objectIn.readObject();
        }

        assertTrue(restored.isDismantlingBridge(), "The restored platoon must still be dismantling");
        assertEquals(engineers.getBridgeDismantleRequiredTurns(), restored.getBridgeDismantleRequiredTurns(),
              "The dismantling duration must survive serialization");
        assertEquals(engineers.getBridgeDismantleTurns(), restored.getBridgeDismantleTurns(),
              "The dismantling progress must survive serialization");
    }

    // endregion

    // region Bridge repair (unofficial: rebuilding a destroyed section)

    /**
     * Loads a board, makes it the game's board and the engineer-platoon board, and returns it. Used by the repair
     * tests, which each need a custom board carrying a surviving span next to a gap.
     */
    private Board useRepairBoard(String name, String boardData) {
        initializeBoard(name, boardData);
        setBoard(name);
        Board repairBoard = getBoard(name);
        game.setBoard(repairBoard);
        this.board = repairBoard;
        return repairBoard;
    }

    @Test
    void endGapNextToASurvivingSpanAndBankIsARepairSite() {
        // A bridge ran north-south over water onto a south land bank; the southern section (the gap) was destroyed.
        // The surviving span to the north points into the gap and the south bank anchors the straight far side.
        Board repairBoard = useRepairBoard("BRIDGE_REPAIR_END_GAP", """
              size 1 3
              hex 0101 0 "water:1;bridge:1:09;bridge_cf:30;bridge_elev:0" ""
              hex 0102 0 "water:1" ""
              hex 0103 0 "" ""
              end"""
        );
        Coords gap = new Coords(0, 1);
        assertTrue(BridgeConstruction.isBridgeRepairSite(repairBoard, gap, EXITS_NORTH_SOUTH),
              "A gap with a surviving span on one side and a straight bank on the other must be a repair site");
    }

    @Test
    void midSpanGapBetweenTwoSurvivingSpansIsARepairSite() {
        // Both neighbors of the gap are surviving spans pointing into it; the repaired section reconnects them.
        Board repairBoard = useRepairBoard("BRIDGE_REPAIR_MID_SPAN", """
              size 1 4
              hex 0101 0 "" ""
              hex 0102 0 "water:1;bridge:1:09;bridge_cf:30;bridge_elev:0" ""
              hex 0103 0 "water:1" ""
              hex 0104 0 "water:1;bridge:1:09;bridge_cf:30;bridge_elev:0" ""
              end"""
        );
        Coords gap = new Coords(0, 2);
        assertTrue(BridgeConstruction.isBridgeRepairSite(repairBoard, gap, EXITS_NORTH_SOUTH),
              "A gap flanked by two surviving spans must be a repair site");
    }

    @Test
    void aGapWithNoSurvivingSpanIsNotARepairSite() {
        // A water hex with plain land banks and no adjacent bridge is a fresh build, not a repair.
        assertEquals(BridgeConstruction.BridgeRepairIssue.NO_SURVIVING_SPAN,
              BridgeConstruction.bridgeRepairIssue(board, WATER_TARGET_COORDS, EXITS_NORTH_SOUTH),
              "Without a surviving span pointing in, there is nothing to repair");
    }

    @Test
    void aHexThatStillHoldsABridgeIsNotARepairGap() {
        // 0502 on the test board already holds a map-authored bridge; there is no gap to fill.
        Coords existingBridge = new Coords(4, 1);
        assertEquals(BridgeConstruction.BridgeRepairIssue.NOT_A_GAP,
              BridgeConstruction.bridgeRepairIssue(board, existingBridge, EXITS_NORTH_SOUTH),
              "A hex that still holds a bridge is not a repairable gap");
    }

    @Test
    void aBentRepairToAnArbitrarySideBankIsRejected() {
        // The surviving span points in from the north; a repair that bends to a side bank instead of continuing the
        // run straight south is rejected, so the repaired section stays in the bridge's line.
        Board repairBoard = useRepairBoard("BRIDGE_REPAIR_BENT", """
              size 3 3
              hex 0101 0 "" ""
              hex 0201 0 "water:1;bridge:1:09;bridge_cf:30;bridge_elev:0" ""
              hex 0301 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "water:1" ""
              hex 0302 0 "" ""
              hex 0103 0 "" ""
              hex 0203 0 "" ""
              hex 0303 0 "" ""
              end"""
        );
        Coords gap = new Coords(1, 1);
        assertTrue(BridgeConstruction.isBridgeRepairSite(repairBoard, gap, BridgeConstruction.exitsFor(0, 3)),
              "The straight north-south repair that continues the run must be valid");
        assertEquals(BridgeConstruction.BridgeRepairIssue.FAR_SIDE_UNANCHORED,
              BridgeConstruction.bridgeRepairIssue(repairBoard, gap, BridgeConstruction.exitsFor(0, 1)),
              "A repair that bends to a side bank instead of continuing the run straight must be rejected");
    }

    @Test
    void aRepairedSectionMatchesTheSurvivingSpanDeckNotTheLowerBank() {
        // The surviving span's deck is raised 2 above the water; the south bank is at level 0. A fresh build would
        // dip the deck to the lower bank, but a repair must match the span so the bridge reconnects at its height.
        Board repairBoard = useRepairBoard("BRIDGE_REPAIR_DECK", """
              size 1 3
              hex 0101 0 "water:1;bridge:1:09;bridge_cf:30;bridge_elev:2" ""
              hex 0102 0 "water:1" ""
              hex 0103 0 "" ""
              end"""
        );
        Coords gap = new Coords(0, 1);
        BridgeConstruction.placeRepairedBridge(repairBoard, gap, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT, 30);
        assertEquals(2, repairBoard.getHex(gap).terrainLevel(Terrains.BRIDGE_ELEV),
              "A repaired section must sit at the surviving span's deck (elevation 2), not the lower south bank");
    }

    @Test
    void aRepairedSectionIsMarkedAndAFreshBuildIsNot() {
        // The repaired section carries the persistent BRIDGE_REPAIRED marker terrain (which drives the field-repair
        // badge and survives save games); a freshly built bridge does not, so the two are distinguishable.

        // A fresh build on the default board's water site must not be marked as a repair.
        BridgeConstruction.placeBridge(board, WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT,
              30);
        assertFalse(board.getHex(WATER_TARGET_COORDS).containsTerrain(Terrains.BRIDGE_REPAIRED),
              "A freshly built bridge must not carry the repair marker");

        Board repairBoard = useRepairBoard("BRIDGE_REPAIR_MARKER", """
              size 1 3
              hex 0101 0 "water:1;bridge:1:09;bridge_cf:30;bridge_elev:0" ""
              hex 0102 0 "water:1" ""
              hex 0103 0 "" ""
              end"""
        );
        Coords gap = new Coords(0, 1);
        BridgeConstruction.placeRepairedBridge(repairBoard, gap, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT, 30);
        assertTrue(repairBoard.getHex(gap).containsTerrain(Terrains.BRIDGE_REPAIRED),
              "A repaired section must carry the BRIDGE_REPAIRED marker so the board view can badge it");
    }

    @Test
    void endPhaseCompletesARepairFromTheSurvivingSpanFooting() {
        // The engineer stands on the surviving span deck (bridge-deck footing) and rebuilds the adjacent gap over
        // six turns; the finished section is a repaired span at the run's deck and CF, registered as a structure.
        Board repairBoard = useRepairBoard("BRIDGE_REPAIR_LIFECYCLE", """
              size 1 4
              hex 0101 0 "" ""
              hex 0102 0 "water:1;bridge:1:09;bridge_cf:30;bridge_elev:0" ""
              hex 0103 0 "water:1" ""
              hex 0104 0 "water:1;bridge:1:09;bridge_cf:30;bridge_elev:0" ""
              end"""
        );
        Coords gap = new Coords(0, 2);
        // The engineer works from the surviving span to the north of the gap
        ConvInfantry engineers = createEngineerPlatoon(new Coords(0, 1));
        engineers.startBridgeRepair(gap, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);
        assertTrue(engineers.isBridgeBuildRepair(), "The platoon must be flagged as repairing, not freshly building");

        for (int round = 1; round <= 5; round++) {
            gameManager.checkBuildBridges();
            assertFalse(repairBoard.getHex(gap).containsTerrain(Terrains.BRIDGE),
                  "No section may appear before the repair is done");
            engineers.newRound(round + 1);
        }
        gameManager.checkBuildBridges();

        Hex hex = repairBoard.getHex(gap);
        assertTrue(hex.containsTerrain(Terrains.BRIDGE), "The repaired section must be placed after 6 full turns");
        assertEquals(30, hex.terrainLevel(Terrains.BRIDGE_CF),
              "A light section repaired over water must have its CF doubled to 30 (TO:AUE)");
        assertEquals(0, hex.terrainLevel(Terrains.BRIDGE_ELEV),
              "The repaired section must match the surviving spans' deck so the bridge reconnects");
        assertTrue(hex.containsTerrain(Terrains.BRIDGE_REPAIRED),
              "A completed repair must mark the hex so the field-repair badge shows");
        assertNotNull(repairBoard.getBuildingAt(gap), "The repaired section must register as a board structure");
        assertFalse(engineers.isBuildingBridge(), "The repair must end when the section is completed");
    }

    @Test
    void repairFlagSurvivesSerialization() throws Exception {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeRepair(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOut = new ObjectOutputStream(buffer)) {
            objectOut.writeObject(engineers);
        }
        ConvInfantry restored;
        try (ObjectInputStream objectIn = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            restored = (ConvInfantry) objectIn.readObject();
        }

        assertTrue(restored.isBridgeBuildRepair(),
              "A repair in progress must still be a repair after a save game round trip");
    }

    // endregion

    // region Weight collapse (a built bridge is a real load-bearing structure)

    /** A 1x3 column with land banks at level 2 over a water hex, so a placed bridge deck sits at elevation 2. */
    private static final String COLLAPSE_BOARD = "BRIDGE_COLLAPSE_BOARD";

    private IBuilding placeCollapseTestBridge(int constructionFactor) {
        initializeBoard(COLLAPSE_BOARD, """
              size 1 3
              hex 0101 2 "" ""
              hex 0102 0 "water:1" ""
              hex 0103 2 "" ""
              end"""
        );
        setBoard(COLLAPSE_BOARD);
        Board collapseBoard = getBoard(COLLAPSE_BOARD);
        Coords section = new Coords(0, 1);
        int exits = BridgeConstruction.exitsFor(section.direction(new Coords(0, 0)),
              section.direction(new Coords(0, 2)));
        return BridgeConstruction.placeBridge(collapseBoard, section, exits, ConvInfantry.BRIDGE_TYPE_LIGHT,
              constructionFactor);
    }

    @Test
    void builtBridgeIsALoadBearingStructureThatCanCollapse() {
        // A built bridge is a normal registered structure whose CF is its load capacity in tons; it participates in
        // standard collapse (CF -> 0), so it drops under a unit heavier than its CF. The weight threshold and the
        // rider's fall are standard bridge handling (confirmed in play); here we lock in that an engineer-built
        // bridge is a real, collapsible load-bearing structure with the expected capacity.
        IBuilding bridge = placeCollapseTestBridge(30);
        Coords section = new Coords(0, 1);
        assertNotNull(getBoard(COLLAPSE_BOARD).getBuildingAt(section),
              "The built bridge must be registered as a board structure");
        assertEquals(30, bridge.getCurrentCF(section),
              "The bridge's CF is its load capacity in tons (a unit over 30 tons would collapse it)");

        Map<BoardLocation, List<Entity>> positionMap = new HashMap<>();
        positionMap.put(BoardLocation.of(section, IGame.DEFAULT_BOARD_ID), new ArrayList<>());
        BuildingCollapseHandler collapseHandler = new BuildingCollapseHandler(gameManager);
        collapseHandler.collapseBuilding(bridge, positionMap, section, new Vector<>());

        assertEquals(0, bridge.getCurrentCF(section),
              "The engineer bridge must collapse (CF 0) like any structure");
    }

    // endregion

    // region Crossing a completed bridge

    /** The river hex of the 1x3 crossing boards. */
    private static final Coords CROSSING_RIVER_COORDS = new Coords(0, 1);

    @Test
    void bridgesCanBeChainedAcrossATwoHexRiver() {
        // A two-hex-wide river: span 1 is built from the near bank into the first river hex; the engineers then
        // stand on span 1 to build span 2 in the second river hex, connecting span 1 to the far bank. The second
        // site is valid because a water hex may connect to another bridge (TM p.242), and the two spans form one
        // continuous crossing a ground unit can drive across.
        initializeBoard("BRIDGE_CHAIN_BOARD", """
              size 1 4
              hex 0101 0 "" ""
              hex 0102 0 "water:1" ""
              hex 0103 0 "water:1" ""
              hex 0104 0 "" ""
              end"""
        );
        setBoard("BRIDGE_CHAIN_BOARD");
        Board chainBoard = getBoard("BRIDGE_CHAIN_BOARD");
        Coords nearBank = new Coords(0, 0);
        Coords riverHexOne = new Coords(0, 1);
        Coords riverHexTwo = new Coords(0, 2);
        Coords farBank = new Coords(0, 3);

        // Span 1: near bank <-> first river hex's far side (the second river hex)
        int spanOneExits = BridgeConstruction.exitsFor(riverHexOne.direction(nearBank),
              riverHexOne.direction(riverHexTwo));
        assertTrue(BridgeConstruction.isValidBridgeSite(chainBoard, riverHexOne, spanOneExits),
              "Span 1 over the first river hex must be a valid site (one bank is land)");
        BridgeConstruction.placeBridge(chainBoard, riverHexOne, spanOneExits, ConvInfantry.BRIDGE_TYPE_LIGHT, 30);

        // Span 2: first river hex (now bridged) <-> far bank
        int spanTwoExits = BridgeConstruction.exitsFor(riverHexTwo.direction(riverHexOne),
              riverHexTwo.direction(farBank));
        assertTrue(BridgeConstruction.isValidBridgeSite(chainBoard, riverHexTwo, spanTwoExits),
              "Span 2 must be a valid site by connecting to span 1 (a bridge) and the far bank");
        BridgeConstruction.placeBridge(chainBoard, riverHexTwo, spanTwoExits, ConvInfantry.BRIDGE_TYPE_LIGHT, 30);

        // A tank drives the full crossing: near bank -> span 1 -> span 2 -> far bank
        MovePath path = getMovePathFor(new Tank(), EntityMovementMode.TRACKED,
              MoveStepType.CLIMB_MODE_ON, MoveStepType.FORWARDS, MoveStepType.FORWARDS, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal(), "A tank must be able to drive across two chained bridge spans; path was: "
              + describeSteps(path));
    }

    @Test
    void laidBridgeHasExitsOnlyTowardItsBanks() {
        // The exits the bridge is laid with must point at its two banks and nowhere else; movement uses them to
        // decide where a unit may get on and off the bridge
        setBoard(CROSSING_BOARD);
        BridgeConstruction.placeBridge(getBoard(CROSSING_BOARD), CROSSING_RIVER_COORDS, EXITS_NORTH_SOUTH,
              ConvInfantry.BRIDGE_TYPE_LIGHT, 30);

        Hex bridgeHex = getBoard(CROSSING_BOARD).getHex(CROSSING_RIVER_COORDS);
        assertTrue(bridgeHex.containsTerrainExit(Terrains.BRIDGE, 0), "Bridge must have an exit toward its north bank");
        assertTrue(bridgeHex.containsTerrainExit(Terrains.BRIDGE, 3), "Bridge must have an exit toward its south bank");
        for (int nonBankDirection : new int[] { 1, 2, 4, 5 }) {
            assertFalse(bridgeHex.containsTerrainExit(Terrains.BRIDGE, nonBankDirection),
                  "Bridge must not have an exit toward non-bank side " + nonBankDirection);
        }
    }

    @Test
    void crossingOntoABridgeFromANonBankSideIsIllegal() {
        // A bridge over water whose exits face its side banks, not north/south: a tank approaching from the north
        // (a side the bridge does not connect) may not step onto the deck - you board a bridge at its ends
        initializeBoard("BRIDGE_EXIT_BOARD", """
              size 2 3
              hex 0101 0 "" ""
              hex 0201 0 "" ""
              hex 0102 0 "water:1" ""
              hex 0202 0 "" ""
              hex 0103 0 "" ""
              hex 0203 0 "" ""
              end"""
        );
        setBoard("BRIDGE_EXIT_BOARD");
        Board exitBoard = getBoard("BRIDGE_EXIT_BOARD");
        Coords middle = new Coords(0, 1);
        Coords northBank = new Coords(0, 0);
        Coords southBank = new Coords(0, 2);

        // Connect two on-board side banks that are not the north or south neighbour the tank will approach from
        int firstSide = -1;
        int secondSide = -1;
        for (int direction = 0; direction < 6; direction++) {
            Coords neighbour = middle.translated(direction);
            if (exitBoard.contains(neighbour) && !neighbour.equals(northBank) && !neighbour.equals(southBank)) {
                if (firstSide < 0) {
                    firstSide = direction;
                } else if (secondSide < 0) {
                    secondSide = direction;
                }
            }
        }
        int sideExits = BridgeConstruction.exitsFor(firstSide, secondSide);
        BridgeConstruction.placeBridge(exitBoard, middle, sideExits, ConvInfantry.BRIDGE_TYPE_LIGHT, 30);

        MovePath path = getMovePathFor(new Tank(), EntityMovementMode.TRACKED,
              MoveStepType.CLIMB_MODE_ON, MoveStepType.FORWARDS, MoveStepType.FORWARDS);
        assertFalse(path.isMoveLegal(), "A tank may not cross onto a bridge from a side the bridge does not "
              + "connect; path was: " + describeSteps(path));
    }

    @Test
    void infantryCanCrossACompletedBridgeWithClimbModeOn() {
        // Crossing on top of a bridge requires climb mode "climb up"; with "go through" units prefer to wade
        // under the bridge instead (TO:AR p.115, see also the climb mode notes in the README)
        setBoard(CROSSING_BOARD);
        BridgeConstruction.placeBridge(getBoard(CROSSING_BOARD), CROSSING_RIVER_COORDS, EXITS_NORTH_SOUTH,
              ConvInfantry.BRIDGE_TYPE_LIGHT, 30);

        MovePath path = getMovePathFor(createCrossingInfantry(),
              MoveStepType.CLIMB_MODE_ON, MoveStepType.FORWARDS, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal(), "Foot infantry must be able to cross a completed engineer bridge but the "
              + "path was: " + describeSteps(path) + " walkMP=" + path.getEntity().getWalkMP()
              + " runMP=" + path.getEntity().getRunMP()
              + " lastMpUsed=" + path.getLastStep().getMpUsed());
    }

    @Test
    void infantryCanCrossAMapAuthoredBridgeWithClimbModeOn() {
        // Control: the same crossing over a bridge that was part of the board file must also be legal,
        // proving the placed bridge behaves no differently
        setBoard(CROSSING_BOARD_AUTHORED);

        MovePath path = getMovePathFor(createCrossingInfantry(),
              MoveStepType.CLIMB_MODE_ON, MoveStepType.FORWARDS, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal(), "Foot infantry must be able to cross a map-authored bridge");
    }

    @Test
    void tankCanCrossACompletedBridgeWithoutClimbMode() {
        // For a tank the river is impassable, so it mounts the bridge even with climb mode off
        setBoard(CROSSING_BOARD);
        BridgeConstruction.placeBridge(getBoard(CROSSING_BOARD), CROSSING_RIVER_COORDS, EXITS_NORTH_SOUTH,
              ConvInfantry.BRIDGE_TYPE_LIGHT, 30);

        MovePath path = getMovePathFor(new Tank(), EntityMovementMode.TRACKED,
              MoveStepType.FORWARDS, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal(), "A tank must be able to cross a completed engineer bridge but the path was: "
              + describeSteps(path));
    }

    @Test
    void infantryCanCrossAGapBridgeWithClimbModeOn() {
        // Bisect: the same crossing over a dry gap instead of water isolates whether the water hex is the problem
        initializeBoard("BRIDGE_GAP_BOARD", """
              size 1 3
              hex 0101 1 "" ""
              hex 0102 0 "bridge:1:09;bridge_cf:30;bridge_elev:1" ""
              hex 0103 1 "" ""
              end"""
        );
        setBoard("BRIDGE_GAP_BOARD");

        MovePath path = getMovePathFor(createCrossingInfantry(),
              MoveStepType.CLIMB_MODE_ON, MoveStepType.FORWARDS, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal(), "Foot infantry must be able to cross a gap bridge but the path was: "
              + describeSteps(path));
    }

    @Test
    void infantryCanStepOffABridgeDeck() {
        // Isolates the dismount: the platoon starts on the bridge deck and steps onto the same-level bank
        initializeBoard("BRIDGE_DISMOUNT_BOARD", """
              size 1 2
              hex 0101 0 "water:1;bridge:1:09;bridge_cf:30;bridge_elev:1" ""
              hex 0102 1 "" ""
              end"""
        );
        setBoard("BRIDGE_DISMOUNT_BOARD");

        MovePath path = getMovePathFor(new ConvInfantry(), 1, EntityMovementMode.INF_LEG,
              MoveStepType.CLIMB_MODE_ON, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal(), "Foot infantry must be able to step off a bridge deck onto a same-level "
              + "bank but the path was: " + describeSteps(path));
    }

    @Test
    void tankCanCrossACompletedBridgeWithClimbModeOn() {
        setBoard(CROSSING_BOARD);
        BridgeConstruction.placeBridge(getBoard(CROSSING_BOARD), CROSSING_RIVER_COORDS, EXITS_NORTH_SOUTH,
              ConvInfantry.BRIDGE_TYPE_LIGHT, 30);

        MovePath path = getMovePathFor(new Tank(), EntityMovementMode.TRACKED,
              MoveStepType.CLIMB_MODE_ON, MoveStepType.FORWARDS, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal(), "A tank with climb mode on must be able to cross a completed engineer "
              + "bridge but the path was: " + describeSteps(path));
    }

    @Test
    void bridgeBuildStateSurvivesSerialization() throws Exception {
        // Players can save mid-build, and clients receive the build state through entity update packets. Both
        // save games (XStream field reflection) and packets (Java serialization) persist all non-transient
        // fields, so a Java serialization round trip proves none of the build state is lost.
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_MEDIUM);
        engineers.spendBridgeBuildPoints(ConvInfantry.BRIDGE_TYPE_MEDIUM);
        // Bank two turns of work, then take casualties to extend the required turns
        gameManager.checkBuildBridges();
        engineers.newRound(2);
        gameManager.checkBuildBridges();
        engineers.setInternal(5, ConvInfantry.LOC_INFANTRY);
        engineers.updateBridgeBuildCasualties();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOut = new ObjectOutputStream(buffer)) {
            objectOut.writeObject(engineers);
        }
        ConvInfantry restored;
        try (ObjectInputStream objectIn = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            restored = (ConvInfantry) objectIn.readObject();
        }

        assertTrue(restored.isBuildingBridge(), "The restored platoon must still be building");
        assertEquals(engineers.getBridgeBuildTurns(), restored.getBridgeBuildTurns(),
              "Completed build turns must survive a save game round trip");
        assertEquals(engineers.getBridgeBuildRequiredTurns(), restored.getBridgeBuildRequiredTurns(),
              "The casualty-extended required turns must survive a save game round trip");
        assertEquals(WATER_TARGET_COORDS, restored.getBridgeTargetCoords(),
              "The bridge site must survive a save game round trip");
        assertEquals(EXITS_NORTH_SOUTH, restored.getBridgeExits(),
              "The bridge orientation must survive a save game round trip");
        assertEquals(ConvInfantry.BRIDGE_TYPE_MEDIUM, restored.getBridgeType(),
              "The bridge type must survive a save game round trip");
        assertEquals(0, restored.getBridgeBuildPoints(),
              "The spent bridge building budget must survive a save game round trip");
    }

    /**
     * @return A motorized-speed test platoon (3 MP) so a full bank-bridge-bank crossing fits in one turn; a foot
     *       platoon (1 MP) legitimately needs two turns for it.
     */
    private static ConvInfantry createCrossingInfantry() {
        ConvInfantry infantry = new ConvInfantry();
        // Mode first: setting the movement mode resets the walk MP to the mode default of 1
        infantry.setMovementMode(EntityMovementMode.INF_LEG);
        infantry.setOriginalWalkMP(3);
        return infantry;
    }

    /** Builds a diagnostic description of every step in the path for assertion messages. */
    private static String describeSteps(MovePath path) {
        StringBuilder description = new StringBuilder();
        for (megamek.common.moves.MoveStep step : path.getStepVector()) {
            description.append(String.format("[%s to %s elev %d type %s] ",
                  step.getType(), step.getPosition(), step.getElevation(), step.getMovementType(false)));
        }
        return description.toString();
    }

    @Test
    void completedBridgeHexMatchesMapAuthoredBridgeHex() {
        // The hex a finished build produces must look exactly like the same bridge written in a board file
        setBoard(CROSSING_BOARD);
        BridgeConstruction.placeBridge(getBoard(CROSSING_BOARD), CROSSING_RIVER_COORDS, EXITS_NORTH_SOUTH,
              ConvInfantry.BRIDGE_TYPE_LIGHT, 30);

        Hex placedHex = getBoard(CROSSING_BOARD).getHex(CROSSING_RIVER_COORDS);
        Hex authoredHex = getBoard(CROSSING_BOARD_AUTHORED).getHex(CROSSING_RIVER_COORDS);
        assertEquals(authoredHex.terrainLevel(Terrains.BRIDGE), placedHex.terrainLevel(Terrains.BRIDGE),
              "Placed bridge type must match the map-authored bridge");
        assertEquals(authoredHex.terrainLevel(Terrains.BRIDGE_ELEV), placedHex.terrainLevel(Terrains.BRIDGE_ELEV),
              "Placed bridge elevation must match the map-authored bridge");
        assertEquals(authoredHex.terrainLevel(Terrains.BRIDGE_CF), placedHex.terrainLevel(Terrains.BRIDGE_CF),
              "Placed bridge CF must match the map-authored bridge");
        assertEquals(authoredHex.getTerrain(Terrains.BRIDGE).getExits(),
              placedHex.getTerrain(Terrains.BRIDGE).getExits(),
              "Placed bridge exits must match the map-authored bridge");
    }

    // endregion
}
