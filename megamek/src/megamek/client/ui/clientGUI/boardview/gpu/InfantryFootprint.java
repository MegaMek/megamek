/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.List;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import megamek.common.board.Coords;

/** Fits cosmetic member positions against the board's hex edges, leaving mesh size and heading intact. */
final class InfantryFootprint {
    private static final Vector3[] SIDES = sides();
    private static final float EDGE_MARGIN = .5f;

    private InfantryFootprint() { }

    private static Vector3[] sides() {
        var result = new Vector3[6];
        var hex = new Coords(0, 0);
        var center = BoardGeometry.center(hex, 0);
        for (int i = 0; i < result.length; i++) {
            var a = BoardGeometry.corner(hex, 0, i).sub(center);
            var edge = BoardGeometry.corner(hex, 0, i + 1).sub(center).sub(a);
            var normal = new Vector3(edge.y, -edge.x, 0).nor();
            result[i] = new Vector3(normal.x, normal.y, normal.dot(a) / BoardGeometry.HEX_SCALE);
        }
        return result;
    }

    static boolean fit(Vector3 position, BoundingBox bounds, float heading, float scale) {
        float originalX = position.x, originalY = position.y;
        float cosine = MathUtils.cosDeg(heading), sine = MathUtils.sinDeg(heading);
        float[] limits = new float[SIDES.length];
        for (int i = 0; i < SIDES.length; i++) {
            var side = SIDES[i];
            float x = side.x * cosine - side.y * sine, y = side.x * sine + side.y * cosine;
            float extent = Math.max(x * bounds.min.x, x * bounds.max.x) + Math.max(y * bounds.min.y, y * bounds.max.y);
            limits[i] = (side.z - EDGE_MARGIN) * BoardGeometry.HEX_SCALE / scale - extent;
        }
        // Alternating projections find a nearby position inside all six inset edges. Z is untouched.
        for (int pass = 0; pass < 12; pass++) {
            float correction = 0;
            for (int i = 0; i < SIDES.length; i++) {
                var side = SIDES[i];
                float excess = Math.max(0, side.x * position.x + side.y * position.y - limits[i]);
                position.add(-side.x * excess, -side.y * excess, 0);
                correction = Math.max(correction, excess);
            }
            if (correction < .0001f) { return true; }
        }
        position.set(originalX, originalY, position.z);
        return false;
    }

    /** Keep the two transports apart; an overcrowded group may bleed beyond the hex, never shrink or intersect. */
    static boolean fitPair(Vector3 a, BoundingBox boundsA, float headingA, Vector3 b, BoundingBox boundsB,
          float headingB, float scale) {
        var preferredA = new Vector3(a);
        var preferredB = new Vector3(b);
        var shapeA = polygon(boundsA, headingA);
        var shapeB = polygon(boundsB, headingB);
        var separation = new Intersector.MinimumTranslationVector();
        for (int pass = 0; pass < 12; pass++) {
            boolean fits = fit(a, boundsA, headingA, scale) & fit(b, boundsB, headingB, scale);
            shapeA.setPosition(a.x, a.y);
            shapeB.setPosition(b.x, b.y);
            if (!Intersector.overlapConvexPolygons(shapeA, shapeB, separation)) { return fits; }
            separate(a, b, separation, scale);
        }
        a.set(preferredA);
        b.set(preferredB);
        shapeA.setPosition(a.x, a.y);
        shapeB.setPosition(b.x, b.y);
        if (Intersector.overlapConvexPolygons(shapeA, shapeB, separation)) {
            separate(a, b, separation, scale);
        }
        return false;
    }

    static void avoid(Vector3 position, BoundingBox bounds, float heading, List<Polygon> obstacles, float scale) {
        var preferred = new Vector3(position);
        var shape = polygon(bounds, heading);
        for (int pass = 0; pass < 12; pass++) {
            if (!separate(position, shape, obstacles, scale)) { return; }
            if (!fit(position, bounds, heading, scale)) { break; }
        }
        // Preserve the requested size. Walking around a vehicle may require space outside this hex.
        position.set(preferred);
        for (int pass = 0; pass < 12 && separate(position, shape, obstacles, scale); pass++) { }
    }

    private static boolean separate(Vector3 position, Polygon shape, List<Polygon> obstacles, float scale) {
        boolean moved = false;
        var separation = new Intersector.MinimumTranslationVector();
        for (var obstacle : obstacles) {
            shape.setPosition(position.x, position.y);
            if (Intersector.overlapConvexPolygons(shape, obstacle, separation)) {
                float distance = separation.depth + EDGE_MARGIN * BoardGeometry.HEX_SCALE / scale;
                position.add(separation.normal.x * distance, separation.normal.y * distance, 0);
                moved = true;
            }
        }
        return moved;
    }

    static Polygon polygon(BoundingBox bounds, float heading) {
        var result = new Polygon(new float[] { bounds.min.x, bounds.min.y, bounds.max.x, bounds.min.y,
              bounds.max.x, bounds.max.y, bounds.min.x, bounds.max.y });
        result.setRotation(-heading);
        return result;
    }

    private static void separate(Vector3 a, Vector3 b, Intersector.MinimumTranslationVector separation, float scale) {
        float distance = separation.depth * .5f + EDGE_MARGIN * BoardGeometry.HEX_SCALE / scale;
        a.add(separation.normal.x * distance, separation.normal.y * distance, 0);
        b.add(-separation.normal.x * distance, -separation.normal.y * distance, 0);
    }
}
