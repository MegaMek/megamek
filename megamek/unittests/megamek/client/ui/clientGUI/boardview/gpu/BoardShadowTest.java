/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.GdxNativesLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BoardShadowTest {
    @BeforeAll
    static void loadMathNatives() {
        GdxNativesLoader.load();
    }

    @Test
    void closeupDetailIsIndependentOfMapSizeAndIncludesOffscreenCasters() {
        BoardCamera view = new BoardCamera();
        view.resize(1000, 700);
        view.zoom(0.3f);
        Vector3 direction = new Vector3(2, 1, -1).nor();
        BoundingBox small = new BoundingBox(new Vector3(-1000, -1000, -30), new Vector3(1000, 1000, 120));
        BoundingBox large = new BoundingBox(new Vector3(-20000, -20000, -30), new Vector3(20000, 20000, 120));
        OrthographicCamera target = new OrthographicCamera();
        for (boolean isometric : new boolean[] { false, true }) {
            view.setIsometric(isometric);
            GpuTerrain.fitShadowCamera(view.camera, target, small, direction);
            float width = target.viewportWidth;
            float height = target.viewportHeight;
            GpuTerrain.fitShadowCamera(view.camera, target, large, direction);
            assertEquals(width, target.viewportWidth, 0.01f);
            assertEquals(height, target.viewportHeight, 0.01f);
            assertTrue(Math.max(width, height) / GpuTerrain.SHADOW_RESOLUTION < 0.5f,
                  "Closeups must retain sub-world-unit shadow texels even on a huge board");
            Vector3 right = new Vector3(view.camera.direction).crs(view.camera.up).nor();
            for (float x : new float[] { -0.49f, 0, 0.49f }) {
                for (float y : new float[] { -0.49f, 0, 0.49f }) {
                    Vector3 origin = new Vector3(view.camera.position)
                          .mulAdd(right, x * view.camera.viewportWidth * view.camera.zoom)
                          .mulAdd(view.camera.up, y * view.camera.viewportHeight * view.camera.zoom);
                    Vector3 receiver = new Vector3(origin).mulAdd(view.camera.direction, -origin.z / view.camera.direction.z);
                    assertInside(target, receiver);
                    // This upstream caster can be outside the player's viewport, but shadows the visible receiver.
                    Vector3 caster = new Vector3(receiver).mulAdd(direction, -100 / Math.abs(direction.z));
                    assertInside(target, caster);
                }
            }
        }
    }

    @Test
    void followsPanningAndRemainsValidAtTheBoardEdgeAndWithOverheadSun() {
        BoardCamera view = new BoardCamera();
        view.resize(1000, 700);
        view.zoom(0.2f);
        BoundingBox bounds = new BoundingBox(new Vector3(0, -15000, -30), new Vector3(13000, 0, 100));
        OrthographicCamera target = new OrthographicCamera();
        for (Vector3 direction : new Vector3[] { new Vector3(0, 0, -1), new Vector3(2, 1, -1).nor() }) {
            for (Vector3 focus : new Vector3[] { new Vector3(5000, -5000, 0), new Vector3(13000, -15000, 0) }) {
                view.center(focus);
                GpuTerrain.fitShadowCamera(view.camera, target, bounds, direction);
                assertInside(target, focus);
                assertTrue(target.viewportWidth < 1000 && target.viewportHeight < 1000);
                assertEquals(1, target.up.len(), 0.0001f);
                assertEquals(0, target.up.dot(target.direction), 0.0001f);
            }
        }
    }

    private void assertInside(OrthographicCamera camera, Vector3 point) {
        Vector3 projected = new Vector3(point).prj(camera.combined);
        assertTrue(Math.abs(projected.x) <= 1.001f && Math.abs(projected.y) <= 1.001f
                    && Math.abs(projected.z) <= 1.001f,
              () -> point + " falls outside the shadow camera: " + projected);
    }
}
