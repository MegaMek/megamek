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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import megamek.common.Player;
import megamek.common.bays.MekBay;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.BLKStructureFile;
import megamek.common.moves.MobileStructureMovement;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.net.packets.Packet;
import megamek.common.options.GameOptions;
import megamek.common.pathfinder.ShortestPathFinder;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.IBuilding;
import megamek.common.units.MobileStructure;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.util.BuildingBlock;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Native plotting, clipping and server execution; no UI controls or rendering are involved. */
class MobileStructurePathfindingTest {
    private static final Coords ORIGIN = new Coords(8, 8);
    private static final CubeCoords NORTH = new CubeCoords(0, -1, 1);

    private static class LocalManager extends TWGameManager {
        @Override public void send(Packet packet) { }
        @Override public void send(int id, Packet packet) { }
    }

    private record Fixture(TWGameManager manager, MobileStructure mobile) { }

    @BeforeAll static void equipment() { EquipmentType.initializeTypes(); }

    private Fixture fixture(EntityMovementMode mode, double mp) {
        var mobile = new MobileStructure(BuildingType.HEAVY, IBuilding.HANGAR);
        mobile.configureConstruction(BuildingType.HEAVY, IBuilding.HANGAR, 3, 40, 0,
              List.of(CubeCoords.ZERO, NORTH));
        mobile.setMovementMode(mode);
        mobile.setMaximumMP(mp);
        return fixture(mobile);
    }

    private Fixture fixture(MobileStructure mobile) {
        var manager = spy(new LocalManager());
        manager.getGame().setOptions(new GameOptions());
        manager.setDamageManager(new TWDamageManager());
        doNothing().when(manager).entityUpdate(anyInt());
        doNothing().when(manager).sendChangedHex(any(Coords.class), anyInt());
        doNothing().when(manager).sendChangedBuildings(any());
        var game = manager.getGame();
        game.setBoard(BoardLoader.initializeBoard("size 20 20\nend\n"));
        if (mobile.isWaterStructure()) {
            for (int x = 0; x < 20; x++) for (int y = 0; y < 20; y++) {
                game.getBoard().getHex(new Coords(x, y)).addTerrain(new Terrain(Terrains.WATER, 20));
            }
        }
        game.addPlayer(0, new Player(0, "Movement test"));
        game.setPhase(GamePhase.MOVEMENT);
        game.setRoundCount(1);
        mobile.setId(1);
        mobile.setOwner(game.getPlayer(0));
        mobile.setDeployed(true);
        game.addEntity(mobile);
        mobile.setPosition(ORIGIN);
        mobile.setElevation(mobile.getMovementMode() == EntityMovementMode.VTOL ? 5 : 0);
        mobile.updateBuildingEntityHexes(0, manager);
        return new Fixture(manager, mobile);
    }

    static Stream<Arguments> directionsAndBudgets() {
        return Stream.of(EntityMovementMode.TRACKED, EntityMovementMode.NAVAL,
                    EntityMovementMode.SUBMARINE, EntityMovementMode.VTOL)
              .flatMap(mode -> Stream.of(.25, 1.0)
                    .flatMap(mp -> IntStream.range(0, 6).mapToObj(direction -> Arguments.of(mode, mp, direction))));
    }

    private MovePath plot(Fixture f, Coords destination) {
        var path = new MovePath(f.manager().getGame(), f.mobile());
        path.findPathTo(destination, MoveStepType.FORWARDS);
        path.clipToPossible();
        assertEquals(destination, path.getFinalCoords(), "the native click-to-hex path must reach the selected hex");
        assertTrue(path.isMoveLegal());
        assertEquals(1, path.length(), "translation must not insert a pivot or a climb over the mobile's own roof");
        assertEquals(f.mobile().getFacing(), path.getFinalFacing());
        return path;
    }

