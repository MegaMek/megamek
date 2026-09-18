/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import megamek.common.board.Coords;
import megamek.common.units.EntityMovementType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
                BoardScene scene = scene(false, direction, roadOnHighSide, !roadOnHighSide, 2);
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
        BoardScene scene = scene(false, 1, false, false, 2);
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
    void largeCliffsLeaveRoadEndsFlatUnlessBothRoadExitsConnect() {
        for (int direction = 0; direction < 6; direction++) {
            Coords neighbor = FIRST.translated(direction);
            Vector3 gate = BoardGeometry.center(FIRST, 0).lerp(BoardGeometry.center(neighbor, 0), 0.5f);
            for (int elevation : new int[] { -3, 3 }) {
                for (boolean roadOnFirstSide : List.of(true, false)) {
                    BoardScene scene = scene(false, direction, roadOnFirstSide, !roadOnFirstSide, elevation);
                    BoardSurface first = new BoardSurface(scene, scene.tile(FIRST));
                    BoardSurface second = new BoardSurface(scene, scene.tile(neighbor));
                    assertEquals(elevation * BoardGeometry.LEVEL, first.height(gate.x, gate.y), 0.01f,
                          "A road end at a large cliff must retain its own elevation");
                    assertEquals(0, second.height(gate.x, gate.y), 0.01f,
                          "The adjacent hex must also retain its flat surface");
                }
                BoardScene connected = scene(false, direction, true, true, elevation);
                BoardSurface first = new BoardSurface(connected, connected.tile(FIRST));
                BoardSurface second = new BoardSurface(connected, connected.tile(neighbor));
                assertEquals(elevation * BoardGeometry.LEVEL / 2, first.height(gate.x, gate.y), 0.01f);
                assertEquals(first.height(gate.x, gate.y), second.height(gate.x, gate.y), 0.01f,
                      "An actual connecting road still meets at the shared ramp height");
            }
        }
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
            int edge = Math.floorMod(1 - direction, 6);
            Vector3 a = BoardGeometry.corner(center, 0, edge), b = BoardGeometry.corner(center, 0, edge + 1);
            float edgeWidth = a.dst(b);
            assertTrue(max - min > edgeWidth * 0.75f && max - min < edgeWidth * 0.85f,
                  "The water uses most of the shared edge while leaving room for the sand fade");
            var mouth = surface.water.stream().filter(point -> onEdge(point, a, b)).toList();
            float mouthWidth = 0;
            for (Vector3 first : mouth) {
                for (Vector3 second : mouth) {
                    mouthWidth = Math.max(mouthWidth, first.dst(second));
                }
            }
            // On 84x72 hexes, diagonal mouth normals and centre-to-centre directions differ slightly.
            assertEquals(mouthWidth, max - min, mouthWidth * 0.01f, "The channel keeps its mouth width through the hex");
            for (Vector3 corner : List.of(a, b)) {
                assertTrue(surface.faces.stream().filter(face -> face.finish() == BoardSurface.Finish.SHORE)
                      .anyMatch(face -> face.a().epsilonEquals(corner, 0.01f)
                            || face.b().epsilonEquals(corner, 0.01f) || face.c().epsilonEquals(corner, 0.01f)),
                      "The sandy shoreline starts at the shared edge's corners");
            }
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
    void waterAndBanksStayInsideTheirHexForEveryNeighborPattern() {
        Coords center = new Coords(7, 33);
        for (int mask = 0; mask < 64; mask++) {
            BoardScene scene = riverScene(center, mask);
            BoardSurface surface = new BoardSurface(scene, scene.tile(center));
            List<BoardSurface.Face> faces = new ArrayList<>(surface.faces);
            faces.addAll(surface.waterFaces);
            for (BoardSurface.Face face : faces) {
                for (Vector3 point : List.of(face.a(), face.b(), face.c())) {
                    for (int edge = 0; edge < 6; edge++) {
                        Vector3 a = BoardGeometry.corner(center, 0, edge);
                        Vector3 b = BoardGeometry.corner(center, 0, edge + 1);
                        Vector3 outward = new Vector3(b).sub(a).crs(Vector3.Z).nor();
                        float outside = new Vector3(point).sub(a).dot(outward);
                        assertTrue(outside <= 0.01f * BoardGeometry.HEX_SCALE,
                              "Water neighbor mask " + mask + ", " + face.finish() + " outside edge " + edge + ": " + point);
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(floats = { 0.5f, 1, 2 })
    void dryRiverEdgesRetainTheirFullHeight(float scale) {
        BoardGeometry.Tuning original = BoardGeometry.tuning();
        try {
            BoardGeometry.tune(new BoardGeometry.Tuning(scale, 0.6f, 0.87f, 18, 0.8f));
            Coords center = new Coords(7, 33);
            for (int mask = 0; mask < 64; mask++) {
                BoardScene scene = riverScene(center, mask);
                BoardSurface surface = new BoardSurface(scene, scene.tile(center));
                float top = scene.tile(center).elevation() * BoardGeometry.LEVEL;
                for (int edge = 0; edge < 6; edge++) {
                    if ((mask & (1 << BoardGeometry.edgeDirection(edge))) != 0) {
                        continue;
                    }
                    Vector3 a = BoardGeometry.corner(center, 0, edge);
                    Vector3 b = BoardGeometry.corner(center, 0, edge + 1);
                    for (int sample = 1; sample < 100; sample++) {
                        Vector3 point = new Vector3(a).lerp(b, sample / 100f);
                        assertEquals(top, surface.height(point.x, point.y), 0.01f * scale,
                              "Water neighbor mask " + mask + ", dry edge " + edge + ", sample " + sample);
                    }
                }
                for (BoardSurface.Side side : surface.sides(scene, BoardGeometry.floor(scene))) {
                    if ((mask & (1 << BoardGeometry.edgeDirection(side.edge()))) == 0) {
                        assertEquals(top, side.a().z, 0.01f * scale, "The cliff must close beneath the dry bank");
                        assertEquals(top, side.b().z, 0.01f * scale, "The cliff must close beneath the dry bank");
                    }
                }
            }
        } finally {
            BoardGeometry.tune(original);
        }
    }

    private static BoardScene riverScene(Coords center, int mask) {
        Map<Coords, Integer> levels = new HashMap<>();
        levels.put(center, 2);
        for (int direction = 0; direction < 6; direction++) {
            if ((mask & (1 << direction)) != 0) {
                levels.put(center.translated(direction), 0);
            }
        }
        return riverScene(levels, false);
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
        int width = Math.max(5, levels.keySet().stream().mapToInt(Coords::getX).max().orElseThrow() + 2);
        int height = Math.max(5, levels.keySet().stream().mapToInt(Coords::getY).max().orElseThrow() + 2);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Coords coords = new Coords(x, y);
                tiles.add(new BoardScene.Tile(coords, levels.getOrDefault(coords, 0), levels.containsKey(coords) ? 1 : -1,
                      frozen && levels.containsKey(coords), 0, BoardScene.Surface.GRASS, art, null, null, List.of(), List.of()));
            }
        }
        return new BoardScene(0, width, height, tiles, List.of(), List.of(), -1, "", List.of());
    }

    private static BoardScene scene(boolean river) {
        return scene(river, 1, true, true, 2);
    }

    private static BoardScene scene(boolean river, int direction, boolean firstRoad, boolean secondRoad, int elevation) {
        var art = new BoardScene.Pixels(new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB));
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                Coords coords = new Coords(x, y);
                boolean first = coords.equals(FIRST), second = coords.equals(FIRST.translated(direction));
                tiles.add(new BoardScene.Tile(coords, !river && first ? elevation : 0,
                      river && (first || second) ? 2 : -1, false,
                      river ? 0 : first && firstRoad ? 1 << direction
                            : second && secondRoad ? 1 << ((direction + 3) % 6) : 0, BoardScene.Surface.GRASS,
                      art, null, null, List.of(), List.of()));
            }
        }
        return new BoardScene(0, 4, 4, tiles, List.of(), List.of(), -1, "", List.of());
    }
}
