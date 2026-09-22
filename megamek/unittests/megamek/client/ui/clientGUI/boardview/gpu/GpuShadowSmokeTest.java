/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.environment.ShadowMap;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Sunlit cliff interiors must not acquire a shadow-map pattern as the board camera moves. */
@Tag("on-demand")
class GpuShadowSmokeTest {
    @Test
    void sunlitCliffsStayUnshadowedAcrossCameraAnglesAndZooms() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    checkCliffs();
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) {
            throw new AssertionError("Cliff shadow regression", failure.get());
        }
    }

    private static void checkCliffs() throws Exception {
        BufferedImage ground = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        var graphics = ground.createGraphics();
        graphics.setColor(new java.awt.Color(160, 160, 160));
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        BoardScene.Pixels pixels = new BoardScene.Pixels(ground);
        Coords raised = new Coords(4, 4);
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                Coords coords = new Coords(x, y);
                tiles.add(new BoardScene.Tile(coords, coords.equals(raised) ? 4 : 0, -1, false, 0,
                      BoardScene.Surface.GRASS, pixels, null, null, List.of(), List.of()));
            }
        }
        BoardScene scene = new BoardScene(0, 9, 9, tiles, List.of(), List.of(), -1, "", List.of());
        Pixmap white = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        white.setColor(1, 1, 1, 1);
        white.fill();
        Texture unshadowed = new Texture(white);
        white.dispose();
        GpuTerrain terrain = new GpuTerrain();
        BoardCamera camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.setIsometric(true);
        camera.center(BoardGeometry.center(raised, 2));
        terrain.update(scene);
        var lighting = BoardAtmosphere.lighting(BoardAtmosphere.DEFAULTS);
        var sides = new BoardSurface(scene, scene.tile(raised)).sides(scene, BoardGeometry.floor(scene));
        int compared = 0, mismatched = 0;
        boolean captured = false;
        try {
            for (float zoom : new float[] { 0.35f, 0.7f }) {
                camera.camera.zoom = zoom;
                for (float tilt : new float[] { 35, 55, 75 }) {
                    camera.tilt(tilt - camera.tilt());
                    for (int angle = 0; angle < 24; angle++) {
                        camera.orbit(15, 0);
                        var relative = lighting.relativeTo(camera.camera);
                        terrain.setAtmosphere(relative);
                        terrain.renderShadows(camera.camera, List.of());
                        List<Vector3> samples = new ArrayList<>();
                        for (BoardSurface.Side side : sides) {
                            Vector3 normal = new Vector3(side.b()).sub(side.a()).crs(Vector3.Z).nor();
                            if (normal.dot(camera.camera.direction) > -0.3f || normal.dot(relative.direction()) > -0.04f) {
                                continue;
                            }
                            for (int along = 2; along <= 8; along++) {
                                for (int height = 2; height <= 7; height++) {
                                    Vector3 point = new Vector3(side.a()).lerp(side.b(), along / 10f);
                                    point.z = BoardGeometry.LEVEL * 4 * height / 10f;
                                    samples.add(point);
                                }
                            }
                        }
                        Pixmap actual = draw(terrain, camera);
                        Pixmap reference = drawUnshadowed(terrain, camera, unshadowed);
                        try {
                            int errors = 0;
                            for (Vector3 point : samples) {
                                compared++;
                                if (difference(sample(actual, camera, point), sample(reference, camera, point)) > 2) {
                                    errors++;
                                }
                            }
                            mismatched += errors;
                            if (!captured && errors > 0) {
                                draw(terrain, camera).dispose();
                                capture("shadow-cliff-banding.png");
                                captured = true;
                            }
                        } finally {
                            actual.dispose();
                            reference.dispose();
                        }
                    }
                }
            }
            assertTrue(compared > 5000, "Probe sunlit faces through full orbits, multiple tilts and zooms");
            assertEquals(0, mismatched, "Sunlit cliff interiors must match the unshadowed reference; probes=" + compared);
            camera.setIsometric(true);
            camera.camera.zoom = 0.35f;
            camera.orbit(15, 35 - camera.tilt());
            terrain.setAtmosphere(lighting.relativeTo(camera.camera));
            terrain.renderShadows(camera.camera, List.of());
            draw(terrain, camera).dispose();
            capture("shadow-cliff-clean.png");
            checkCastShadow(terrain, camera, raised, sides, lighting, unshadowed);
            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
        } finally {
            terrain.dispose();
            unshadowed.dispose();
        }
    }

    private static void checkCastShadow(GpuTerrain terrain, BoardCamera camera, Coords raised,
          List<BoardSurface.Side> sides, BoardAtmosphere.Lighting lighting, Texture unshadowed) {
        camera.setIsometric(false);
        camera.center(BoardGeometry.center(raised, 0));
        terrain.setAtmosphere(lighting);
        terrain.renderShadows(camera.camera, List.of());
        Vector3 direction = new Vector3(lighting.direction().x, lighting.direction().y, 0).nor();
        BoardSurface.Side downstream = sides.stream().max(java.util.Comparator.comparingDouble(side ->
              new Vector3(side.b()).sub(side.a()).crs(Vector3.Z).nor().dot(direction))).orElseThrow();
        Vector3 foot = new Vector3(downstream.a()).lerp(downstream.b(), 0.5f);
        foot.z = 0;
        float length = BoardGeometry.LEVEL * 4 * (float) Math.hypot(lighting.direction().x, lighting.direction().y)
              / -lighting.direction().z;
        Pixmap actual = draw(terrain, camera);
        Pixmap reference = drawUnshadowed(terrain, camera, unshadowed);
        try {
            for (float distance : new float[] { 2, length / 2, length + 8 }) {
                Vector3 point = foot.cpy().mulAdd(direction, distance);
                int lit = sample(reference, camera, point) >>> 24;
                int shaded = sample(actual, camera, point) >>> 24;
                if (distance < length) {
                    assertTrue(lit - shaded > 20, "The cliff must still cast a shadow, including at its foot");
                } else {
                    assertEquals(lit, shaded, 2, "Ground beyond the cast shadow must stay lit");
                }
            }
        } finally {
            actual.dispose();
            reference.dispose();
        }
    }

    private static Pixmap drawUnshadowed(GpuTerrain terrain, BoardCamera camera, Texture unshadowed) {
        var shadow = terrain.environment().shadowMap;
        // Keep the same shader variant and light uniforms, but supply a map with no occluders.
        terrain.environment().shadowMap = new ShadowMap() {
            private final TextureDescriptor<Texture> depth = new TextureDescriptor<>(unshadowed);

            @Override
            public Matrix4 getProjViewTrans() { return shadow.getProjViewTrans(); }

            @Override
            public TextureDescriptor<Texture> getDepthMap() { return depth; }
        };
        try {
            return draw(terrain, camera);
        } finally {
            terrain.environment().shadowMap = shadow;
        }
    }

    private static Pixmap draw(GpuTerrain terrain, BoardCamera camera) {
        ScreenUtils.clear(0.03f, 0.045f, 0.06f, 1, true);
        terrain.render(camera.camera, false);
        return Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    }

    private static int sample(Pixmap image, BoardCamera camera, Vector3 point) {
        Vector3 screen = camera.camera.project(point.cpy());
        int x = (int) (screen.x * image.getWidth() / Gdx.graphics.getWidth());
        int y = (int) (screen.y * image.getHeight() / Gdx.graphics.getHeight());
        assertTrue(x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight(), "Probe must be on screen");
        return image.getPixel(x, y);
    }

    private static int difference(int a, int b) {
        return Math.max(Math.abs((a >>> 24) - (b >>> 24)), Math.max(Math.abs((a >>> 16 & 255) - (b >>> 16 & 255)),
              Math.abs((a >>> 8 & 255) - (b >>> 8 & 255))));
    }

    private static void capture(String name) throws Exception {
        File directory = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(directory.isDirectory() || directory.mkdirs());
        GpuBoardTestUi.capture(new File(directory, name));
    }
}
