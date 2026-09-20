/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.clientGUI.boardview.BoardTactical;
import megamek.common.board.Coords;

/** Tessellation and terrain clipping only; all tactical decisions are supplied by the client painters. */
final class BoardTacticalGeometry {
    private static final float WALL_CLEARANCE = 0.6f;

    record Triangle(Vector3 a, Vector3 b, Vector3 c, int argb) { }
    private record Edge(float x1, float y1, float x2, float y2) {
        float x(float y) {
            return x1 + (x2 - x1) * (y - y1) / (y2 - y1);
        }
    }

    private BoardTacticalGeometry() { }

    /** Horizontal trapezoids preserve holes, dashed strokes, concave polygons and glyph counters. */
    static List<Triangle> flat(BoardTactical.Fill fill) {
        List<Edge> edges = new ArrayList<>();
        TreeSet<Float> levels = new TreeSet<>();
        PathIterator path = new Area(fill.shape()).getPathIterator(null, 0.25);
        float[] point = new float[6];
        float x = 0, y = 0, startX = 0, startY = 0;
        while (!path.isDone()) {
            int type = path.currentSegment(point);
            if (type == PathIterator.SEG_MOVETO) {
                startX = x = point[0];
                startY = y = point[1];
            } else {
                float nextX = type == PathIterator.SEG_CLOSE ? startX : point[0];
                float nextY = type == PathIterator.SEG_CLOSE ? startY : point[1];
                if (y != nextY) {
                    edges.add(new Edge(x, y, nextX, nextY));
                    levels.add(y);
                    levels.add(nextY);
                }
                x = nextX;
                y = nextY;
            }
            path.next();
        }
        List<Triangle> result = new ArrayList<>();
        List<Float> rows = new ArrayList<>(levels);
        for (int row = 1; row < rows.size(); row++) {
            float bottom = rows.get(row - 1), top = rows.get(row), middle = (bottom + top) / 2;
            List<Edge> crossings = edges.stream()
                  .filter(edge -> middle > Math.min(edge.y1(), edge.y2()) && middle < Math.max(edge.y1(), edge.y2()))
                  .sorted(Comparator.comparingDouble(edge -> edge.x(middle))).toList();
            for (int i = 1; i < crossings.size(); i += 2) {
                Edge left = crossings.get(i - 1), right = crossings.get(i);
                Vector3 a = new Vector3(left.x(bottom), bottom, 0), b = new Vector3(right.x(bottom), bottom, 0);
                Vector3 c = new Vector3(right.x(top), top, 0), d = new Vector3(left.x(top), top, 0);
                add(result::add, a, b, c, fill.argb());
                add(result::add, a, c, d, fill.argb());
            }
        }
        return result;
    }

    static void drape(BoardScene scene, Consumer<Triangle> destination) {
        drape(scene, scene.tactical().fills(), destination);
    }

    private static void drape(BoardScene scene, List<BoardTactical.Fill> fills, Consumer<Triangle> destination) {
        Map<Coords, BoardSurface> surfaces = new HashMap<>();
        int layer = 0;
        for (BoardTactical.Fill fill : fills) {
            float lift = (0.35f + Math.min(layer++, 10000) * 0.0001f) * BoardGeometry.HEX_SCALE;
            for (Triangle triangle : flat(fill)) {
                int firstX = Math.max(0, (int) Math.floor(minX(triangle) / (BoardGeometry.TILE_WIDTH * 0.75f)) - 1);
                int lastX = Math.min(scene.width() - 1, (int) Math.floor(maxX(triangle) / (BoardGeometry.TILE_WIDTH * 0.75f)));
                int firstY = Math.max(0, (int) Math.floor(minY(triangle) / BoardGeometry.TILE_HEIGHT) - 1);
                int lastY = Math.min(scene.height() - 1, (int) Math.floor(maxY(triangle) / BoardGeometry.TILE_HEIGHT));
                Triangle world = new Triangle(world(triangle.a()), world(triangle.b()), world(triangle.c()), fill.argb());
                for (int x = firstX; x <= lastX; x++) {
                    for (int y = firstY; y <= lastY; y++) {
                        Coords coords = new Coords(x, y);
                        BoardSurface surface = surfaces.computeIfAbsent(coords, key -> new BoardSurface(scene, scene.tile(key)));
                        clipSurface(world, surface, lift, destination);
                    }
                }
            }
        }
    }

    /** Cache both presentations once; the camera only selects which one to draw. */
    static void walls(BoardScene scene, boolean flat, Consumer<Triangle> destination,
          BiConsumer<BoardTactical.Wall, Triangle> outline) {
        if (flat) {
            drape(scene, scene.tactical().flatWalls(), destination);
        }
        for (BoardTactical.Wall wall : scene.tactical().walls()) {
            if (scene.tile(wall.coords()) == null) {
                continue;
            }
            Vector3 c = wallPoint(scene, wall, wall.b(), true), d = wallPoint(scene, wall, wall.a(), true);
            if (!flat) {
                Vector3 a = wallPoint(scene, wall, wall.a(), false), b = wallPoint(scene, wall, wall.b(), false);
                destination.accept(new Triangle(a, b, c, wall.argb()));
                destination.accept(new Triangle(a, c, d, wall.argb()));
                wallOutline(wall, d, c, triangle -> outline.accept(wall, triangle));
            } else {
                BoardSurface surface = new BoardSurface(scene, scene.tile(wall.coords()));
                wallOutline(wall, d, c, triangle -> clipSurface(triangle, surface,
                      WALL_CLEARANCE * BoardGeometry.HEX_SCALE, clipped -> outline.accept(wall, clipped)));
            }
        }
    }

