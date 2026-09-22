/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Real GL integration: wet ground and transparent water share rain animation without extra draws. */
@Tag("on-demand")
class GpuRainSurfaceSmokeTest {
    @Test
    void rainAnimatesNearbySurfacesAndDisappearsAtOverviewScale() throws Exception {
        Hex[] hexes = new Hex[12 * 8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 12; x++) {
                Hex hex = new Hex(0);
                hex.setTheme("grass");
                if (x < 4) { hex.addTerrain(new Terrain(Terrains.PAVEMENT, 1)); }
                else if (x < 8) { hex.addTerrain(new Terrain(Terrains.WATER, 1)); }
                hexes[y * 12 + x] = hex;
            }
        }
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create(new Board(12, 8, hexes))) {
            SwingUtilities.invokeAndWait(fixture.source::refresh);
            BoardScene scene = fixture.source.takeFrame().scene();
            new Lwjgl3Application(new ApplicationAdapter() {
                @Override
                public void create() {
                    GpuTerrain terrain = new GpuTerrain();
                    GLProfiler profiler = new GLProfiler(Gdx.graphics);
                    try {
                        Gdx.graphics.setVSync(false);
                        terrain.update(scene);
                        var settings = new BoardAtmosphere.Settings(13, 0.8f, 0, 2.5f, 0, 0,
                              new BoardAtmosphere.Effects(1, 0, 0, 0, 0, 0, 0));
                        terrain.setAtmosphere(BoardAtmosphere.lighting(settings));
                        BoardCamera camera = new BoardCamera();
                        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                        profiler.enable();
                        StringBuilder timings = new StringBuilder(Gdx.gl.glGetString(GL20.GL_RENDERER))
                              .append("\nSynchronized terrain-frame median, 24 alternating samples; includes CPU submission.\n");
                        for (boolean isometric : new boolean[] { false, true }) {
                            camera.setIsometric(isometric);
                            camera.fit(scene);
                            // A close view keeps the rings resolvable in both camera presets.
                            camera.camera.zoom *= 0.55f;
                            camera.camera.update();
                            terrain.renderShadows(List.of());
                            timings.append(isometric ? "isometric: " : "top: ").append(measure(terrain, camera)).append('\n');
                            profiler.reset();
                            Pixmap dry = frame(terrain, camera, 0);
                            int draws = profiler.getDrawCalls();
                            profiler.reset();
                            Pixmap wet = frame(terrain, camera, 1);
                            assertEquals(draws, profiler.getDrawCalls(), "Rain must not add draw calls");
                            Pixmap same = frame(terrain, camera, 1);
                            assertEquals(wet.getPixels(), same.getPixels(), "Rendering alone must not advance the rain clock");
                            terrain.animate(0.37f, List.of());
                            Pixmap laterDry = frame(terrain, camera, 0);
                            Pixmap laterWet = frame(terrain, camera, 1);
                            try {
                                save(wet, "rain-surfaces-" + (isometric ? "isometric" : "top"));
                                assertTrue(animatedRain(dry, wet, laterDry, laterWet, camera, new Coords(5, 4)) > 5,
                                      "Water must show animated rain beyond its existing animated texture");
                                int puddles = 0;
                                for (int x = 0; x < 4; x++) {
                                    for (int y = 1; y < 7; y++) {
                                        puddles += animatedRain(dry, wet, laterDry, laterWet, camera, new Coords(x, y));
                                    }
                                }
                                assertTrue(puddles > 10, "Visible paved puddles must show animated rain: " + puddles);
                            } finally {
                                dry.dispose(); wet.dispose(); same.dispose(); laterDry.dispose(); laterWet.dispose();
                            }
                        }
                        camera.setIsometric(false);
                        camera.fit(scene);
                        camera.camera.zoom = BoardGeometry.WIDTH * Gdx.graphics.getBackBufferWidth()
                              / (camera.camera.viewportWidth * 10);
                        camera.camera.update();
                        terrain.renderShadows(List.of());
                        timings.append("overview: ").append(measure(terrain, camera)).append('\n');
                        Pixmap distantDry = frame(terrain, camera, 0);
                        Pixmap distantWet = frame(terrain, camera, 1);
                        try {
                            Vector3 water = camera.camera.project(BoardGeometry.center(new Coords(5, 4), 0));
                            assertEquals(distantDry.getPixel((int) water.x, (int) water.y),
                                  distantWet.getPixel((int) water.x, (int) water.y),
                                  "Distant water skips both rain reflection and ripples");
                        } finally {
                            distantDry.dispose(); distantWet.dispose();
                        }
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        checkRainAmount();
                        Files.writeString(new File(System.getProperty("megamek.gpu.screenshots"), "rain-surface-timing.txt")
                              .toPath(), timings);
                    } catch (Throwable error) {
                        failure.set(error);
                    } finally {
                        profiler.disable();
                        terrain.dispose();
                        Gdx.app.exit();
                    }
                }
            }, GpuBoardWindow.configuration(false));
        }
        if (failure.get() != null) { throw new AssertionError(failure.get()); }
    }

    /** Use neutral ground and a blue sky so reflected water coverage is measurable independently of darkening. */
    private static void checkRainAmount() throws Exception {
        BufferedImage gray = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 72; y++) {
            for (int x = 0; x < 84; x++) { gray.setRGB(x, y, 0xff888888); }
        }
        BoardScene.Pixels pixels = new BoardScene.Pixels(gray);
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 8; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), 0, -1, false, 0,
                      BoardScene.Surface.CONCRETE, pixels, null, null, List.of(), List.of()));
            }
        }
        BoardScene scene = new BoardScene(0, 12, 8, tiles, List.of(), List.of(), -1, "", List.of());
        GpuTerrain terrain = new GpuTerrain();
        try {
            terrain.update(scene);
            Color sky = new Color(0.12f, 0.4f, 0.8f, 1);
            terrain.setAtmosphere(new BoardAtmosphere.Lighting(new Vector3(0, 0, -1), Color.BLACK, Color.WHITE,
                  Color.BLACK, sky, sky, Color.WHITE, 1, 0, false));
            BoardCamera camera = new BoardCamera();
            camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            for (boolean isometric : new boolean[] { false, true }) {
                camera.setIsometric(isometric);
                camera.fit(scene);
                camera.camera.zoom *= 0.75f;
                camera.camera.update();
                Pixmap light = frame(terrain, camera, 0.2f), moderate = frame(terrain, camera, 0.6f), heavy = frame(terrain, camera, 1);
                int[] a = patch(light, camera), b = patch(moderate, camera), c = patch(heavy, camera);
                try {
                    int lightArea = puddlePixels(a), moderateArea = puddlePixels(b), heavyArea = puddlePixels(c);
                    assertTrue(moderateArea > lightArea && heavyArea > moderateArea * 1.3 && heavyArea > 1000,
                          "Rain must expand visible puddles: " + lightArea + ", " + moderateArea + ", " + heavyArea);
                    save(light, "puddle-amount-light-" + isometric);
                    save(moderate, "puddle-amount-moderate-" + isometric);
                    save(heavy, "puddle-amount-heavy-" + isometric);
                    // The former fixed ripple centers repeated their complete pattern every 1.25 seconds.
                    terrain.animate(1.25f, List.of());
                    Pixmap laterLight = frame(terrain, camera, 0.2f), laterHeavy = frame(terrain, camera, 1);
                    try {
                        int lightImpacts = changed(a, patch(laterLight, camera));
                        int heavyImpacts = changed(c, patch(laterHeavy, camera));
                        assertTrue(heavyImpacts > lightImpacts && heavyImpacts > 50,
                              "Rain must increase impacts and avoid repeating the fixed pattern: " + lightImpacts + ", " + heavyImpacts);
                    } finally {
                        laterLight.dispose(); laterHeavy.dispose();
                    }
                } finally {
                    light.dispose(); moderate.dispose(); heavy.dispose();
                }
            }
        } finally {
            terrain.dispose();
        }
    }

    private static int[] patch(Pixmap pixels, BoardCamera camera) {
        int[] result = new int[128 * 128];
        Vector3 center = BoardGeometry.center(new Coords(6, 4), 0);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                Vector3 screen = camera.camera.project(center.cpy().add((x - 64) * 2, (y - 64) * 2, 0));
                result[y * 128 + x] = pixels.getPixel(Math.round(screen.x), Math.round(screen.y));
            }
        }
        return result;
    }

    private static int puddlePixels(int[] pixels) {
        int count = 0;
        for (int pixel : pixels) { if (((pixel >>> 8) & 255) - (pixel >>> 24) > 5) { count++; } }
        return count;
    }

    private static int changed(int[] first, int[] second) {
        int count = 0;
        for (int i = 0; i < first.length; i++) {
            if (Math.abs(((first[i] >>> 8) & 255) - ((second[i] >>> 8) & 255)) > 1) { count++; }
        }
        return count;
    }

    private static Pixmap frame(GpuTerrain terrain, BoardCamera camera, float wetness) {
        draw(terrain, camera, wetness);
        return Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    }

    private static void draw(GpuTerrain terrain, BoardCamera camera, float wetness) {
        terrain.setWetness(wetness);
        ScreenUtils.clear(0.08f, 0.10f, 0.12f, 1, true);
        terrain.render(camera.camera, false);
        terrain.renderTransparent(camera.camera);
    }

    private static String measure(GpuTerrain terrain, BoardCamera camera) {
        double[][] samples = new double[2][24];
        for (int i = -8; i < 24; i++) {
            for (int wet = 0; wet < 2; wet++) {
                Gdx.gl.glFinish();
                long start = System.nanoTime();
                draw(terrain, camera, wet);
                Gdx.gl.glFinish();
                if (i >= 0) { samples[wet][i] = (System.nanoTime() - start) / 1e6; }
            }
        }
        Arrays.sort(samples[0]);
        Arrays.sort(samples[1]);
        return "dry=" + samples[0][12] + " ms, rain=" + samples[1][12] + " ms";
    }

    /** Subtract the dry frame at each time to exclude the water texture's own motion. */
    private static int animatedRain(Pixmap dry, Pixmap wet, Pixmap laterDry, Pixmap laterWet,
          BoardCamera camera, Coords target) {
        int changed = 0;
        for (int y = -24; y <= 24; y++) {
            for (int x = -24; x <= 24; x++) {
                Vector3 screen = camera.camera.project(BoardGeometry.center(target, 0).add(x, y, 0));
                int px = (int) screen.x, py = (int) screen.y;
                if (px < 0 || py < 0 || px >= dry.getWidth() || py >= dry.getHeight()) { continue; }
                int a = dry.getPixel(px, py), b = wet.getPixel(px, py);
                int c = laterDry.getPixel(px, py), d = laterWet.getPixel(px, py);
                int difference = 0;
                for (int shift : new int[] { 8, 16, 24 }) {
                    difference += Math.abs(((b >>> shift & 255) - (a >>> shift & 255))
                          - ((d >>> shift & 255) - (c >>> shift & 255)));
                }
                if (difference > 2) { changed++; }
            }
        }
        return changed;
    }

    private static void save(Pixmap pixels, String name) {
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        PixmapIO.writePNG(Gdx.files.absolute(new File(output, name + ".png").getAbsolutePath()), pixels);
    }
}
