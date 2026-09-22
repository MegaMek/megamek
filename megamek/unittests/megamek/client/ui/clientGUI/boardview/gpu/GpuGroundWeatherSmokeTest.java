/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.BufferUtils;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Native ground-weather appearance, height, advection and render cost. */
@Tag("on-demand")
class GpuGroundWeatherSmokeTest {
    private final File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));

    @Test
    void groundWeatherUsesTheExistingAtmosphereStages() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    checkWeather();
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) { throw new AssertionError(failure.get()); }
    }

    private void checkWeather() throws Exception {
        assertTrue(output.isDirectory() || output.mkdirs());
        Gdx.graphics.setVSync(false);
        BoardScene scene = scene(0, -1);
        BoardCamera camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.setIsometric(true);
        camera.fit(scene);
        GpuAtmosphere atmosphere = new GpuAtmosphere();
        GpuTerrain terrain = new GpuTerrain();
        try {
            terrain.update(scene);
            checkColdStart(terrain, camera, scene);
            measureCost(atmosphere, terrain, camera, scene);
            checkAppearance(atmosphere, terrain, camera, scene);
            checkHeightAndBaseline(atmosphere, terrain, camera);
            checkDesertAndWind(atmosphere, terrain, camera, scene);
            checkVolumeAndBase(atmosphere, terrain, camera, scene);
            checkSandOpacity(atmosphere, terrain, camera);
            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
        } finally {
            atmosphere.dispose();
            terrain.dispose();
        }
    }

    private void checkSandOpacity(GpuAtmosphere atmosphere, GpuTerrain terrain, BoardCamera camera) {
        camera.setIsometric(false);
        double clearContrast = 0, dustContrast = 0;
        var maximum = new BoardAtmosphere.Settings(13, 0, 0, 8, 0, 0,
              new BoardAtmosphere.Effects(0, 0, 0, 1, 0, 0, 0));
        for (int gray : new int[] { 85, 160 }) {
            BoardScene scene = scene(0, -1, gray);
            terrain.update(scene);
            camera.fit(scene);
            Vector3 point = camera.camera.project(BoardGeometry.center(new Coords(4, 4), 0));
            Pixmap clear = frame(atmosphere, terrain, camera, scene, BoardAtmosphere.DEFAULTS, 0);
            Pixmap dusty = frame(atmosphere, terrain, camera, scene, maximum, 0);
            try {
                int sign = gray == 85 ? -1 : 1;
                clearContrast += sign * linearLuminance(clear.getPixel((int) point.x, (int) point.y));
                dustContrast += sign * linearLuminance(dusty.getPixel((int) point.x, (int) point.y));
            } finally {
                clear.dispose();
                dusty.dispose();
            }
        }
        assertEquals(1 - GpuAtmosphere.MAX_SAND_OPACITY, dustContrast / clearContrast, 0.025,
              "Maximum sand must retain at least 75% of scene contrast before display conversion");
    }

    private static double linearLuminance(int pixel) {
        return 0.2126 * Math.pow((pixel >>> 24) / 255.0, 2.2)
              + 0.7152 * Math.pow((pixel >>> 16 & 255) / 255.0, 2.2)
              + 0.0722 * Math.pow((pixel >>> 8 & 255) / 255.0, 2.2);
    }

    private void checkVolumeAndBase(GpuAtmosphere atmosphere, GpuTerrain terrain, BoardCamera camera, BoardScene scene) {
        terrain.update(scene);
        camera.setIsometric(false);
        camera.orbit(35, 75);
        camera.fit(scene);
        atmosphere.setOptions(GpuAtmosphere.Options.DEFAULTS);
        Pixmap clear = frame(atmosphere, terrain, camera, scene, BoardAtmosphere.DEFAULTS, 0);
        var depth = BufferUtils.newFloatBuffer(clear.getWidth() * clear.getHeight());
        Gdx.gl.glReadPixels(0, 0, clear.getWidth(), clear.getHeight(), GL20.GL_DEPTH_COMPONENT, GL20.GL_FLOAT, depth);
        try {
            for (boolean dusty : new boolean[] { true, false }) {
                var settings = new BoardAtmosphere.Settings(13, 0, dusty ? 0 : 0.6f, 2.5f, 0, 0,
                      new BoardAtmosphere.Effects(0, 0, 0, dusty ? 1 : 0, 0, 0.4f, 60));
                Pixmap weather = frame(atmosphere, terrain, camera, scene, settings, 0);
                try {
                    int air = 0, base = 0, changedBase = 0;
                    Vector3 point = new Vector3();
                    for (int y = 2; y < clear.getHeight() - 2; y += 2) {
                        for (int x = 2; x < clear.getWidth() - 2; x += 2) {
                            float z = depth.get(y * clear.getWidth() + x);
                            int difference = rgbDifference(clear.getPixel(x, y), weather.getPixel(x, y));
                            if (z >= 0.99999f) {
                                if (difference > 4) { air++; }
                            } else {
                                point.set((x + 0.5f) / clear.getWidth() * 2 - 1, (y + 0.5f) / clear.getHeight() * 2 - 1,
                                      z * 2 - 1).prj(camera.camera.invProjectionView);
                                if (point.z < -BoardGeometry.LEVEL * 0.2f) {
                                    base++;
                                    if (difference > 4) { changedBase++; }
                                }
                            }
                        }
                    }
                    assertTrue(air > 300, "Weather must have visible volume above the board silhouette: " + air);
                    assertTrue(base > 300, "The low-angle fixture must expose the solid base");
                    assertTrue(changedBase < base * 0.02, "Weather cannot paint the solid map base: " + changedBase + "/" + base);
                    save(weather, dusty ? "ground-sand-volume" : "ground-fog-volume");
                } finally {
                    weather.dispose();
                }
            }
            save(clear, "ground-volume-clear");
        } finally {
            clear.dispose();
        }
    }

    private void checkColdStart(GpuTerrain terrain, BoardCamera camera, BoardScene scene) {
        for (boolean dusty : new boolean[] { true, false }) {
            GpuAtmosphere fresh = new GpuAtmosphere();
            var settings = new BoardAtmosphere.Settings(13, 0, dusty ? 0 : 0.3f, 2.5f, 0, 0,
                  new BoardAtmosphere.Effects(0, 0, 0, dusty ? 0.6f : 0, 0, 0, 0));
            Pixmap first = frame(fresh, terrain, camera, scene, settings, 0);
            Pixmap next = frame(fresh, terrain, camera, scene, settings, 0);
            try {
                assertEquals(first.getPixels(), next.getPixels(),
                      "Lazy noise creation must preserve the bound scene/depth textures: " + (dusty ? "sand" : "fog"));
            } finally {
                first.dispose();
                next.dispose();
                fresh.dispose();
            }
        }
    }

    private void checkAppearance(GpuAtmosphere atmosphere, GpuTerrain terrain, BoardCamera camera, BoardScene scene) {
        var sand = new BoardAtmosphere.Effects(0, 0, 0, 1, 0, 0.4f, 60);
        var calm = BoardAtmosphere.Effects.NONE;
        var sandSettings = new BoardAtmosphere.Settings(13, 0, 0, 2.5f, 0, 0, sand);
        var fogSettings = new BoardAtmosphere.Settings(13, 0, 0.35f, 2.5f, 0, 0, calm);
        GLProfiler profiler = new GLProfiler(Gdx.graphics);
        profiler.enable();
        try {
            for (boolean tilted : new boolean[] { false, true }) {
                camera.setIsometric(tilted);
                camera.fit(scene);
                String view = tilted ? "isometric" : "top";
                atmosphere.setOptions(GpuAtmosphere.Options.DEFAULTS);
                Pixmap clear = frame(atmosphere, terrain, camera, scene, BoardAtmosphere.DEFAULTS, 0);
                capture(atmosphere, terrain, camera, scene, sandSettings, 0);
                profiler.reset();
                atmosphere.end(camera.camera, terrain, scene, 0);
                atmosphere.renderWeather(camera.camera, scene);
                assertEquals(1, profiler.getDrawCalls(), "Sand uses only the existing composite");
                Pixmap dusty = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
                var lighterSand = new BoardAtmosphere.Settings(13, 0, 0, 2.5f, 0, 0,
                      new BoardAtmosphere.Effects(0, 0, 0, 0.5f, 0, 0.4f, 60));
                Pixmap lightDust = frame(atmosphere, terrain, camera, scene, lighterSand, 0);
                atmosphere.setOptions(new GpuAtmosphere.Options(0.5f, false, 0.5f, 1, 0.35f, 0, 0));
                Pixmap uniform = frame(atmosphere, terrain, camera, scene, fogSettings, 0);
                atmosphere.setOptions(GpuAtmosphere.Options.DEFAULTS);
                Pixmap banks = frame(atmosphere, terrain, camera, scene, fogSettings, 0);
                Pixmap stationary = frame(atmosphere, terrain, camera, scene, fogSettings, 0);
                for (int i = 0; i < 100; i++) { capture(atmosphere, terrain, camera, scene, fogSettings, 0.1f); }
                profiler.reset();
                atmosphere.end(camera.camera, terrain, scene, 0);
                atmosphere.renderWeather(camera.camera, scene);
                assertEquals(2, profiler.getDrawCalls(), "Calm drift reuses the fog and composite passes");
                Pixmap drifting = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
                try {
                    assertTrue(difference(clear, dusty) > 1, "Sand must disturb visibility: " + view);
                    assertTrue(difference(clear, dusty) > difference(clear, lightDust) * 1.2,
                          "Sand strength must increase the effect without cutting grain coverage");
                    assertEquals(clear.getPixel(1, 1), dusty.getPixel(1, 1), "Sand cannot paint a box on the sky");
                    assertTrue(difference(uniform, banks) > 0.1, "Fog banks must vary across the ground: " + view);
                    assertEquals(banks.getPixels(), stationary.getPixels(), "Zero elapsed time cannot flicker");
                    assertTrue(difference(banks, drifting) > 0.02, "Calm fog banks must visibly drift: " + view);
                    save(clear, "ground-clear-" + view);
                    save(dusty, "ground-sand-" + view);
                    save(uniform, "ground-fog-uniform-" + view);
                    save(banks, "ground-fog-banks-" + view);
                    save(drifting, "ground-fog-calm-drift-" + view);
                } finally {
                    clear.dispose();
                    dusty.dispose();
                    lightDust.dispose();
                    uniform.dispose();
                    banks.dispose();
                    stationary.dispose();
                    drifting.dispose();
                }
            }
        } finally {
            profiler.disable();
        }
    }

    private void checkHeightAndBaseline(GpuAtmosphere atmosphere, GpuTerrain terrain, BoardCamera camera) {
        camera.setIsometric(false);
        atmosphere.setOptions(new GpuAtmosphere.Options(0, false, 0.5f, 1, 0, 0, 0));
        BoardScene base = scene(0, -1);
        var tiles = base.tiles().stream().map(tile -> new BoardScene.Tile(tile.coords(),
              tile.coords().getX() >= 3 && tile.coords().getX() <= 5 ? 8 : 0, -1, false, 0,
              tile.surface(), tile.ground(), null, null, List.of(), List.of())).toList();
        BoardScene ridge = new BoardScene(0, 9, 9, tiles, List.of(), List.of(), -1, "", List.of());
        terrain.update(ridge);
        camera.fit(ridge);
        var sand = new BoardAtmosphere.Effects(0, 0, 0, 1, 0, 0.4f, 60);
        Vector3 low = camera.camera.project(BoardGeometry.center(new Coords(1, 4), 0));
        Vector3 high = camera.camera.project(BoardGeometry.center(new Coords(4, 4), 8));
        Pixmap clear = frame(atmosphere, terrain, camera, ridge, BoardAtmosphere.DEFAULTS, 0);
        try {
            for (boolean dusty : new boolean[] { true, false }) {
                var effect = dusty ? sand : BoardAtmosphere.Effects.NONE;
                var lowLayer = new BoardAtmosphere.Settings(13, 0, dusty ? 0 : 0.25f, 1, 0, 0, effect);
                var highLayer = new BoardAtmosphere.Settings(13, 0, dusty ? 0 : 0.25f, 8, 0, 0, effect);
                Pixmap shallow = frame(atmosphere, terrain, camera, ridge, lowLayer, 0);
                Pixmap deep = frame(atmosphere, terrain, camera, ridge, highLayer, 0);
                try {
                    assertTrue(patchDifference(clear, shallow, low) > patchDifference(clear, shallow, high) + 1,
                          "Elevated terrain must emerge above the " + (dusty ? "sand" : "fog") + " layer");
                    assertTrue(patchDifference(clear, deep, high) > patchDifference(clear, shallow, high) + 1,
                          "The shared height control must raise " + (dusty ? "sand" : "fog"));
                    save(shallow, dusty ? "ground-sand-low-ridge" : "ground-fog-low-ridge");
                    save(deep, dusty ? "ground-sand-high-ridge" : "ground-fog-high-ridge");
                } finally {
                    shallow.dispose();
                    deep.dispose();
                }
            }
        } finally {
            clear.dispose();
        }
        // Keep the captured terrain identical while changing only the weather snapshot's water depth.
        // Water's rendering is unrelated to the atmospheric baseline; its recess must never lower the layer.
        var weather = new BoardAtmosphere.Settings(13, 0, 0.25f, 2.5f, 0, 0, sand);
        for (int level : new int[] { -3, 0, 4 }) {
            BoardScene dry = scene(level, -1), water = scene(level, 20);
            terrain.update(dry);
            camera.fit(dry);
            Pixmap reference = frame(atmosphere, terrain, camera, dry, weather, 0);
            Pixmap deepWater = frame(atmosphere, terrain, camera, water, weather, 0);
            try {
                assertEquals(reference.getPixels(), deepWater.getPixels(), "Water depth cannot lower fog/sand at level " + level);
            } finally {
                reference.dispose();
                deepWater.dispose();
            }
        }
        atmosphere.setOptions(GpuAtmosphere.Options.DEFAULTS);
    }

    private void checkDesertAndWind(GpuAtmosphere atmosphere, GpuTerrain terrain, BoardCamera camera, BoardScene original)
          throws Exception {
        var sand = new BoardAtmosphere.Effects(0, 0, 0, 0.6f, 0, 0.7f, 90);
        var wind = new BoardAtmosphere.Effects(0, 0, 0, 0, 0, 0.7f, 90);
        for (String texture : List.of("hq_boring/sand_0.png", "desert/beige_plains_0.gif")) {
            var pixels = new BoardScene.Pixels(ImageIO.read(new File("data/images/hexes", texture)));
            var tiles = original.tiles().stream().map(tile -> new BoardScene.Tile(tile.coords(), 0, -1, false, 0,
                  BoardScene.Surface.SAND, pixels, null, null, List.of(), List.of())).toList();
            BoardScene desert = new BoardScene(0, 9, 9, tiles, List.of(), List.of(), -1, "", List.of());
            terrain.update(desert);
            for (boolean tilted : new boolean[] { false, true }) {
                camera.setIsometric(tilted);
                camera.fit(desert);
                for (float zoom : new float[] { 1, 1.8f }) {
                    camera.fit(desert);
                    camera.zoom(zoom);
                    Pixmap clear = frame(atmosphere, terrain, camera, desert, BoardAtmosphere.DEFAULTS, 0);
                    for (boolean dusty : new boolean[] { true, false }) {
                        var settings = new BoardAtmosphere.Settings(13, 0, dusty ? 0 : 0.35f, 2.5f, 0, 0, dusty ? sand : wind);
                        Pixmap first = frame(atmosphere, terrain, camera, desert, settings, 0);
                        for (int step = 0; step < 10; step++) {
                            capture(atmosphere, terrain, camera, desert, settings, 0.1f);
                            atmosphere.end(camera.camera, terrain, desert, 0);
                        }
                        Pixmap next = frame(atmosphere, terrain, camera, desert, settings, 0);
                        try {
                            assertTrue(difference(clear, first) > 0.1, "Weather must remain visible over " + texture);
                            assertTrue(difference(first, next) > 0.02, "Wind must move the rendered " + (dusty ? "sand" : "fog"));
                            save(first, "ground-" + (dusty ? "sand" : "fog") + (texture.startsWith("hq") ? "-hq" : "-beige")
                                  + (tilted ? "-iso-" : "-top-") + zoom);
                        } finally {
                            first.dispose();
                            next.dispose();
                        }
                    }
                    clear.dispose();
                }
            }
        }
    }

    private static double patchDifference(Pixmap first, Pixmap next, Vector3 point) {
        long total = 0;
        for (int y = (int) point.y - 6; y <= (int) point.y + 6; y++) {
            for (int x = (int) point.x - 6; x <= (int) point.x + 6; x++) {
                total += rgbDifference(first.getPixel(x, y), next.getPixel(x, y));
            }
        }
        return total / (3.0 * 13 * 13);
    }

    private void save(Pixmap pixels, String name) {
        PixmapIO.writePNG(Gdx.files.absolute(new File(output, name + ".png").getAbsolutePath()), pixels, -1, true);
    }

    private static double difference(Pixmap first, Pixmap next) {
        long difference = 0;
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                difference += rgbDifference(first.getPixel(x, y), next.getPixel(x, y));
            }
        }
        return (double) difference / (3 * first.getWidth() * first.getHeight());
    }

    private static int rgbDifference(int a, int b) {
        return Math.abs((a >>> 24) - (b >>> 24)) + Math.abs((a >>> 16 & 255) - (b >>> 16 & 255))
              + Math.abs((a >>> 8 & 255) - (b >>> 8 & 255));
    }

    private void measureCost(GpuAtmosphere atmosphere, GpuTerrain terrain, BoardCamera camera, BoardScene scene)
          throws Exception {
        StringBuilder report = new StringBuilder("Renderer: " + Gdx.gl.glGetString(GL20.GL_RENDERER)
              + "\n1280x800 gray 9x9 board, isometric; atmosphere composite/scattering plus weather draws only; "
              + "16 warmups, 40 interleaved samples per mode; glFinish outside GPU timestamps\n");
        String[] names = { "Clear", "Maximum sand", "Fog" };
        var sand = new BoardAtmosphere.Effects(0, 0, 0, 1, 0, 0.4f, 60);
        var breeze = new BoardAtmosphere.Effects(0, 0, 0, 0, 0, 0.4f, 60);
        BoardAtmosphere.Settings[] modes = { BoardAtmosphere.DEFAULTS,
              new BoardAtmosphere.Settings(13, 0, 0, 2.5f, 0, 0, sand),
              new BoardAtmosphere.Settings(13, 0, 0.35f, 2.5f, 0, 0, breeze) };
        try (var timing = new GpuStageTimings()) {
            for (int frame = 0; frame < 56; frame++) {
                for (int mode = 0; mode < modes.length; mode++) {
                    capture(atmosphere, terrain, camera, scene, modes[mode], 1f / 60);
                    if (frame >= 16) {
                        timing.beginFrame();
                        timing.stage(names[mode]);
                    }
                    atmosphere.end(camera.camera, terrain, scene, 0);
                    atmosphere.renderWeather(camera.camera, scene);
                    if (frame >= 16) { timing.stage(null); }
                    Gdx.gl.glFinish();
                }
            }
            timing.appendReport(report, "Scene capture/UI excluded; camera and settings held fixed per mode");
        }
        Files.writeString(new File(output, "ground-weather-timing.txt").toPath(), report);
    }

    private static void capture(GpuAtmosphere atmosphere, GpuTerrain terrain, BoardCamera camera, BoardScene scene,
          BoardAtmosphere.Settings settings, float delta) {
        atmosphere.configure(settings);
        atmosphere.updateLight(camera.camera);
        terrain.setAtmosphere(atmosphere.lighting());
        atmosphere.prepareClouds(terrain, scene, delta);
        ScreenUtils.clear(0, 0, 0, 1, true);
        atmosphere.begin((int) camera.camera.viewportWidth, (int) camera.camera.viewportHeight, delta);
        terrain.render(camera.camera, false);
    }

    private static Pixmap frame(GpuAtmosphere atmosphere, GpuTerrain terrain, BoardCamera camera, BoardScene scene,
          BoardAtmosphere.Settings settings, float delta) {
        capture(atmosphere, terrain, camera, scene, settings, delta);
        atmosphere.end(camera.camera, terrain, scene, 0);
        atmosphere.renderWeather(camera.camera, scene);
        return Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    }

    private static BoardScene scene(int level, int waterDepth) {
        return scene(level, waterDepth, 145);
    }

    private static BoardScene scene(int level, int waterDepth, int gray) {
        BufferedImage image = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setColor(new java.awt.Color(gray, gray, gray));
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        var pixels = new BoardScene.Pixels(image);
        var tiles = new ArrayList<BoardScene.Tile>();
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), level, waterDepth, false, 0,
                      BoardScene.Surface.GRASS, pixels, null, null, List.of(), List.of()));
            }
        }
        return new BoardScene(0, 9, 9, tiles, List.of(), List.of(), -1, "", List.of());
    }
}
