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

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Same-camera A/B measurements and actual rain/splash pixels, rather than assuming shader arithmetic is faster. */
@Tag("on-demand")
class GpuWaterShaderSmokeTest {
    @Test
    void proceduralWaterRetainsItsPaletteShowsRainAndSplashesAndMeasuresBothColorPaths() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                GpuTerrain gif = new GpuTerrain(true, false), procedural = new GpuTerrain(true, true);
                try {
                    Gdx.graphics.setVSync(false);
                    BoardScene scene = scene(false, 0);
                    for (GpuTerrain terrain : List.of(gif, procedural)) {
                        terrain.update(scene);
                        terrain.setAtmosphere(BoardAtmosphere.lighting(new BoardAtmosphere.Settings(13, 0.8f, 0, 2.5f, 0, 0,
                              new BoardAtmosphere.Effects(0, 0, 0, 0, 0, 0, 0))));
                    }
                    BoardCamera camera = new BoardCamera();
                    camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    StringBuilder report = new StringBuilder(Gdx.gl.glGetString(GL20.GL_RENDERER))
                          .append("\nSynchronized animated terrain-frame medians, 40 interleaved samples after warmup; includes CPU updates/submission.\n");
                    for (boolean isometric : new boolean[] { false, true }) {
                        camera.setIsometric(isometric);
                        camera.fit(scene);
                        camera.camera.zoom *= 0.65f;
                        camera.camera.update();
                        gif.renderShadows(List.of());
                        procedural.renderShadows(List.of());
                        for (int rain = 0; rain <= 1; rain++) {
                            report.append(isometric ? "isometric" : "top").append(", rain=").append(rain).append(": ")
                                  .append(measure(gif, procedural, camera, rain)).append('\n');
                        }
                        Pixmap authored = frame(gif, camera, 0), dry = frame(procedural, camera, 0);
                        Pixmap light = frame(procedural, camera, 0.2f), wet = frame(procedural, camera, 1);
                        try {
                            save(authored, "water-color-gif-" + isometric);
                            save(dry, "water-color-procedural-" + isometric);
                            save(wet, "water-downpour-" + isometric);
                            int[] original = sample(authored, camera, new Coords(5, 4), 60);
                            int[] replacement = sample(dry, camera, new Coords(5, 4), 60);
                            for (int shift : new int[] { 8, 16, 24 }) {
                                assertEquals(mean(original, shift), mean(replacement, shift), 24,
                                      "Procedural water should retain the authored depth-two palette");
                            }
                            int[] drizzle = sample(light, camera, new Coords(5, 4), 60);
                            int[] downpour = sample(wet, camera, new Coords(5, 4), 60);
                            int lightPixels = differences(replacement, drizzle, 6);
                            int wetPixels = differences(replacement, downpour, 6);
                            assertTrue(wetPixels > replacement.length / 6 && wetPixels > lightPixels * 1.5,
                                  "Rain must be clearly visible and denser at downpour: " + lightPixels + ", " + wetPixels);
                            procedural.animate(0.31f, List.of());
                            Pixmap later = frame(procedural, camera, 1);
                            try { save(later, "water-downpour-later-" + isometric); } finally { later.dispose(); }
                        } finally {
                            authored.dispose(); dry.dispose(); light.dispose(); wet.dispose();
                        }
                    }
                    File output = output();
                    Files.writeString(new File(output, "water-color-timing.txt").toPath(), report);
                    checkSplash(procedural, camera, "procedural");
                    checkSplash(gif, camera, "gif");
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    gif.dispose(); procedural.dispose();
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) { throw new AssertionError(failure.get()); }
    }

    private static void checkSplash(GpuTerrain terrain, BoardCamera camera, String mode) {
        BoardScene waterfall = scene(true, 3);
        terrain.update(waterfall);
        // Isolate foam from the extra shadow cast by the raised upstream bank in the live-edit comparison.
        terrain.setAtmosphere(new BoardAtmosphere.Lighting(new Vector3(0, 0, -1), Color.BLACK, Color.WHITE,
              Color.BLACK, Color.GRAY, Color.GRAY, Color.WHITE, 1, 0, false));
        camera.setIsometric(true);
        camera.fit(waterfall);
        camera.center(BoardGeometry.center(new Coords(4, 5), 0));
        camera.camera.zoom *= 0.55f;
        camera.camera.update();
        terrain.renderShadows(List.of());
        Pixmap splash = frame(terrain, camera, 0);
        try {
            save(splash, "waterfall-splash-" + mode);
            Vector3 base = BoardGeometry.center(new Coords(4, 5), 0).add(0, BoardGeometry.HEIGHT * 0.36f, 0);
            base.z = BoardGeometry.waterZ(waterfall.tile(new Coords(4, 5)));
            int[] before = sample(splash, camera, base, 9);
            terrain.animate(0.2f, List.of());
            Pixmap animated = frame(terrain, camera, 0);
            try {
                save(animated, "waterfall-splash-later-" + mode);
                assertTrue(differences(before, sample(animated, camera, base, 9), 6) > 20,
                      "Impact foam must animate at the waterfall base without rain");
            } finally { animated.dispose(); }
            // Removing the height difference must also remove the receiving surface's impact effect.
            terrain.update(scene(true, 0));
            terrain.renderShadows(List.of());
            Pixmap flat = frame(terrain, camera, 0);
            try {
                int[] after = sample(flat, camera, base, 9);
                assertTrue(mean(before, 8) > mean(after, 8) + 10,
                      "An ordinary flat connection must not retain bright impact foam after a live edit: "
                            + mean(before, 8) + " -> " + mean(after, 8));
            } finally { flat.dispose(); }
        } finally { splash.dispose(); }
    }

    private static String measure(GpuTerrain gif, GpuTerrain procedural, BoardCamera camera, float rain) {
        double[][] times = new double[2][40];
        GpuTerrain[] terrain = { gif, procedural };
        for (int sample = -12; sample < 40; sample++) {
            for (int order = 0; order < 2; order++) {
                int mode = (sample + order) & 1;
                Gdx.gl.glFinish();
                long start = System.nanoTime();
                terrain[mode].animate(1f / 60, List.of());
                draw(terrain[mode], camera, rain);
                Gdx.gl.glFinish();
                if (sample >= 0) { times[mode][sample] = (System.nanoTime() - start) / 1e6; }
            }
        }
        Arrays.sort(times[0]); Arrays.sort(times[1]);
        return "GIF=" + times[0][20] + " ms, procedural=" + times[1][20] + " ms";
    }

    private static Pixmap frame(GpuTerrain terrain, BoardCamera camera, float rain) {
        draw(terrain, camera, rain);
        return Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    }

    private static void draw(GpuTerrain terrain, BoardCamera camera, float rain) {
        terrain.setWetness(rain);
        ScreenUtils.clear(0.04f, 0.06f, 0.08f, 1, true);
        terrain.render(camera.camera, false);
        terrain.renderTransparent(camera.camera);
    }

    private static int[] sample(Pixmap image, BoardCamera camera, Coords coords, int radius) {
        return sample(image, camera, BoardGeometry.center(coords, 0).add(0, 0, -BoardGeometry.HEX_SCALE), radius);
    }

    private static int[] sample(Pixmap image, BoardCamera camera, Vector3 center, int radius) {
        int side = radius * 2 + 1;
        int[] result = new int[side * side];
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                Vector3 screen = camera.camera.project(new Vector3(center).add(x, y, 0));
                result[(y + radius) * side + x + radius] = image.getPixel(Math.round(screen.x), Math.round(screen.y));
            }
        }
        return result;
    }

    private static double mean(int[] image, int shift) {
        long sum = 0;
        for (int pixel : image) { sum += (pixel >>> shift) & 255; }
        return sum / (double) image.length;
    }

    private static int differences(int[] first, int[] second, int threshold) {
        int count = 0;
        for (int i = 0; i < first.length; i++) {
            int difference = 0;
            for (int shift : new int[] { 8, 16, 24 }) { difference += Math.abs(((first[i] >>> shift) & 255) - ((second[i] >>> shift) & 255)); }
            if (difference > threshold) { count++; }
        }
        return count;
    }

    private static File output() {
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        return output;
    }

    private static void save(Pixmap image, String name) {
        PixmapIO.writePNG(Gdx.files.absolute(new File(output(), name + ".png").getAbsolutePath()), image);
    }

    private static BoardScene scene(boolean river, int drop) {
        BufferedImage gray = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 72; y++) {
            for (int x = 0; x < 84; x++) { gray.setRGB(x, y, 0xff888888); }
        }
        BoardScene.Pixels pixels = new BoardScene.Pixels(gray);
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 10; y++) {
                boolean water = !river || x == 4;
                tiles.add(new BoardScene.Tile(new Coords(x, y), river && y < 5 ? drop : 0, water ? 2 : -1, false,
                      0, BoardScene.Surface.ROCK, pixels, null, null, List.of(), List.of()));
            }
        }
        return new BoardScene(0, 12, 10, tiles, List.of(), List.of(), -1, "", List.of());
    }
}
