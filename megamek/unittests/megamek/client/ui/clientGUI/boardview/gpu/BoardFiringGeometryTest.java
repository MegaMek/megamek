/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.badlogic.gdx.math.Vector3;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Test;

class BoardFiringGeometryTest {
    private static BoardScene scene() {
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                List<BoardScene.Feature> features = x == 4 && y == 4
                      ? List.of(new BoardScene.Feature("tower", 0, 0, 0, 1, 5, 0)) : List.of();
                tiles.add(new BoardScene.Tile(new Coords(x, y), x == 4 || x == 6 ? 4 : 0,
                      -1, false, 0, BoardScene.Surface.GRASS, null, null, null, features, List.of()));
            }
        }
        return new BoardScene(0, 9, 9, tiles, List.of(), List.of(), -1, "Firing", List.of());
    }

    private static BoardScene.FiringLine line(Coords start, float startHeight, Coords end, float endHeight, boolean indirect) {
        return new BoardScene.FiringLine(new BoardScene.Waypoint(start, startHeight, 0),
              new BoardScene.Waypoint(end, endHeight, 0), 0x00FFAA, indirect);
    }

    @Test
    void directFireConnectsExactEndpointsRegardlessOfInterveningCliffs() {
        var line = line(new Coords(1, 4), 1, new Coords(7, 4), 6, false);
        List<Vector3> path = BoardFiringGeometry.trajectory(scene(), line);
        assertEquals(List.of(BoardGeometry.center(line.source().coords(), 1),
              BoardGeometry.center(line.target().coords(), 6)), path);
    }

    @Test
    void indirectFireClearsEveryCrossedHexAndItsStructuresIncludingEdges() {
        BoardScene scene = scene();
        for (Coords start : List.of(new Coords(1, 4), new Coords(3, 4), new Coords(1, 2))) {
            for (float targetHeight : List.of(1f, 12f)) {
                var line = line(start, 1, new Coords(7, 4), targetHeight, true);
                List<Vector3> path = BoardFiringGeometry.trajectory(scene, line);
                assertEquals(BoardGeometry.center(start, 1), path.getFirst());
                assertEquals(BoardGeometry.center(line.target().coords(), targetHeight), path.getLast());
                // Sample the rendered straight mesh segments, not just the ideal curve's vertices.
                for (int i = 1; i < path.size(); i++) {
                    for (int sample = 0; sample <= 8; sample++) {
                        Vector3 point = new Vector3(path.get(i - 1)).lerp(path.get(i), sample / 8f);
                        for (BoardScene.Tile tile : scene.tiles()) {
                            if (!tile.coords().equals(start) && !tile.coords().equals(line.target().coords())
                                  && BoardGeometry.contains(tile.coords(), point.x, point.y)) {
                                float roof = tile.elevation() + tile.features().stream()
                                      .map(BoardScene.Feature::height).max(Float::compare).orElse(0f);
                                assertTrue(point.z > roof * BoardGeometry.LEVEL,
                                      () -> "Trajectory intersects " + tile.coords() + " at " + point);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void ridgeWallsMeetAtIdenticalVerticesAcrossFourLevelCliffs() {
        BoardScene scene = scene();
        for (BoardScene.Tile tile : scene.tiles()) {
            for (int edge = 0; edge < 6; edge++) {
                Coords adjacent = tile.coords().translated(BoardGeometry.edgeDirection(edge));
                if (scene.tile(adjacent) == null) {
                    continue;
                }
                var first = BoardFiringGeometry.rangeEdge(scene, tile.coords(), edge);
                var opposite = BoardFiringGeometry.rangeEdge(scene, adjacent, (edge + 3) % 6);
                assertTrue(first.topA().epsilonEquals(opposite.topB(), 0.001f));
                assertTrue(first.topB().epsilonEquals(opposite.topA(), 0.001f));
                assertTrue(first.bottomA().epsilonEquals(opposite.bottomB(), 0.001f));
                assertTrue(first.topA().z - first.bottomA().z
                      >= BoardFiringGeometry.RANGE_HEIGHT * BoardGeometry.LEVEL - 0.001f);
            }
        }
    }

    @Test
    void contourFollowsTheSurfaceAcrossWaterRegardlessOfDepthOrIce() {
        for (int level : new int[] { 1, -2 }) {
            for (int depth : new int[] { 0, 1, 5 }) {
                for (boolean frozen : new boolean[] { false, true }) {
                    List<BoardScene.Tile> tiles = new ArrayList<>();
                    for (int x = 0; x < 3; x++) {
                        tiles.add(new BoardScene.Tile(new Coords(x, 0), level, x == 1 ? depth : -1,
                              x == 1 && frozen, 0, BoardScene.Surface.GRASS, null, null, null, List.of(), List.of()));
                    }
                    BoardScene scene = new BoardScene(0, 3, 1, tiles, List.of(), List.of(), -1, "Firing", List.of());
                    float bottom = level * BoardGeometry.LEVEL
                          + BoardFiringGeometry.RANGE_CLEARANCE * BoardGeometry.HEX_SCALE;
                    float top = bottom + BoardFiringGeometry.RANGE_HEIGHT * BoardGeometry.LEVEL;
                    for (var tile : tiles) {
                        for (int edge = 0; edge < 6; edge++) {
                            var wall = BoardFiringGeometry.rangeEdge(scene, tile.coords(), edge);
                            assertEquals(bottom, wall.bottomA().z, 0.001f);
                            assertEquals(bottom, wall.bottomB().z, 0.001f);
                            assertEquals(top, wall.topA().z, 0.001f);
                            assertEquals(top, wall.topB().z, 0.001f);
                        }
                    }
                }
            }
        }
    }

    static List<BoardScene.RangeBorder> borders(Set<Coords> coverage, String label) {
        return coverage.stream().map(coords -> {
            int mask = 0;
            for (int direction = 0; direction < 6; direction++) {
                if (!coverage.contains(coords.translated(direction))) {
                    mask |= 1 << direction;
                }
            }
            return new BoardScene.RangeBorder(coords, mask, 0x40FF90, label);
        }).filter(border -> border.edges() != 0).toList();
    }

    @Test
    void contoursJoinEveryEdgeOnceIncludingHolesAndDisconnectedAreas() {
        Set<Coords> coverage = new HashSet<>(new Coords(4, 4).allAtDistanceOrLess(2));
        coverage.remove(new Coords(4, 4));
        coverage.add(new Coords(0, 0));
        List<BoardScene.RangeBorder> borders = borders(coverage, "S");
        BoardScene terrain = scene();
        BoardScene scene = new BoardScene(0, 9, 9, terrain.tiles(), List.of(), List.of(), -1, "Firing", List.of(),
              null, List.of(), borders);
        var contours = BoardFiringGeometry.rangeContours(scene);
        assertEquals(3, contours.size());
        var visited = new HashSet<BoardFiringGeometry.RangeSide>();
        for (var contour : contours) {
            for (int i = 0; i < contour.size(); i++) {
                var side = contour.get(i);
                assertTrue(visited.add(side), "Each boundary edge must occur exactly once");
                var wall = BoardFiringGeometry.rangeWall(scene, side);
                var next = BoardFiringGeometry.rangeWall(scene, contour.get((i + 1) % contour.size()));
                assertTrue(wall.topB().epsilonEquals(next.topA(), 0.1f),
                      () -> "Contour must meet across the seam: " + wall.topB() + " / " + next.topA());
                assertTrue(wall.bottomB().epsilonEquals(next.bottomA(), 0.1f));
            }
        }
        assertEquals(borders.stream().mapToInt(border -> Integer.bitCount(border.edges())).sum(), visited.size());
    }

    @Test
    void adjoiningBracketsWithTheSameColorKeepTheirOwnLabelsAndContours() {
        BoardScene terrain = scene();
        List<BoardScene.RangeBorder> borders = new ArrayList<>(borders(Set.of(new Coords(4, 4)), "S"));
        borders.addAll(borders(Set.of(new Coords(4, 5)), "M"));
        BoardScene scene = new BoardScene(0, 9, 9, terrain.tiles(), List.of(), List.of(), -1, "Firing", List.of(),
              null, List.of(), borders);
        var contours = BoardFiringGeometry.rangeContours(scene);
        assertEquals(2, contours.size());
        for (var contour : contours) {
            assertEquals(6, contour.size());
            assertEquals(1, contour.stream().map(side -> side.border().label()).distinct().count());
        }
    }

    @Test
    void sameHexAndVerticalShotsHaveFiniteGeometry() {
        Coords coords = new Coords(2, 2);
        for (boolean indirect : List.of(false, true)) {
            for (float height : List.of(1f, 10f)) {
                List<Vector3> path = BoardFiringGeometry.trajectory(scene(), line(coords, 1, coords, height, indirect));
                assertTrue(path.stream().allMatch(point -> Float.isFinite(point.x)
                      && Float.isFinite(point.y) && Float.isFinite(point.z)));
                assertEquals(BoardGeometry.center(coords, height), path.getLast());
            }
        }
    }
}
