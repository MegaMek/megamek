/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.List;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector3;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.Hex;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Terrains;

/** Actual game occupancy projected into the model's local axes. Shared by every camera and unit family. */
final class UnitFootprint {
    record Layout(float width, float depth, float offsetX, float offsetY) { }

    /** The same GL-frame pose used to place a moving model, never a replacement for game occupancy. */
    record Pose(BoardScene.Unit unit, Vector3 position, float facing) {
        Pose {
            position = position.cpy();
        }

        Vector3 outlinePoint(Coords occupied, int corner) {
            Vector3 center = BoardGeometry.center(occupied, 0);
            return BoardGeometry.markerPoint(BoardGeometry.corner(occupied, 0, corner), center)
                  .sub(BoardGeometry.center(unit.location().coords(), 0))
                  .rotate(Vector3.Z, unit.location().facing() * 60 - facing).add(position).add(0, 0, .5f);
        }
    }

    private UnitFootprint() { }

    static float support(Board board, Coords origin, List<Coords> occupied, float relativeElevation) {
        Hex anchor = board.getHex(origin);
        float highest = (anchor == null ? 0 : anchor.getLevel()) + relativeElevation;
        int surface = -1;
        if (anchor != null && relativeElevation > 0) {
            for (int candidate : new int[] { Terrains.BRIDGE_ELEV, Terrains.BLDG_ELEV }) {
                if (anchor.terrainLevel(candidate) == relativeElevation) {
                    surface = candidate;
                    break;
                }
            }
        }
        for (Coords coords : occupied) {
            var hex = board.getHex(coords);
            if (hex != null) {
                highest = Math.max(highest, hex.getLevel() + (surface >= 0 && hex.containsTerrain(surface) ? hex.terrainLevel(surface) : 0));
            }
        }
        return highest;
    }

    static boolean terrainSupported(EntityMovementMode mode) {
        return mode != EntityMovementMode.NAVAL && mode != EntityMovementMode.HYDROFOIL && mode != EntityMovementMode.SUBMARINE;
    }

    /** Continuous visual clearance for a moving large hull. The game path and occupied coordinates are unchanged. */
    static void clearTerrain(BoardScene scene, BoardScene.Unit unit, Vector3 position, float facing) {
        clearTerrain(scene, unit, position, facing, unit.airborne());
    }

    static void clearTerrain(BoardScene scene, BoardScene.Unit unit, Vector3 position, float facing, boolean airborne) {
        if (airborne || unit.footprint().size() < 2) {
            return;
        }
        var mode = unit.model() == null || unit.model().state() == null ? null : unit.model().state().structure().movement();
        if (!terrainSupported(mode)) {
            return;
        }
        float support = -Float.MAX_VALUE;
        Vector3 origin = BoardGeometry.center(unit.location().coords(), 0);
        float turn = unit.location().facing() * 60 - facing;
        for (Coords occupied : unit.footprint()) {
            float[] vertices = new float[12];
            float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
            for (int corner = 0; corner < 6; corner++) {
                Vector3 point = BoardGeometry.corner(occupied, 0, corner).sub(origin).rotate(Vector3.Z, turn).add(position);
                vertices[corner * 2] = point.x;
                vertices[corner * 2 + 1] = point.y;
                minX = Math.min(minX, point.x);
                maxX = Math.max(maxX, point.x);
                minY = Math.min(minY, point.y);
                maxY = Math.max(maxY, point.y);
            }
            Polygon moved = new Polygon(vertices.clone());
            int left = Math.max(0, (int) (minX / (BoardGeometry.WIDTH * .75f)) - 1);
            int right = Math.min(scene.width() - 1, (int) (maxX / (BoardGeometry.WIDTH * .75f)) + 1);
            int top = Math.max(0, (int) (-maxY / BoardGeometry.HEIGHT) - 1);
            int bottom = Math.min(scene.height() - 1, (int) (-minY / BoardGeometry.HEIGHT) + 1);
            for (int x = left; x <= right; x++) {
                for (int y = top; y <= bottom; y++) {
                    var tile = scene.tile(new Coords(x, y));
                    for (int corner = 0; corner < 6; corner++) {
                        Vector3 point = BoardGeometry.corner(tile.coords(), 0, corner);
                        vertices[corner * 2] = point.x;
                        vertices[corner * 2 + 1] = point.y;
                    }
                    if (Intersector.overlapConvexPolygons(moved, new Polygon(vertices))) {
                        support = Math.max(support, tile.elevation());
                    }
                }
            }
        }
        if (support != -Float.MAX_VALUE) {
            position.z = Math.max(position.z, support * BoardGeometry.LEVEL);
        }
    }

    static Layout layout(Coords origin, List<Coords> occupied, float facing) {
        Vector3 center = BoardGeometry.center(origin, 0);
        Vector3 point = new Vector3();
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (Coords coords : occupied) {
            for (int corner = 0; corner < 6; corner++) {
                BoardGeometry.corner(point, coords, 0, corner).sub(center).rotate(Vector3.Z, facing);
                minX = Math.min(minX, point.x);
                maxX = Math.max(maxX, point.x);
                minY = Math.min(minY, point.y);
                maxY = Math.max(maxY, point.y);
            }
        }
        if (occupied.isEmpty()) {
            return new Layout(BoardGeometry.WIDTH, BoardGeometry.HEIGHT, 0, 0);
        }
        point.set((minX + maxX) / 2, (minY + maxY) / 2, 0).rotate(Vector3.Z, -facing);
        return new Layout(maxX - minX, maxY - minY, point.x, point.y);
    }
}
