/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.GdxNativesLoader;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Test;

class InfantryFootprintTest {
    @Test
    void fitsDifferentHeadingsAtBothBoardScalesWithoutChangingHeightOrMesh() {
        GdxNativesLoader.load();
        var bounds = new BoundingBox(new Vector3(-6, -5, 0), new Vector3(6, 8, 32));
        var hex = new Coords(0, 0);
        for (float scale : new float[] { .7f, 1 }) {
            for (int heading = 0; heading < 360; heading += 15) {
                var position = new Vector3(42, 26, 7);
                assertTrue(InfantryFootprint.fit(position, bounds, heading, scale));
                assertEquals(7, position.z);
                for (float x : new float[] { bounds.min.x, bounds.max.x }) {
                    for (float y : new float[] { bounds.min.y, bounds.max.y }) {
                        var point = new Vector3(x, y, 0).rotate(Vector3.Z, -heading).add(position).scl(scale)
                              .add(BoardGeometry.center(hex, 0));
                        assertTrue(BoardGeometry.contains(hex, point.x, point.y), point.toString());
                    }
                }
            }
        }
    }

    @Test
    void crowdedTransportsBleedInsteadOfShrinkingOrOverlapping() {
        var bounds = new BoundingBox(new Vector3(-20, -29, 0), new Vector3(20, 29, 15));
        var a = new Vector3(-24, 10, 0);
        var b = new Vector3(24, -10, 0);
        assertFalse(InfantryFootprint.fitPair(a, bounds, -17, b, bounds, 15, 1));
        var shapeA = InfantryFootprint.polygon(bounds, -17);
        var shapeB = InfantryFootprint.polygon(bounds, 15);
        shapeA.setPosition(a.x, a.y);
        shapeB.setPosition(b.x, b.y);
        assertFalse(Intersector.overlapConvexPolygons(shapeA, shapeB));
        assertEquals(40, bounds.getWidth());
        assertEquals(58, bounds.getHeight());
        var impossible = new BoundingBox(new Vector3(-50, -50, 0), new Vector3(50, 50, 15));
        var unchanged = new Vector3(10, 20, 3);
        assertFalse(InfantryFootprint.fit(unchanged, impossible, 0, 1));
        assertEquals(new Vector3(10, 20, 3), unchanged);
    }

    @Test
    void troopsClearParkedVehiclesEvenWhenTheyMustLeaveTheHex() {
        var vehicle = InfantryFootprint.polygon(new BoundingBox(new Vector3(-50, -50, 0), new Vector3(50, 50, 15)), 0);
        var troop = new BoundingBox(new Vector3(-4, -4, 0), new Vector3(4, 4, 23));
        var position = new Vector3(35, 0, 3);
        InfantryFootprint.avoid(position, troop, 0, List.of(vehicle), 1);
        var shape = InfantryFootprint.polygon(troop, 0);
        shape.setPosition(position.x, position.y);
        assertFalse(Intersector.overlapConvexPolygons(shape, vehicle));
        var hex = new Coords(0, 0);
        assertFalse(BoardGeometry.contains(hex, position.x + BoardGeometry.centerX(hex), position.y + BoardGeometry.centerY(hex)));
        assertEquals(3, position.z);
        assertEquals(8, troop.getWidth());
    }
}
