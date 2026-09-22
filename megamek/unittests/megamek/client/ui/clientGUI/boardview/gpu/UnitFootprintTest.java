/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Stream;
import javax.swing.SwingUtilities;

import megamek.common.Configuration;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Aero;
import megamek.common.units.Dropship;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.units.UnitLocation;
import org.junit.jupiter.api.Test;

class UnitFootprintTest {
    @Test
    void sevenHexUnionIsCapturedOnceAtTheHighestOccupiedHexAndTracksLiftOff() throws Exception {
        try (var fixture = GpuBoardFixture.create()) {
            var ship = (Dropship) new MekFileParser(new File(Configuration.dataDir(), "mekfiles/unit_files.zip"),
                  "dropships/TRO3057R/IS/Union (2708) (Cargo).blk").getEntity();
            SwingUtilities.invokeAndWait(() -> {
                ship.setId(110);
                ship.setOwner(fixture.player);
                ship.setDeployed(true);
                fixture.game.addEntity(ship, false);
                ship.land();
                ship.setPosition(new Coords(4, 4));
                ship.setElevation(0);
                ship.getOccupiedCoords().forEach(coords -> fixture.game.getBoard().setHex(coords, new Hex(0)));
                fixture.game.getBoard().setHex(ship.getPosition().translated(1), new Hex(4));
                fixture.source.refresh();
            });
            var ships = fixture.source.takeFrame().scene().units().stream().filter(unit -> unit.id() == ship.getId()).toList();
            assertEquals(1, ships.size());
            var captured = ships.getFirst();
            assertEquals(-1, captured.part());
            assertEquals(7, captured.footprint().size());
            assertEquals(ship.getOccupiedCoords(), Set.copyOf(captured.footprint()));
            assertEquals(4, captured.location().elevation());
            assertEquals(BoardScene.AeroState.LANDED, captured.location().aeroState());
            assertEquals("units/modular/families/spheroid.json", captured.model().asset());
            SwingUtilities.invokeAndWait(() -> {
                ship.liftOff(6);
                fixture.source.refresh();
            });
            var flying = fixture.source.takeFrame().scene().units().stream().filter(unit -> unit.id() == ship.getId()).findFirst().orElseThrow();
            assertTrue(flying.airborne());
            assertEquals(BoardScene.AeroState.AIRBORNE, flying.location().aeroState());
            assertEquals(6, flying.location().elevation());
            assertEquals(ship.getOccupiedCoords(), Set.copyOf(flying.footprint()));
            assertEquals(7, captured.footprint().size(), "The old snapshot must remain immutable");
            SwingUtilities.invokeAndWait(() -> {
                ship.land();
                ship.setPosition(new Coords(4, 4));
                fixture.source.refresh();
            });
            var landed = fixture.source.takeFrame().scene().units().stream().filter(unit -> unit.id() == ship.getId()).findFirst().orElseThrow();
            assertEquals(captured.footprint(), landed.footprint());
            assertEquals(4, landed.location().elevation());
            assertEquals(BoardScene.AeroState.LANDED, landed.location().aeroState());
            SwingUtilities.invokeAndWait(() -> {
                ship.setElevation(1);
                fixture.source.refresh();
            });
            var elevated = fixture.source.takeFrame().scene().units().stream().filter(unit -> unit.id() == ship.getId()).findFirst().orElseThrow();
            assertEquals(BoardScene.AeroState.ELEVATED, elevated.location().aeroState(),
                  "Ground support height must not erase a ship's positive relative elevation");
        }
    }

    @Test
    void unionLandingAndTakeoffPathsMeetTheHighestSupportInsteadOfTheLowerCentreHex() throws Exception {
        try (var fixture = GpuBoardFixture.create()) {
            var file = new File(Configuration.dataDir(), "mekfiles/unit_files.zip");
            String entry = "dropships/TRO3057R/IS/Union (2708) (Cargo).blk";
            var ship = (Dropship) new MekFileParser(file, entry).getEntity();
            var before = (Dropship) new MekFileParser(file, entry).getEntity();
            var landing = new Coords(4, 4);
            var approach = new Coords(3, 4);
            before.setId(111);
            before.setPosition(approach);
            before.setAltitude(6);
            SwingUtilities.invokeAndWait(() -> {
                ship.setId(111);
                ship.setOwner(fixture.player);
                ship.setDeployed(true);
                fixture.game.addEntity(ship, false);
                ship.land();
                ship.setPosition(landing);
                ship.getOccupiedCoords().forEach(coords -> fixture.game.getBoard().setHex(coords, new Hex(0)));
                fixture.game.getBoard().setHex(landing.translated(1), new Hex(1));
                fixture.source.refresh();
                var path = new Vector<UnitLocation>(List.of(
                      new UnitLocation(111, approach, 0, Aero.AERO_EFFECTIVE_ELEVATION, 0),
                      new UnitLocation(111, landing, 0, 0, 0)));
                fixture.game.fireGameEvent(new GameEntityChangeEvent(fixture.game, ship, path, before));
            });
            SwingUtilities.invokeAndWait(() -> { });
            var frame = fixture.source.takeFrame();
            var path = frame.movements().getFirst().path();
            assertEquals(1, path.getLast().elevation(), "Landing approach meets the whole hull's support height");
            var motion = new UnitMotion(path.getFirst());
            motion.append(path, EntityMovementType.MOVE_SAFE_THRUST, 0);
            motion.advance(motion.remainingSeconds() * .99, 1);
            assertTrue(motion.position().z >= BoardGeometry.LEVEL, "The approach must not dip through the raised hex");
            SwingUtilities.invokeAndWait(() -> {
                before.land();
                before.setPosition(landing);
                ship.liftOff(6);
                ship.setPosition(approach);
                var takeoff = new Vector<UnitLocation>(List.of(
                      new UnitLocation(111, landing, 0, 0, 0),
                      new UnitLocation(111, approach, 0, Aero.AERO_EFFECTIVE_ELEVATION, 0)));
                fixture.game.fireGameEvent(new GameEntityChangeEvent(fixture.game, ship, takeoff, before));
            });
            SwingUtilities.invokeAndWait(() -> { });
            path = fixture.source.takeFrame().movements().getFirst().path();
            assertEquals(1, path.getFirst().elevation(), "Takeoff starts from the previous supported hull");
            assertEquals(7, path.getFirst().footprint().size(), "Retraction retains the grounded fitting footprint");
        }
    }

