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

import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.moves.MobileStructureAirMovement;
import megamek.common.moves.MobileStructureMovement;
import megamek.common.moves.MovePath;
import megamek.common.net.packets.Packet;
import megamek.common.units.*;
import megamek.common.util.SerializationHelper;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Movement and publication across physical wall segments and bridge decks, TO:AUE pp.33-36; TO:AR p.115. */
class MobileStructureWallBridgeTest {
    private static final Coords ORIGIN = new Coords(8, 8);
    private static final CubeCoords EAST = new CubeCoords(1, 0, -1);
    private static final List<CubeCoords> SPAN = List.of(new CubeCoords(-1, 0, 1), CubeCoords.ZERO, EAST);

    private static class LocalManager extends TWGameManager {
        @Override public void send(Packet packet) { }
        @Override public void send(int id, Packet packet) { }
    }

    @BeforeAll static void equipment() { EquipmentType.initializeTypes(); }

    private TWGameManager manager(EntityMovementMode mode, int elevation, List<CubeCoords> footprint, int ground) {
        var manager = spy(new LocalManager());
        manager.getGame().getOptions().initialize();
        doNothing().when(manager).entityUpdate(anyInt());
        doNothing().when(manager).sendChangedHex(any(Coords.class), anyInt());
        doNothing().when(manager).sendChangedBuildings(any());
        var game = manager.getGame();
        game.setBoard(BoardLoader.initializeBoard("size 20 20\nend\n"));
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                var hex = game.getBoard().getHex(new Coords(x, y));
                hex.setLevel(ground);
                if (mode.isSubmarine() || mode == EntityMovementMode.NAVAL) {
                    hex.addTerrain(new Terrain(Terrains.WATER, 20));
                }
            }
        }
        game.addPlayer(0, new Player(0, "Owner"));
        var mobile = new MobileStructure(BuildingType.HARDENED, IBuilding.FORTRESS);
        mobile.configureConstruction(BuildingType.HARDENED, IBuilding.FORTRESS, 4, 150, 0, footprint);
        mobile.setId(1);
        mobile.setOwner(game.getPlayer(0));
        mobile.setChassis("Bridge and wall collision");
        mobile.setMovementMode(mode);
        mobile.setMaximumMP(mode == EntityMovementMode.TRACKED ? 2 : 1);
        mobile.setPosition(ORIGIN);
        mobile.setElevation(elevation);
        game.addEntity(mobile);
        mobile.setDeployed(true);
        mobile.setDone(false);
        mobile.updateBuildingEntityHexes(0, manager);
        game.setPhase(GamePhase.MOVEMENT);
        return manager;
    }

    private MobileStructure mobile(TWGameManager manager) { return (MobileStructure) manager.getGame().getEntity(1); }

    private BuildingEntity structure(TWGameManager manager, int classification, int height, int deck, Coords position) {
        var structure = new BuildingEntity(BuildingType.HARDENED, classification);
        structure.configureConstruction(BuildingType.HARDENED, classification, height, 150, 0,
              classification == IBuilding.BRIDGE ? SPAN : List.of(CubeCoords.ZERO));
        structure.setId(2);
        structure.setChassis("Physical obstacle");
        structure.setOwner(manager.getGame().getPlayer(0));
        structure.setPosition(position);
        if (classification == IBuilding.BRIDGE) {
            SPAN.forEach(relative -> structure.getDesign().getBridgeDecks().put(relative, deck));
        } else {
            structure.getDesign().getWallSides().put(CubeCoords.ZERO, 1);
        }
        manager.getGame().addEntity(structure);
        structure.setDeployed(true);
        structure.updateBuildingEntityHexes(0, manager);
        return structure;
    }

    private BuildingTerrain mapBridge(TWGameManager manager, int deck, Coords position) {
        var board = manager.getGame().getBoard();
        List<Coords> cells = SPAN.stream().map(relative -> position.toCube().add(relative).toOffset()).toList();
        for (Coords coords : cells) {
            int exits = 0;
            for (int side = 0; side < 6; side++) {
                if (cells.contains(coords.translated(side))) { exits |= 1 << side; }
            }
            var hex = board.getHex(coords);
            hex.addTerrain(new Terrain(Terrains.BRIDGE, 2, true, exits));
            hex.addTerrain(new Terrain(Terrains.BRIDGE_CF, 40));
            hex.addTerrain(new Terrain(Terrains.BRIDGE_ELEV, deck));
        }
        var bridge = new BuildingTerrain(position, board, Terrains.BRIDGE, BasementType.NONE);
        bridge.setBoardId(0);
        board.addBuildingToBoard(bridge);
        assertEquals(3, bridge.getCoordsList().size());
        return bridge;
    }

    @ParameterizedTest @ValueSource(ints = { -6, 0 })
    void aSubmergedMobileDestroysOnlyAWallThatActuallyMeetsItsHull(int elevation) {
        var manager = manager(EntityMovementMode.SUBMARINE, elevation, List.of(CubeCoords.ZERO), 0);
        var mobile = mobile(manager);
        Coords destination = ORIGIN.translated(0);
        var wall = structure(manager, IBuilding.WALL, 3, 0, destination);
        var segment = WallRules.at(manager.getGame(), 0, destination, 0).getFirst();
        assertEquals(4, MobileStructureMovement.cost(manager.getGame(), mobile, ORIGIN, 0, elevation,
              destination, 0, elevation));
        new MobileStructureMovementHandler(manager).process(mobile,
              new MovePath(manager.getGame(), mobile).addStep(MoveStepType.FORWARDS));
        assertEquals(destination, mobile.getPosition());
        assertEquals(elevation < 0 ? 150 : 0, segment.cf());
        assertEquals(150, mobile.getCurrentCF(destination), "wall segments do not also act as solid building hexes");
        assertEquals(150, wall.getInternalBuilding().getCurrentCF(CubeCoords.ZERO));
    }

    @ParameterizedTest @ValueSource(ints = { 2, 4 })
    void spanningTracksPayForAndDestroyOnlyWallsAboveTheirActualSupport(int wallHeight) {
        var manager = manager(EntityMovementMode.TRACKED, 0, SPAN, 3);
        var mobile = mobile(manager);
        Coords destination = ORIGIN.translated(0);
        manager.getGame().getBoard().getHex(destination).setLevel(0);
        structure(manager, IBuilding.WALL, wallHeight, 0, destination);
        var segment = WallRules.at(manager.getGame(), 0, destination, 0).getFirst();
        assertEquals(wallHeight > 3 ? 8 : 4, MobileStructureMovement.cost(manager.getGame(), mobile, ORIGIN, 0, 0,
              destination, 0, 0));
        new MobileStructureMovementHandler(manager).process(mobile,
              new MovePath(manager.getGame(), mobile).addStep(MoveStepType.FORWARDS));
        assertEquals(destination, mobile.getPosition());
        assertEquals(5, mobile.getBaseElevation(destination));
        assertEquals(wallHeight > 3 ? 0 : 150, segment.cf());
        assertEquals(150, mobile.getCurrentCF(destination));
    }

    @Test void surfaceVesselMovesUnderALongerBridgeAndBothSurviveRepublicationAndDeparture() {
        var manager = manager(EntityMovementMode.NAVAL, 0, List.of(CubeCoords.ZERO), 0);
        var mobile = mobile(manager);
        Coords destination = ORIGIN.translated(0);
        var bridge = structure(manager, IBuilding.BRIDGE, 1, 5, destination);
        var handler = new MobileStructureMovementHandler(manager);
        handler.process(mobile, new MovePath(manager.getGame(), mobile).addStep(MoveStepType.FORWARDS));
        assertEquals(destination, mobile.getPosition());
        assertEquals(150, bridge.getCurrentCF(destination));
        assertEquals(150, mobile.getCurrentCF(destination));
        assertTrue(BuildingElevation.canCoexist(mobile, bridge));
        assertTrue(BuildingElevation.canCoexist(bridge, mobile));
        assertFalse(mobile.getCoordsList().containsAll(bridge.getCoordsList()));
        var board = manager.getGame().getBoard();
        assertTrue(board.getBuildingsAt(destination).containsAll(List.of(bridge, mobile)));
        assertEquals(5, board.getHex(destination).terrainLevel(Terrains.BRIDGE_ELEV));
        assertEquals(2, board.getHex(destination).terrainLevel(Terrains.BLDG_ELEV));
        board.getHex(destination).addTerrain(new Terrain(Terrains.BRIDGE_REPAIRED, 1));
        bridge.updateBuildingEntityHexes(0, manager);
        assertTrue(board.getBuildingsAt(destination).containsAll(List.of(bridge, mobile)));
        assertEquals(2, board.getHex(destination).terrainLevel(Terrains.BLDG_ELEV));
        handler.relocate(mobile, destination.translated(0), 0, 0);
        assertEquals(List.of(bridge), board.getBuildingsAt(destination));
        assertEquals(5, board.getHex(destination).terrainLevel(Terrains.BRIDGE_ELEV));
        assertFalse(board.getHex(destination).containsTerrain(Terrains.BUILDING));
        assertTrue(board.getHex(destination).containsTerrain(Terrains.BRIDGE_REPAIRED),
              "a departing mobile must retain the surviving bridge's repair badge");
        board.removeBuilding(bridge);
        assertFalse(board.getHex(destination).containsTerrain(Terrains.BRIDGE_REPAIRED),
              "clean removal of the last bridge also removes its repair badge");
    }

    @Test void aSurfaceVesselActuallyContactingTheDeckDemolishesOnlyThatBridgeHex() {
        var manager = manager(EntityMovementMode.NAVAL, 0, List.of(CubeCoords.ZERO), 0);
        var mobile = mobile(manager);
        Coords destination = ORIGIN.translated(0);
        var bridge = structure(manager, IBuilding.BRIDGE, 1, 1, destination);
        new MobileStructureMovementHandler(manager).process(mobile,
              new MovePath(manager.getGame(), mobile).addStep(MoveStepType.FORWARDS));
        assertEquals(destination, mobile.getPosition());
        assertFalse(bridge.hasCFIn(destination));
        assertEquals(2, bridge.getCoordsList().size(), "each bank still supports its own remaining hex");
        assertTrue(mobile.getCurrentCF(destination) < 150);
    }

    @Test void surfacingIntoABridgeIsContactEvenWithoutAHorizontallyNewHex() {
        var manager = manager(EntityMovementMode.SUBMARINE, -1, List.of(CubeCoords.ZERO), 0);
        var mobile = mobile(manager);
        var bridge = structure(manager, IBuilding.BRIDGE, 1, 1, ORIGIN);
        manager.getGame().getBoard().getHex(ORIGIN).addTerrain(new Terrain(Terrains.BRIDGE_REPAIRED, 1));
        assertTrue(BuildingElevation.canCoexist(mobile, bridge));
        new MobileStructureMovementHandler(manager).process(mobile,
              new MovePath(manager.getGame(), mobile).addStep(MoveStepType.UP));
        assertEquals(0, mobile.getElevation());
        assertFalse(bridge.hasCFIn(ORIGIN));
        assertTrue(mobile.getCurrentCF(ORIGIN) < 150);
        assertFalse(manager.getGame().getBoard().getHex(ORIGIN).containsTerrain(Terrains.BRIDGE));
        assertFalse(manager.getGame().getBoard().getHex(ORIGIN).containsTerrain(Terrains.BRIDGE_REPAIRED),
              "the surviving mobile is not a repaired bridge");
    }

    @Test void bridgeTargetsAndRoofUseEachDeckPlaneWhileConstructionHeightStaysOne() {
        var manager = manager(EntityMovementMode.NAVAL, 0, List.of(CubeCoords.ZERO), 0);
        var bridge = structure(manager, IBuilding.BRIDGE, 1, 5, ORIGIN.translated(0));
        bridge.getDesign().getBridgeDecks().put(EAST, 6);
        bridge.setElevation(2);
        bridge.updateBuildingEntityHexes(0, manager);
        Coords east = bridge.relativeToBoard(EAST);
        assertEquals(8, BuildingElevation.base(bridge, east));
        assertEquals(8, BuildingElevation.roof(bridge, east));
        assertEquals(1, bridge.getHeight(east));
        assertFalse(BuildingElevation.contains(bridge, east, 7));
        var target = new BuildingTarget(east, manager.getGame().getBoard(), false);
        assertEquals(8, target.getElevation());
        assertEquals(0, target.getHeight());
        assertEquals(8, manager.getGame().getBoard().getHex(east).terrainLevel(Terrains.BRIDGE_ELEV));
    }

    @ParameterizedTest @ValueSource(ints = { 1, 5 })
    void nativeMapBridgeUsesItsActualDeckForMobilePassageAndCollision(int deck) {
        var manager = manager(EntityMovementMode.NAVAL, 0, List.of(CubeCoords.ZERO), 0);
        var mobile = mobile(manager);
        Coords destination = ORIGIN.translated(0);
        var bridge = mapBridge(manager, deck, destination);
        assertEquals(IBuilding.BRIDGE, bridge.getBldgClass());
        assertEquals(deck, BuildingElevation.base(bridge, destination));
        assertEquals(deck, BuildingElevation.roof(bridge, destination));
        assertEquals(1, bridge.getHeight(destination));
        assertEquals(deck > 2, BuildingElevation.canCoexist(mobile, List.of(destination), -2, bridge));
        assertEquals(deck > 2, mobile.isPositionAndFacingValid(destination, 0, 0, 0),
              "placement below a native bridge must use the same physical clearance as movement");
        var handler = new MobileStructureMovementHandler(manager);
        handler.process(mobile, new MovePath(manager.getGame(), mobile).addStep(MoveStepType.FORWARDS));
        assertEquals(destination, mobile.getPosition());
        assertEquals(deck > 2, bridge.isIn(destination));
        if (deck > 2) {
            assertEquals(40, bridge.getCurrentCF(destination));
            assertTrue(manager.getGame().getBoard().getBuildingsAt(destination).containsAll(List.of(mobile, bridge)));
            assertEquals(0, new BuildingTarget(destination, manager.getGame().getBoard(), false).getHeight());
            handler.relocate(mobile, destination.translated(0), 0, 0);
            assertEquals(deck, manager.getGame().getBoard().getHex(destination).terrainLevel(Terrains.BRIDGE_ELEV));
        } else {
            assertEquals(2, bridge.getCoordsList().size());
            assertTrue(mobile.getCurrentCF(destination) < 150);
        }
    }

    @Test void aircraftCanPassOneLevelAboveTheDeckWithoutAddingAnotherDeckHeight() {
        var manager = manager(EntityMovementMode.VTOL, 6, List.of(CubeCoords.ZERO), 0);
        var mobile = mobile(manager);
        Coords destination = ORIGIN.translated(0);
        var bridge = mapBridge(manager, 5, destination);
        assertEquals(5, MobileStructureAirMovement.obstructionCeiling(mobile, destination));
        assertEquals(4, MobileStructureMovement.cost(manager.getGame(), mobile, ORIGIN, 0, 6, destination, 0, 6));
        new MobileStructureMovementHandler(manager).process(mobile,
              new MovePath(manager.getGame(), mobile).addStep(MoveStepType.FORWARDS));
        assertEquals(destination, mobile.getPosition());
        assertEquals(40, bridge.getCurrentCF(destination));
    }

    @Test void anActualOldJavaBridgeSaveRecoversDeckMetadataBesideAMobileAndResavesToXml() throws Exception {
        var manager = manager(EntityMovementMode.NAVAL, 0, List.of(CubeCoords.ZERO), 0);
        var mobile = mobile(manager);
        Coords coords = new Coords(1, 1);
        new MobileStructureMovementHandler(manager).relocate(mobile, coords, 0, 0);
        var board = manager.getGame().getBoard();
        var hex = board.getHex(coords);
        hex.addTerrain(new Terrain(Terrains.BRIDGE, 2));
        hex.addTerrain(new Terrain(Terrains.BRIDGE_CF, 40));
        hex.addTerrain(new Terrain(Terrains.BRIDGE_ELEV, 5));
        assertTrue(hex.containsTerrain(Terrains.BUILDING));
        BuildingTerrain bridge;
        // Produced with the old compiled class, before structureType/bridgeDecks existed, not simulated null fields.
        try (var stream = new ObjectInputStream(Files.newInputStream(
              Path.of("testresources/megamek/common/units/LegacyMapBridge.ser")))) {
            bridge = (BuildingTerrain) stream.readObject();
        }
        assertEquals(0, bridge.getInternalBuilding().getBuildingHeight());
        bridge.setBoardId(0);
        board.addBuildingToBoard(bridge);
        assertEquals(IBuilding.BRIDGE, bridge.getBldgClass());
        assertEquals(5, BuildingElevation.roof(bridge, coords));
        assertEquals(40, bridge.getCurrentCF(coords));
        assertTrue(board.getBuildingsAt(coords).containsAll(List.of(bridge, mobile)));
        String saved = SerializationHelper.getSaveGameXStream().toXML(manager.getGame());
        var restored = (megamek.common.game.Game) SerializationHelper.getLoadSaveGameXStream().fromXML(saved);
        var restoredBridge = restored.getBoard().getBuildingsAt(coords).stream()
              .filter(BuildingTerrain.class::isInstance).findFirst().orElseThrow();
        assertEquals(5, BuildingElevation.roof(restoredBridge, coords));
        assertEquals(40, restoredBridge.getCurrentCF(coords));
        assertEquals(2, restored.getBoard().getBuildingsAt(coords).size());
    }

    @Test void aSinkingHullCannotSkipAStaticWallBelowItsOriginalKeel() {
        var manager = manager(EntityMovementMode.NAVAL, 0, List.of(CubeCoords.ZERO), 0);
        var mobile = mobile(manager);
        var wall = structure(manager, IBuilding.WALL, 1, 0, ORIGIN);
        wall.setElevation(-5);
        var segment = WallRules.at(manager.getGame(), 0, ORIGIN, 0).getFirst();
        assertEquals(-4, segment.topAltitude());
        assertEquals(-2, mobile.getBaseElevation(ORIGIN));
        manager.destroyEntity(mobile, "sinking past a submerged wall", true);
        new MobileStructureNavalHandler(manager).endMovement();
        assertEquals(-5, mobile.getElevation());
        assertEquals(0, segment.cf());
    }

    @Test void destructionAboveWaterDoesNotReplaceAircraftHandlingWithNavalSinking() {
        var manager = manager(EntityMovementMode.VTOL, 10, List.of(CubeCoords.ZERO), 0);
        var mobile = mobile(manager);
        manager.getGame().getBoard().getHex(ORIGIN).addTerrain(new Terrain(Terrains.WATER, 20));
        assertFalse(new MobileStructureNavalHandler(manager).mustSink(mobile));
        manager.destroyEntity(mobile, "destroyed while flying above the sea", true);
        assertFalse(mobile.getNavalState().isSinking());
    }

    @Test void destroyedTracksSupportedAboveWaterDoNotSinkThroughTheirSupport() {
        var manager = manager(EntityMovementMode.TRACKED, 0, SPAN, 3);
        var mobile = mobile(manager);
        var water = manager.getGame().getBoard().getHex(ORIGIN);
        water.setLevel(0);
        water.addTerrain(new Terrain(Terrains.WATER, 20));
        assertEquals(5, mobile.getBaseElevation(ORIGIN));
        assertFalse(new MobileStructureNavalHandler(manager).mustSink(mobile));
        manager.destroyEntity(mobile, "destroyed on supported tracks above the water", true);
        assertFalse(mobile.getNavalState().isSinking());
    }
}
