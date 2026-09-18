/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import megamek.common.board.Coords;
import megamek.common.units.EntityMovementType;
import org.junit.jupiter.api.Test;

class BoardSurfaceTest {
    private static final Coords FIRST = new Coords(1, 1);
    private static final Coords SECOND = FIRST.translated(1);

    @Test
    void connectedRoadsMeetWithoutAnExposedStepAndPickingFollowsTheCut() {
        BoardScene scene = scene(false);
        BoardSurface high = new BoardSurface(scene, scene.tile(FIRST));
        BoardSurface low = new BoardSurface(scene, scene.tile(SECOND));
        Vector3 gate = BoardGeometry.corner(FIRST, 0, 0).lerp(BoardGeometry.corner(FIRST, 0, 1), 0.5f);
        assertEquals(BoardGeometry.LEVEL, high.height(gate.x, gate.y), 0.01f);
        assertEquals(high.height(gate.x, gate.y), low.height(gate.x, gate.y), 0.01f);
        Vector3 highCenter = BoardGeometry.center(FIRST, 2);
        Vector3 lowCenter = BoardGeometry.center(SECOND, 0);
        Vector3 cut = new Vector3(gate).lerp(highCenter, 0.15f);
        Vector3 ramp = new Vector3(gate).lerp(lowCenter, 0.15f);
        assertTrue(high.height(cut.x, cut.y) < highCenter.z);
        assertTrue(low.height(ramp.x, ramp.y) > lowCenter.z);
        assertEquals(FIRST, BoardGeometry.pick(scene, new Ray(new Vector3(cut.x, cut.y, 200), new Vector3(0, 0, -1))));
        assertEquals(SECOND, BoardGeometry.pick(scene, new Ray(new Vector3(ramp.x, ramp.y, 200), new Vector3(0, 0, -1))));
        UnitMotion movement = new UnitMotion(new BoardScene.Waypoint(FIRST, 2, 1));
        movement.append(List.of(new BoardScene.Waypoint(FIRST, 2, 1), new BoardScene.Waypoint(SECOND, 0, 1)),
              EntityMovementType.MOVE_WALK, 0);
        movement.advance(UnitMotion.WALK_SECONDS * 0.4, 1);
        Vector3 moving = movement.surfacePosition(scene);
        assertEquals(high.height(moving.x, moving.y), moving.z, 0.01f, "Walking must not sink into the upper road shoulder");
    }

    @Test
    void aRoadEndingAtTheEdgeStillCutsAndFillsBothApproachesInEveryDirection() {
        for (int direction = 0; direction < 6; direction++) {
            for (boolean roadOnHighSide : List.of(true, false)) {
                BoardScene scene = scene(false, direction, roadOnHighSide, !roadOnHighSide);
                Coords neighbor = FIRST.translated(direction);
                BoardSurface high = new BoardSurface(scene, scene.tile(FIRST));
                BoardSurface low = new BoardSurface(scene, scene.tile(neighbor));
                Vector3 highCenter = BoardGeometry.center(FIRST, 2);
                Vector3 lowCenter = BoardGeometry.center(neighbor, 0);
                Vector3 gate = new Vector3(highCenter).lerp(lowCenter, 0.5f);
                assertEquals(BoardGeometry.LEVEL, high.height(gate.x, gate.y), 0.01f);
                assertEquals(high.height(gate.x, gate.y), low.height(gate.x, gate.y), 0.01f);
                Vector3 cut = new Vector3(gate).lerp(highCenter, 0.15f);
                Vector3 fill = new Vector3(gate).lerp(lowCenter, 0.15f);
                assertTrue(high.height(cut.x, cut.y) < highCenter.z);
                assertTrue(low.height(fill.x, fill.y) > lowCenter.z);
                UnitMotion movement = new UnitMotion(new BoardScene.Waypoint(FIRST, 2, direction));
                movement.append(List.of(new BoardScene.Waypoint(FIRST, 2, direction),
                      new BoardScene.Waypoint(neighbor, 0, direction)), EntityMovementType.MOVE_WALK, 0);
                movement.advance(UnitMotion.WALK_SECONDS * 0.4, 1);
                Vector3 moving = movement.surfacePosition(scene);
                assertEquals(high.height(moving.x, moving.y), moving.z, 0.01f);
                movement.advance(UnitMotion.WALK_SECONDS * 0.2, 1);
                moving = movement.surfacePosition(scene);
                assertEquals(low.height(moving.x, moving.y), moving.z, 0.01f);
            }
        }
        BoardScene scene = scene(false, 1, false, false);
        BoardSurface high = new BoardSurface(scene, scene.tile(FIRST));
        assertEquals(0, high.ramps, "Ground without an edge-reaching road retains the cliff");
        assertEquals(6, high.faces.size(), "Ordinary flat hexes do not need road subdivisions");
    }

