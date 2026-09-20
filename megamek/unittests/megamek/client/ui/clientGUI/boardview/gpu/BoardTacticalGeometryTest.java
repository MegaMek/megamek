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
import megamek.common.board.Coords;
import org.junit.jupiter.api.Test;

class BoardTacticalGeometryTest {
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
