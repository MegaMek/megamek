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

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.BridgeConstruction;
import megamek.common.board.Coords;
import megamek.common.equipment.BridgeLayerState;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Tank;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Integration tests for the END-phase placement of a declared Bridge-Layer (AVLB) deployment (TM p.242 / TW), driven
 * through {@link TWGameManager#checkDeployBridges()} -> {@link AvlbDeployPhaseHandler}. The board site validation and
 * bridge placement themselves live in {@code BridgeConstruction} (covered by {@code BridgeBuildingTest}); here the
 * focus is the AVLB-specific lifecycle: the bridge is laid the turn AFTER the declaration only if the unit stayed
 * stationary, the over-water 2x CF flotation, the plain CF over a dry gap, and the cancellation paths.
 *
 * @author Claude Code (Opus 4.8)
 */
public class BridgeLayerDeployTest extends GameBoardTestCase {

    /** Column board: land bank (level 0) / water (depth 2) / land bank (level 0). A valid over-water site at (0,1). */
    private static final String WATER_BOARD = "AVLB_WATER_BOARD";
    /** Column board: level-1 bank / level-0 clear gap / level-2 bank. A valid dry-gap site at (0,1). */
    private static final String GAP_BOARD = "AVLB_GAP_BOARD";

    private static final Coords NEAR_BANK = new Coords(0, 0);
    private static final Coords TARGET = new Coords(0, 1);
    /** Facing south (3): the front hex of a unit at (0,0) is the target (0,1). */
    private static final int FACING_SOUTH = 3;
    /** Heavy Bridge-Layer carries a CF 45 bridge. */
    private static final int HEAVY_CF = 45;

    private TWGameManager gameManager;
    private Game game;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        // Re-load the boards each test: placement mutates hexes and registers structures.
        initializeBoard(WATER_BOARD, """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "water:2" ""
              hex 0103 0 "" ""
              end""");
        initializeBoard(GAP_BOARD, """
              size 1 3
              hex 0101 1 "" ""
              hex 0102 0 "" ""
              hex 0103 2 "" ""
              end""");

        gameManager = Mockito.spy(new TWGameManager());
        // Stub the methods that need a live Server connection.
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), anyInt());
        Mockito.doNothing().when(gameManager).entityUpdate(anyInt());
        Mockito.doNothing().when(gameManager).sendNewBuildings(any());
        Mockito.doNothing().when(gameManager).sendChangedBuildings(any());

        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Test"));
    }

    /** Exits connecting the north (0) and south (3) hexsides - a straight bridge along the column. */
    private static int northSouthExits() {
        return BridgeConstruction.exitsFor(0, FACING_SOUTH);
    }

    private Tank createBridgelayerTank(Board board) throws LocationFullException {
        Tank tank = new Tank();
        tank.setId(game.getNextEntityId());
        tank.setOwner(game.getPlayer(0));
        tank.setCrew(new Crew(CrewType.CREW));
        tank.setBoardId(board.getBoardId());
        tank.setPosition(NEAR_BANK);
        tank.setFacing(FACING_SOUTH);
        tank.setElevation(0);
        tank.setDeployed(true);
        tank.addEquipment(MiscType.createHeavyBridgeLayer(), Tank.LOC_RIGHT);
        game.addEntity(tank);
        return tank;
    }

    private static BridgeLayerState bridgeState(Tank tank) {
        return tank.getMisc().get(0).getBridgeLayerState();
    }

    @Test
    @DisplayName("a bridge declared over water is placed the following turn at double CF (flotation)")
    void deploysOverWaterAtDoubleCF() throws LocationFullException {
        Board board = getBoard(WATER_BOARD);
        game.setBoard(board);
        game.setRoundCount(2);
        Tank tank = createBridgelayerTank(board);
        bridgeState(tank).startDeploy(TARGET, northSouthExits(), 1);
        tank.delta_distance = 0;

        gameManager.checkDeployBridges();

        assertNotNull(board.getBuildingAt(TARGET), "the bridge is placed over the water hex");
        assertEquals(HEAVY_CF * 2, board.getHex(TARGET).terrainLevel(Terrains.BRIDGE_CF),
              "an over-water bridge is placed at double CF (45 -> 90) for the flotation support rule");
        assertTrue(bridgeState(tank).isDeployed());
        assertFalse(bridgeState(tank).isDeployPending());
    }

    @Test
    @DisplayName("a bridge declared across a dry gap is placed at its plain CF (no flotation)")
    void deploysAcrossDryGapAtPlainCF() throws LocationFullException {
        Board board = getBoard(GAP_BOARD);
        game.setBoard(board);
        game.setRoundCount(2);
        Tank tank = createBridgelayerTank(board);
        bridgeState(tank).startDeploy(TARGET, northSouthExits(), 1);
        tank.delta_distance = 0;

        gameManager.checkDeployBridges();

        assertNotNull(board.getBuildingAt(TARGET), "the bridge spans the gap between the two elevated banks");
        assertEquals(HEAVY_CF, board.getHex(TARGET).terrainLevel(Terrains.BRIDGE_CF),
              "a dry-gap bridge is placed at its plain CF (45)");
        assertTrue(bridgeState(tank).isDeployed());
    }

    @Test
    @DisplayName("the bridge is not placed in the declaration round, only the turn after")
    void notPlacedUntilTheTurnAfterDeclaration() throws LocationFullException {
        Board board = getBoard(WATER_BOARD);
        game.setBoard(board);
        game.setRoundCount(1);
        Tank tank = createBridgelayerTank(board);
        bridgeState(tank).startDeploy(TARGET, northSouthExits(), 1);
        tank.delta_distance = 0;

        gameManager.checkDeployBridges();
        assertNull(board.getBuildingAt(TARGET), "nothing is placed during the round the deployment was declared");
        assertTrue(bridgeState(tank).isDeployPending(), "the deployment remains pending");

        game.setRoundCount(2);
        gameManager.checkDeployBridges();
        assertNotNull(board.getBuildingAt(TARGET), "the bridge is placed on the following turn");
    }

    @Test
    @DisplayName("moving instead of staying stationary cancels the deployment")
    void movingCancelsTheDeployment() throws LocationFullException {
        Board board = getBoard(WATER_BOARD);
        game.setBoard(board);
        game.setRoundCount(2);
        Tank tank = createBridgelayerTank(board);
        bridgeState(tank).startDeploy(TARGET, northSouthExits(), 1);
        tank.delta_distance = 2;

        gameManager.checkDeployBridges();

        assertNull(board.getBuildingAt(TARGET), "no bridge is placed when the unit moved");
        assertFalse(bridgeState(tank).isDeployPending(), "the cancelled deployment is cleared");
        assertFalse(bridgeState(tank).isDeployed());
    }

    @Test
    @DisplayName("a crit-disabled deploy mechanism cancels a pending deployment")
    void disabledMechanismCancelsTheDeployment() throws LocationFullException {
        Board board = getBoard(WATER_BOARD);
        game.setBoard(board);
        game.setRoundCount(2);
        Tank tank = createBridgelayerTank(board);
        BridgeLayerState state = bridgeState(tank);
        state.startDeploy(TARGET, northSouthExits(), 1);
        state.setDeployMechanismDisabled(true);
        tank.delta_distance = 0;

        gameManager.checkDeployBridges();

        assertNull(board.getBuildingAt(TARGET), "no bridge is placed when the deploy mechanism was disabled");
        assertFalse(state.isDeployPending(), "the cancelled deployment is cleared");
    }
}