    @Test
    void riverBanksOccupyOnlyTheLandEdgesAndNeighboringWaterMouthsMatch() {
        BoardScene scene = scene(true);
        BoardSurface first = new BoardSurface(scene, scene.tile(FIRST));
        BoardSurface second = new BoardSurface(scene, scene.tile(SECOND));
        Vector3 center = BoardGeometry.center(FIRST, 0);
        assertEquals(-2 * BoardGeometry.LEVEL, first.height(center.x, center.y), 0.01f);
        Vector3 bank = BoardGeometry.corner(FIRST, 0, 3).lerp(BoardGeometry.corner(FIRST, 0, 4), 0.5f);
        assertEquals(0, first.height(bank.x, bank.y), 0.01f);
        Vector3 a = BoardGeometry.corner(FIRST, 0, 0), b = BoardGeometry.corner(FIRST, 0, 1);
        List<Vector3> mouth = first.water.stream().filter(p -> onEdge(p, a, b)).toList();
        List<Vector3> neighbor = second.water.stream().filter(p -> onEdge(p, a, b)).toList();
        assertTrue(mouth.size() >= 2);
        assertEquals(mouth.size(), neighbor.size());
        assertTrue(mouth.stream().allMatch(p -> neighbor.stream().anyMatch(n -> n.epsilonEquals(p, 0.01f))));
        assertTrue(first.water.stream().anyMatch(p -> p.dst2(BoardGeometry.corner(FIRST, 0, 3)) > 1));
    }

    private static boolean onEdge(Vector3 p, Vector3 a, Vector3 b) {
        return Math.abs((b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)) < 0.01f;
    }

    @Test
    void straightRiversKeepTheirMouthWidthThroughTheHexInsteadOfMakingAPool() {
        for (int direction = 0; direction < 6; direction++) {
            Coords center = new Coords(2, 2);
            BoardScene scene = riverScene(Map.of(center, 0, center.translated(direction), 0,
                  center.translated((direction + 3) % 6), 0), false);
            BoardSurface surface = new BoardSurface(scene, scene.tile(center));
            Vector3 along = BoardGeometry.center(center.translated(direction), 0).sub(BoardGeometry.center(center, 0)).nor();
            Vector3 across = new Vector3(along).crs(Vector3.Z);
            float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
            for (Vector3 point : surface.water) {
                float offset = new Vector3(point).sub(BoardGeometry.center(center, 0)).dot(across);
                min = Math.min(min, offset);
                max = Math.max(max, offset);
            }
            assertTrue(max - min < 26 * BoardGeometry.HEX_SCALE, "The centre must stay as narrow as the river mouths");
            assertEquals(-BoardGeometry.LEVEL, surface.height(BoardGeometry.centerX(center), BoardGeometry.centerY(center)), 0.01f);
        }
    }