    @ParameterizedTest
    @MethodSource("directionsAndBudgets")
    void plottedAdjacentMovesReachTheServerInEveryDirection(EntityMovementMode mode, double mp, int direction) {
        var f = fixture(mode, mp);
        Coords destination = ORIGIN.translated(direction);
        int initialElevation = f.mobile().getElevation();
        var path = plot(f, destination);
        var envelope = ShortestPathFinder.newInstanceOfOneToAll(f.mobile().getWalkMP(), MoveStepType.FORWARDS,
              f.manager().getGame());
        envelope.run(new MovePath(f.manager().getGame(), f.mobile()));
        assertNotNull(envelope.getComputedPath(destination), "the movement envelope must include a partial commitment");
        new MovePathHandler(f.manager(), f.mobile(), path, Map.of()).processMovement();
        assertEquals(mp < 1 ? ORIGIN : destination, f.mobile().getPosition());
        assertEquals(initialElevation, f.mobile().getElevation());
        assertEquals((int) (mp * 4), f.mobile().mpUsed);
        assertTrue(f.mobile().isDone());
        if (mp < 1) {
            assertEquals(destination, f.mobile().getMovementProgress().destination());
            assertEquals(1, f.mobile().getMovementProgress().quarters());
        } else {
            assertNull(f.mobile().getMovementProgress());
        }
    }

    @ParameterizedTest
    @EnumSource(value = EntityMovementMode.class, names = { "TRACKED", "NAVAL", "SUBMARINE", "VTOL" })
    void quarterMpCommitmentFinishesOnTheFourthPlottedTurn(EntityMovementMode mode) {
        var f = fixture(mode, .25);
        Coords destination = ORIGIN.translated(2);
        for (int round = 1; round <= 4; round++) {
            f.manager().getGame().setRoundCount(round);
            f.mobile().newRound(round);
            var path = plot(f, destination);
            assertEquals(5 - round, path.getMpUsed(), "preview deducts only already committed quarter points");
            new MovePathHandler(f.manager(), f.mobile(), path, Map.of()).processMovement();
            assertEquals(round == 4 ? destination : ORIGIN, f.mobile().getPosition());
            assertEquals(1, f.mobile().mpUsed);
            assertTrue(f.mobile().isDone());
        }
        assertNull(f.mobile().getMovementProgress());
    }

    @ParameterizedTest
    @EnumSource(value = EntityMovementMode.class, names = { "TRACKED", "NAVAL", "SUBMARINE", "VTOL" })
    void aTwoHexRouteRemainsStraightAndReachesTheServer(EntityMovementMode mode) {
        var f = fixture(mode, 2);
        if (f.mobile().isWaterStructure()) {
            f.mobile().declareWaterSpeed(4, 0); // The preceding round permits acceleration to 2 MP now.
        }
        Coords destination = ORIGIN.translated(5, 2);
        var path = new MovePath(f.manager().getGame(), f.mobile());
        path.findPathTo(destination, MoveStepType.FORWARDS);
        path.clipToPossible();
        assertTrue(path.isMoveLegal());
        assertEquals(destination, path.getFinalCoords());
        assertEquals(0, path.getFinalFacing());
        assertEquals(2, path.length());
        assertEquals(8, path.getMpUsed());
        new MovePathHandler(f.manager(), f.mobile(), path, Map.of()).processMovement();
        assertEquals(destination, f.mobile().getPosition());
        assertEquals(0, f.mobile().getFacing());
        assertEquals(8, f.mobile().mpUsed);
        assertNull(f.mobile().getMovementProgress());
    }

    @ParameterizedTest
    @EnumSource(value = EntityMovementMode.class, names = { "TRACKED", "NAVAL", "SUBMARINE", "VTOL" })
    void anExplicitPivotStillConsumesTheFullMovementAllowance(EntityMovementMode mode) {
        var f = fixture(mode, 1);
        var path = new MovePath(f.manager().getGame(), f.mobile()).addStep(MoveStepType.TURN_RIGHT);
        assertTrue(path.isMoveLegal());
        assertEquals(1, path.getFinalFacing());
        assertEquals(4, path.getMpUsed(), "TO:AUE p.35: an optional pivot costs the full movement allowance");
        Coords pivotedOrigin = path.getFinalCoords();
        new MovePathHandler(f.manager(), f.mobile(), path, Map.of()).processMovement();
        assertEquals(pivotedOrigin, f.mobile().getPosition());
        assertEquals(1, f.mobile().getFacing());
        assertEquals(4, f.mobile().mpUsed);
        assertNull(f.mobile().getMovementProgress());
    }

