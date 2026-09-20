/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.common.board.Coords;
import megamek.common.planetaryConditions.Fog;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.Weather;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Actual GL shaders, depth reconstruction, live Scene2D controls, and framebuffer resizing. */
@Tag("on-demand")
class GpuAtmosphereSmokeTest {
    private final File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));

    @Test
    void fogStopsAtGeometryAndTimeChangesLightingWithoutReallocatingShadows() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    checkRendering();
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    private void checkRendering() throws Exception {
        assertTrue(output.isDirectory() || output.mkdirs());
        BufferedImage image = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setColor(new java.awt.Color(155, 155, 155));
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        BoardScene.Pixels pixels = new BoardScene.Pixels(image);
        BufferedImage darkImage = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        graphics = darkImage.createGraphics();
        graphics.setColor(new java.awt.Color(65, 65, 65));
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        BoardScene.Pixels darkPixels = new BoardScene.Pixels(darkImage);
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 7; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), 0, -1, false, 0, BoardScene.Surface.GRASS,
                      x == 1 && y == 2 ? darkPixels : pixels, null, null, List.of(), List.of()));
            }
        }
        BoardScene scene = new BoardScene(0, 7, 7, tiles, List.of(), List.of(), -1, "", List.of());
        GpuTerrain terrain = new GpuTerrain();
        GpuAtmosphere atmosphere = new GpuAtmosphere();
        ModelBatch batch = new ModelBatch();
        var model = new ModelBuilder().createBox(36, 36, 108,
              new Material(ColorAttribute.createDiffuse(new Color(0.61f, 0.61f, 0.61f, 1))),
              VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        ModelInstance tower = new ModelInstance(model);
        Vector3 center = BoardGeometry.center(new Coords(3, 3), 0);
        tower.transform.setToTranslation(center.x, center.y, 54);
        BoardCamera camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.setIsometric(false);
        camera.fit(scene);
        terrain.update(scene);
        try {
            BoardAtmosphere.Settings clear = new BoardAtmosphere.Settings(12, 0, 0, 1.5f, 0, 0);
            draw(atmosphere, terrain, batch, tower, camera, scene, clear);
            var shadow = terrain.environment().shadowMap;
            Vector3 ground = BoardGeometry.center(new Coords(1, 3), 0);
            Vector3 darkGround = BoardGeometry.center(new Coords(1, 2), 0);
            Vector3 roof = new Vector3(center).add(0, 0, 108);
            Color clearGround = sample(camera, ground);
            Color clearRoof = sample(camera, roof);
            float clearContrast = linearLuminance(clearGround) - linearLuminance(sample(camera, darkGround));
            GpuBoardTestUi.capture(new File(output, "atmosphere-noon.png"));
            draw(atmosphere, terrain, batch, tower, camera, scene,
                  new BoardAtmosphere.Settings(12, 0, 0.8f, 1.5f, 0, 0));
            Color fogGround = sample(camera, ground);
            Color fogRoof = sample(camera, roof);
            GpuBoardTestUi.capture(new File(output, "atmosphere-depth-fog.png"));
            assertTrue(difference(clearGround, fogGround) > 0.02f, "Ground fog must subtly affect low terrain");
            assertTrue(difference(clearGround, fogGround) > difference(clearRoof, fogRoof) * 2,
                  "Camera depth and exponential height fog must leave the raised roof clearer: ground "
                        + clearGround + " -> " + fogGround + ", roof " + clearRoof + " -> " + fogRoof);
            draw(atmosphere, terrain, batch, tower, camera, scene,
                  new BoardAtmosphere.Settings(12, 0, 1, 8, 1, 0));
            float fogContrast = linearLuminance(sample(camera, ground)) - linearLuminance(sample(camera, darkGround));
            assertEquals(1 - BoardAtmosphere.MAX_FOG_OPACITY, fogContrast / clearContrast, 0.025f,
                  "Maximum fog and haze must retain the configured share of scene contrast in linear light");
            GpuBoardTestUi.capture(new File(output, "atmosphere-maximum-fog.png"));
            draw(atmosphere, terrain, batch, tower, camera, scene,
                  new BoardAtmosphere.Settings(0, 0, 0, 1.5f, 0, 0));
            Color night = sample(camera, ground);
            assertTrue(luminance(clearGround) > luminance(night) * 1.25f, "Neutral daylight must be visibly brighter than night");
            assertTrue(luminance(night) > 0.4f, "Full moon lighting must keep the ground readable");
            assertTrue(night.b > night.r, "Moonlit gray ground must have a blue tint");
            assertSame(shadow, terrain.environment().shadowMap, "Changing time must reuse the shadow framebuffer");
            GpuBoardTestUi.capture(new File(output, "atmosphere-night.png"));
            for (float hour : new float[] { 4, 5, 5.5f, 6, 6.5f, 7, 17, 17.5f, 18, 18.5f, 19, 20 }) {
                draw(atmosphere, terrain, batch, tower, camera, scene,
                      new BoardAtmosphere.Settings(hour, 1, 0, 1.5f, 0, 0));
                Color twilight = sample(camera, ground);
                assertTrue(luminance(twilight) > 0.4f,
                      "The rendered board must not go dark during twilight at " + hour + ": " + twilight);
            }
            checkParticles(atmosphere, terrain, batch, tower, camera, scene);
            for (boolean isometric : List.of(false, true)) {
                camera.setIsometric(isometric);
                camera.fit(scene);
                for (int size : new int[] { 900, 2043, 1280 }) {
                    camera.resize(size, 600);
                    draw(atmosphere, terrain, batch, tower, camera, scene, BoardAtmosphere.DEFAULTS);
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                }
            }
            camera.resize(1280, 800);
            camera.setIsometric(true);
            camera.fit(scene);
            StringBuilder timing = new StringBuilder("Renderer: " + Gdx.gl.glGetString(GL20.GL_RENDERER)
                  + "\n1280x800 synthetic 7x7 board and tower; warmed, synchronous GL completion; no vsync wait\n");
            String[] modes = { "Clear", "Maximum fog/haze", "Downpour",
                  "Maximum snow", "Maximum hail", "Maximum sand", "All precipitation at maximum" };
            for (int mode = 0; mode < modes.length; mode++) {
                var effects = new BoardAtmosphere.Effects(mode == 2 || mode == 6 ? 1 : 0,
                      mode == 3 || mode == 6 ? 1 : 0, mode == 4 || mode == 6 ? 1 : 0,
                      mode == 5 || mode == 6 ? 1 : 0, 0, 0.4f, 60);
                var settings = new BoardAtmosphere.Settings(13, 0, mode == 1 ? 1 : 0, 8,
                      mode == 1 ? 1 : 0, 0, effects);
                List<Double> millis = new ArrayList<>();
                for (int frame = 0; frame < 45; frame++) {
                    long start = System.nanoTime();
                    draw(atmosphere, terrain, batch, tower, camera, scene, settings);
                    Gdx.gl.glFinish();
                    if (frame >= 15) {
                        millis.add((System.nanoTime() - start) / 1_000_000.0);
                    }
                }
                millis.sort(Double::compareTo);
                timing.append(String.format(Locale.ROOT, "%s: median %.3f ms, p95 %.3f ms%n",
                      modes[mode],
                      millis.get(millis.size() / 2), millis.get((int) (millis.size() * 0.95))));
            }
            Files.writeString(new File(output, "atmosphere-timing.txt").toPath(), timing);
        } finally {
            atmosphere.dispose();
            terrain.dispose();
            batch.dispose();
            model.dispose();
        }
    }

    private void draw(GpuAtmosphere atmosphere, GpuTerrain terrain, ModelBatch batch, ModelInstance tower,
          BoardCamera camera, BoardScene scene, BoardAtmosphere.Settings settings) {
        atmosphere.configure(settings);
        terrain.setAtmosphere(atmosphere.lighting());
        terrain.renderShadows(List.of(tower));
        ScreenUtils.clear(0, 0, 0, 1, true);
        atmosphere.begin((int) camera.camera.viewportWidth, (int) camera.camera.viewportHeight, 0.1f);
        terrain.render(camera.camera, false);
        batch.begin(camera.camera);
        batch.render(tower, terrain.environment());
        batch.end();
        atmosphere.end(camera.camera, terrain, List.of(tower), scene, 0);
        atmosphere.restoreDepth(camera.camera, terrain, List.of(tower));
        atmosphere.renderWeather(camera.camera, scene);
    }

    private void checkParticles(GpuAtmosphere atmosphere, GpuTerrain terrain, ModelBatch batch, ModelInstance tower,
          BoardCamera camera, BoardScene scene) throws IOException {
        camera.setIsometric(true);
        camera.fit(scene);
        draw(atmosphere, terrain, batch, tower, camera, scene, BoardAtmosphere.DEFAULTS);
        long clear = GpuBoardTestUi.capture(new File(output, "weather-clear.png"));
        String[] names = { "rain", "snow", "hail", "sand" };
        for (int kind = 0; kind < names.length; kind++) {
            var effects = new BoardAtmosphere.Effects(kind == 0 ? 1 : 0, kind == 1 ? 1 : 0,
                  kind == 2 ? 1 : 0, kind == 3 ? 1 : 0, 0, 0.4f, 60);
            var weather = new BoardAtmosphere.Settings(13, 0, 0, 2.5f, 0, 0, effects);
            draw(atmosphere, terrain, batch, tower, camera, scene, weather);
            long first = GpuBoardTestUi.capture(new File(output, "weather-" + names[kind] + ".png"));
            assertNotEquals(clear, first, names[kind] + " must be visible");
            draw(atmosphere, terrain, batch, tower, camera, scene, weather);
            assertNotEquals(first, GpuBoardTestUi.capture(new File(output, "weather-" + names[kind] + "-moving.png")),
                  names[kind] + " must move over time");
            draw(atmosphere, terrain, batch, tower, camera, scene, BoardAtmosphere.DEFAULTS);
            assertEquals(clear, GpuBoardTestUi.capture(new File(output, "weather-off.png")),
                  "Disabling weather must remove every particle immediately");
            atmosphere.configure(weather);
            Gdx.gl.glClearDepthf(0);
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
            atmosphere.renderWeather(camera.camera, scene);
            assertEquals(clear, GpuBoardTestUi.capture(new File(output, "weather-occluded.png")),
                  "Precipitation must respect opaque depth rather than painting through roofs");
            Gdx.gl.glClearDepthf(1);
            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
        }
        checkSandDensity(atmosphere, terrain, batch, tower, camera, scene);
        camera.setIsometric(false);
        camera.fit(scene);
        var calmRain = new BoardAtmosphere.Settings(13, 0, 0, 2.5f, 0, 0,
              new BoardAtmosphere.Effects(1, 0, 0, 0, 0, 0, 0));
        draw(atmosphere, terrain, batch, tower, camera, scene, calmRain);
        long overhead = GpuBoardTestUi.capture(new File(output, "weather-rain-top.png"));
        draw(atmosphere, terrain, batch, tower, camera, scene, calmRain);
        assertNotEquals(overhead, GpuBoardTestUi.capture(new File(output, "weather-rain-top-moving.png")),
              "Rain must remain animated when looking straight down without wind");
        var lightning = new BoardAtmosphere.Settings(13, 0, 0, 2.5f, 0, 0,
              new BoardAtmosphere.Effects(0, 0, 0, 0, 1, 0, 0));
        float darkest = 1, brightest = 0;
        for (int frame = 0; frame < 80; frame++) {
            draw(atmosphere, terrain, batch, tower, camera, scene, lightning);
            float brightness = luminance(sample(camera, BoardGeometry.center(new Coords(1, 3), 0)));
            darkest = Math.min(darkest, brightness);
            brightest = Math.max(brightest, brightness);
            if (frame == 4) {
                assertTrue(brightest > darkest * 1.2f, "Enabling lightning must visibly flash within half a second");
                GpuBoardTestUi.capture(new File(output, "weather-lightning.png"));
            }
        }
        assertTrue(brightest > darkest * 1.2f, "A lightning storm must provide clearly visible illumination");
        assertTrue(brightest < darkest * 1.5f, "Lightning must retain terrain detail instead of whitening the board");
        draw(atmosphere, terrain, batch, tower, camera, scene, BoardAtmosphere.DEFAULTS);
        assertEquals(darkest, luminance(sample(camera, BoardGeometry.center(new Coords(1, 3), 0))), 0.005f,
              "Disabling lightning must immediately restore normal exposure");
    }

    private void checkSandDensity(GpuAtmosphere atmosphere, GpuTerrain terrain, ModelBatch batch,
          ModelInstance tower, BoardCamera camera, BoardScene scene) throws IOException {
        GpuWeatherParticles particles = new GpuWeatherParticles();
        draw(atmosphere, terrain, batch, tower, camera, scene, BoardAtmosphere.DEFAULTS);
        Pixmap clear = ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        try {
            int previousCoverage = 0;
            for (float strength : new float[] { 0.5f, 0.6f, 1 }) {
                draw(atmosphere, terrain, batch, tower, camera, scene, BoardAtmosphere.DEFAULTS);
                particles.render(camera.camera, scene, new BoardAtmosphere.Effects(0, 0, 0, strength, 0, 0.4f, 60),
                      Color.WHITE, 3);
                Pixmap dusty = ScreenUtils.getFrameBufferPixmap(0, 0, clear.getWidth(), clear.getHeight());
                int coverage = 0;
                try {
                    coverage = changedPixels(clear, dusty, null, 12);
                } finally {
                    dusty.dispose();
                }
                assertTrue(coverage > clear.getWidth() * clear.getHeight() * 0.003,
                      "Even half-strength sand must remain visible as fine grains: " + coverage);
                assertTrue(coverage < clear.getWidth() * clear.getHeight() * 0.08,
                      "Sand must not blanket the board with broad opaque flakes: " + coverage);
                assertTrue(coverage > previousCoverage, "Increasing sand strength must increase visible coverage");
                previousCoverage = coverage;
                GpuBoardTestUi.capture(new File(output, "weather-sand-strength-" + Math.round(strength * 100) + ".png"));
            }
            checkSandContrast(atmosphere, terrain, batch, tower, camera, scene, particles);
            checkSandMotion(particles, scene);
        } finally {
            clear.dispose();
            particles.dispose();
        }
    }

    private void checkSandContrast(GpuAtmosphere atmosphere, GpuTerrain terrain, ModelBatch batch,
          ModelInstance tower, BoardCamera camera, BoardScene scene, GpuWeatherParticles particles) throws IOException {
        try {
            for (String texture : List.of("hq_boring/sand_0.png", "desert/beige_plains_0.gif")) {
                BoardScene.Pixels sand = new BoardScene.Pixels(ImageIO.read(new File("data/images/hexes", texture)));
                var tiles = scene.tiles().stream().map(tile -> new BoardScene.Tile(tile.coords(), 0, -1, false, 0,
                      BoardScene.Surface.SAND, sand, null, null, List.of(), List.of())).toList();
                BoardScene desert = new BoardScene(0, scene.width(), scene.height(), tiles,
                      List.of(), List.of(), -1, "", List.of());
                terrain.update(desert);
                for (boolean isometric : List.of(false, true)) {
                    camera.setIsometric(isometric);
                    for (float zoom : new float[] { 1, 1.8f }) {
                        camera.fit(desert);
                        camera.zoom(zoom);
                        draw(atmosphere, terrain, batch, tower, camera, desert, BoardAtmosphere.DEFAULTS);
                        Pixmap clear = ScreenUtils.getFrameBufferPixmap(0, 0,
                              Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                        particles.render(camera.camera, desert, new BoardAtmosphere.Effects(0, 0, 0, 0.5f, 0, 0.4f, 60),
                              atmosphere.lighting().ambient(), 3);
                        Pixmap dusty = ScreenUtils.getFrameBufferPixmap(0, 0, clear.getWidth(), clear.getHeight());
                        try {
                            // Measure only the interior terrain: grains over the blue backdrop do not count.
                            Coords[] corners = { new Coords(1, 1), new Coords(5, 1), new Coords(5, 5), new Coords(1, 5) };
                            float[] vertices = new float[8];
                            for (int index = 0; index < corners.length; index++) {
                                Vector3 point = camera.camera.project(BoardGeometry.center(corners[index], 0));
                                vertices[index * 2] = point.x;
                                vertices[index * 2 + 1] = point.y;
                            }
                            Polygon ground = new Polygon(vertices);
                            int visible = changedPixels(clear, dusty, ground, 45);
                            assertTrue(visible > Math.abs(ground.area()) * 0.015f,
                                  "Half-strength sand must contrast with " + texture + " at zoom " + zoom
                                        + " (isometric " + isometric + "): " + visible + " / " + Math.abs(ground.area()));
                            GpuBoardTestUi.capture(new File(output, "weather-sand-"
                                  + (texture.startsWith("hq_boring") ? "hq" : "beige")
                                  + (isometric ? "-iso-" : "-top-") + zoom + ".png"));
                        } finally {
                            clear.dispose();
                            dusty.dispose();
                        }
                    }
                }
            }
        } finally {
            terrain.update(scene);
            camera.setIsometric(true);
            camera.fit(scene);
        }
    }

    private static int changedPixels(Pixmap clear, Pixmap dusty, Polygon region, int threshold) {
        int coverage = 0;
        for (int y = 0; y < clear.getHeight(); y++) {
            for (int x = 0; x < clear.getWidth(); x++) {
                if (region != null && !region.contains(x + 0.5f, y + 0.5f)) {
                    continue;
                }
                int before = clear.getPixel(x, y), after = dusty.getPixel(x, y);
                int difference = Math.abs((before >>> 24) - (after >>> 24))
                      + Math.abs(((before >>> 16) & 255) - ((after >>> 16) & 255))
                      + Math.abs(((before >>> 8) & 255) - ((after >>> 8) & 255));
                if (difference > threshold) {
                    coverage++;
                }
            }
        }
        return coverage;
    }

    private void checkSandMotion(GpuWeatherParticles particles, BoardScene scene) {
        BoardCamera camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.setIsometric(false);
        camera.fit(scene);
        camera.camera.zoom = 1;
        camera.update();
        for (int direction : new int[] { 0, 90, 180, 270 }) {
            float calmDistance = 0;
            for (float wind : new float[] { 0, 1 }) {
                var effects = new BoardAtmosphere.Effects(0, 0, 0, 0.000001f, 0, wind, direction);
                Grain first = sandGrain(particles, camera, scene, effects, 3);
                Grain next = sandGrain(particles, camera, scene, effects, 3 + 1 / 60f);
                float dx = next.x() - first.x(), dy = next.y() - first.y();
                float along = direction == 0 ? dy : direction == 90 ? dx : direction == 180 ? -dy : -dx;
                float across = direction == 0 || direction == 180 ? dx : dy;
                assertTrue(along > 1.5f && along < 20,
                      "Sand must stream quickly in the selected direction even in calm wind: " + along);
                assertTrue(Math.abs(across) < 1.5f, "Small turbulent motion must not overwhelm the wind direction");
                if (wind == 0) {
                    calmDistance = along;
                } else {
                    assertTrue(along > calmDistance * 1.4f, "Increasing wind must visibly accelerate the sand");
                }
            }
        }
    }

    private record Grain(float x, float y) { }

    /** Measure one rendered grain, including its filtering and trail, instead of duplicating shader math. */
    private Grain sandGrain(GpuWeatherParticles particles, BoardCamera camera, BoardScene scene,
          BoardAtmosphere.Effects effects, float clock) {
        ScreenUtils.clear(0, 0, 0, 1, true);
        particles.render(camera.camera, scene, effects, Color.WHITE, clock);
        Pixmap frame = ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        try {
            float weight = 0, xSum = 0, ySum = 0;
            int minX = frame.getWidth(), minY = frame.getHeight(), maxX = 0, maxY = 0;
            for (int y = 0; y < frame.getHeight(); y++) {
                for (int x = 0; x < frame.getWidth(); x++) {
                    int red = frame.getPixel(x, y) >>> 24;
                    if (red > 2) {
                        weight += red;
                        xSum += red * x;
                        ySum += red * y;
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
            assertTrue(weight > 0, "A filtered sand grain must remain visible at " + clock + " with " + effects);
            assertTrue(maxX - minX < 6 && maxY - minY < 6,
                  "A grain and its motion trail must stay small at normal zoom");
            return new Grain(xSum / weight, ySum / weight);
        } finally {
            frame.dispose();
        }
    }

    private Color sample(BoardCamera camera, Vector3 point) {
        Vector3 projected = camera.camera.project(new Vector3(point), 0, 0,
              Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        Pixmap pixel = Pixmap.createFromFrameBuffer(Math.round(projected.x), Math.round(projected.y), 1, 1);
        try {
            return new Color(pixel.getPixel(0, 0));
        } finally {
            pixel.dispose();
        }
    }

    private static float difference(Color a, Color b) {
        return Math.abs(a.r - b.r) + Math.abs(a.g - b.g) + Math.abs(a.b - b.b);
    }

    private static float luminance(Color color) {
        return 0.2126f * color.r + 0.7152f * color.g + 0.0722f * color.b;
    }

    private static float linearLuminance(Color color) {
        return (float) (0.2126 * Math.pow(color.r, 2.2) + 0.7152 * Math.pow(color.g, 2.2)
              + 0.0722 * Math.pow(color.b, 2.2));
    }

    @Test
    void scenarioInitializesTheLiveControlsAndDefaultsRestoresItAfterWeatherOverrides() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                var conditions = fixture.game.getPlanetaryConditions();
                conditions.setLight(Light.FULL_MOON);
                conditions.setWeather(Weather.HEAVY_SNOW);
                conditions.setFog(Fog.FOG_HEAVY);
                fixture.source.refresh();
            });
            var initial = fixture.source.takeFrame().scenarioAtmosphere();
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                @Override
                public void render() {
                    try {
                        super.render();
                        if (frames() == 3) {
                            assertEquals(0, value("Time of day"));
                            assertEquals(1, value("Snow"));
                            assertEquals(0, value("Rain"));
                            assertEquals(1, value("Ground fog"));
                            assertEquals(1, value("Haze"));
                            assertEquals(initial.fogHeight(), value("Fog height"));
                            GpuBoardTestUi.capture(new File(output, "weather-scenario-night-snow.png"));
                            GpuBoardTestUi.click("tuning");
                        } else if (frames() == 5) {
                            GpuBoardTestUi.click("atmosphere-CLEAR");
                            assertEquals(0, value("Snow"));
                            assertEquals(0, value("Ground fog"));
                            assertEquals(0, value("Haze"));
                            Slider time = GpuBoardTestUi.stage().getRoot().findActor("Time of day");
                            time.setValue(13);
                            Actor rain = GpuBoardTestUi.stage().getRoot().findActor("weather-toggle-Rain");
                            ScrollPane scroll = GpuBoardTestUi.stage().getRoot().findActor("tuning-scroll");
                            scroll.scrollTo(rain.getX(), rain.getY(), rain.getWidth(), rain.getHeight(), false, true);
                            scroll.updateVisualScroll();
                        } else if (frames() == 7) {
                            GpuBoardTestUi.click("weather-toggle-Rain");
                            assertTrue(value("Rain") > 0);
                        } else if (frames() == 10) {
                            assertEquals(13, value("Time of day"), "Frame updates must preserve tuning overrides");
                            assertEquals(0, value("Snow"));
                            assertTrue(value("Rain") > 0);
                            GpuBoardTestUi.capture(new File(output, "weather-rain-controls.png"));
                            GpuBoardTestUi.click("weather-toggle-Rain");
                            assertEquals(0, value("Rain"));
                            GpuBoardTestUi.click("tuning-defaults");
                        } else if (frames() == 12) {
                            assertEquals(0, value("Time of day"), "Defaults restores scenario moonlight");
                            assertEquals(1, value("Snow"), "Defaults restores scenario snowfall");
                            assertEquals(0, value("Rain"));
                            assertEquals(1, value("Ground fog"), "Defaults restores scenario fog");
                            assertEquals(1, value("Haze"));
                            GpuBoardTestUi.click("weather-toggle-Snow");
                            assertEquals(0, value("Snow"));
                            GpuBoardTestUi.click("weather-toggle-Snow");
                            assertTrue(value("Snow") > 0);
                            assertEquals(0, fixture.clicks.get(), "Weather controls must not issue game orders");
                            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                private float value(String name) {
                    Slider slider = GpuBoardTestUi.stage().getRoot().findActor(name);
                    return slider.getValue();
                }
            }, GpuBoardWindow.configuration(false));
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(Light.FULL_MOON, fixture.game.getPlanetaryConditions().getLight());
                assertEquals(Weather.HEAVY_SNOW, fixture.game.getPlanetaryConditions().getWeather());
                assertEquals(Fog.FOG_HEAVY, fixture.game.getPlanetaryConditions().getFog());
            });
        }
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @Test
    void controlsChangeTheLiveSceneAndKeepTheirPanelInsideTheViewport() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                private long daylight;

                @Override
                public void render() {
                    try {
                        super.render();
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        if (frames() == 2) {
                            boardCamera.setIsometric(true);
                            boardCamera.fit(fixture.source.takeFrame().scene());
                        } else if (frames() == 4) {
                            daylight = GpuBoardTestUi.capture(new File(output, "atmosphere-board-day.png"));
                            GpuBoardTestUi.click("tuning");
                        } else if (frames() == 6) {
                            slider("Time of day").setValue(17.5f);
                            GpuBoardTestUi.click("atmosphere-MIST");
                        } else if (frames() == 8) {
                            assertEquals(17.5f, slider("Time of day").getValue(), "Weather preserves time");
                            assertEquals(BoardAtmosphere.Weather.MIST.apply(BoardAtmosphere.DEFAULTS).fog(),
                                  slider("Ground fog").getValue(), 0.001f);
                            assertNotEquals(daylight, GpuBoardTestUi.capture(new File(output, "atmosphere-controls.png")));
                            GpuBoardTestUi.click("tuning");
                        } else if (frames() == 10) {
                            GpuBoardTestUi.capture(new File(output, "atmosphere-board-sunset-mist.png"));
                            slider("Time of day").setValue(0);
                        } else if (frames() == 12) {
                            GpuBoardTestUi.capture(new File(output, "atmosphere-board-night.png"));
                            GpuBoardTestUi.click("tuning");
                            GpuBoardTestUi.click("atmosphere-OVERCAST");
                            assertEquals(1, slider("Cloud cover").getValue());
                            GpuBoardTestUi.click("atmosphere-CLEAR");
                            assertEquals(0, slider("Cloud cover").getValue());
                            assertEquals(0, slider("Ground fog").getValue());
                            assertEquals(0, slider("Haze").getValue());
                            assertEquals(0, slider("Time of day").getValue(), "Clear preserves the clock");
                            GpuBoardTestUi.click("tuning-defaults");
                            assertEquals(BoardAtmosphere.DEFAULTS.hour(), slider("Time of day").getValue());
                            assertEquals(0, slider("Cloud cover").getValue());
                            Gdx.graphics.setWindowedMode(900, 600);
                        } else if (frames() == 16) {
                            Actor panel = GpuBoardTestUi.stage().getRoot().findActor("board-tuning");
                            assertTrue(panel.getX() >= 0 && panel.getRight() <= GpuBoardTestUi.stage().getWidth());
                            assertTrue(panel.getY() >= GpuBoardUi.TURN_HEIGHT);
                            assertTrue(panel.getTop() <= GpuBoardTestUi.stage().getHeight() - GpuBoardUi.TOP_HEIGHT);
                            GpuBoardTestUi.capture(new File(output, "atmosphere-controls-small.png"));
                            assertEquals(0, fixture.clicks.get(), "Visual controls must not issue orders");
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                private Slider slider(String name) {
                    return GpuBoardTestUi.stage().getRoot().findActor(name);
                }
            }, GpuBoardWindow.configuration(false));
        }
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }
}