    @Test
    void bentChannelsTriangulateTheirActualOutlineAndMeetTheNeighboringMouths() {
        Coords center = new Coords(2, 2);
        for (int from = 0; from < 6; from++) {
            for (int separation : new int[] { 2, 3, 4 }) {
                int to = (from + separation) % 6;
                BoardScene scene = riverScene(Map.of(center, 0, center.translated(from), 0,
                      center.translated(to), 0), false);
                BoardSurface surface = new BoardSurface(scene, scene.tile(center));
                assertEquals(-BoardGeometry.LEVEL, surface.height(BoardGeometry.centerX(center), BoardGeometry.centerY(center)), 0.01f,
                      "A unit at the centre of a bend must stand on its full-depth bed");
                float polygonArea = 0, trianglesArea = 0;
                for (int i = 0; i < surface.water.size(); i++) {
                    Vector3 a = surface.water.get(i), b = surface.water.get((i + 1) % surface.water.size());
                    polygonArea += a.x * b.y - a.y * b.x;
                }
                for (BoardSurface.Face face : surface.waterFaces) {
                    float area = new Vector3(face.b()).sub(face.a()).crs(new Vector3(face.c()).sub(face.a())).z;
                    assertTrue(area >= -0.01f, "No inverted water triangles");
                    trianglesArea += area;
                }
                assertEquals(polygonArea, trianglesArea, 0.1f, "Water must not fan across the curved banks");
                for (int direction : new int[] { from, to }) {
                    BoardSurface neighbor = new BoardSurface(scene, scene.tile(center.translated(direction)));
                    int edge = Math.floorMod(1 - direction, 6);
                    Vector3 a = BoardGeometry.corner(center, 0, edge), b = BoardGeometry.corner(center, 0, edge + 1);
                    var mouth = surface.water.stream().filter(p -> onEdge(p, a, b)).toList();
                    assertTrue(mouth.size() >= 2);
                    assertTrue(mouth.stream().allMatch(p -> neighbor.water.stream().anyMatch(n -> n.epsilonEquals(p, 0.01f))));
                }
            }
        }
    }

    @Test
    void waterfallsJoinTheUpperAndLowerWaterSurfacesOnlyOnceAndArePickable() {
        Coords high = new Coords(2, 2);
        for (int direction = 0; direction < 6; direction++) {
            Coords low = high.translated(direction);
            BoardScene scene = riverScene(Map.of(high, 3, low, 0), false);
            BoardSurface upper = new BoardSurface(scene, scene.tile(high));
            BoardSurface lower = new BoardSurface(scene, scene.tile(low));
            assertEquals(1, upper.waterfalls.size());
            assertTrue(lower.waterfalls.isEmpty());
            BoardSurface.Side fall = upper.waterfalls.getFirst();
            assertEquals(BoardGeometry.waterZ(scene.tile(high)), fall.a().z, 0.001f);
            assertEquals(BoardGeometry.waterZ(scene.tile(low)), fall.lowA(), 0.001f);
            Vector3 point = new Vector3(fall.a()).lerp(fall.b(), 0.5f).add(0, 0, -2);
            Vector3 outward = new Vector3(fall.b()).sub(fall.a()).crs(Vector3.Z).nor();
            assertEquals(high, BoardGeometry.pick(scene, new Ray(new Vector3(point).mulAdd(outward, 6), outward.scl(-1))));
        }
        BoardScene frozen = riverScene(Map.of(high, 3, high.translated(3), 0), true);
        assertTrue(new BoardSurface(frozen, frozen.tile(high)).waterfalls.isEmpty());
    }

    private static BoardScene riverScene(Map<Coords, Integer> levels, boolean frozen) {
        var art = new BoardScene.Pixels(new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB));
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                Coords coords = new Coords(x, y);
                tiles.add(new BoardScene.Tile(coords, levels.getOrDefault(coords, 0), levels.containsKey(coords) ? 1 : -1,
                      frozen && levels.containsKey(coords), 0, BoardScene.Surface.GRASS, art, null, null, List.of(), List.of()));
            }
        }
        return new BoardScene(0, 5, 5, tiles, List.of(), List.of(), -1, "", List.of());
    }

    private static BoardScene scene(boolean river) {
        return scene(river, 1, true, true);
    }

    private static BoardScene scene(boolean river, int direction, boolean firstRoad, boolean secondRoad) {
        var art = new BoardScene.Pixels(new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB));
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                Coords coords = new Coords(x, y);
                boolean first = coords.equals(FIRST), second = coords.equals(FIRST.translated(direction));
                tiles.add(new BoardScene.Tile(coords, !river && first ? 2 : 0,
                      river && (first || second) ? 2 : -1, false,
                      river ? 0 : first && firstRoad ? 1 << direction
                            : second && secondRoad ? 1 << ((direction + 3) % 6) : 0, BoardScene.Surface.GRASS,
                      art, null, null, List.of(), List.of()));
            }
        }
        return new BoardScene(0, 4, 4, tiles, List.of(), List.of(), -1, "", List.of());
    }
}
