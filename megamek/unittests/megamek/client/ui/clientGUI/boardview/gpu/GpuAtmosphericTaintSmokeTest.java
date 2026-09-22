/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import megamek.common.board.Coords;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.AtmosphericTaint;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Native palette propagation, unchanged draw budget, surface colors, and the live tuning control. */
@Tag("on-demand")
class GpuAtmosphericTaintSmokeTest {
    private final File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));

    @Test
    void taintReusesExistingPassesAndTuningUpdatesTheCachedPalette() {
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
        if (failure.get() != null) { throw new AssertionError(failure.get()); }
    }

    private void checkRendering() throws Exception {
        assertTrue(output.isDirectory() || output.mkdirs());
        var camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.setIsometric(true);
        var scene = scene();
        camera.fit(scene);
        var atmosphere = new GpuAtmosphere();
        var terrain = new GpuTerrain();
        var profiler = new GLProfiler(Gdx.graphics);
        StringBuilder report = new StringBuilder("Renderer: " + Gdx.gl.glGetString(GL20.GL_RENDERER) + "\n");
        try {
            terrain.update(scene);
            atmosphere.setOptions(new GpuAtmosphere.Options(0, false, 0, 0, 0));
            profiler.enable();
            for (float fog : new float[] { 0, 0.8f }) {
                int originalSky = 0, originalGround = 0;
                for (var taint : AtmosphericTaint.values()) {
                    var settings = settings(12, fog, taint, Atmosphere.STANDARD);
                    atmosphere.configure(settings);
                    atmosphere.updateLight(camera.camera);
                    terrain.setAtmosphere(atmosphere.lighting());
                    terrain.renderShadows(List.of());
                    atmosphere.prepareClouds(terrain, scene, 0);
                    ScreenUtils.clear(0, 0, 0, 1, true);
                    atmosphere.begin((int) camera.camera.viewportWidth, (int) camera.camera.viewportHeight, 0);
                    terrain.render(camera.camera, false);
                    profiler.reset();
                    atmosphere.end(camera.camera, terrain, scene, 0);
                    assertEquals(fog > 0 ? 2 : 1, profiler.getDrawCalls(), "Taint must not enable a pass: " + taint);
                    report.append(taint.name()).append(" fog=").append(fog).append(": ")
                          .append(profiler.getDrawCalls()).append(" postprocess draws\n");
                    Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(),
                          Gdx.graphics.getBackBufferHeight());
                    try {
                        var ground = camera.camera.project(BoardGeometry.center(new Coords(4, 4), 0));
                        int x = Math.round(ground.x * pixels.getWidth() / camera.camera.viewportWidth);
                        int y = Math.round(ground.y * pixels.getHeight() / camera.camera.viewportHeight);
                        int skyColor = pixels.getPixel(5, pixels.getHeight() - 6);
                        int groundColor = pixels.getPixel(x, y);
                        if (taint.isBreathable()) {
                            originalSky = skyColor;
                            originalGround = groundColor;
                        } else {
                            assertNotEquals(originalSky, skyColor, "Existing sky uniforms must carry the taint palette");
                            if (fog == 0) {
                                assertEquals(originalGround, groundColor, "Clear-air taint must preserve terrain paint");
                            } else {
                                assertNotEquals(originalGround, groundColor, "Existing fog must inherit the taint color");
                            }
                        }
                    } finally {
                        pixels.dispose();
                    }
                    GpuBoardTestUi.capture(new File(output, "taint-" + taint.name() + (fog > 0 ? "-fog" : "-clear") + ".png"));
                }
            }
            profiler.disable();
            checkTuning(atmosphere);
            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
            Files.writeString(new File(output, "taint-draw-budget.txt").toPath(), report);
        } finally {
            profiler.disable();
            atmosphere.dispose();
            terrain.dispose();
        }
    }

    private void checkTuning(GpuAtmosphere atmosphere) throws Exception {
        var skin = new GpuBoardSkin();
        var stage = new Stage(new ScreenViewport());
        try {
            var tuning = new GpuBoardTuning(skin.skin);
            var original = settings(12, 0, AtmosphericTaint.TOXIC_POISON, Atmosphere.STANDARD);
            tuning.useScenario(original, true);
            Slider strength = tuning.panel().findActor("Taint strength");
            SelectBox<AtmosphericTaint> taint = tuning.panel().findActor("tuning-atmospheric-taint");
            assertFalse(strength.isDisabled());
            assertEquals(original.taint(), taint.getSelected());
            atmosphere.configure(original);
            atmosphere.setOptions(tuning.atmosphereOptions());
            var tinted = atmosphere.lighting();
            strength.setValue(0);
            atmosphere.setOptions(tuning.atmosphereOptions());
            var clear = BoardAtmosphere.lighting(settings(12, 0, AtmosphericTaint.BREATHABLE, Atmosphere.STANDARD));
            assertEquals(clear, atmosphere.lighting(), "Strength changes must invalidate the palette cache immediately");
            strength.setValue(1);
            atmosphere.setOptions(tuning.atmosphereOptions());
            assertEquals(tinted, atmosphere.lighting());
            var cached = atmosphere.lighting();
            atmosphere.configure(original);
            atmosphere.setOptions(tuning.atmosphereOptions());
            assertSame(cached, atmosphere.lighting(), "Unchanged settings must reuse the palette");

            Slider hour = tuning.panel().findActor("Time of day");
            hour.setValue(6);
            assertEquals(original.taint(), tuning.atmosphere().taint());
            tuning.panel().<Slider>findActor("Ground fog").setValue(0.6f);
            assertEquals(original.taint(), tuning.atmosphere().taint());
            strength.setValue(2);
            var updated = settings(12, 0, AtmosphericTaint.TAINTED_FLAME, Atmosphere.STANDARD);
            tuning.useScenario(updated, false);
            assertEquals(updated.taint(), tuning.atmosphere().taint());
            assertEquals(6, tuning.atmosphere().hour(), "Scenario taint updates preserve clock overrides");
            assertEquals(2, tuning.atmosphereOptions().taintStrength());
            assertEquals(updated.taint(), taint.getSelected());

            for (var unavailable : List.of(settings(12, 0, AtmosphericTaint.BREATHABLE, Atmosphere.STANDARD),
                  settings(12, 0, AtmosphericTaint.TOXIC_POISON, Atmosphere.VACUUM))) {
                tuning.useScenario(unavailable, false);
                assertTrue(strength.isDisabled());
            }
            TextButton defaults = tuning.panel().findActor("tuning-defaults");
            defaults.fire(new ChangeEvent());
            assertEquals(original, tuning.atmosphere());
            assertEquals(BoardAtmosphere.DEFAULT_TAINT_STRENGTH, tuning.atmosphereOptions().taintStrength());
            assertFalse(strength.isDisabled());
            stage.addActor(tuning.panel());
            tuning.panel().setBounds(20, 20, 340, Gdx.graphics.getHeight() - 40);
            stage.act(0);
            tuning.panel().validate();
            ScrollPane scroll = tuning.panel().findActor("tuning-scroll");
            scroll.setScrollPercentY(1);
            scroll.updateVisualScroll();
            stage.act(0);
            stage.draw();
            assertTrue(taint.getWidth() > 100, "The taint selector must have room to show its current value");
            GpuBoardTestUi.capture(new File(output, "taint-tuning.png"));
        } finally {
            stage.dispose();
            skin.dispose();
        }
    }

    private static BoardAtmosphere.Settings settings(float hour, float fog, AtmosphericTaint taint, Atmosphere pressure) {
        return new BoardAtmosphere.Settings(hour, 0, fog, 2, fog, 0, BoardAtmosphere.Effects.NONE, pressure, 25, true, taint);
    }

    private static BoardScene scene() {
        var image = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setColor(new java.awt.Color(145, 145, 145));
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        var pixels = new BoardScene.Pixels(image);
        var tiles = new ArrayList<BoardScene.Tile>();
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), x == 2 && y == 5 ? 2 : 0, -1, false, 0,
                      BoardScene.Surface.GRASS, pixels, null, null, List.of(), List.of()));
            }
        }
        return new BoardScene(0, 9, 9, tiles, List.of(), List.of(), -1, "", List.of());
    }
}
