/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.tileset.HexTileset;
import megamek.common.board.Coords;

/** One world coordinate convention for terrain, sprites, movement, and picking in either camera view. */
final class BoardGeometry {
    public static final float WIDTH = HexTileset.HEX_W;
    public static final float HEIGHT = HexTileset.HEX_H;
    public static final float LEVEL = BoardView.ISOMETRIC_OFFSET;

    private BoardGeometry() { }

    public static float centerX(Coords coords) {
        return coords.getX() * WIDTH * 0.75f + WIDTH / 2;
    }

    public static float centerY(Coords coords) {
        return -(coords.getY() * HEIGHT + (coords.getX() & 1) * HEIGHT / 2 + HEIGHT / 2);
    }

    public static Vector3 center(Coords coords, float elevation) {
        return new Vector3(centerX(coords), centerY(coords), elevation * LEVEL);
    }

    public static Vector3 corner(Coords coords, float elevation, int corner) {
        Vector3 point = center(coords, elevation);
        return switch (Math.floorMod(corner, 6)) {
            case 0 -> point.add(WIDTH / 2, 0, 0);
            case 1 -> point.add(WIDTH / 4, HEIGHT / 2, 0);
            case 2 -> point.add(-WIDTH / 4, HEIGHT / 2, 0);
            case 3 -> point.add(-WIDTH / 2, 0, 0);
            case 4 -> point.add(-WIDTH / 4, -HEIGHT / 2, 0);
            default -> point.add(WIDTH / 4, -HEIGHT / 2, 0);
        };
    }

    /** Selects the nearest rendered surface, including cliff faces; no separate isometric hit-test approximation. */
    public static Coords pick(BoardScene scene, Ray ray) {
        Coords result = null;
        float nearest = Float.POSITIVE_INFINITY;
        Vector3 hit = new Vector3();
        Vector3 a = new Vector3();
        Vector3 b = new Vector3();
        Vector3 lowA = new Vector3();
        Vector3 lowB = new Vector3();
        float floor = floor(scene);
        for (BoardScene.Tile tile : scene.tiles()) {
            float z = tile.elevation() * LEVEL;
            if (Math.abs(ray.direction.z) > 0.00001f) {
                float t = (z - ray.origin.z) / ray.direction.z;
                float dx = Math.abs(ray.origin.x + ray.direction.x * t - centerX(tile.coords()));
                float dy = Math.abs(ray.origin.y + ray.direction.y * t - centerY(tile.coords()));
                if (t >= 0 && t * t < nearest && dy <= HEIGHT / 2
                      && (HEIGHT / 2) * dx + (WIDTH / 4) * dy <= WIDTH * HEIGHT / 4) {
                    nearest = t * t;
                    result = tile.coords();
                }
            }
            // Avoid triangle tests unless the ray crosses this hex's column.
            a.set(centerX(tile.coords()), centerY(tile.coords()), (z + floor) / 2);
            b.set(WIDTH, HEIGHT, z - floor);
            if (!Intersector.intersectRayBoundsFast(ray, a, b)) {
                continue;
            }
            for (int edge = 0; edge < 6; edge++) {
                float bottom = wallBottom(scene, tile, edge, floor);
                if (bottom >= z) {
                    continue;
                }
                a.set(corner(tile.coords(), tile.elevation(), edge));
                b.set(corner(tile.coords(), tile.elevation(), edge + 1));
                lowA.set(a.x, a.y, bottom);
                lowB.set(b.x, b.y, bottom);
                if (Intersector.intersectRayTriangle(ray, a, lowA, lowB, hit)
                      || Intersector.intersectRayTriangle(ray, a, lowB, b, hit)) {
                    float distance = ray.origin.dst2(hit);
                    if (distance < nearest) {
                        nearest = distance;
                        result = tile.coords();
                    }
                }
            }
        }
        return result;
    }

    public static float wallBottom(BoardScene scene, BoardScene.Tile tile, int edge, float floor) {
        BoardScene.Tile neighbor = scene.tile(tile.coords().translated(Math.floorMod(1 - edge, 6)));
        return neighbor == null ? floor : neighbor.elevation() * LEVEL;
    }

    public static float floor(BoardScene scene) {
        return (scene.tiles().stream().mapToInt(BoardScene.Tile::elevation).min().orElse(0) - 1) * LEVEL;
    }
}