    @Test
    void footprintLayoutCoversAllSevenHexesInEitherUnitOrientation() {
        var origin = new Coords(5, 5);
        List<Coords> occupied = new ArrayList<>(List.of(origin));
        for (int facing = 0; facing < 6; facing++) {
            occupied.add(origin.translated(facing));
        }
        for (int facing = 0; facing < 6; facing++) {
            var layout = UnitFootprint.layout(origin, occupied, facing * 60);
            assertTrue(layout.width() > BoardGeometry.WIDTH * 2.4f);
            assertTrue(layout.depth() > BoardGeometry.HEIGHT * 2.8f);
            assertEquals(0, layout.offsetX(), .001f);
            assertEquals(0, layout.offsetY(), .001f);
        }
    }

    @Test
    void elongatedFootprintKeepsItsCenterAndProportionsAtTheMapEdgeWhenTuningChanges() {
        var original = BoardGeometry.tuning();
        var origin = new Coords(0, 0);
        var occupied = List.of(new Coords(0, -1), origin, new Coords(0, 1), new Coords(0, 2));
        try {
            var base = UnitFootprint.layout(origin, occupied, 0);
            assertEquals(BoardGeometry.WIDTH, base.width(), .001f);
            assertEquals(BoardGeometry.HEIGHT * 4, base.depth(), .001f);
            assertEquals(-BoardGeometry.HEIGHT / 2, base.offsetY(), .001f);
            var turned = UnitFootprint.layout(origin, occupied, 90);
            assertEquals(base.depth(), turned.width(), .001f);
            assertEquals(base.width(), turned.depth(), .001f);
            assertEquals(base.offsetY(), turned.offsetY(), .001f);
            BoardGeometry.tune(new BoardGeometry.Tuning(original.hexScale() * 1.5f, original.unitScale(),
                  original.unitHeightScale(), original.levelHeight(), original.gridShade(), original.multiHexUnitScale()));
            var scaled = UnitFootprint.layout(origin, occupied, 0);
            assertEquals(base.width() * 1.5f, scaled.width(), .001f);
            assertEquals(base.depth() * 1.5f, scaled.depth(), .001f);
            assertEquals(base.offsetY() * 1.5f, scaled.offsetY(), .001f);
        } finally {
            BoardGeometry.tune(original);
        }
    }

    @Test
    void relativeElevationIsNotAddedAgainToTheHighestSupport() {
        var board = new Board(3, 3, Stream.generate(Hex::new).limit(9).toArray(Hex[]::new));
        var origin = new Coords(1, 1);
        var neighbor = origin.translated(1);
        board.getHex(origin).setLevel(2);
        board.getHex(neighbor).setLevel(5);
        assertEquals(5, UnitFootprint.support(board, origin, List.of(origin, neighbor), 3));
        board.getHex(origin).addTerrain(new Terrain(Terrains.BRIDGE_ELEV, 3));
        board.getHex(neighbor).addTerrain(new Terrain(Terrains.BRIDGE_ELEV, 2));
        assertEquals(7, UnitFootprint.support(board, origin, List.of(origin, neighbor), 3));
    }

    @Test
    void movingFootprintClearsAnIntermediateHillButNotAnUnoccupiedNeighbor() {
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), x == 3 && y == 3 ? 4 : x == 7 ? 12 : 0,
                      -1, false, 0, BoardScene.Surface.GRASS, null, null, null, List.of(), List.of()));
            }
        }
        var destination = new Coords(5, 5);
        var footprint = new ArrayList<>(List.of(destination));
        for (int facing = 0; facing < 6; facing++) {
            footprint.add(destination.translated(facing));
        }
        var unit = new BoardScene.Unit(1, -1, "Large unit", new BoardScene.Waypoint(destination, 0, 0), null,
              false, null, 4, false, null, 0, footprint);
        var scene = new BoardScene(0, 8, 8, tiles, List.of(unit), List.of(), -1, "", List.of());
        var position = BoardGeometry.center(new Coords(3, 3), 0);
        UnitFootprint.clearTerrain(scene, unit, position, 60);
        assertEquals(4 * BoardGeometry.LEVEL, position.z);
        position.set(BoardGeometry.center(new Coords(1, 1), 0));
        UnitFootprint.clearTerrain(scene, unit, position, 0);
        assertEquals(0, position.z, "The distant 12-level column must not lift the hull");
    }
}
