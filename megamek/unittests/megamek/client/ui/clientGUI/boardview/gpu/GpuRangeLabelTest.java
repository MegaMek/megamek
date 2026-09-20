/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxNativesLoader;
import megamek.common.board.Coords;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GpuRangeLabelTest {
    @BeforeAll
    static void loadMathNatives() {
        GdxNativesLoader.load();
    }

    @Test
    void labelsFaceTheViewerAndStayUprightAtEveryBearingAndTilt() {
        BoardCamera camera = new BoardCamera();
        camera.resize(1200, 800);
        BoardScene.Tile tile = tile(2, -1);
        for (float tilt : new float[] { 0, 29, 31, 54.73561f, 80 }) {
            for (int bearing = 0; bearing < 360; bearing += 30) {
                camera.setIsometric(false);
                camera.orbit(bearing, tilt);
                Matrix4 pose = new Matrix4();
                GpuFireControl.labelTransform(pose, camera.camera, tile);
                assertEquals(1, new Vector3(Vector3.Y).rot(pose).dot(camera.camera.up), 0.0001f);
                assertEquals(-1, new Vector3(Vector3.Z).rot(pose).dot(camera.camera.direction), 0.0001f);
                Vector3 center = pose.getTranslation(new Vector3());
                assertEquals(BoardGeometry.centerX(tile.coords()), center.x, 0.0001f);
                assertEquals(BoardGeometry.centerY(tile.coords()), center.y, 0.0001f);
                Vector3 screenCenter = new Vector3(center).prj(camera.camera.combined);
                Vector3 screenRight = new Vector3(10, 0, 0).mul(pose).prj(camera.camera.combined);
                Vector3 screenUp = new Vector3(0, 10, 0).mul(pose).prj(camera.camera.combined);
                assertTrue(screenRight.x > screenCenter.x, "Letters must not be mirrored");
                assertTrue(screenUp.y > screenCenter.y, "Letters must not turn upside down");
            }
        }
    }

    @Test
    void flatArtworkClearsTheSurfaceAtAllTiltsAndIgnoresWaterDepth() {
        BoardGeometry.Tuning original = BoardGeometry.tuning();
        try {
            for (float scale : new float[] { 0.5f, 1, 2 }) {
                BoardGeometry.tune(new BoardGeometry.Tuning(scale, 0.6f, 0.87f, 18, 0.8f));
                BoardCamera camera = new BoardCamera();
                for (int elevation : new int[] { -2, 0, 3 }) {
                    for (int tilt = 0; tilt <= 80; tilt += 10) {
                        camera.setIsometric(false);
                        camera.orbit(137, tilt);
                        Matrix4 dry = new Matrix4(), water = new Matrix4();
                        GpuFireControl.labelTransform(dry, camera.camera, tile(elevation, -1));
                        GpuFireControl.labelTransform(water, camera.camera, tile(elevation, 4));
                        assertArrayEquals(dry.val, water.val, 0.0001f, "Water depth must not lower range labels");
                        for (float x : new float[] { -0.5f, 0.5f }) {
                            for (float y : new float[] { -0.5f, 0.5f }) {
                                Vector3 corner = new Vector3(x * BoardGeometry.WIDTH, y * BoardGeometry.HEIGHT, 0).mul(water);
                                assertTrue(corner.z >= elevation * BoardGeometry.LEVEL
                                            + GpuFireControl.RANGE_LABEL_CLEARANCE_LEVELS * BoardGeometry.LEVEL - 0.001f,
                                      "The whole label must clear the surface when it tilts toward the viewer");
                            }
                        }
                    }
                }
            }
        } finally {
            BoardGeometry.tune(original);
        }
    }

    private static BoardScene.Tile tile(int elevation, int depth) {
        return new BoardScene.Tile(new Coords(2, 3), elevation, depth, false, 0, BoardScene.Surface.GRASS,
              null, null, null, List.of(), List.of());
    }
}
