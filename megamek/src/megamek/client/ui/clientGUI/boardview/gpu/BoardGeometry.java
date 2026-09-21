/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import megamek.client.ui.tileset.HexTileset;
import megamek.common.board.Coords;

/** Z-up, tightly tiled hex columns. Rendering and picking use these same surfaces. */
final class BoardGeometry {
    /** Independent default for a whole unit occupying more than one game hex. */
    static final float DEFAULT_MULTI_HEX_UNIT_SCALE = 0.85f;

    record Tuning(float hexScale, float unitScale, float unitHeightScale, int levelHeight, float gridShade,
          float multiHexUnitScale) {
        Tuning(float hexScale, float unitScale, float unitHeightScale, int levelHeight, float gridShade) {
            this(hexScale, unitScale, unitHeightScale, levelHeight, gridShade, DEFAULT_MULTI_HEX_UNIT_SCALE);
        }

        Tuning {
            if (!Float.isFinite(hexScale) || hexScale <= 0 || !Float.isFinite(unitScale) || unitScale <= 0
                  || !Float.isFinite(unitHeightScale) || unitHeightScale <= 0 || levelHeight < 1
                  || !Float.isFinite(gridShade) || gridShade < 0 || gridShade > 1
                  || !Float.isFinite(multiHexUnitScale) || multiHexUnitScale <= 0) {
                throw new IllegalArgumentException("Invalid board dimensions");
            }
        }
    }

    static final Tuning DEFAULTS = new Tuning(1, 0.7f, 1.0f, 18, 0.8f);
    /**
     * Native tactical markers keep this fraction of the hex radius clear of the shared hex edges. Exactly on
     * an edge a marker is coplanar with the terrain there and flickers against it while the camera rotates.
     */
    static final float MARKER_INSET = 0.1f;
    static final float TILE_WIDTH = HexTileset.HEX_W;
    static final float TILE_HEIGHT = HexTileset.HEX_H;
    static float HEX_SCALE;
    static float WIDTH;
    static float HEIGHT;
    static float LEVEL;
    static float UNIT_SCALE;
    static float MULTI_HEX_UNIT_SCALE;
    static float UNIT_HEIGHT_SCALE;
    private static Tuning tuning;
    private static int revision;

    static {
        tune(DEFAULTS);
    }

    private BoardGeometry() { }

    static Tuning tuning() {
        return tuning;
    }

    static void tune(Tuning next) {
        if (next.equals(tuning)) {
            return;
        }
        tuning = next;
        HEX_SCALE = next.hexScale();
        WIDTH = TILE_WIDTH * HEX_SCALE;
        HEIGHT = TILE_HEIGHT * HEX_SCALE;
        LEVEL = next.levelHeight() * HEX_SCALE;
        UNIT_SCALE = next.unitScale();
        MULTI_HEX_UNIT_SCALE = next.multiHexUnitScale();
        UNIT_HEIGHT_SCALE = next.unitHeightScale();
        revision++;
    }

    static int revision() {
        return revision;
    }

    static float centerX(Coords coords) {
        return coords.getX() * WIDTH * 0.75f + WIDTH / 2;
    }

    static float centerY(Coords coords) {
        return -(coords.getY() * HEIGHT + (coords.getX() & 1) * HEIGHT / 2 + HEIGHT / 2);
    }

    static Vector3 center(Coords coords, float elevation) {
        return new Vector3(centerX(coords), centerY(coords), elevation * LEVEL);
    }

    static Vector3 corner(Coords coords, float elevation, int corner) {
        return corner(new Vector3(), coords, elevation, corner);
    }

    static Vector3 corner(Vector3 out, Coords coords, float elevation, int corner) {
        out.set(centerX(coords), centerY(coords), elevation * LEVEL);
        return switch (Math.floorMod(corner, 6)) {
            case 0 -> out.add(WIDTH / 2, 0, 0);
            case 1 -> out.add(WIDTH / 4, HEIGHT / 2, 0);
            case 2 -> out.add(-WIDTH / 4, HEIGHT / 2, 0);
            case 3 -> out.add(-WIDTH / 2, 0, 0);
            case 4 -> out.add(-WIDTH / 4, -HEIGHT / 2, 0);
            default -> out.add(WIDTH / 4, -HEIGHT / 2, 0);
        };
    }

    static int edgeDirection(int edge) {
        return Math.floorMod(1 - edge, 6);
    }

    /** Pulls a point toward a hex center inside the hex plane; the point keeps its elevation. */
    static Vector3 inset(Vector3 point, Vector3 center, float fraction) {
        return point.set(point.x + (center.x - point.x) * fraction,
              point.y + (center.y - point.y) * fraction, point.z);
    }