    private static void clipSurface(Triangle triangle, BoardSurface surface, float lift, Consumer<Triangle> destination) {
        for (BoardSurface.Face face : surface.faces) {
            boolean top = surface.tile.frozen() ? face.finish() == BoardSurface.Finish.ICE
                  : face.finish() == BoardSurface.Finish.TOP || face.finish() == BoardSurface.Finish.SHORE;
            if (top) {
                clip(triangle, face, lift, destination);
            }
        }
        if (!surface.tile.frozen()) {
            for (BoardSurface.Face face : surface.waterFaces) {
                clip(triangle, face, lift, destination);
            }
        }
    }

    /** A continuous ribbon: the original dash pattern is supplied by a scrolling texture. */
    private static void wallOutline(BoardTactical.Wall wall, Vector3 a, Vector3 b, Consumer<Triangle> destination) {
        if (wall.outline() == null) {
            return;
        }
        var stroke = wall.outline().stroke();
        Vector3 side = new Vector3(a.y - b.y, b.x - a.x, 0).nor()
              .scl(stroke.getLineWidth() * BoardGeometry.HEX_SCALE / 2);
        Vector3 first = new Vector3(a).sub(side), second = new Vector3(b).sub(side);
        Vector3 third = new Vector3(b).add(side), fourth = new Vector3(a).add(side);
        destination.accept(new Triangle(first, second, third, wall.outline().argb()));
        destination.accept(new Triangle(first, third, fourth, wall.outline().argb()));
    }

    private static Vector3 wallPoint(BoardScene scene, BoardTactical.Wall wall, BoardTactical.Point point, boolean top) {
        Vector3 world = new Vector3(point.x() * BoardGeometry.HEX_SCALE, -point.y() * BoardGeometry.HEX_SCALE, 0);
        // Like the firing contour, adjoining panels share the full ridge span at their common endpoints.
        // Use surface elevation, never water depth or the lakebed, when crossing a change in level.
        float level = scene.tile(wall.coords()).elevation();
        for (int direction = 0; direction < 6; direction++) {
            Coords coords = wall.coords().translated(direction);
            BoardScene.Tile neighbor = scene.tile(coords);
            if (neighbor != null && BoardGeometry.contains(coords, world.x, world.y)) {
                level = top ? Math.max(level, neighbor.elevation()) : Math.min(level, neighbor.elevation());
            }
        }
        world.z = (level + (top ? wall.height() : 0)) * BoardGeometry.LEVEL + WALL_CLEARANCE * BoardGeometry.HEX_SCALE;
        return world;
    }

    private static Vector3 world(Vector3 point) {
        return new Vector3(point.x * BoardGeometry.HEX_SCALE, -point.y * BoardGeometry.HEX_SCALE, 0);
    }

    private static float cross(Vector3 a, Vector3 b, Vector3 c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    private static void clip(Triangle triangle, BoardSurface.Face face, float lift, Consumer<Triangle> destination) {
        float area = cross(face.a(), face.b(), face.c());
        if (Math.abs(area) < 0.00001f) {
            return;
        }
        List<Vector3> polygon = List.of(triangle.a(), triangle.b(), triangle.c());
        Vector3[] corners = { face.a(), face.b(), face.c() };
        float sign = Math.signum(area);
        for (int edge = 0; edge < 3 && !polygon.isEmpty(); edge++) {
            Vector3 a = corners[edge], b = corners[(edge + 1) % 3];
            List<Vector3> clipped = new ArrayList<>();
            Vector3 previous = polygon.getLast();
            float before = sign * cross(a, b, previous);
            for (Vector3 point : polygon) {
                float after = sign * cross(a, b, point);
                if ((before >= 0) != (after >= 0)) {
                    clipped.add(new Vector3(previous).lerp(point, before / (before - after)));
                }
                if (after >= 0) {
                    clipped.add(new Vector3(point));
                }
                previous = point;
                before = after;
            }
            polygon = clipped;
        }
        for (Vector3 point : polygon) {
            float b = cross(face.a(), point, face.c()) / area;
            float c = cross(face.a(), face.b(), point) / area;
            point.z = face.a().z + b * (face.b().z - face.a().z) + c * (face.c().z - face.a().z) + lift;
        }
        for (int i = 2; i < polygon.size(); i++) {
            add(destination, polygon.getFirst(), polygon.get(i - 1), polygon.get(i), triangle.argb());
        }
    }

    private static void add(Consumer<Triangle> destination, Vector3 a, Vector3 b, Vector3 c, int argb) {
        if (Math.abs(cross(a, b, c)) > 0.00001f) {
            destination.accept(new Triangle(a, b, c, argb));
        }
    }

    private static float minX(Triangle triangle) { return Math.min(triangle.a().x, Math.min(triangle.b().x, triangle.c().x)); }
    private static float maxX(Triangle triangle) { return Math.max(triangle.a().x, Math.max(triangle.b().x, triangle.c().x)); }
    private static float minY(Triangle triangle) { return Math.min(triangle.a().y, Math.min(triangle.b().y, triangle.c().y)); }
    private static float maxY(Triangle triangle) { return Math.max(triangle.a().y, Math.max(triangle.b().y, triangle.c().y)); }
}
