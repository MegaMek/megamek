/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import javax.swing.SwingUtilities;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.common.board.Coords;
import megamek.common.planetaryConditions.Fog;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.PlanetaryConditions;
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
        // Measure render work independently of the live window's VSync preference.
        Gdx.graphics.setVSync(false);
        assertTrue(output.isDirectory() || output.mkdirs());
        checkScatteringPhase();
        checkFogCloudBounds();
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
        BufferedImage normalImage = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        graphics = normalImage.createGraphics();
        graphics.setColor(new java.awt.Color(128, 128, 255));
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        BoardScene.Pixels normals = new BoardScene.Pixels(normalImage);
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 7; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), 0, -1, false, 0, BoardScene.Surface.GRASS,
                      x == 1 && y == 2 ? darkPixels : pixels, x % 2 == 0 ? normals : null, null, null,
                      List.of(), List.of()));
            }
        }
        BoardScene scene = new BoardScene(0, 7, 7, tiles, List.of(), List.of(), -1, "", List.of());
        GpuTerrain terrain = new GpuTerrain();
        GpuAtmosphere atmosphere = new GpuAtmosphere();
        ModelBatch batch = new ModelBatch(GpuUnitShader.provider());
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
            float shadowShare = luminance(sample(camera, new Vector3(center).add(0, -32, 0))) / luminance(clearGround);
            assertTrue(shadowShare > 0.4f && shadowShare < 0.6f,
                  "Native daylight shadows retain sky fill while showing clear contrast: " + shadowShare);
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
            var fullMoon = new BoardAtmosphere.Settings(0, 0, 0, 1.5f, 0, 0);
            atmosphere.setOptions(new GpuAtmosphere.Options(0, false, 0, 0, 0, 0, 0, 0));
            draw(atmosphere, terrain, batch, tower, camera, scene, fullMoon);
            Color originalMoonGround = sample(camera, ground);
            Vector3 moonShadow = new Vector3(center).add(0, 32, 0);
            Color originalMoonShade = sample(camera, moonShadow);
            GpuBoardTestUi.capture(new File(output, "atmosphere-night-original-shadows.png"));
            atmosphere.setOptions(GpuAtmosphere.Options.DEFAULTS);
            draw(atmosphere, terrain, batch, tower, camera, scene, fullMoon);
            Color night = sample(camera, ground);
            assertTrue(difference(originalMoonGround, night) < 0.015f, "Moon contrast must preserve lit ground brightness");
            assertTrue(luminance(sample(camera, moonShadow)) < luminance(originalMoonShade) * 0.95f,
                  "The Full Moon contrast control must visibly deepen native cast shadows without a settings change");
            assertTrue(luminance(clearGround) > luminance(night) * 1.25f, "Neutral daylight must be visibly brighter than night");
            // Retain a readable display-space floor after the night exposure and cool tint.
            float nightBrightnessFloor = 0.29f;
            assertTrue(luminance(night) > nightBrightnessFloor,
                  "Full moon lighting must keep the ground readable at its default exposure: " + night);
            assertTrue(night.b > night.r, "Moonlit gray ground must have a blue tint");
            assertSame(shadow, terrain.environment().shadowMap, "Changing time must reuse the shadow framebuffer");
            GpuBoardTestUi.capture(new File(output, "atmosphere-night.png"));
            // Twilight is the sun's lowest lit window: 6:00-6:45 at dawn, 17:15-18:00 at dusk. Earlier and later
            // hours are night, which is darker by design, so only the twilight hours have to stay readable.
            for (float hour : new float[] { 6, 6.25f, 6.5f, 6.75f, 17.25f, 17.5f, 17.75f, 18 }) {
                draw(atmosphere, terrain, batch, tower, camera, scene,
                      new BoardAtmosphere.Settings(hour, 1, 0, 1.5f, 0, 0));
                Color twilight = sample(camera, ground);
                assertTrue(luminance(twilight) > nightBrightnessFloor,
                      "The rendered board must not go dark during twilight at " + hour + ": " + twilight);
            }
            checkTwilightShadows(atmosphere, terrain, batch, tower, camera, scene);
            checkTwilightAndCloudPalette(atmosphere, terrain, batch, tower, camera, scene);
            checkCloudOpacity(scene);
            checkFixedLightingAndMoonModes(atmosphere, terrain, batch, tower, camera, scene);
            checkClouds(atmosphere, terrain, batch, tower, camera, scene);
            checkWetGround(atmosphere, terrain, batch, tower, camera, scene);
            checkParticles(atmosphere, terrain, batch, tower, camera, scene);
            for (boolean isometric : List.of(false, true)) {
                camera.setIsometric(isometric);
                camera.fit(scene);
                for (int size : new int[] { 900, 2043, 1280 }) {
                    camera.resize(size, 600);
                    draw(atmosphere, terrain, batch, tower, camera, scene, BoardAtmosphere.DEFAULTS);
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                    draw(atmosphere, terrain, batch, tower, camera, scene,
                          new BoardAtmosphere.Settings(13, 0.6f, 0, 2.5f, 0, 0));
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError(), "Cloud and shaft buffers must resize together");
                }
            }
            camera.resize(1280, 800);
            camera.setIsometric(true);
            camera.fit(scene);
            StringBuilder timing = new StringBuilder("Renderer: " + Gdx.gl.glGetString(GL20.GL_RENDERER)
                  + "\n1280x800 synthetic 7x7 board and tower; warmed, synchronous GL completion; no vsync wait\n");
            String[] modes = { "Clear", "Maximum fog/haze", "Downpour",
                  "Maximum snow", "Maximum hail", "Maximum sand", "All precipitation at maximum",
                  "Broken clouds and shafts", "Overcast clouds", "Storm clouds and shafts" };
            int[] particles = { 0, 0, 4608, 4608, 3072, 0, 12288, 0, 0, 4608 };
            int[] particleDraws = { 0, 0, 1, 1, 1, 0, 3, 0, 0, 1 };
            for (int mode = 0; mode < modes.length; mode++) {
                var effects = new BoardAtmosphere.Effects(mode == 2 || mode == 6 || mode == 9 ? 1 : 0,
                      mode == 3 || mode == 6 ? 1 : 0, mode == 4 || mode == 6 ? 1 : 0,
                      mode == 5 || mode == 6 ? 1 : 0, 0, 0.4f, 60);
                var settings = new BoardAtmosphere.Settings(13, mode >= 8 ? 1 : mode == 7 ? 0.6f : 0,
                      mode == 1 ? 1 : 0, 8, mode == 1 ? 1 : mode >= 7 ? 0.15f : 0, 0, effects);
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
                // Instrument outside the timed window: no board redraw during postprocessing, no density reductions.
                var profiler = new GLProfiler(Gdx.graphics);
                profiler.enable();
                try {
                    atmosphere.end(camera.camera, terrain, scene, 0);
                    int postDraws = profiler.getDrawCalls();
                    assertEquals(mode == 1 || mode >= 7 ? 2 : 1, postDraws,
                          modes[mode] + " must only composite, plus scattering when enabled");
                    profiler.reset();
                    atmosphere.renderWeather(camera.camera, scene);
                    assertEquals(particleDraws[mode], profiler.getDrawCalls(), "Weather draw budget: " + modes[mode]);
                    assertEquals(particles[mode] * 6f, profiler.getVertexCount().total,
                          "All particle quads must remain at full density: " + modes[mode]);
                    timing.append(String.format(Locale.ROOT, "  Postprocess draws: %d; particle draws: %d; particles: %d%n",
                          postDraws, profiler.getDrawCalls(), particles[mode]));
                } finally {
                    profiler.disable();
                }
            }
            Files.writeString(new File(output, "atmosphere-timing.txt").toPath(), timing);
            checkLargeBoard(atmosphere, terrain, batch, tower, camera, pixels, normals);
        } finally {
            atmosphere.dispose();
            terrain.dispose();
            batch.dispose();
            model.dispose();
        }
    }

    private void draw(GpuAtmosphere atmosphere, GpuTerrain terrain, ModelBatch batch, ModelInstance tower,
          BoardCamera camera, BoardScene scene, BoardAtmosphere.Settings settings) {
        draw(atmosphere, terrain, batch, tower, camera, scene, settings, null);
    }

    private void draw(GpuAtmosphere atmosphere, GpuTerrain terrain, ModelBatch batch, ModelInstance tower,
          BoardCamera camera, BoardScene scene, BoardAtmosphere.Settings settings, GpuCloudShadow cloudOverride) {
        draw(atmosphere, terrain, batch, List.of(tower), camera, scene, settings, cloudOverride, 0.1f);
    }

    private void draw(GpuAtmosphere atmosphere, GpuTerrain terrain, ModelBatch batch, List<ModelInstance> units,
          BoardCamera camera, BoardScene scene, BoardAtmosphere.Settings settings, GpuCloudShadow cloudOverride, float delta) {
        drawScene(atmosphere, terrain, batch, units, camera, scene, settings, cloudOverride, delta);
        atmosphere.renderWeather(camera.camera, scene);
    }

    /** Surface checks sample before precipitation; both paths use the actual weather-derived material uniforms. */
    private void drawScene(GpuAtmosphere atmosphere, GpuTerrain terrain, ModelBatch batch, List<ModelInstance> units,
          BoardCamera camera, BoardScene scene, BoardAtmosphere.Settings settings, GpuCloudShadow cloudOverride, float delta) {
        atmosphere.configure(settings);
        atmosphere.updateLight(camera.camera);
        terrain.setAtmosphere(atmosphere.lighting());
        terrain.renderShadows(units);
        atmosphere.prepareClouds(terrain, scene, delta);
        if (cloudOverride != null) { terrain.environment().set(cloudOverride); }
        ScreenUtils.clear(0, 0, 0, 1, true);
        atmosphere.begin((int) camera.camera.viewportWidth, (int) camera.camera.viewportHeight, delta);
        terrain.render(camera.camera, false);
        batch.begin(camera.camera);
        batch.render(units, terrain.environment());
        batch.end();
        atmosphere.end(camera.camera, terrain, scene, 0);
    }

    private void checkFixedLightingAndMoonModes(GpuAtmosphere atmosphere, GpuTerrain terrain, ModelBatch batch,
          ModelInstance tower, BoardCamera camera, BoardScene scene) throws IOException {
        var originalShadow = terrain.environment().shadowMap;
        var cloudy = new BoardAtmosphere.Settings(13, 0.6f, 0, 2.5f, 0, 0);
        camera.setIsometric(true);
        atmosphere.setOptions(new GpuAtmosphere.Options(0.5f, true));
        Vector3 screenDirection = null;
        for (int turn = 0; turn < 3; turn++) {
            camera.orbit(60, 5);
            camera.fit(scene);
            draw(atmosphere, terrain, batch, List.of(tower), camera, scene, cloudy, null, 0);
            var lighting = atmosphere.lighting();
            var light = terrain.environment().get(DirectionalLightsAttribute.class, DirectionalLightsAttribute.Type);
            assertEquals(lighting.direction(), light.lights.first().direction);
            Vector3 screen = lighting.direction().cpy().rot(camera.camera.view);
            if (screenDirection == null) { screenDirection = screen; }
            assertTrue(screenDirection.epsilonEquals(screen, 0.00001f));
            assertSame(originalShadow, terrain.environment().shadowMap, "Fixed lighting reuses the native shadow map");
            var cloud = terrain.environment().get(GpuCloudShadow.class, GpuCloudShadow.TYPE);
            Vector3 point = BoardGeometry.center(new Coords(2, 2), 0);
            Vector3 alongLight = point.cpy().mulAdd(lighting.direction(), 100);
            Vector3 cloudA = point.cpy().mul(cloud.projection), cloudB = alongLight.cpy().mul(cloud.projection);
            assertEquals(cloudA.x, cloudB.x, 0.00001f);
            assertEquals(cloudA.y, cloudB.y, 0.00001f, "Cloud transmission must project along the same camera-relative light");
            Vector3 shadowA = point.cpy().prj(originalShadow.getProjViewTrans());
            Vector3 shadowB = alongLight.cpy().prj(originalShadow.getProjViewTrans());
            assertEquals(shadowA.x, shadowB.x, 0.00001f);
            assertEquals(shadowA.y, shadowB.y, 0.00001f, "Geometry shadows must follow that same light");
            GpuBoardTestUi.capture(new File(output, "atmosphere-fixed-sun-" + turn + ".png"));
        }
        atmosphere.setOptions(GpuAtmosphere.Options.DEFAULTS);
        draw(atmosphere, terrain, batch, tower, camera, scene, cloudy);
        assertEquals(BoardAtmosphere.lighting(cloudy).direction(), atmosphere.lighting().direction(),
              "Disabling the option restores the world-space direction");
        var conditions = new PlanetaryConditions();
        conditions.setFog(Fog.FOG_LIGHT);
        atmosphere.setOptions(new GpuAtmosphere.Options(0.5f, true));
        for (Light night : new Light[] { Light.FULL_MOON, Light.MOONLESS, Light.PITCH_BLACK, Light.FULL_MOON }) {
            conditions.setLight(night);
            draw(atmosphere, terrain, batch, tower, camera, scene, BoardAtmosphere.fromScenario(conditions, false, 0.5));
            boolean moon = night == Light.FULL_MOON;
            assertEquals(moon, atmosphere.lighting().hasDirectLight());
            var lights = terrain.environment().get(DirectionalLightsAttribute.class, DirectionalLightsAttribute.Type);
            assertEquals(moon ? 1 : 0, lights == null ? 0 : lights.lights.size,
                  "Moonless/pitch black must detach the directional source");
            assertEquals(moon, terrain.environment().shadowMap != null);
            assertEquals(moon, terrain.environment().has(GpuCloudShadow.TYPE));
            if (moon) { assertSame(originalShadow, terrain.environment().shadowMap, "Reenabling moonlight reuses its resources"); }
            GpuBoardTestUi.capture(new File(output, "atmosphere-" + night.name().toLowerCase(Locale.ROOT) + ".png"));
        }
        atmosphere.setOptions(GpuAtmosphere.Options.DEFAULTS);
        camera.setIsometric(false);
        camera.fit(scene);
        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
    }

    /** Compare the same receivers with/without a small caster so the entire long shadow fits on the fixture. */
    private void checkTwilightShadows(GpuAtmosphere atmosphere, GpuTerrain terrain, ModelBatch batch,
          ModelInstance tower, BoardCamera camera, BoardScene scene) throws IOException {
        float height = 36;
        Vector3 center = BoardGeometry.center(new Coords(3, 3), 0);
        ModelInstance caster = new ModelInstance(tower.model);
        caster.transform.setToTranslation(center.x, center.y, height / 2).scale(0.35f, 0.35f, height / 108);
        StringBuilder report = new StringBuilder("Rendered twilight shadows: identical ground probes with/without a 36-unit caster\n");
        try {
            for (boolean fixed : new boolean[] { false, true }) {
                atmosphere.setOptions(new GpuAtmosphere.Options(0, fixed));
                for (boolean isometric : new boolean[] { false, true }) {
                    camera.setIsometric(isometric);
                    camera.fit(scene);
                    for (float hour : new float[] { 6, 6.25f, 6.5f, 6.75f, 17.25f, 17.5f, 17.75f, 18 }) {
                        var settings = new BoardAtmosphere.Settings(hour, 0, 0, 2.5f, 0, 0);
                        draw(atmosphere, terrain, batch, List.of(), camera, scene, settings, null, 0);
                        var lighting = atmosphere.lighting();
                        assertTrue(lighting.sunlight());
                        Vector3 tail = new Vector3(lighting.direction()).scl(height / -lighting.direction().z);
                        tail.z = 0;
                        if (!fixed) {
                            assertTrue(tail.len() > height * 4, "The rendered source must cast a long twilight shadow");
                        }
                        Vector3 inside = center.cpy().mulAdd(tail, 0.7f);
                        Vector3 beyond = center.cpy().add(tail).mulAdd(tail.cpy().nor(), 25);
                        Color lit = sample(camera, inside);
                        Color outside = sample(camera, beyond);
                        draw(atmosphere, terrain, batch, List.of(caster), camera, scene, settings, null, 0);
                        float ratio = luminance(sample(camera, inside)) / luminance(lit);
                        String label = hour + ", fixed=" + fixed + ", isometric=" + isometric;
                        assertTrue(ratio < 0.87f && ratio > 0.5f,
                              "Twilight must cast a visibly shaded, readable trail at " + label + ": " + ratio);
                        assertTrue(difference(outside, sample(camera, beyond)) < 0.025f,
                              "The shadow must end at the source's projected length, not shade all ground: " + label);
                        report.append(String.format(Locale.ROOT, "%s: length/height=%.2f, shadow/lit=%.3f%n",
                              label, tail.len() / height, ratio));
                        if (!fixed && (hour == 6 || hour == 18)) {
                            GpuBoardTestUi.capture(new File(output, "twilight-shadow-" + hour + "-" + (isometric ? "iso" : "top") + ".png"));
                        }
                    }
                }
            }
            Files.writeString(new File(output, "twilight-shadow-checks.txt").toPath(), report);
        } finally {
            atmosphere.setOptions(GpuAtmosphere.Options.DEFAULTS);
            camera.setIsometric(false);
            camera.fit(scene);
        }
    }

    private void checkTwilightAndCloudPalette(GpuAtmosphere atmosphere, GpuTerrain terrain, ModelBatch batch,
          ModelInstance tower, BoardCamera camera, BoardScene scene) throws IOException {
        Vector3 ground = BoardGeometry.center(new Coords(1, 3), 0);
        for (float hour : new float[] { 0, 6, 6.75f, 12, 17.25f, 18 }) {
            var clear = new BoardAtmosphere.Settings(hour, 0, 0, 2.5f, 0, 0);
            draw(atmosphere, terrain, batch, List.of(tower), camera, scene, clear, null, 0);
            int[] sky = backgroundColumn();
            Color low = new Color(sky[sky.length / 10]);
            Color high = new Color(sky[sky.length * 9 / 10]);
            if (hour > 0 && hour != 12) {
                Color surface = sample(camera, ground);
                assertTrue(surface.r > surface.b * 1.1f, "Twilight must warm rendered gray ground at " + hour + ": " + surface);
                assertTrue(low.r > low.b * 1.5f, "The rendered twilight horizon must be warm: " + low);
                assertTrue(high.b > high.r, "The upper twilight sky must retain cooler color: " + high);
            }
            GpuBoardTestUi.capture(new File(output, "atmosphere-clock-" + hour + ".png"));
            float previous = 0;
            for (float cover : new float[] { 0.25f, 0.6f, 1 }) {
                draw(atmosphere, terrain, batch, List.of(tower), camera, scene,
                      new BoardAtmosphere.Settings(hour, cover, 0, 2.5f, 0, 0), null, 0);
                int[] cloudy = backgroundColumn();
                float overhead = difference(high, new Color(cloudy[cloudy.length * 9 / 10]));
                assertTrue(overhead > previous, "Increasing cover must visibly tint the sky at " + hour);
                assertTrue(overhead > difference(low, new Color(cloudy[cloudy.length / 10])),
                      "The rendered cloud tint must fade down toward the horizon at " + hour);
                previous = overhead;
            }
            GpuBoardTestUi.capture(new File(output, "atmosphere-clock-overcast-" + hour + ".png"));
        }
    }

    /** Exercise the real atlas shader at a fixed density field, independently of surface texture colors. */
    private void checkCloudOpacity(BoardScene scene) throws ReflectiveOperationException {
        var quad = GpuAtmosphere.screenQuad();
        var clouds = new GpuClouds(quad);
        try {
            var settings = new BoardAtmosphere.Settings(13, 0.65f, 0, 2.5f, 0, 0);
            var lighting = BoardAtmosphere.lighting(settings);
            var field = GpuClouds.class.getDeclaredField("shadow");
            field.setAccessible(true);
            var buffer = (FrameBuffer) field.get(clouds);
            assertEquals(192, buffer.getWidth());
            var texture = clouds.shadow().texture;
            int[] opaque = null;
            for (float strength : new float[] { 1, 0.2f, 0 }) {
                clouds.update(settings, lighting, scene, 0, strength);
                assertSame(texture, clouds.shadow().texture, "Strength changes reuse the fixed Low atlas");
                buffer.begin();
                Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, buffer.getWidth(), buffer.getHeight());
                buffer.end();
                try {
                    int[] transmission = new int[pixels.getWidth() * pixels.getHeight()];
                    int darkest = 255;
                    for (int index = 0; index < transmission.length; index++) {
                        int value = pixels.getPixel(index % pixels.getWidth(), index / pixels.getWidth()) >>> 24;
                        transmission[index] = value;
                        darkest = Math.min(darkest, value);
                        if (opaque != null) {
                            assertEquals(255 - (255 - opaque[index]) * strength, value, 1.1f,
                                  "Strength scales opacity while retaining the same cloud shapes and clear openings");
                        }
                    }
                    if (opaque == null) {
                        assertTrue(darkest < 220, "The fixed field must contain cloud shadows");
                        opaque = transmission;
                    }
                } finally {
                    pixels.dispose();
                }
            }
        } finally {
            clouds.dispose();
            quad.dispose();
        }
    }

    private void checkClouds(GpuAtmosphere atmosphere, GpuTerrain terrain, ModelBatch batch, ModelInstance tower,
          BoardCamera camera, BoardScene scene) throws IOException {
        checkCloudSurfaceLighting(atmosphere, terrain, batch, tower, camera, scene);
        var backwards = new BoardAtmosphere.Settings(13, 0.65f, 0, 2.5f, 0, 0,
              new BoardAtmosphere.Effects(0, 0, 0, 0, 0, 1, 225));
        List<Vector3> wrapProbes = List.of(BoardGeometry.center(new Coords(0, 3), 0),
              BoardGeometry.center(new Coords(1, 3), 0), BoardGeometry.center(new Coords(5, 5), 0));
        draw(atmosphere, terrain, batch, List.of(tower), camera, scene, backwards, null, 0);
        List<Color> beforeWrap = wrapProbes.stream().map(point -> sample(camera, point)).toList();
        draw(atmosphere, terrain, batch, List.of(tower), camera, scene, backwards, null, 0.0001f);
        for (int index = 0; index < wrapProbes.size(); index++) {
            assertTrue(difference(beforeWrap.get(index), sample(camera, wrapProbes.get(index))) < 0.025f,
                  "The first negative wind step must wrap the noise without teleporting the cloud shadows");
        }
        var wind = new BoardAtmosphere.Effects(0, 0, 0, 0, 0, 1, 90);
        var broken = new BoardAtmosphere.Settings(13, 0.6f, 0, 2.5f, 0.15f, 0, wind);
        camera.setIsometric(true);
        camera.fit(scene);
        draw(atmosphere, terrain, batch, tower, camera, scene, broken);
        assertNotNull(atmosphere.depthTexture(), "Cloud shafts must stop at captured opaque depth");
        GpuCloudShadow cloud = terrain.environment().get(GpuCloudShadow.class, GpuCloudShadow.TYPE);
        assertNotNull(cloud, "Terrain and units must share the cloud transmission map");
        var texture = cloud.texture;
        var projection = cloud.projection.cpy();
        int[] sky = backgroundColumn();
        long still = GpuBoardTestUi.capture(new File(output, "clouds-broken.png"));
        for (int frame = 0; frame < 60; frame++) {
            draw(atmosphere, terrain, batch, tower, camera, scene, broken);
        }
        assertNotEquals(still, GpuBoardTestUi.capture(new File(output, "clouds-wind.png")),
              "Wind must move cloud lighting across the board");
        assertArrayEquals(sky, backgroundColumn(), "The sky stays a smooth color gradient while cloud shadows move");
        for (int y = 1; y < sky.length; y++) {
            assertTrue(difference(new Color(sky[y - 1]), new Color(sky[y])) < 0.025f,
                  "The backdrop must have no cloud texture or hard color transitions");
        }
        assertSame(texture, terrain.environment().get(GpuCloudShadow.class, GpuCloudShadow.TYPE).texture,
              "Animation must reuse the cloud atlas");
        camera.setIsometric(false);
        camera.fit(scene);
        draw(atmosphere, terrain, batch, tower, camera, scene, broken);
        assertArrayEquals(projection.val, cloud.projection.val, "Camera changes must not move or rescale world-space shadows");
        GpuBoardTestUi.capture(new File(output, "clouds-top.png"));
        float darkest = 1, brightest = 0;
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 7; y++) {
                if (x == 3 || x == 1 && y == 2) { continue; }
                float value = luminance(sample(camera, BoardGeometry.center(new Coords(x, y), 0)));
                darkest = Math.min(darkest, value);
                brightest = Math.max(brightest, value);
            }
        }
        assertTrue(brightest - darkest > 0.025f, "Broken clouds must create distinct lit and shaded patches");
        camera.setIsometric(true);
        camera.fit(scene);
        var controlled = new BoardAtmosphere.Settings(13, 0.85f, 0, 2.5f, 0, 0);
        atmosphere.setOptions(new GpuAtmosphere.Options(0, false));
        draw(atmosphere, terrain, batch, List.of(tower), camera, scene, controlled, null, 0);
        var depth = atmosphere.depthTexture();
        long withoutRays = GpuBoardTestUi.capture(new File(output, "clouds-rays-off.png"));
        atmosphere.setOptions(GpuAtmosphere.Options.DEFAULTS);
        draw(atmosphere, terrain, batch, List.of(tower), camera, scene, controlled, null, 0);
        assertSame(depth, atmosphere.depthTexture(), "Rays reuse scene depth without an extra depth target");
        assertNotEquals(withoutRays, GpuBoardTestUi.capture(new File(output, "clouds-rays-default.png")));
        draw(atmosphere, terrain, batch, tower, camera, scene,
              new BoardAtmosphere.Settings(13, 0.65f, 0, 2.5f, 0, 0, wind, Atmosphere.THIN));
        GpuBoardTestUi.capture(new File(output, "clouds-thin.png"));
        draw(atmosphere, terrain, batch, tower, camera, scene,
              new BoardAtmosphere.Settings(16, 0.65f, 0.05f, 2.5f, 0.35f, 0, wind));
        GpuBoardTestUi.capture(new File(output, "clouds-afternoon-shafts.png"));
        draw(atmosphere, terrain, batch, tower, camera, scene, overcast(broken));
        GpuBoardTestUi.capture(new File(output, "clouds-overcast.png"));
        draw(atmosphere, terrain, batch, tower, camera, scene,
              new BoardAtmosphere.Settings(13, 1, 0.1f, 2.5f, 0.2f, 0,
                    new BoardAtmosphere.Effects(1, 0, 0, 0, 0.65f, 0.8f, 60)));
        GpuBoardTestUi.capture(new File(output, "clouds-storm.png"));
        draw(atmosphere, terrain, batch, tower, camera, scene,
              new BoardAtmosphere.Settings(0, 0.6f, 0, 2.5f, 0, 0));
        assertSame(depth, atmosphere.depthTexture(), "Night reuses the same hardware scene depth");
        GpuBoardTestUi.capture(new File(output, "clouds-night.png"));
        draw(atmosphere, terrain, batch, tower, camera, scene, BoardAtmosphere.DEFAULTS);
        GpuBoardTestUi.capture(new File(output, "clouds-clear.png"));
        Vector3 ground = BoardGeometry.center(new Coords(1, 3), 0);
        Color clear = sample(camera, ground);
        assertNull(terrain.environment().get(GpuCloudShadow.class, GpuCloudShadow.TYPE));
        draw(atmosphere, terrain, batch, tower, camera, scene,
              new BoardAtmosphere.Settings(13, 1, 1, 2.5f, 1, 0, wind, Atmosphere.VACUUM));
        GpuBoardTestUi.capture(new File(output, "clouds-vacuum.png"));
        assertEquals(clear, sample(camera, ground),
              "Vacuum must suppress clouds, haze and weather while preserving readable surface light");
        assertTrue(atmosphere.lighting().sky().b < 0.01f, "An airless world has no blue atmospheric sky");
        assertSame(depth, atmosphere.depthTexture());
        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
        camera.setIsometric(false);
        camera.fit(scene);
    }

    private void checkCloudSurfaceLighting(GpuAtmosphere atmosphere, GpuTerrain terrain, ModelBatch batch,
          ModelInstance tower, BoardCamera camera, BoardScene scene) {
        Pixmap pixels = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        pixels.setColor(Color.WHITE);
        pixels.fill();
        Texture texture = new Texture(pixels);
        Matrix4 projection = new Matrix4().setToScaling(0, 0, 0);
        projection.setTranslation(0.5f, 0.5f, -1);
        GpuCloudShadow cloud = new GpuCloudShadow(texture, projection);
        List<Vector3> probes = List.of(BoardGeometry.center(new Coords(0, 3), 0),
              BoardGeometry.center(new Coords(1, 3), 0), BoardGeometry.center(new Coords(3, 3), 0).add(0, 0, 108));
        try {
            var clear = BoardAtmosphere.DEFAULTS;
            draw(atmosphere, terrain, batch, tower, camera, scene, clear);
            List<Color> lit = probes.stream().map(point -> sample(camera, point)).toList();
            draw(atmosphere, terrain, batch, tower, camera, scene, clear, cloud);
            for (int i = 0; i < probes.size(); i++) {
                assertEquals(lit.get(i), sample(camera, probes.get(i)),
                      "An open cloud column must leave native ground/normal-map/unit lighting unchanged");
            }
            atmosphere.setOptions(new GpuAtmosphere.Options(0, false));
            draw(atmosphere, terrain, batch, tower, camera, scene,
                  new BoardAtmosphere.Settings(13, 1, 0, 2.5f, 0, 0), cloud);
            for (int i = 0; i < probes.size(); i++) {
                assertEquals(lit.get(i), sample(camera, probes.get(i)),
                      "Full cloud coverage must not apply a second global attenuation to an open sun column");
            }
            pixels.setColor(Color.BLACK);
            pixels.fill();
            texture.draw(pixels, 0, 0);
            draw(atmosphere, terrain, batch, tower, camera, scene, clear, cloud);
            float expected = atmosphere.lighting().ambient().r / (atmosphere.lighting().ambient().r
                  - atmosphere.lighting().direct().r * atmosphere.lighting().direction().z);
            for (int i = 0; i < probes.size(); i++) {
                assertEquals(expected, sample(camera, probes.get(i)).r / lit.get(i).r, 0.025f,
                      "An opaque cloud must remove direct light and retain ambient light on all surface shaders");
            }
        } finally {
            atmosphere.setOptions(GpuAtmosphere.Options.DEFAULTS);
            terrain.environment().remove(GpuCloudShadow.TYPE);
            texture.dispose();
            pixels.dispose();
        }
    }

    private void checkWetGround(GpuAtmosphere atmosphere, GpuTerrain terrain, ModelBatch batch, ModelInstance tower,
          BoardCamera camera, BoardScene scene) throws IOException {
        camera.setIsometric(false);
        camera.fit(scene);
        Vector3 plain = BoardGeometry.center(new Coords(1, 3), 0);
        // Use an interior normal-mapped tile; the outer cliff rim has its own material response.
        Vector3 normal = BoardGeometry.center(new Coords(2, 1), 0);
        Vector3 roof = BoardGeometry.center(new Coords(3, 3), 0).add(0, 0, 108);
        draw(atmosphere, terrain, batch, tower, camera, scene, BoardAtmosphere.DEFAULTS);
        Color dryPlain = sample(camera, plain), dryNormal = sample(camera, normal), dryRoof = sample(camera, roof);
        var rain = new BoardAtmosphere.Settings(13, 0, 0, 2.5f, 0, 0,
              new BoardAtmosphere.Effects(1, 0, 0, 0, 0, 0, 0));
        drawScene(atmosphere, terrain, batch, List.of(tower), camera, scene, rain, null, 0);
        assertTrue(luminance(sample(camera, plain)) < luminance(dryPlain) * 0.85f, "Wet ground darkens its albedo");
        assertTrue(luminance(sample(camera, normal)) < luminance(dryNormal) * 0.97f,
              "Wetness also reaches normal-mapped ground with its independently tuned material response");
        assertEquals(dryRoof, sample(camera, roof), "Ground wetness must not tint unrelated unit materials");
        GpuBoardTestUi.capture(new File(output, "terrain-wet.png"));
        draw(atmosphere, terrain, batch, tower, camera, scene, BoardAtmosphere.DEFAULTS);
        assertEquals(dryPlain, sample(camera, plain), "Stopping rain removes the wet material response immediately");
        var lunar = new BoardAtmosphere.Settings(13, 0, 0, 2.5f, 0, 0, rain.effects(), Atmosphere.VACUUM);
        draw(atmosphere, terrain, batch, tower, camera, scene, lunar);
        assertEquals(dryPlain, sample(camera, plain), "Airless conditions suppress rain and wet ground together");
        var snowTiles = scene.tiles().stream().map(tile -> new BoardScene.Tile(tile.coords(), 0, -1, false, 0,
              BoardScene.Surface.SNOW, tile.ground(), tile.normals(), null, null, List.of(), List.of())).toList();
        var snow = new BoardScene(0, 7, 7, snowTiles, List.of(), List.of(), -1, "", List.of());
        terrain.update(snow);
        draw(atmosphere, terrain, batch, tower, camera, snow, BoardAtmosphere.DEFAULTS);
        Color frozenGround = sample(camera, plain);
        drawScene(atmosphere, terrain, batch, List.of(tower), camera, snow, rain, null, 0);
        assertEquals(frozenGround, sample(camera, plain), "Snow artwork must not acquire a liquid-water sheen");
        var pavementTiles = scene.tiles().stream().map(tile -> new BoardScene.Tile(tile.coords(), 0, -1, false, 0,
              BoardScene.Surface.CONCRETE, tile.ground(), tile.normals(), null, null, List.of(), List.of())).toList();
        var pavement = new BoardScene(0, 7, 7, pavementTiles, List.of(), List.of(), -1, "", List.of());
        terrain.update(pavement);
        Vector3 sun = atmosphere.lighting().direction().cpy().scl(-1);
        // Observe a horizontal water film at the reflected sun direction, then rotate away at the same tilt.
        camera.orbit((float) Math.toDegrees(Math.atan2(-sun.x, sun.y)), (float) Math.toDegrees(Math.acos(sun.z)));
        camera.fit(pavement);
        drawScene(atmosphere, terrain, batch, List.of(tower), camera, pavement, rain, null, 0);
        Color highlight = sample(camera, plain);
        GpuBoardTestUi.capture(new File(output, "terrain-wet-sun-reflection.png"));
        camera.orbit(90, 0);
        camera.fit(pavement);
        drawScene(atmosphere, terrain, batch, List.of(tower), camera, pavement, rain, null, 0);
        assertTrue(luminance(highlight) > luminance(sample(camera, plain)) + 0.035f,
              "Wet pavement must reflect the sun as the viewing angle changes, not merely become darker");
        atmosphere.setOptions(GpuAtmosphere.Options.DEFAULTS);
        camera.setIsometric(false);
        camera.fit(scene);
        terrain.update(scene);
    }

    /** A reproducible larger workload, not a whole-game FPS claim: static terrain and 64 simple lit casters. */
    private void checkLargeBoard(GpuAtmosphere atmosphere, GpuTerrain terrain, ModelBatch batch, ModelInstance tower,
          BoardCamera camera, BoardScene.Pixels pixels, BoardScene.Pixels normals) throws IOException {
        List<BoardScene.Tile> tiles = new ArrayList<>();
        List<ModelInstance> units = new ArrayList<>();
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), 0, -1, false, 0, BoardScene.Surface.GRASS,
                      pixels, x % 2 == 0 ? normals : null, null, null, List.of(), List.of()));
                if (x % 8 == 4 && y % 8 == 4) {
                    ModelInstance unit = new ModelInstance(tower.model);
                    unit.transform.setToTranslation(BoardGeometry.center(new Coords(x, y), 0).add(0, 0, 54));
                    units.add(unit);
                }
            }
        }
        var scene = new BoardScene(0, 64, 64, tiles, List.of(), List.of(), -1, "", List.of());
        terrain.update(scene);
        StringBuilder report = new StringBuilder("Renderer: " + Gdx.gl.glGetString(GL20.GL_RENDERER)
              + "\n1280x800; synthetic 64x64 board, 64 simple casters; warmed 15 frames, sampled 30; glFinish; no vsync\n");
        var broken = new BoardAtmosphere.Settings(13, 0.65f, 0, 2.5f, 0.15f, 0,
              new BoardAtmosphere.Effects(0, 0, 0, 0, 0, 0.4f, 60));
        for (boolean isometric : new boolean[] { false, true }) {
            camera.setIsometric(isometric);
            camera.fit(scene);
            for (var settings : List.of(BoardAtmosphere.DEFAULTS, broken, overcast(broken))) {
                List<Double> times = new ArrayList<>();
                for (int frame = 0; frame < 45; frame++) {
                    long start = System.nanoTime();
                    draw(atmosphere, terrain, batch, units, camera, scene, settings, null, 1f / 60);
                    Gdx.gl.glFinish();
                    if (frame >= 15) { times.add((System.nanoTime() - start) / 1_000_000.0); }
                }
                times.sort(Double::compareTo);
                report.append(String.format(Locale.ROOT, "%s cover %.2f: median %.3f ms, p95 %.3f ms%n",
                      isometric ? "Orbit" : "Top", settings.clouds(), times.get(15), times.get(28)));
                assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                if (settings.clouds() > 0) {
                    var atlas = terrain.environment().get(GpuCloudShadow.class, GpuCloudShadow.TYPE);
                    assertEquals(192, atlas.texture.getWidth(), "Cloud shadows always use the Low budget");
                    var previous = atlas.texture;
                    draw(atmosphere, terrain, batch, units, camera, scene, settings, null, 0);
                    assertSame(previous, terrain.environment().get(GpuCloudShadow.class, GpuCloudShadow.TYPE).texture,
                          "Animation and camera changes reuse the fixed atlas");
                }
                if (isometric && settings.clouds() == broken.clouds()) {
                    GpuBoardTestUi.capture(new File(output, "clouds-large-low.png"));
                }
            }
        }
        atmosphere.setOptions(GpuAtmosphere.Options.DEFAULTS);
        Files.writeString(new File(output, "cloud-shadow-timing.txt").toPath(), report);
    }

    private static BoardAtmosphere.Settings overcast(BoardAtmosphere.Settings current) {
        return new BoardAtmosphere.Settings(current.hour(), 1, 0, BoardAtmosphere.STANDARD_GROUND_LAYER_HEIGHT, 0.02f,
              current.exposure(), new BoardAtmosphere.Effects(0, 0, 0, 0, 0, current.effects().wind(),
              current.effects().windDirection()), current.pressure(), current.temperature(), current.moonlight(), current.taint());
    }

    private int[] backgroundColumn() {
        Pixmap pixels = Pixmap.createFromFrameBuffer(5, 5, 1, Gdx.graphics.getHeight() - 10);
        try {
            int[] result = new int[pixels.getHeight()];
            for (int y = 0; y < result.length; y++) { result[y] = pixels.getPixel(0, y); }
            return result;
        } finally {
            pixels.dispose();
        }
    }

    /** Raising the shaft ceiling must not silently lengthen the fog/haze column below it. */
    private void checkFogCloudBounds() {
        var shader = GpuAtmosphere.shader("atmosphere-fog.frag");
        var quad = GpuAtmosphere.screenQuad();
        var buffer = GpuAtmosphere.buffer(1, 1, false);
        var pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(0.5f, 0, 0, 1);
        pixel.fill();
        var depth = new Texture(pixel);
        pixel.dispose();
        try {
            buffer.begin();
            GpuAtmosphere.screenState();
            shader.bind();
            depth.bind(0);
            shader.setUniformi("u_depth", 0);
            var inverse = new Matrix4();
            inverse.val[Matrix4.M22] = -100;
            shader.setUniformMatrix("u_inverseView", inverse);
            shader.setUniformf("u_direction", 0, 0, -1);
            shader.setUniformf("u_boundsMin", -50, -50, -1);
            shader.setUniformf("u_fog", 0, 2, 0);
            shader.setUniformf("u_haze", 0.01f);
            shader.setUniformf("u_noiseScale", 0.5f);
            shader.setUniformf("u_layerScreenBounds", 0, 0, 1, 1);
            shader.setUniformf("u_fogColor", 0.5f, 0.5f, 0.5f);
            shader.setUniformf("u_maxOpacity", BoardAtmosphere.MAX_FOG_OPACITY);
            shader.setUniformf("u_rays", 0);
            int reference = 0;
            for (float ceiling : new float[] { 12, 60 }) {
                shader.setUniformf("u_boundsMax", 50, 50, ceiling);
                quad.render(shader, GL20.GL_TRIANGLES);
                Pixmap result = Pixmap.createFromFrameBuffer(0, 0, 1, 1);
                try {
                    int rgba = result.getPixel(0, 0);
                    if (ceiling == 12) {
                        reference = rgba;
                        assertEquals(0.885f, new Color(rgba).a, 0.01f, "Uncapped haze must retain its original column density");
                    } else {
                        assertEquals(reference, rgba, "Cloud altitude must not thicken the same fog/haze");
                    }
                } finally {
                    result.dispose();
                }
            }
        } finally {
            buffer.end();
            buffer.dispose();
            depth.dispose();
            quad.dispose();
            shader.dispose();
            Gdx.gl.glDepthMask(true);
        }
    }

    /** Exercise the air-scattering GLSL, including its direction convention. */
    private void checkScatteringPhase() {
        String root = "megamek/client/ui/clientGUI/boardview/gpu/";
        String phase = Gdx.files.classpath(root + "scattering-phase.glsl").readString("UTF-8");
        ShaderProgram shader = new ShaderProgram(Gdx.files.classpath(root + "atmosphere.vert").readString("UTF-8"),
              "varying vec2 v_uv;\n" + phase + "\nvoid main() {\n"
                    + "float cosine = v_uv.x * 2.0 - 1.0;\n"
                    + "float forward = scatteringPhase(cosine, 0.5);\n"
                    + "gl_FragColor = vec4(forward / (1.0 + forward), scatteringPhase(cosine, 0.0) * 0.5, 0.0, 1.0);\n}");
        assertTrue(shader.isCompiled(), shader.getLog());
        var quad = GpuAtmosphere.screenQuad();
        var buffer = GpuAtmosphere.buffer(32, 1, false);
        try {
            buffer.begin();
            GpuAtmosphere.screenState();
            shader.bind();
            quad.render(shader, GL20.GL_TRIANGLES);
            Pixmap result = Pixmap.createFromFrameBuffer(0, 0, 32, 1);
            try {
                float away = new Color(result.getPixel(0, 0)).r;
                float side = new Color(result.getPixel(16, 0)).r;
                float toward = new Color(result.getPixel(31, 0)).r;
                assertTrue(toward > side * 1.8f && side > away,
                      "Forward scattering must strengthen when looking toward the sun");
                for (int x = 0; x < 32; x++) {
                    assertEquals(0.5f, new Color(result.getPixel(x, 0)).g, 0.005f,
                          "Isotropic scattering must be finite and independent of viewing angle");
                }
            } finally {
                result.dispose();
                buffer.end();
            }
        } finally {
            buffer.dispose();
            quad.dispose();
            shader.dispose();
            // The isolated screen-space probe must not leave depth writes disabled for the scene's shadow clear.
            Gdx.gl.glDepthMask(true);
        }
    }

    private void checkParticles(GpuAtmosphere atmosphere, GpuTerrain terrain, ModelBatch batch, ModelInstance tower,
          BoardCamera camera, BoardScene scene) throws IOException {
        camera.setIsometric(true);
        camera.fit(scene);
        draw(atmosphere, terrain, batch, tower, camera, scene, BoardAtmosphere.DEFAULTS);
        long clear = GpuBoardTestUi.capture(new File(output, "weather-clear.png"));
        String[] names = { "rain", "snow", "hail" };
        for (int kind = 0; kind < names.length; kind++) {
            var effects = new BoardAtmosphere.Effects(kind == 0 ? 1 : 0, kind == 1 ? 1 : 0,
                  kind == 2 ? 1 : 0, 0, 0, 0.4f, 60);
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
                private float initialMoonFill;

                @Override
                public void render() {
                    try {
                        super.render();
                        if (frames() == 3) {
                            assertEquals(initial.hour(), value("Time of day"));
                            assertEquals(1, value("Snow"));
                            assertEquals(0, value("Rain"));
                            assertEquals(1, value("Ground fog"));
                            assertEquals(1, value("Haze"));
                            assertEquals(initial.groundLayerHeight(), value("Ground layer height"));
                            CheckBox fixed = GpuBoardTestUi.stage().getRoot().findActor("tuning-fixed-sun");
                            assertFalse(fixed.isChecked());
                            assertEquals(BoardAtmosphere.MOONLIGHT_SHADOW_CONTRAST, value("Moon shadow contrast"), 0.0001f);
                            initialMoonFill = renderedAtmosphere(this).lighting().ambient().r;
                            GpuBoardTestUi.stage().getRoot().<Slider>findActor("Moon shadow contrast").setValue(0);
                            GpuBoardTestUi.capture(new File(output, "weather-scenario-night-snow.png"));
                            GpuBoardTestUi.click("tuning");
                        } else if (frames() == 5) {
                            assertTrue(renderedAtmosphere(this).lighting().ambient().r > initialMoonFill,
                                  "The moon contrast slider must update the live light without changing scenario settings");
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
                            assertNull(GpuBoardTestUi.stage().getRoot().findActor("tuning-weather-wetness"));
                            assertNull(GpuBoardTestUi.stage().getRoot().findActor("Terrain wetness"));
                            assertNull(GpuBoardTestUi.stage().getRoot().findActor("Cloud density"));
                            assertNull(GpuBoardTestUi.stage().getRoot().findActor("cloud-quality-LOW"));
                            assertNull(GpuBoardTestUi.stage().getRoot().findActor("cloud-quality-MEDIUM"));
                            assertNull(GpuBoardTestUi.stage().getRoot().findActor("cloud-quality-HIGH"));
                            Slider rays = GpuBoardTestUi.stage().getRoot().findActor("God rays");
                            rays.setValue(0);
                            Slider glare = GpuBoardTestUi.stage().getRoot().findActor("Sun glare");
                            assertEquals(GpuAtmosphere.Options.DEFAULTS.sunGlare(), glare.getValue(), 0.0001f);
                            glare.setValue(0);
                            assertEquals(GpuAtmosphere.FOG_HEIGHT_VARIATION, value("Fog height variation"), 0.0001f);
                            assertEquals(GpuAtmosphere.FOG_DENSITY_VARIATION, value("Fog density variation"), 0.0001f);
                            GpuBoardTestUi.stage().getRoot().<Slider>findActor("Fog height variation").setValue(1.25f);
                            GpuBoardTestUi.stage().getRoot().<Slider>findActor("Fog density variation").setValue(0.5f);
                            Slider minimum = GpuBoardTestUi.stage().getRoot().findActor("Cloud shadow min");
                            Slider maximum = GpuBoardTestUi.stage().getRoot().findActor("Cloud shadow max");
                            assertEquals(GpuClouds.MIN_SHADOW_STRENGTH, minimum.getValue(), 0.0001f);
                            assertEquals(GpuClouds.MAX_SHADOW_STRENGTH, maximum.getValue(), 0.0001f);
                            minimum.setValue(0.2f);
                            maximum.setValue(0.3f);
                            minimum.setValue(0.4f);
                            assertEquals(minimum.getValue(), maximum.getValue(), "The panel must display the effective range");
                            minimum.setValue(0.1f);
                            Slider cover = GpuBoardTestUi.stage().getRoot().findActor("Cloud cover");
                            Slider wind = GpuBoardTestUi.stage().getRoot().findActor("Wind strength");
                            wind.setValue(0);
                            cover.setValue(0.05f);
                            assertEquals(0, wind.getValue());
                            wind.setValue(0.1f);
                            assertEquals(0.1f, wind.getValue(), "Cloud cover preserves the selected wind strength");
                            cover.setValue(0);
                            wind.setValue(0);
                            assertEquals(0, wind.getValue(), "Clear weather permits calm wind");
                            GpuBoardTestUi.click("tuning-fixed-sun");
                            boardCamera.orbit(60, 10);
                        } else if (frames() == 10) {
                            assertEquals(13, value("Time of day"), "Frame updates must preserve tuning overrides");
                            assertEquals(0, value("Snow"));
                            assertTrue(value("Rain") > 0);
                            assertEquals(0, value("God rays"));
                            assertEquals(0, value("Sun glare"));
                            assertEquals(BoardAtmosphere.MOONLIGHT_SHADOW_CONTRAST, value("Moon shadow contrast"), 0.0001f,
                                  "The Clear conditions preset restores the extra effect constants");
                            assertEquals(1.25f, value("Fog height variation"), 0.0001f);
                            assertEquals(0.5f, value("Fog density variation"), 0.0001f);
                            assertEquals(0.1f, value("Cloud shadow min"), 0.0001f);
                            assertEquals(0.4f, value("Cloud shadow max"), 0.0001f);
                            CheckBox fixed = GpuBoardTestUi.stage().getRoot().findActor("tuning-fixed-sun");
                            assertTrue(fixed.isChecked());
                            assertNotEquals(BoardAtmosphere.lighting(BoardAtmosphere.DEFAULTS).direction(),
                                  renderedAtmosphere(this).lighting().direction(), "The live checkbox must change the light direction");
                            GpuBoardTestUi.capture(new File(output, "weather-rain-controls.png"));
                            GpuBoardTestUi.click("weather-toggle-Rain");
                            assertEquals(0, value("Rain"));
                            GpuBoardTestUi.click("tuning-defaults");
                        } else if (frames() == 12) {
                            assertEquals(0.5f, value("God rays"));
                            assertEquals(GpuAtmosphere.Options.DEFAULTS.sunGlare(), value("Sun glare"), 0.0001f);
                            assertEquals(BoardAtmosphere.MOONLIGHT_SHADOW_CONTRAST, value("Moon shadow contrast"), 0.0001f);
                            assertEquals(GpuAtmosphere.FOG_HEIGHT_VARIATION, value("Fog height variation"), 0.0001f);
                            assertEquals(GpuAtmosphere.FOG_DENSITY_VARIATION, value("Fog density variation"), 0.0001f);
                            assertEquals(GpuClouds.MIN_SHADOW_STRENGTH, value("Cloud shadow min"), 0.0001f);
                            assertEquals(GpuClouds.MAX_SHADOW_STRENGTH, value("Cloud shadow max"), 0.0001f);
                            assertEquals(initial.effects().wind(), value("Wind strength"));
                            CheckBox fixed = GpuBoardTestUi.stage().getRoot().findActor("tuning-fixed-sun");
                            assertTrue(fixed.isChecked(), "Defaults keeps the fixed sun/moon frame the user chose");
                            assertNotEquals(BoardAtmosphere.lighting(BoardAtmosphere.DEFAULTS).direction(),
                                  renderedAtmosphere(this).lighting().direction());
                            assertEquals(initial.hour(), value("Time of day"), "Defaults restores the original moonlit time");
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
                            SwingUtilities.invokeAndWait(() -> {
                                fixture.game.getPlanetaryConditions().setLight(Light.MOONLESS);
                                fixture.source.refresh();
                            });
                        } else if (frames() == 14) {
                            assertFalse(renderedAtmosphere(this).lighting().hasDirectLight());
                            assertTrue(GpuBoardTestUi.stage().getRoot().<Slider>findActor("Moon shadow contrast").isDisabled());
                            Slider time = GpuBoardTestUi.stage().getRoot().findActor("Time of day");
                            time.setValue(13);
                        } else if (frames() == 16) {
                            assertTrue(renderedAtmosphere(this).lighting().hasDirectLight(), "A daytime preview still gets sunlight");
                            Slider time = GpuBoardTestUi.stage().getRoot().findActor("Time of day");
                            time.setValue(0);
                            GpuBoardTestUi.click("tuning-fixed-sun");
                        } else if (frames() == 18) {
                            assertFalse(renderedAtmosphere(this).lighting().hasDirectLight(),
                                  "Manual time and the fixed-light option must preserve an explicit moonless night");
                            assertEquals(0, fixture.clicks.get());
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
            if (failure.get() != null) {
                throw new AssertionError(failure.get());
            }
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(Light.MOONLESS, fixture.game.getPlanetaryConditions().getLight());
                assertEquals(Weather.HEAVY_SNOW, fixture.game.getPlanetaryConditions().getWeather());
                assertEquals(Fog.FOG_HEAVY, fixture.game.getPlanetaryConditions().getFog());
            });
        }
    }

    private GpuAtmosphere renderedAtmosphere(GpuBattleView view) throws ReflectiveOperationException {
        var field = GpuBattleView.class.getDeclaredField("atmosphere");
        field.setAccessible(true);
        return (GpuAtmosphere) field.get(view);
    }

    @Test
    void controlsChangeTheLiveSceneAndKeepTheirPanelInsideTheViewport() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            float initialHour = fixture.source.takeFrame().scenarioAtmosphere().hour();
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
                            slider("Time of day").setValue(13);
                        } else if (frames() == 4) {
                            daylight = GpuBoardTestUi.capture(new File(output, "atmosphere-board-day.png"));
                            GpuBoardTestUi.click("tuning");
                        } else if (frames() == 6) {
                            GpuBoardTestUi.click("atmosphere-LIGHT_FOG");
                            slider("Time of day").setValue(17.5f);
                        } else if (frames() == 8) {
                            assertEquals(17.5f, slider("Time of day").getValue(), "The clock can override a complete preset");
                            assertEquals(AtmospherePreset.LIGHT_FOG.settings(0.5).fog(),
                                  slider("Ground fog").getValue(), 0.001f);
                            assertNotEquals(daylight, GpuBoardTestUi.capture(new File(output, "atmosphere-controls.png")));
                            GpuBoardTestUi.click("tuning");
                        } else if (frames() == 10) {
                            GpuBoardTestUi.capture(new File(output, "atmosphere-board-sunset-mist.png"));
                            slider("Time of day").setValue(0);
                        } else if (frames() == 12) {
                            GpuBoardTestUi.capture(new File(output, "atmosphere-board-night.png"));
                            slider("Time of day").setValue(6);
                            slider("Cloud cover").setValue(0);
                            slider("Ground fog").setValue(0);
                            slider("Haze").setValue(0);
                        } else if (frames() == 14) {
                            GpuBoardTestUi.capture(new File(output, "atmosphere-board-dawn.png"));
                            slider("Cloud cover").setValue(1);
                        } else if (frames() == 16) {
                            GpuBoardTestUi.capture(new File(output, "atmosphere-board-dawn-overcast.png"));
                            slider("Time of day").setValue(13);
                        } else if (frames() == 18) {
                            GpuBoardTestUi.capture(new File(output, "atmosphere-board-day-overcast.png"));
                            slider("Time of day").setValue(0);
                        } else if (frames() == 20) {
                            GpuBoardTestUi.click("tuning");
                            GpuBoardTestUi.click("atmosphere-HEAVY_FOG");
                            assertEquals(AtmospherePreset.HEAVY_FOG.settings(0.5).clouds(), slider("Cloud cover").getValue(), 0.001f);
                            GpuBoardTestUi.click("atmosphere-CLEAR");
                            assertEquals(0, slider("Cloud cover").getValue());
                            assertEquals(0, slider("Ground fog").getValue());
                            assertEquals(0, slider("Haze").getValue());
                            assertEquals(fixture.source.atmosphereFor(AtmospherePreset.CLEAR).hour(),
                                  slider("Time of day").getValue(), "Clear day restores scenario-derived daylight");
                            GpuBoardTestUi.click("tuning-defaults");
                            assertEquals(initialHour, slider("Time of day").getValue());
                            assertEquals(0, slider("Cloud cover").getValue());
                            Gdx.graphics.setWindowedMode(900, 600);
                        } else if (frames() == 24) {
                            Actor panel = GpuBoardTestUi.stage().getRoot().findActor("board-tuning");
                            assertTrue(panel.getX() >= 0 && panel.getRight() <= GpuBoardTestUi.stage().getWidth());
                            assertTrue(panel.getY() >= GpuBoardUi.TURN_HEIGHT);
                            assertTrue(panel.getTop() <= GpuBoardTestUi.stage().getHeight() - GpuBoardUi.TOP_HEIGHT);
                            GpuBoardTestUi.capture(new File(output, "atmosphere-controls-small.png"));
                            assertEquals(0, fixture.clicks.get(), "Visual controls must not issue orders");
                            GpuBoardTestUi.click("tuning");
                            slider("Time of day").setValue(6);
                            slider("Cloud cover").setValue(0);
                            slider("Ground fog").setValue(0);
                            slider("Haze").setValue(0);
                            slider("Sun glare").setValue(0);
                            Gdx.graphics.setWindowedMode(1280, 800);
                        } else if (frames() == 26) {
                            Vector3 sun = BoardAtmosphere.lighting(new BoardAtmosphere.Settings(6, 0, 0, 2.5f, 0, 0))
                                  .direction();
                            boardCamera.setIsometric(false);
                            boardCamera.orbit(com.badlogic.gdx.math.MathUtils.atan2Deg(sun.x, -sun.y), 70);
                            boardCamera.fit(fixture.source.takeFrame().scene());
                        } else if (frames() == 28) {
                            GpuBoardTestUi.capture(new File(output, "atmosphere-board-sun-glare-off.png"));
                            slider("Sun glare").setValue(GpuAtmosphere.Options.DEFAULTS.sunGlare());
                        } else if (frames() == 30) {
                            GpuBoardTestUi.capture(new File(output, "atmosphere-board-sun-glare-on.png"));
                        } else if (frames() == 32) {
                            slider("Sun glare").setValue(0);
                            slider("Time of day").setValue(13);
                            slider("Ground layer height").setValue(2.5f);
                            slider("Blowing sand").setValue(1);
                            slider("Wind strength").setValue(0.4f);
                            boardCamera.setIsometric(false);
                            boardCamera.orbit(35, 75);
                            boardCamera.fit(fixture.source.takeFrame().scene());
                        } else if (frames() == 34) {
                            GpuBoardTestUi.capture(new File(output, "atmosphere-board-sand-volume.png"));
                            slider("Blowing sand").setValue(0);
                            slider("Ground fog").setValue(0.6f);
                        } else if (frames() == 36) {
                            GpuBoardTestUi.capture(new File(output, "atmosphere-board-fog-volume.png"));
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