    /** One point of a native tactical marker, kept clear of the shared hex edges. */
    static Vector3 markerPoint(Vector3 point, Vector3 center) {
        return inset(point, center, MARKER_INSET);
    }

    /** Liquid beds include a two-world-unit visual recess even without positive game water depth. */
    static float groundZ(BoardScene.Tile tile) {
        return tile.elevation() * LEVEL - (tile.liquid().present() ? Math.max(2 * HEX_SCALE, tile.waterDepth() * LEVEL) : 0);
    }

    static float waterZ(BoardScene.Tile tile) {
        return tile.elevation() * LEVEL - HEX_SCALE;
    }

    static float surfaceZ(BoardScene.Tile tile) {
        if (tile.frozen()) {
            return tile.elevation() * LEVEL;
        }
        return tile.liquid().present() ? waterZ(tile) : groundZ(tile);
    }

    static float floor(BoardScene scene) {
        float lowest = Float.POSITIVE_INFINITY;
        for (BoardScene.Tile tile : scene.tiles()) {
            lowest = Math.min(lowest, groundZ(tile));
        }
        return lowest - LEVEL;
    }

    /** Shared atmosphere baseline: hex LEVEL, never a riverbed, water DEPTH, or model height. */
    static float weatherBase(BoardScene scene) {
        return scene.tiles().stream().mapToInt(BoardScene.Tile::elevation).min().orElse(0) * LEVEL;
    }

    static boolean contains(Coords coords, float x, float y) {
        float dx = Math.abs(x - centerX(coords));
        float dy = Math.abs(y - centerY(coords));
        return dy <= HEIGHT / 2 + 0.001f && (HEIGHT / 2) * dx + (WIDTH / 4) * dy <= WIDTH * HEIGHT / 4 + 0.001f;
    }

    record Hit(Coords coords, float distance) { }

    static Coords pick(BoardScene scene, Ray ray) {
        Hit hit = hit(scene, ray);
        return hit == null ? null : hit.coords();
    }

    /** Intersect the same triangles used to draw carved roads, banks, beds and exposed sides. */
    static Hit hit(BoardScene scene, Ray ray) {
        return hit(scene, ray, scene.tiles(), floor(scene));
    }

    static Hit hit(BoardScene scene, Ray ray, Iterable<BoardScene.Tile> candidates, float floor) {
        Coords result = null;
        float nearest = Float.POSITIVE_INFINITY;
        Vector3 hit = new Vector3();
        for (BoardScene.Tile tile : candidates) {
            float high = tile.elevation() * LEVEL;
            for (int direction = 0; direction < 6; direction++) {
                BoardScene.Tile neighbor = scene.tile(tile.coords().translated(direction));
                high = Math.max(high, BoardSurface.roadEdgeElevation(tile, neighbor, direction) * LEVEL);
            }
            if (!Intersector.intersectRayBoundsFast(ray,
                  new Vector3(centerX(tile.coords()), centerY(tile.coords()), (floor + high) / 2),
                  new Vector3(WIDTH, HEIGHT, high - floor + 0.01f))) {
                continue;
            }
            BoardSurface surface = new BoardSurface(scene, tile);
            for (BoardSurface.Face face : surface.faces) {
                if (Intersector.intersectRayTriangle(ray, face.a(), face.b(), face.c(), hit)
                      && ray.origin.dst2(hit) < nearest) {
                    nearest = ray.origin.dst2(hit);
                    result = tile.coords();
                }
            }
            for (BoardSurface.Face face : surface.waterFaces) {
                if (Intersector.intersectRayTriangle(ray, face.a(), face.b(), face.c(), hit) && ray.origin.dst2(hit) < nearest) {
                    nearest = ray.origin.dst2(hit);
                    result = tile.coords();
                }
            }
            var sides = surface.sides(scene, floor);
            sides.addAll(surface.waterfalls);
            for (BoardSurface.Side side : sides) {
                Vector3 lowerA = new Vector3(side.a().x, side.a().y, side.lowA());
                Vector3 lowerB = new Vector3(side.b().x, side.b().y, side.lowB());
                float distance = Float.POSITIVE_INFINITY;
                if (Intersector.intersectRayTriangle(ray, side.a(), lowerA, lowerB, hit)) {
                    distance = ray.origin.dst2(hit);
                }
                if (Intersector.intersectRayTriangle(ray, side.a(), lowerB, side.b(), hit)) {
                    distance = Math.min(distance, ray.origin.dst2(hit));
                }
                if (distance < nearest) {
                    nearest = distance;
                    result = tile.coords();
                }
            }
        }
        return result == null ? null : new Hit(result, nearest);
    }
}
