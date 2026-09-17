/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Exercises actual atlas packing/upload boundaries and shadow visibility in a native OpenGL context. */
@Tag("on-demand")
class GpuResourcesSmokeTest {
    @Test
    void packsResizedHudAndCastsHeightDependentShadows() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    checkAtlas();
                    checkShadows();
                    checkMeepleShadows();
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    private void checkAtlas() {
        GpuTextures<String> atlas = new GpuTextures<>();
        try {
            for (int[] size : new int[][] { { 2042, 8 }, { 2043, 8 }, { 2048, 8 }, { 8, 2043 },
                  { 3840, 2160 }, { 1280, 800 } }) {
                BoardScene.Pixels pixels = new BoardScene.Pixels(new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB));
                assertTrue(atlas.update(Map.of("hud", pixels)));
                assertEquals(size[0], atlas.region("hud").getRegionWidth());
                assertEquals(size[1], atlas.region("hud").getRegionHeight());
                assertFalse(atlas.update(Map.of("hud", pixels)));
                BoardScene.Pixels changed = new BoardScene.Pixels(new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB));
                assertFalse(atlas.update(Map.of("hud", changed)), "A pixel update must retain the atlas layout");
                assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
            }
        } finally {
            atlas.dispose();
        }
    }

    private void checkShadows() {
        BufferedImage white = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = white.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        BoardScene.Pixels pixels = new BoardScene.Pixels(white);
        GpuTerrain terrain = new GpuTerrain();
        BoardCamera camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        try {
            for (float direction : new float[] { 38, -38 }) {
                BoardScene scene = shadowScene(pixels, 4, new BoardScene.Light(direction, 0));
                camera.fit(scene);
                draw(terrain, camera, scene);
                int left = brightness(camera, -70);
                int right = brightness(camera, 70);
                assertTrue(direction > 0 ? right < left - 20 : left < right - 20,
                      "The raised hex must cast onto the down-light neighbor: left=" + left + ", right=" + right);
                if (direction > 0) {
                    File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
                    assertTrue(output.isDirectory() || output.mkdirs());
                    GpuBoardTestUi.capture(new File(output, "hex-shadow-top.png"));
                    camera.setIsometric(true);
                    camera.fit(scene);
                    draw(terrain, camera, scene);
                    GpuBoardTestUi.capture(new File(output, "hex-shadow-isometric.png"));
                    camera.setIsometric(false);
                }
            }
            BoardScene flat = shadowScene(pixels, 0, new BoardScene.Light(38, 0));
            camera.fit(flat);
            draw(terrain, camera, flat);
            assertEquals(brightness(camera, -70), brightness(camera, 70), 3,
                  "Lowering the caster must invalidate its old shadow");
            BoardScene disabled = shadowScene(pixels, 4, null);
            camera.fit(disabled);
            draw(terrain, camera, disabled);
            assertEquals(brightness(camera, -70), brightness(camera, 70), 3,
                  "Disabling terrain shadows must remove the shadow map");
        } finally {
            terrain.dispose();
        }
    }

    private void checkMeepleShadows() {
        BufferedImage artwork = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = artwork.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(30, 24, 24, 24);
        graphics.dispose();
        BoardScene.Pixels tokenPixels = new BoardScene.Pixels(artwork);
        BufferedImage ground = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        graphics = ground.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        GpuTextures<String> atlas = new GpuTextures<>();
        GpuTerrain terrain = new GpuTerrain();
        ModelBatch batch = new ModelBatch();
        atlas.update(Map.of("token", tokenPixels));
        GpuMeeple meeple = new GpuMeeple(tokenPixels, atlas.region("token"));
        ModelInstance unit = new ModelInstance(meeple.instance.model);
        BoardCamera camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        BoardScene.Pixels floor = new BoardScene.Pixels(ground);
        Vector3 position = BoardGeometry.center(new Coords(2, 2), 0);
        try {
            for (float direction : new float[] { 38, -38 }) {
                BoardScene scene = shadowScene(floor, 0, new BoardScene.Light(direction, 0));
                terrain.update(scene);
                camera.setIsometric(false);
                camera.fit(scene);
                meeple.place(unit, camera.camera, position, 0, 2);
                BoundingBox bounds = unit.calculateBoundingBox(new BoundingBox()).mul(unit.transform);
                assertEquals(2 * BoardGeometry.LEVEL, bounds.getDepth(), 0.001f);
                assertEquals(0.5f, bounds.min.z, 0.001f);
                terrain.renderShadows(List.of(unit));
                drawMeeple(terrain, camera, batch, unit);
                int left = brightness(camera, -35);
                int right = brightness(camera, 35);
                assertTrue(direction > 0 ? right < left - 20 : left < right - 20,
                      "Meeple shadow must follow light: left=" + left + ", right=" + right);
            }
            meeple.place(unit, camera.camera, position, 0, 1);
            assertEquals(BoardGeometry.LEVEL,
                  unit.calculateBoundingBox(new BoundingBox()).mul(unit.transform).getDepth(), 0.001f);
            meeple.place(unit, camera.camera, new Vector3(position).add(0, -100, 0), 0, 2);
            terrain.renderShadows(List.of(unit));
            drawMeeple(terrain, camera, batch, unit);
            assertEquals(brightness(camera, -35), brightness(camera, 35), 3,
                  "Moving a meeple must clear its old shadow");

            camera.setIsometric(true);
            camera.center(position);
            camera.zoom(0.4f);
            meeple.place(unit, camera.camera, position, 0, 2);
            int[] sideBrightness = new int[2];
            int index = 0;
            for (float direction : new float[] { 38, -38 }) {
                terrain.update(shadowScene(floor, 0, new BoardScene.Light(direction, 0)));
                terrain.renderShadows(List.of(unit));
                drawMeeple(terrain, camera, batch, unit);
                sideBrightness[index++] = brightness(camera, new Vector3(position).add(12, 0, BoardGeometry.LEVEL));
            }
            assertTrue(Math.abs(sideBrightness[0] - sideBrightness[1]) > 20,
                  "Meeple side normals must respond to light direction");
            File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
            assertTrue(output.isDirectory() || output.mkdirs());
            GpuBoardTestUi.capture(new File(output, "meeple-shadow.png"));
        } finally {
            meeple.dispose();
            batch.dispose();
            terrain.dispose();
            atlas.dispose();
        }
    }

    private void drawMeeple(GpuTerrain terrain, BoardCamera camera, ModelBatch batch, ModelInstance unit) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        ScreenUtils.clear(0.035f, 0.055f, 0.075f, 1, true);
        terrain.render(camera.camera, false);
        batch.begin(camera.camera);
        batch.render(unit, terrain.environment());
        batch.end();
    }

    private BoardScene shadowScene(BoardScene.Pixels pixels, int elevation, BoardScene.Light light) {
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), x == 2 && y == 2 ? elevation : 0, pixels));
            }
        }
        return new BoardScene(0, 5, 5, tiles, List.of(), List.of(), -1, "", List.of(), light);
    }

    private void draw(GpuTerrain terrain, BoardCamera camera, BoardScene scene) {
        terrain.update(scene);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        ScreenUtils.clear(0.035f, 0.055f, 0.075f, 1, true);
        terrain.render(camera.camera, false);
    }

    private int brightness(BoardCamera camera, float dx) {
        return brightness(camera, BoardGeometry.center(new Coords(2, 2), 0).add(dx, 0, 0));
    }

    private int brightness(BoardCamera camera, Vector3 position) {
        Vector3 point = camera.camera.project(position,
              0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        Pixmap sample = Pixmap.createFromFrameBuffer((int) point.x, (int) point.y, 1, 1);
        try {
            return sample.getPixel(0, 0) >>> 24;
        } finally {
            sample.dispose();
        }
    }
}
