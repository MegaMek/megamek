/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.AtmosphericTaint;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.planetaryConditions.Weather;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("on-demand")
class GpuBoardTuningSmokeTest {
    @Test
    void presetsAndDerivedControlsStayVisualAndResetEffectTuning() throws Exception {
        try (var fixture = GpuBoardFixture.create()) {
            var initial = fixture.source.takeFrame().scenarioAtmosphere();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            new Lwjgl3Application(new ApplicationAdapter() {
                @Override
                public void create() {
                    try {
                        checkControls(fixture.source, initial);
                    } catch (Throwable error) {
                        failure.set(error);
                    } finally {
                        Gdx.app.exit();
                    }
                }
            }, GpuBoardWindow.configuration(false));
            if (failure.get() != null) { throw new AssertionError(failure.get()); }
            SwingUtilities.invokeAndWait(() -> assertEquals(initial,
                  fixture.source.atmosphereFor(fixture.game.getPlanetaryConditions(), false)));
            assertEquals(0, fixture.clicks.get(), "Tuning must never issue gameplay orders");
        }
    }

    private void checkControls(GpuBoardSource source, BoardAtmosphere.Settings initial) {
        var skin = new GpuBoardSkin();
        var stage = new Stage(new ScreenViewport());
        try {
            var tuning = new GpuBoardTuning(skin.skin, source);
            tuning.useScenario(initial, true);
            stage.addActor(tuning.panel());
            Gdx.input.setInputProcessor(new InputMultiplexer(stage));
            var dock = new GpuPanelDock(skin.skin, () -> { }, null, tuning.panel());
            dock.resize(1280, 800, 0, 0, 0, 0);
            dock.show(tuning.panel());
            stage.act(0);
            stage.draw();
            assertTrue(tuning.panel().findActor("tuning-general-scroll").isVisible());
            assertFalse(tuning.panel().findActor("tuning-scroll").isVisible());
            for (var family : UnitFamilyScale.values()) {
                Slider slider = tuning.panel().findActor("tuning-size-" + family.name());
                assertEquals(1, slider.getValue(), "Family sizes start neutral");
                slider.setValue(1.5f);
                assertEquals(1.5f, family.UNIT_SCALE);
                assertEquals(family.heightScale(), family.HEIGHT_SCALE, "Uniform size does not change height proportions");
            }
            capture(stage, "tuning-general.png");
            GpuBoardTestUi.click("tuning-defaults");
            for (var family : UnitFamilyScale.values()) { assertEquals(1, family.UNIT_SCALE); }
            ScrollPane generalScroll = tuning.panel().findActor("tuning-general-scroll");
            generalScroll.setScrollPercentY(0.6f);
            generalScroll.updateVisualScroll();
            float generalPosition = generalScroll.getScrollY();
            GpuBoardTestUi.click("tuning-tab-atmosphere");
            assertFalse(generalScroll.isVisible());
            assertTrue(tuning.panel().findActor("tuning-scroll").isVisible());
            GpuBoardTestUi.click("tuning-tab-general");
            assertEquals(generalPosition, generalScroll.getScrollY(), "Each tab preserves its scroll position");
            var defaultEffects = tuning.atmosphereOptions();
            assertNull(tuning.panel().findActor("Speed gain / hex"));
            set(tuning, "God rays", 0.8f);
            set(tuning, "Cloud shadow min", 0.3f);
            set(tuning, "Cloud shadow max", 0.9f);
            set(tuning, "Moon shadow contrast", 0.45f);
            set(tuning, "Sun glare", 0.2f);
            set(tuning, "Fog height variation", 0.5f);
            set(tuning, "Fog density variation", 0.4f);
            set(tuning, "Taint strength", 1.5f);
            for (var preset : AtmospherePreset.values()) {
                GpuBoardTestUi.click("atmosphere-" + preset.name());
                assertEquals(source.atmosphereFor(preset), tuning.atmosphere(), preset.label);
                assertEquals(defaultEffects, tuning.atmosphereOptions(), "Presets reset the extra controls to their constants");
                assertEquals(tuning.atmosphere().gravity(), tuning.gravityOverride());
                set(tuning, "God rays", 0.8f);
                set(tuning, "Moon shadow contrast", 0.45f);
            }
            var rainyConditions = new PlanetaryConditions();
            rainyConditions.setWeather(Weather.HEAVY_RAIN);
            var rainy = source.atmosphereFor(rainyConditions, false);
            tuning.useScenario(rainy, true);
            set(tuning, "Temperature (C)", 10);
            assertEquals(rainy.clouds(), tuning.atmosphere().clouds(), 0.000001f,
                  "Editing temperature must not round scenario-derived cloud cover to a different value");
            tuning.useScenario(initial, true);
            assertTrue(Float.isNaN(tuning.gravityOverride()), "Without an override, jumps retain their captured gravity");
            set(tuning, "Gravity (g)", 0.5f);
            assertEquals(0.5f, tuning.gravityOverride());
            GpuBoardTestUi.click("atmosphere-FULL_MOON");
            GpuBoardTestUi.click("tuning-moonlight");
            assertFalse(tuning.atmosphere().moonlight());
            assertFalse(BoardAtmosphere.lighting(tuning.atmosphere()).hasDirectLight());
            GpuBoardTestUi.click("atmosphere-PITCH_BLACK");
            assertEquals(-1, tuning.atmosphere().exposure());
            assertFalse(tuning.panel().<CheckBox>findActor("tuning-moonlight").isChecked());
            GpuBoardTestUi.click("atmosphere-MOONLESS");
            assertEquals(-0.6f, tuning.atmosphere().exposure(), 0.00001f);
            assertFalse(tuning.atmosphere().moonlight());
            set(tuning, "Time of day", 12);
            assertTrue(BoardAtmosphere.lighting(tuning.atmosphere()).hasDirectLight());
            set(tuning, "Time of day", 0);
            assertFalse(BoardAtmosphere.lighting(tuning.atmosphere()).hasDirectLight());

            GpuBoardTestUi.click("atmosphere-RAIN_STORM");
            assertTrue(BoardAtmosphere.wetness(tuning.atmosphere()) > 0);
            set(tuning, "Temperature (C)", 0);
            assertEquals(0, BoardAtmosphere.wetness(tuning.atmosphere()));
            set(tuning, "Temperature (C)", 10);
            assertTrue(BoardAtmosphere.wetness(tuning.atmosphere()) > 0);
            SelectBox<Atmosphere> pressure = tuning.panel().findActor("tuning-atmosphere-pressure");
            pressure.setSelected(Atmosphere.VACUUM);
            assertEquals(BoardAtmosphere.Effects.NONE, tuning.atmosphere().effects());
            assertEquals(0, tuning.atmosphere().clouds());
            assertTrue(tuning.panel().<Slider>findActor("Rain").isDisabled());
            pressure.setSelected(Atmosphere.STANDARD);
            assertFalse(tuning.panel().<Slider>findActor("Rain").isDisabled());
            SelectBox<AtmosphericTaint> taint = tuning.panel().findActor("tuning-atmospheric-taint");
            taint.setSelected(AtmosphericTaint.TOXIC_POISON);
            assertEquals(taint.getSelected(), tuning.atmosphere().taint());
            assertFalse(tuning.panel().<Slider>findActor("Taint strength").isDisabled());
            set(tuning, "Ground fog", 0.6f);
            assertEquals(AtmosphericTaint.TOXIC_POISON, tuning.atmosphere().taint());
            capture(stage, "tuning-atmosphere.png");
            GpuBoardTestUi.click("tuning-atmospheric-taint");
            assertTrue(taint.getScrollPane().hasParent(), "The dropdown must open through actual pointer input");
            stage.act(0.3f);
            capture(stage, "tuning-taint-choices.png");
            taint.hideList();
            stage.act(0.3f);

            GpuBoardTestUi.click("tuning-defaults");
            assertEquals(initial, tuning.atmosphere());
            assertEquals(defaultEffects, tuning.atmosphereOptions());
            for (int[] size : new int[][] { { 1280, 800 }, { 900, 600 } }) {
                dock.resize(size[0], size[1], 30, 45, 0, 0);
                stage.act(0);
                stage.draw();
                GpuBoardTestUi.assertHorizontalBounds(tuning.panel(), tuning.panel());
                assertTrue(tuning.panel().getTop() <= size[1] - 30);
                assertTrue(tuning.panel().getY() >= 45);
                GpuBoardTestUi.click("tuning-tab-general");
                GpuBoardTestUi.assertHorizontalBounds(tuning.panel(), tuning.panel());
                GpuBoardTestUi.click("tuning-tab-atmosphere");
            }
            GpuBoardTestUi.click("atmosphere-DAWN");
            capture(stage, "tuning-presets-small.png");
            ScrollPane scroll = tuning.panel().findActor("tuning-scroll");
            scroll.setScrollPercentY(1);
            scroll.updateVisualScroll();
            capture(stage, "tuning-effects-small.png");
        } finally {
            stage.dispose();
            skin.dispose();
        }
    }

    private static void set(GpuBoardTuning tuning, String name, float value) {
        tuning.panel().<Slider>findActor(name).setValue(value);
    }

    private static void capture(Stage stage, String name) {
        ScreenUtils.clear(0.12f, 0.16f, 0.2f, 1);
        stage.act(0);
        stage.draw();
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            var directory = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
            assertTrue(directory.isDirectory() || directory.mkdirs());
            PixmapIO.writePNG(new FileHandle(new File(directory, name)), pixels, -1, true);
        } finally {
            pixels.dispose();
        }
    }
}