    @Test void verticalClearanceUsesTheFootprintAfterAPlottedPivot() {
        var f = fixture(EntityMovementMode.VTOL, 1);
        f.mobile().setElevation(6);
        f.mobile().updateBuildingEntityHexes(0, f.manager());
        var path = new MovePath(f.manager().getGame(), f.mobile()).addStep(MoveStepType.TURN_RIGHT,
              Map.of(MoveStep.MOBILE_PIVOT_Q_KEY, 0, MoveStep.MOBILE_PIVOT_R_KEY, 0));
        Coords sweptHex = f.mobile().computeBuildingCoordsForPositionAndFacing(path.getFinalCoords(), path.getFinalFacing())
              .stream().filter(coords -> !f.mobile().computeBuildingCoordsForPositionAndFacing(ORIGIN, 0)
                    .contains(coords)).findFirst().orElseThrow();
        f.manager().getGame().getBoard().getHex(sweptHex).setLevel(5);
        path = new MovePath(f.manager().getGame(), f.mobile()).addStep(MoveStepType.TURN_RIGHT,
              Map.of(MoveStep.MOBILE_PIVOT_Q_KEY, 0, MoveStep.MOBILE_PIVOT_R_KEY, 0));
        assertTrue(path.isMoveLegal(), "the pivot clears a level-five obstruction at elevation six");
        assertEquals(4, MobileStructureMovement.cost(f.manager().getGame(), f.mobile(), ORIGIN, 0, 6,
              ORIGIN, 0, 5), "the original footprint could descend");
        assertEquals(MobileStructureMovement.PROHIBITED,
              MobileStructureMovement.cost(f.manager().getGame(), f.mobile(), path.getFinalCoords(), path.getFinalFacing(), 6,
                    path.getFinalCoords(), path.getFinalFacing(), 5), "descent must inspect the rotated footprint");
        assertEquals(4,
              MobileStructureMovement.cost(f.manager().getGame(), f.mobile(), path.getFinalCoords(), path.getFinalFacing(), 6,
                    path.getFinalCoords(), path.getFinalFacing(), 7));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void loopingBackToTheOriginalPoseCannotReuseSavedMovementCredit(boolean loadFirst) {
        var f = fixture(EntityMovementMode.TRACKED, .75);
        Coords destination = ORIGIN.translated(0);
        new MovePathHandler(f.manager(), f.mobile(), plot(f, destination), Map.of()).processMovement();
        assertEquals(ORIGIN, f.mobile().getPosition());
        assertEquals(3, f.mobile().getMovementProgress().quarters());

        f.mobile().setMaximumMP(2);
        f.manager().getGame().setRoundCount(2);
        f.mobile().newRound(2);
        var path = new MovePath(f.manager().getGame(), f.mobile());
        BipedMek passenger = null;
        if (loadFirst) {
            passenger = waitingPassenger(f);
            path.addStep(MoveStepType.LOAD, passenger, passenger.getPosition());
            assertTrue(path.isMoveLegal());
            assertEquals(0, path.getMpUsed(), "loading costs the passenger MP, leaving the saved credit available");
        }
        path.addStep(MoveStepType.FORWARDS);
        assertEquals(1, path.getLastStep().getMp());
        path.addStep(MoveStepType.BACKWARDS);
        assertEquals(4, path.getLastStep().getMp());
        path.addStep(MoveStepType.FORWARDS);
        assertEquals(4, path.getLastStep().getMp(), "returning to the initial pose must not deduct the same credit twice");
        assertEquals(9, path.getMpUsed());
        assertTrue(path.isMoveLegal(), "the last step commits the remaining three quarter points");
        path.addStep(MoveStepType.LATERAL_RIGHT);
        assertFalse(path.isMoveLegal(), "no movement can follow a partial commitment in the same turn");
        path.clipToPossible();
        assertEquals(loadFirst ? 4 : 3, path.length());
        assertEquals(destination, path.getFinalCoords());
        assertEquals(9, path.getMpUsed());
        new MovePathHandler(f.manager(), f.mobile(), path, Map.of()).processMovement();
        assertEquals(ORIGIN, f.mobile().getPosition());
        assertEquals(8, f.mobile().mpUsed);
        assertEquals(destination, f.mobile().getMovementProgress().destination());
        assertEquals(3, f.mobile().getMovementProgress().quarters());
        assertTrue(f.mobile().isDone());
        if (loadFirst) {
            assertEquals(f.mobile().getId(), passenger.getTransportId());
        }
    }

    private BipedMek waitingPassenger(Fixture f) {
        f.mobile().addTransporter(new MekBay(1, 1, 0));
        var passenger = new BipedMek();
        passenger.setId(2);
        passenger.setOwner(f.mobile().getOwner());
        passenger.setWeight(50);
        passenger.setOriginalWalkMP(4);
        passenger.setDeployed(true);
        for (int loc = 0; loc < passenger.locations(); loc++) {
            passenger.initializeInternal(30, loc);
            passenger.initializeArmor(100, loc);
        }
        f.manager().getGame().addEntity(passenger);
        passenger.setPosition(ORIGIN.translated(3));
        return passenger;
    }

    @Test void unloadingDoesNotConsumeTheMobileStructuresMovement() {
        var f = fixture(EntityMovementMode.TRACKED, 1);
        var passenger = waitingPassenger(f);
        f.mobile().load(passenger, false, 0);
        passenger.setTransportId(f.mobile().getId());
        passenger.setPosition(null);
        passenger.setLoadedThisTurn(false);
        Coords exit = ORIGIN.translated(3);
        var path = new MovePath(f.manager().getGame(), f.mobile()).addStep(MoveStepType.UNLOAD, passenger, exit);
        assertTrue(path.isMoveLegal());
        assertEquals(0, path.getMpUsed());
        path.addStep(MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal(), "TO:AUE p.38 explicitly permits movement in the turn units dismount");
        assertEquals(4, path.getMpUsed());
        new MovePathHandler(f.manager(), f.mobile(), path, Map.of()).processMovement();
        assertEquals(ORIGIN.translated(0), f.mobile().getPosition());
        assertEquals(4, f.mobile().mpUsed);
        assertEquals(Entity.NONE, passenger.getTransportId());
        assertEquals(exit, passenger.getPosition());
        assertEquals(2, passenger.mpUsed, "the passenger pays half its Walking MP");
    }

    @Test void aNativeMekBayBlkCanBePlottedAndMoved() throws Exception {
        try (var stream = new FileInputStream("testresources/megamek/common/units/MekBayMobileStructure.blk")) {
            var mobile = (MobileStructure) new BLKStructureFile(new BuildingBlock(stream)).getEntity();
            var f = fixture(mobile);
            Coords destination = ORIGIN.translated(5);
            var path = plot(f, destination);
            new MovePathHandler(f.manager(), mobile, path, Map.of()).processMovement();
            assertEquals(destination, mobile.getPosition());
            assertEquals(4, mobile.mpUsed);
            assertTrue(mobile.isDone());
        }
    }

    @Test void plottingUsesTheWholeFootprintAndAllowsThePrintedOneLevelGroundChange() {
        var f = fixture(EntityMovementMode.TRACKED, 1);
        Coords destination = ORIGIN.translated(0);
        Coords leadingHex = destination.translated(0);
        f.manager().getGame().getBoard().getHex(leadingHex).setLevel(1);
        var path = plot(f, destination);
        new MovePathHandler(f.manager(), f.mobile(), path, Map.of()).processMovement();
        assertEquals(destination, f.mobile().getPosition());
    }

    @ParameterizedTest
    @EnumSource(value = EntityMovementMode.class, names = { "TRACKED", "NAVAL", "SUBMARINE", "VTOL" })
    void anImpassableLeadingHexRemainsIllegalForPlottingAndExecution(EntityMovementMode mode) {
        var f = fixture(mode, 1);
        Coords destination = ORIGIN.translated(0);
        f.manager().getGame().getBoard().getHex(destination.translated(0))
              .addTerrain(new Terrain(Terrains.IMPASSABLE, 1));
        var envelope = ShortestPathFinder.newInstanceOfAStar(destination, MoveStepType.FORWARDS,
              f.manager().getGame(), 0);
        envelope.run(new MovePath(f.manager().getGame(), f.mobile()));
        assertNull(envelope.getComputedPath(destination));
        var forged = new MovePath(f.manager().getGame(), f.mobile()).addStep(MoveStepType.FORWARDS);
        assertFalse(forged.isMoveLegal());
        new MovePathHandler(f.manager(), f.mobile(), forged, Map.of()).processMovement();
        assertEquals(ORIGIN, f.mobile().getPosition());
        assertEquals(0, f.mobile().mpUsed);
    }
}
