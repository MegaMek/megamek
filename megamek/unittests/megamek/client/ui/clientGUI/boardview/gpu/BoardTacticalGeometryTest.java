/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.clientGUI.boardview.BoardTacticalGraphics;
import megamek.client.ui.clientGUI.boardview.HexDrawUtilities;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Test;

class BoardTacticalGeometryTest {
    @Test
    void originalWhiteStrokeFollowsTheTopOfTheTranslucentWall() {
        var graphics = new BoardTacticalGraphics();
        try {
            Coords coords = new Coords(0, 0);
            graphics.setColor(new Color(35, 150, 200, 128));
            graphics.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10,
                  new float[] { 2, 2 }, 0));
            graphics.wall(HexDrawUtilities.getHexCrossLine01(4, 10), HexDrawUtilities.getHexCrossArea01(4, 10),
                  coords, 0.5f, Color.WHITE);
            var tile = new BoardScene.Tile(coords, 2, 4, false, 0,
                  BoardScene.Surface.GRASS, null, null, null, List.of(), List.of());
            var scene = new BoardScene(0, 1, 1, List.of(tile), List.of(), List.of(), -1, "", List.of(), null,
                  List.of(), List.of(), List.of(), graphics.snapshot());
            List<BoardTacticalGeometry.Triangle> triangles = new ArrayList<>();
            BoardTacticalGeometry.walls(scene, false, triangles::add, (wall, triangle) -> triangles.add(triangle));
            var face = triangles.stream().filter(t -> (t.argb() >>> 24) == 128).toList();
            var outline = triangles.stream().filter(t -> t.argb() == Color.WHITE.getRGB()).toList();
            assertEquals(2, face.size());
            assertFalse(outline.isEmpty());
            float top = Math.max(face.getFirst().a().z, Math.max(face.getFirst().b().z, face.getFirst().c().z));
            for (var triangle : outline) {
                for (Vector3 point : List.of(triangle.a(), triangle.b(), triangle.c())) {
                    assertEquals(top, point.z, 0.001f);
                }
            }
            double area = outline.stream().mapToDouble(t -> new Vector3(t.b()).sub(t.a())
                  .crs(new Vector3(t.c()).sub(t.a())).len() / 2).sum();
            assertEquals(72 * 1.4 * BoardGeometry.HEX_SCALE * BoardGeometry.HEX_SCALE, area, 0.01,
                  "The original stroke width is retained; the scrolling texture supplies its dash gaps");
        } finally {
            graphics.dispose();
        }
    }

    @Test
    void uprightBoundaryJoinsAcrossElevationChangesWithoutGaps() {
        var graphics = new BoardTacticalGraphics();
        try {
            List<BoardScene.Tile> tiles = new ArrayList<>();
            int[] levels = { 0, 3, 3, -2 };
            for (int y = 0; y < levels.length; y++) {
                Coords coords = new Coords(0, y);
                tiles.add(new BoardScene.Tile(coords, levels[y], y == 3 ? 5 : -1, false, 0,
                      BoardScene.Surface.GRASS, null, null, null, List.of(), List.of()));
                var local = (BoardTacticalGraphics) graphics.create();
                try {
                    local.translate(0, y * BoardGeometry.TILE_HEIGHT);
                    local.wall(HexDrawUtilities.getHexCrossLine01(4, 10), HexDrawUtilities.getHexCrossArea01(4, 10),
                          coords, 0.5f, null);
                } finally {
                    local.dispose();
                }
            }
            var scene = new BoardScene(0, 1, levels.length, tiles, List.of(), List.of(), -1, "", List.of(), null,
                  List.of(), List.of(), List.of(), graphics.snapshot());
            List<BoardTacticalGeometry.Triangle> triangles = new ArrayList<>();
            BoardTacticalGeometry.walls(scene, false, triangles::add, (wall, triangle) -> triangles.add(triangle));
            assertEquals(levels.length * 2, triangles.size());
            for (int y = 1; y < levels.length; y++) {
                float seam = -y * BoardGeometry.HEIGHT;
                var previous = triangles.subList((y - 1) * 2, y * 2).stream()
                      .flatMap(t -> List.of(t.a(), t.b(), t.c()).stream())
                      .filter(p -> Math.abs(p.y - seam) < 0.001f).map(p -> p.z).distinct().sorted().toList();
                var next = triangles.subList(y * 2, (y + 1) * 2).stream()
                      .flatMap(t -> List.of(t.a(), t.b(), t.c()).stream())
                      .filter(p -> Math.abs(p.y - seam) < 0.001f).map(p -> p.z).distinct().sorted().toList();
                assertEquals(2, previous.size());
                assertEquals(previous, next, "Both panels must meet along the same vertical span");
                assertTrue(previous.getFirst() >= Math.min(levels[y - 1], levels[y]) * BoardGeometry.LEVEL);
                assertTrue(previous.getLast() >= (Math.max(levels[y - 1], levels[y]) + 0.5f) * BoardGeometry.LEVEL);
            }
        } finally {
            graphics.dispose();
        }
    }

    @Test
    void flattenedBandAndOutlineStayOnTheSurfaceOverDeepWaterAndIce() {
        var graphics = new BoardTacticalGraphics();
        try {
            Coords coords = new Coords(0, 0);
            graphics.setColor(new Color(35, 150, 200, 128));
            graphics.setStroke(new BasicStroke(1.4f));
            graphics.wall(HexDrawUtilities.getHexCrossLine01(4, 10), HexDrawUtilities.getHexCrossArea01(4, 10),
                  coords, 0.5f, Color.WHITE);
            for (int level : new int[] { -2, 0, 3 }) {
                for (boolean frozen : new boolean[] { false, true }) {
                    var tile = new BoardScene.Tile(coords, level, 5, frozen, 0,
                          BoardScene.Surface.GRASS, null, null, null, List.of(), List.of());
                    var scene = new BoardScene(0, 1, 1, List.of(tile), List.of(), List.of(), -1, "", List.of(), null,
                          List.of(), List.of(), List.of(), graphics.snapshot());
                    List<BoardTacticalGeometry.Triangle> faces = new ArrayList<>(), outlines = new ArrayList<>();
                    BoardTacticalGeometry.walls(scene, true, faces::add, (wall, triangle) -> outlines.add(triangle));
                    assertFalse(faces.isEmpty());
                    assertFalse(outlines.isEmpty());
                    var triangles = new ArrayList<>(faces);
                    triangles.addAll(outlines);
                    for (var triangle : triangles) {
                        for (Vector3 point : List.of(triangle.a(), triangle.b(), triangle.c())) {
                            assertTrue(point.z >= BoardGeometry.surfaceZ(tile), "Stay above water, never on the lakebed");
                            assertTrue(point.z < level * BoardGeometry.LEVEL + BoardGeometry.HEX_SCALE,
                                  "The overhead band must lie on the surface rather than retain the wall height");
                            assertTrue(BoardGeometry.contains(coords, point.x, point.y));
                        }
                    }
                }
            }
        } finally {
            graphics.dispose();
        }
    }

    @Test
    void uprightBoundaryStaysHalfALevelAboveTheSurfaceRegardlessOfDepthOrIce() {
        var graphics = new BoardTacticalGraphics();
        try {
            Coords coords = new Coords(0, 0);
            graphics.setColor(new Color(255, 204, 255, 96));
            graphics.wall(HexDrawUtilities.getHexCrossLine01(4, 10), HexDrawUtilities.getHexCrossArea01(4, 10),
                  coords, 0.5f, null);
            for (int level : new int[] { -2, 0, 3 }) {
                for (int depth : new int[] { -1, 0, 5 }) {
                    for (boolean frozen : new boolean[] { false, true }) {
                        var tile = new BoardScene.Tile(coords, level, depth, frozen, 0,
                              BoardScene.Surface.GRASS, null, null, null, List.of(), List.of());
                        var scene = new BoardScene(0, 1, 1, List.of(tile), List.of(), List.of(), -1, "", List.of(), null,
                              List.of(), List.of(), List.of(), graphics.snapshot());
                        List<BoardTacticalGeometry.Triangle> triangles = new ArrayList<>();
                        BoardTacticalGeometry.walls(scene, false, triangles::add,
                              (wall, triangle) -> triangles.add(triangle));
                        assertEquals(2, triangles.size());
                        var points = triangles.stream().flatMap(t -> List.of(t.a(), t.b(), t.c()).stream()).toList();
                        double bottom = points.stream().mapToDouble(p -> p.z).min().orElseThrow();
                        double top = points.stream().mapToDouble(p -> p.z).max().orElseThrow();
                        assertTrue(bottom >= level * BoardGeometry.LEVEL);
                        assertTrue(bottom < level * BoardGeometry.LEVEL + BoardGeometry.HEX_SCALE);
                        assertEquals(0.5f * BoardGeometry.LEVEL, top - bottom, 0.001);
                        for (Vector3 point : points) {
                            assertEquals(21 * BoardGeometry.HEX_SCALE, point.x, 0.001f,
                                  "The vertical wall keeps the straight path across the hex");
                        }
                        for (var triangle : triangles) {
                            assertTrue(new Vector3(triangle.b()).sub(triangle.a())
                                  .crs(new Vector3(triangle.c()).sub(triangle.a())).len2() > 0);
                            assertEquals(96, triangle.argb() >>> 24);
                        }
                    }
                }
            }
        } finally {
            graphics.dispose();
        }
    }

    @Test
    void tessellationPreservesHolesConcavityAndTranslucency() {
        var graphics = new BoardTacticalGraphics();
        try {
            Area ring = new Area(new Rectangle2D.Float(0, 0, 80, 60));
            ring.subtract(new Area(new Rectangle2D.Float(10, 10, 60, 40)));
            graphics.setColor(new Color(20, 120, 220, 80));
            graphics.fill(ring);
            var fill = graphics.snapshot().fills().getFirst();
            var triangles = BoardTacticalGeometry.flat(fill);
            double area = triangles.stream().mapToDouble(t -> Math.abs((t.b().x - t.a().x) * (t.c().y - t.a().y)
                  - (t.b().y - t.a().y) * (t.c().x - t.a().x)) / 2).sum();
            assertEquals(2400, area, 0.001);
            for (var triangle : triangles) {
                assertEquals(80, triangle.argb() >>> 24);
                assertFalse(Intersector.isPointInTriangle(new Vector2(40, 30),
                      new Vector2(triangle.a().x, triangle.a().y), new Vector2(triangle.b().x, triangle.b().y),
                      new Vector2(triangle.c().x, triangle.c().y)), "The empty centre must remain empty");
            }
            graphics.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10,
                  new float[] { 5, 3 }, 0));
            graphics.drawLine(0, 70, 80, 70);
            assertTrue(graphics.snapshot().fills().getLast().contours().size() > 1, "Dash gaps stay separate");
        } finally {
            graphics.dispose();
        }
    }

    @Test
    void geometryFollowsRaisedAndLoweredHexesAndWaterWithoutCrossingOutsideTheBoard() {
        var graphics = new BoardTacticalGraphics();
        try {
            graphics.setColor(Color.CYAN);
            graphics.fillRect(-20, -20, 300, 150);
            List<BoardScene.Tile> tiles = new ArrayList<>();
            for (int x = 0; x < 3; x++) {
                tiles.add(new BoardScene.Tile(new Coords(x, 0), x - 1, x == 0 ? 1 : -1, false, 0,
                      BoardScene.Surface.GRASS, null, null, null, List.of(), List.of()));
            }
            var scene = new BoardScene(0, 3, 1, tiles, List.of(), List.of(), -1, "", List.of(), null,
                  List.of(), List.of(), List.of(), graphics.snapshot());
            List<BoardTacticalGeometry.Triangle> triangles = new ArrayList<>();
            BoardTacticalGeometry.drape(scene, triangles::add);
            assertFalse(triangles.isEmpty());
            assertTrue(triangles.stream().flatMap(t -> List.of(t.a(), t.b(), t.c()).stream())
                  .anyMatch(p -> p.z > BoardGeometry.LEVEL));
            assertTrue(triangles.stream().flatMap(t -> List.of(t.a(), t.b(), t.c()).stream())
                  .anyMatch(p -> p.z < -BoardGeometry.LEVEL));
            for (var triangle : triangles) {
                Vector3 centre = new Vector3(triangle.a()).add(triangle.b()).add(triangle.c()).scl(1f / 3);
                assertTrue(tiles.stream().anyMatch(tile -> BoardGeometry.contains(tile.coords(), centre.x, centre.y)));
                assertTrue(Float.isFinite(centre.z));
            }
        } finally {
            graphics.dispose();
        }
    }
}
