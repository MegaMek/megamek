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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.BoardType;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.game.Game;
import megamek.common.rules.RulesManager;
import megamek.common.rules.totalwarfare.TWRulesManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers where a tractor and its trailers may walk on. Under walk-on deployment a unit enters along a one-hex strip at
 * its board edge, but a straight train of three hexes or more cannot fit in one hex row, so a train may start as many
 * hexes deep as it is long. Every hex of the train is checked against the tractor's zone.
 */
class WalkOnTrainDeploymentTest {

    private static final int BOARD_SIZE = 20;
    private static final int BOARD_ID = 0;
    private static final int FACING_SOUTH = 3;

    private RulesManager originalRulesManager;
    private Game game;
    private Board board;
    private Player owner;
    private int nextId = 1;

    @BeforeEach
    void setUp() {
        EquipmentType.initializeTypes();
        originalRulesManager = Game.rulesManager;
        Game.rulesManager = new TWRulesManager();
        Game.rulesManager.getRulesGame().setWalkOnDeployment(true);

        Hex[] hexes = new Hex[BOARD_SIZE * BOARD_SIZE];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        board = new Board(BOARD_SIZE, BOARD_SIZE, hexes);
        board.setBoardType(BoardType.GROUND);

        owner = new Player(0, "Owner");
        owner.setTeam(1);
        owner.setStartingPos(Board.START_N);
        game = new Game();
        game.setBoard(board);
        game.addPlayer(0, owner);
        game.setPhase(GamePhase.MOVEMENT);
        // A new game sits at round -1; the units below are due in round 0, so play round 1
        game.setCurrentRound(1);
    }

    @AfterEach
    void tearDown() {
        Game.rulesManager = originalRulesManager;
    }

    private Tank buildVehicle(boolean isTrailer) throws Exception {
        Tank vehicle = new Tank();
        vehicle.setId(nextId++);
        vehicle.setOwner(owner);
        vehicle.setWeight(isTrailer ? 10.0 : 75.0);
        vehicle.setMovementMode(EntityMovementMode.TRACKED);
        vehicle.setTrailer(isTrailer);
        vehicle.addEquipment(EquipmentType.get(EquipmentTypeLookup.HITCH), Tank.LOC_BODY);
        vehicle.setTrailerHitches();
        vehicle.setStartingPos(Board.START_N);
        vehicle.setDeployRound(0);
        game.addEntity(vehicle);
        return vehicle;
    }

    /** An undeployed tractor due to walk on at the north edge, with the requested number of trailers hitched. */
    private Tank buildTrain(int trailerCount) throws Exception {
        Tank tractor = buildVehicle(false);
        for (int index = 0; index < trailerCount; index++) {
            tractor.towUnit(buildVehicle(true).getId());
        }
        return tractor;
    }

    @Test
    void trainLengthCountsTheHexesTheTrainCovers() throws Exception {
        // The first trailer shares the tractor's hex and the rest pack two to a hex behind it
        assertEquals(1, TrainLayout.trainLengthInHexes(game, buildTrain(1)));
        assertEquals(2, TrainLayout.trainLengthInHexes(game, buildTrain(2)));
        assertEquals(2, TrainLayout.trainLengthInHexes(game, buildTrain(3)));
        assertEquals(3, TrainLayout.trainLengthInHexes(game, buildTrain(4)));
    }

    @Test
    void aWalkOnTrainMayStartAsDeepAsItIsLong() throws Exception {
        Tank tractor = buildTrain(4);

        assertTrue(board.isLegalDeployment(new Coords(10, 2), tractor), "a 3-hex train may start 3 hexes deep");
        assertFalse(board.isLegalDeployment(new Coords(10, 3), tractor), "but no deeper");
    }

    @Test
    void aUnitWithoutATrainStillWalksOnAtTheEdge() throws Exception {
        Tank tank = buildVehicle(false);

        assertTrue(board.isLegalDeployment(new Coords(10, 0), tank));
        assertFalse(board.isLegalDeployment(new Coords(10, 1), tank), "a lone unit keeps the one-hex strip");
    }

    @Test
    void aTrainFacingIntoTheBoardFitsWithItsTrailersReachingBackToTheEdge() throws Exception {
        // Two, three and four trailers: the trains the old one-hex strip could not hold
        for (int trailerCount = 2; trailerCount <= 4; trailerCount++) {
            Tank tractor = buildTrain(trailerCount);
            int deepestRow = TrainLayout.trainLengthInHexes(game, tractor) - 1;

            assertNull(TrainLayout.firstIllegalDeploymentHex(game, tractor, new Coords(10, deepestRow), BOARD_ID,
                  FACING_SOUTH), trailerCount + " trailers must fit facing into the board");
            assertTrue(TrainLayout.hasLegalDeploymentFacing(game, tractor, new Coords(10, deepestRow), BOARD_ID));
        }
    }

    @Test
    void aTrainThatWouldStickOffTheBoardIsRejected() throws Exception {
        Tank tractor = buildTrain(4);

        // Facing south from the edge row, the trailers would sit north of the board
        assertEquals(new Coords(10, -1), TrainLayout.firstIllegalDeploymentHex(game, tractor, new Coords(10, 0),
              BOARD_ID, FACING_SOUTH));
    }

    @Test
    void aHexTooDeepForTheTrainHasNoLegalFacing() throws Exception {
        Tank tractor = buildTrain(4);

        assertFalse(TrainLayout.hasLegalDeploymentFacing(game, tractor, new Coords(10, 5), BOARD_ID));
    }
}
