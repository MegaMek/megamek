/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import megamek.common.board.Coords;
import megamek.common.planetaryConditions.Atmosphere;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Exercise the actual composite shader, angular camera response and opaque-depth occlusion. */
@Tag("on-demand")
class GpuSunGlareSmokeTest {
    private final File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));

    @Test
    void glareFollowsTheSunAndHonorsDepthWithoutAnotherPass() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    checkGlare();
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) { throw new AssertionError(failure.get()); }
    }

    private void checkGlare() throws Exception {
        assertTrue(output.isDirectory() || output.mkdirs());
        GpuAtmosphere atmosphere = new GpuAtmosphere();
        GpuTerrain terrain = new GpuTerrain();
        var tile = new BoardScene.Tile(new Coords(0, 0), 0, -1, false, 0, BoardScene.Surface.GRASS,
              null, null, null, List.of(), List.of());
        BoardScene scene = new BoardScene(0, 1, 1, List.of(tile), List.of(), List.of(), -1, "", List.of());
        BoardCamera camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        try {
            for (float hour : new float[] { 6, 18 }) {
                var settings = settings(hour, 0, 0, Atmosphere.STANDARD, true);
                faceSun(camera, settings, 70);
                Glare facing = measure(atmosphere, terrain, scene, camera, settings, false, false, "sun-glare-" + hour);
                assertTrue(facing.energy() > 0.3 && facing.peak() > 120,
                      "Facing a low sun must produce a bright photographic flare");
                assertTrue(Math.abs(facing.peakX() - 0.5) < 0.02 && facing.peakY() > 0.65,
                      "The bright source stays above the center; reflections extend below it");
                assertEquals(0, measure(atmosphere, terrain, scene, camera, settings, false, true, null).energy(),
                      "Opaque depth over the source must suppress the whole flare");
                camera.orbit(20, 0);
                Glare turned = measure(atmosphere, terrain, scene, camera, settings, false, false, null);
                assertTrue(turned.energy() > 0 && Math.abs(turned.peakX() - facing.peakX()) > 0.07,
                      "Orbiting must move the glow across the screen with the sun bearing");
                camera.orbit(160, 0);
                assertEquals(0, measure(atmosphere, terrain, scene, camera, settings, false, false, null).energy(),
                      "The sun behind the camera cannot produce glare");
                camera.setIsometric(false);
                assertEquals(0, measure(atmosphere, terrain, scene, camera, settings, false, false, null).energy(),
                      "Looking straight down cannot face a low sun");
                faceSun(camera, settings, 54.73561f);
                assertTrue(measure(atmosphere, terrain, scene, camera, settings, false, false, null).energy() > 0.1,
                      "The normal isometric tilt must show a subtle glow at the upper edge");
            }
            var dawn = settings(6, 0, 0, Atmosphere.STANDARD, true);
            faceSun(camera, dawn, 70);
            Glare clear = measure(atmosphere, terrain, scene, camera, dawn, false, false, null);
            Glare overcast = measure(atmosphere, terrain, scene, camera,
                  settings(6, 1, 0, Atmosphere.STANDARD, true), false, false, null);
            Glare fog = measure(atmosphere, terrain, scene, camera,
                  settings(6, 0, 1, Atmosphere.STANDARD, true), false, false, null);
            assertTrue(overcast.energy() < clear.energy() * 0.02, "Overcast must strongly suppress direct glare");
            assertTrue(fog.energy() < clear.energy() * 0.4, "Heavy fog must weaken the glare");
            var day = settings(8, 0, 0, Atmosphere.STANDARD, true);
            faceSun(camera, day, 70);
            assertTrue(measure(atmosphere, terrain, scene, camera, day, false, false, null).energy() < clear.energy(),
                  "The low sun must be more pronounced than later daylight");
            for (boolean moon : List.of(false, true)) {
                var night = settings(0, 0, 0, Atmosphere.STANDARD, moon);
                faceSun(camera, night, 70);
                assertEquals(0, measure(atmosphere, terrain, scene, camera, night, false, false, null).energy(),
                      "Neither full moon nor moonless/pitch black night gets a solar flare");
            }
            var vacuum = settings(6, 0, 0, Atmosphere.VACUUM, true);
            faceSun(camera, vacuum, 70);
            assertTrue(measure(atmosphere, terrain, scene, camera, vacuum, false, false, null).energy() > 0,
                  "Optical glare does not require atmospheric clouds or scattering");
            var fixed = settings(12, 0, 0, Atmosphere.STANDARD, true);
            Glare reference = measure(atmosphere, terrain, scene, camera, fixed, true, false, null);
            assertEquals(0, reference.energy(), "The existing fixed light stays on the camera's side of the board");
            camera.orbit(100, -35);
            camera.camera.zoom *= 1.5f;
            camera.focus.add(100, 200, 0);
            camera.update();
            Glare moved = measure(atmosphere, terrain, scene, camera, fixed, true, false, null);
            assertEquals(reference.energy(), moved.energy(), 0.002, "Fixed sun keeps the same optical intensity");
            assertEquals(reference.x(), moved.x(), 0.001, "Fixed sun keeps the same screen position");
            assertEquals(reference.y(), moved.y(), 0.001);
            measureCost(atmosphere, terrain, scene, camera, dawn);
            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
        } finally {
            terrain.dispose();
            atmosphere.dispose();
        }
    }

    private void measureCost(GpuAtmosphere atmosphere, GpuTerrain terrain, BoardScene scene, BoardCamera camera,
          BoardAtmosphere.Settings settings) throws Exception {
        Gdx.graphics.setVSync(false);
        faceSun(camera, settings, 70);
        atmosphere.configure(settings);
        StringBuilder report = new StringBuilder("Renderer: " + Gdx.gl.glGetString(GL20.GL_RENDERER)
              + "\nUnobstructed sky, " + (int) camera.camera.viewportWidth + "x" + (int) camera.camera.viewportHeight
              + "; alternate glare off/default; 16 warmups, 30 samples each; glFinish outside the measured composite\n");
        try (var timing = new GpuStageTimings()) {
            for (int frame = 0; frame < 76; frame++) {
                boolean enabled = frame % 2 == 0;
                atmosphere.setOptions(new GpuAtmosphere.Options(0, false, 0, 0,
                      enabled ? GpuAtmosphere.Options.DEFAULTS.sunGlare() : 0));
                atmosphere.updateLight(camera.camera);
                atmosphere.begin((int) camera.camera.viewportWidth, (int) camera.camera.viewportHeight, 0);
                if (frame >= 16) {
                    timing.beginFrame();
                    timing.stage(enabled ? "Composite with glare" : "Composite without glare");
                }
                atmosphere.end(camera.camera, terrain, scene, 0);
                if (frame >= 16) { timing.stage(null); }
                Gdx.gl.glFinish();
            }
            timing.appendReport(report, "One composite draw in both modes; scene capture/UI excluded");
        }
        Files.writeString(new File(output, "sun-glare-timing.txt").toPath(), report);
    }

    private static BoardAtmosphere.Settings settings(float hour, float clouds, float fog, Atmosphere pressure, boolean moon) {
        return new BoardAtmosphere.Settings(hour, clouds, fog, 2.5f, 0, 0,
              BoardAtmosphere.Effects.NONE, pressure, 25, moon);
    }

    private static void faceSun(BoardCamera camera, BoardAtmosphere.Settings settings, float tilt) {
        Vector3 direction = BoardAtmosphere.lighting(settings).direction();
        camera.setIsometric(false);
        camera.orbit(MathUtils.atan2Deg(direction.x, -direction.y), tilt);
    }

    private record Glare(double energy, double x, double y, int peak, double peakX, double peakY) { }

    private Glare measure(GpuAtmosphere atmosphere, GpuTerrain terrain, BoardScene scene, BoardCamera camera,
          BoardAtmosphere.Settings settings, boolean fixed, boolean occluded, String capture) throws Exception {
        Pixmap before = frame(atmosphere, terrain, scene, camera, settings, fixed, occluded, 0);
        Pixmap after = frame(atmosphere, terrain, scene, camera, settings, fixed, occluded,
              GpuAtmosphere.Options.DEFAULTS.sunGlare());
        try {
            double total = 0, xSum = 0, ySum = 0;
            int peak = 0;
            int peakX = 0, peakY = 0;
            for (int y = 0; y < after.getHeight(); y++) {
                for (int x = 0; x < after.getWidth(); x++) {
                    int a = before.getPixel(x, y), b = after.getPixel(x, y);
                    int delta = ((b >>> 24) - (a >>> 24))
                          + (((b >>> 16) & 255) - ((a >>> 16) & 255))
                          + (((b >>> 8) & 255) - ((a >>> 8) & 255));
                    assertTrue(delta >= 0, "Adding glare must not darken the scene");
                    if (delta > peak) {
                        peak = delta;
                        peakX = x;
                        peakY = y;
                    }
                    total += delta;
                    xSum += x * delta;
                    ySum += y * delta;
                }
            }
            if (capture != null) { GpuBoardTestUi.capture(new File(output, capture + ".png")); }
            return new Glare(total / (after.getWidth() * after.getHeight()),
                  total == 0 ? 0 : xSum / total / after.getWidth(),
                  total == 0 ? 0 : ySum / total / after.getHeight(), peak,
                  peakX / (double) after.getWidth(), peakY / (double) after.getHeight());
        } finally {
            before.dispose();
            after.dispose();
        }
    }

    private static Pixmap frame(GpuAtmosphere atmosphere, GpuTerrain terrain, BoardScene scene, BoardCamera camera,
          BoardAtmosphere.Settings settings, boolean fixed, boolean occluded, float glare) {
        atmosphere.configure(settings);
        atmosphere.setOptions(new GpuAtmosphere.Options(0, fixed, 0, 0, glare));
        atmosphere.updateLight(camera.camera);
        atmosphere.begin((int) camera.camera.viewportWidth, (int) camera.camera.viewportHeight, 0);
        if (occluded) {
            Gdx.gl.glClearDepthf(0.5f);
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
            Gdx.gl.glClearDepthf(1);
        }
        var profiler = new GLProfiler(Gdx.graphics);
        profiler.enable();
        try {
            atmosphere.end(camera.camera, terrain, scene, 0);
            assertEquals(settings.fog() > 0 ? 2 : 1, profiler.getDrawCalls(),
                  "Glare must use the existing composite without another pass");
        } finally {
            profiler.disable();
        }
        return Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    }
}
