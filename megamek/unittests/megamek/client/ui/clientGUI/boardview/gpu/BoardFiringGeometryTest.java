/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

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
                assertTrue(first.topA().z - first.bottomA().z >= 2 * BoardGeometry.LEVEL - 0.001f);
            }
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
