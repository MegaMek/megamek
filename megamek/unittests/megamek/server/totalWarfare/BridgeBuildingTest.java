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

import megamek.common.GameBoardTestCase;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.BridgeConstruction;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.net.packets.Packet;
import megamek.common.units.ConvInfantry;
import megamek.common.units.EntityMovementMode;
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
    /** Clear hex between a level 0 bank to the north and a level 4 bank to the south; bank difference too large. */
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
    void onlyOppositeHexsidePairsAreValidExits() {
        for (int axis = 0; axis < 3; axis++) {
            assertTrue(BridgeConstruction.isValidBridgeExits((1 << axis) | (1 << (axis + 3))),
                  "Opposite hexside pair on axis " + axis + " must be a valid bridge orientation");
        }
        assertFalse(BridgeConstruction.isValidBridgeExits(1 << 2), "A single hexside is not a valid orientation");
        assertFalse(BridgeConstruction.isValidBridgeExits((1 << 0) | (1 << 1)),
              "Adjacent hexsides are not a valid orientation (a bridge cannot bend)");
        assertFalse(BridgeConstruction.isValidBridgeExits((1 << 0) | (1 << 2) | (1 << 3)),
              "Three hexsides are not a valid orientation");
        assertFalse(BridgeConstruction.isValidBridgeExits(0), "No hexsides is not a valid orientation");
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
    void largeBankLevelDifferenceIsInvalid() {
        assertFalse(BridgeConstruction.isValidBridgeSite(board, HIGH_BANK_TARGET_COORDS, EXITS_NORTH_SOUTH),
              "Banks 4 levels apart must not be a valid bridge site (TM p.242 allows at most 1)");
    }

    @Test
    void gapWithOneLevelBankDifferenceIsValid() {
        assertTrue(BridgeConstruction.isValidBridgeSite(board, GAP_TARGET_COORDS, EXITS_NORTH_SOUTH),
              "Gap between banks 1 level apart must be a valid bridge site (TM p.242)");
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
    void placedBridgeAcrossGapSitsAtTheLowerBank() {
        BridgeConstruction.placeBridge(board, GAP_TARGET_COORDS, EXITS_NORTH_SOUTH,
              ConvInfantry.BRIDGE_TYPE_MEDIUM, 40);

        Hex hex = board.getHex(GAP_TARGET_COORDS);
        assertEquals(1, hex.terrainLevel(Terrains.BRIDGE_ELEV),
              "A bridge between level 1 and level 2 banks must sit level with the lower bank");
    }

    // endregion

    // region ConvInfantry build lifecycle

    @Test
    void startingABuildLocksThePlatoonOutOfAllPhases() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);

        assertTrue(engineers.isBuildingBridge(), "The platoon must be building after the build is declared");
        assertFalse(engineers.isEligibleFor(GamePhase.MOVEMENT),
              "A building platoon may take no other action (movement)");
        assertFalse(engineers.isEligibleFor(GamePhase.FIRING), "A building platoon may take no other action (firing)");
        assertFalse(engineers.isEligibleFor(GamePhase.PHYSICAL),
              "A building platoon may take no other action (physical)");
    }

    @Test
    void buildProgressesEachRoundWhileAdjacent() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);

        engineers.newRound(2);
        engineers.newRound(3);

        assertEquals(2, engineers.getBridgeBuildTurns(),
              "Each new round adjacent to the site must add a completed turn of work");
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

        // END of the declaring round: 1 full turn worked, build continues
        gameManager.checkBuildBridges();
        assertTrue(engineers.isBuildingBridge(), "The build must still be in progress after 1 of 6 turns");
        assertFalse(board.getHex(WATER_TARGET_COORDS).containsTerrain(Terrains.BRIDGE),
              "No bridge may appear before the work is done");

        // Rounds 2-6: with the current round counted, the 6th END phase completes the build
        for (int round = 2; round <= 6; round++) {
            engineers.newRound(round);
        }
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
    void casualtiesDelayCompletionByOneTurn() {
        ConvInfantry engineers = createEngineerPlatoon(ENGINEER_COORDS);
        engineers.startBridgeBuild(WATER_TARGET_COORDS, EXITS_NORTH_SOUTH, ConvInfantry.BRIDGE_TYPE_LIGHT);

        // 5 completed turns plus the current round would normally finish the build this END phase...
        for (int round = 2; round <= 6; round++) {
            engineers.newRound(round);
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

    // endregion

    // region Crossing a completed bridge

    /** The river hex of the 1x3 crossing boards. */
    private static final Coords CROSSING_RIVER_COORDS = new Coords(0, 1);

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
        engineers.newRound(2);
        engineers.newRound(3);
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
