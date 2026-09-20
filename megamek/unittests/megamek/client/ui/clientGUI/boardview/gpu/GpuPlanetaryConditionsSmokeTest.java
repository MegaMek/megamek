/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static megamek.client.ui.clientGUI.boardview.gpu.GpuCamouflageReview.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.AWTEvent;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import megamek.client.ui.clientGUI.CancelAction;
import megamek.client.ui.dialogs.clientDialogs.PlanetaryConditionsDialog;
import megamek.common.planetaryConditions.Fog;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.planetaryConditions.Weather;
import megamek.common.planetaryConditions.Wind;
import megamek.common.planetaryConditions.WindDirection;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Real Scene2D button, modal Swing dialog and return to the GL thread, without changing game conditions. */
@Tag("on-demand")
class GpuPlanetaryConditionsSmokeTest {
    @Test
    void acceptedConditionsUpdateThePreviewAndCancellationAndClosingAreSafe() throws Exception {
        var failure = new AtomicReference<Throwable>();
        var opened = new AtomicInteger();
        var selected = new PlanetaryConditions();
        selected.setLight(Light.DUSK);
        selected.setWeather(Weather.LIGHTNING_STORM);
        selected.setFog(Fog.FOG_LIGHT);
        selected.setWind(Wind.MOD_GALE);
        selected.setWindDirection(WindDirection.NORTHWEST);
        var expected = BoardAtmosphere.fromScenario(selected, false);
        try (var fixture = GpuBoardFixture.create()) {
            var initial = fixture.source.takeFrame().scenarioAtmosphere();
            AWTEventListener answerDialog = event -> {
                if (!(event instanceof WindowEvent window) || window.getID() != WindowEvent.WINDOW_OPENED
                      || !(window.getWindow() instanceof PlanetaryConditionsDialog dialog)) { return; }
                SwingUtilities.invokeLater(() -> {
                    try {
                        assertTrue(SwingUtilities.isEventDispatchThread());
                        assertTrue(dialog.isModal());
                        int request = opened.incrementAndGet();
                        assertEquals(initial, BoardAtmosphere.fromScenario(dialog.getConditions(), false),
                              "The editor starts from the scenario conditions");
                        if (request == 3) {
                            fixture.source.close();
                            assertFalse(dialog.isDisplayable(), "Closing the GPU view disposes its open dialog");
                            return;
                        }
                        dialog.update(selected);
                        if (request == 1) { assertTrue(cancel(dialog), "The existing dialog supplies its Cancel action"); }
                        else { ((JButton) field(dialog, "butOkay")).doClick(); }
                    } catch (Throwable error) {
                        failure.compareAndSet(null, error);
                        dialog.dispose();
                    }
                });
            };
            Toolkit.getDefaultToolkit().addAWTEventListener(answerDialog, AWTEvent.WINDOW_EVENT_MASK);
            try {
                new Lwjgl3Application(new GpuBattleView(fixture.source) {
                    private int step;
                    private final long deadline = System.nanoTime() + 60_000_000_000L;

                    @Override
                    public void render() {
                        try {
                            if (failure.get() != null) { throw new AssertionError(failure.get()); }
                            assertTrue(System.nanoTime() < deadline, "The dialog must return without blocking rendering");
                            super.render();
                            TextButton conditions = GpuBoardTestUi.stage().getRoot().findActor("tuning-planetary-conditions");
                            if (step == 0 && frames() >= 3) {
                                GpuBoardTestUi.click("tuning");
                                step++;
                            } else if (step == 1) {
                                GpuBoardTestUi.click("tuning-planetary-conditions");
                                assertTrue(conditions.isDisabled(), "One editor can be open at a time");
                                step++;
                            } else if (step == 2 && !conditions.isDisabled()) {
                                assertSettings(initial);
                                GpuBoardTestUi.click("tuning-planetary-conditions");
                                step++;
                            } else if (step == 3 && !conditions.isDisabled()) {
                                assertSettings(expected);
                                assertTrue(conditions.getWidth() >= conditions.getLabel().getPrefWidth(), "Button text fits");
                                GpuBoardTestUi.capture(new File(System.getProperty("megamek.gpu.screenshots"),
                                      "planetary-conditions-tuning.png"));
                                SwingUtilities.invokeAndWait(fixture.source::refresh);
                                step++;
                            } else if (step == 4) {
                                assertSettings(expected);
                                GpuBoardTestUi.click("tuning-defaults");
                                assertSettings(initial);
                                assertEquals(0, fixture.clicks.get());
                                GpuBoardTestUi.click("tuning-planetary-conditions");
                                step++;
                            }
                        } catch (Throwable error) {
                            failure.compareAndSet(null, error);
                            Gdx.app.exit();
                        }
                    }
                }, GpuBoardWindow.configuration(false));
                if (failure.get() != null) { throw new AssertionError("Planetary conditions preview failed", failure.get()); }
                assertEquals(3, opened.get());
                SwingUtilities.invokeAndWait(() -> assertEquals(initial,
                      BoardAtmosphere.fromScenario(fixture.game.getPlanetaryConditions(), false),
                      "Previewing conditions never changes the game"));
            } finally {
                Toolkit.getDefaultToolkit().removeAWTEventListener(answerDialog);
            }
        }
    }

    private static boolean cancel(Container parent) {
        for (var child : parent.getComponents()) {
            if (child instanceof JButton button && button.getAction() instanceof CancelAction) {
                button.doClick();
                return true;
            }
            if (child instanceof Container container && cancel(container)) { return true; }
        }
        return false;
    }

    private static void assertSettings(BoardAtmosphere.Settings settings) {
        String[] names = { "Time of day", "Cloud cover", "Ground fog", "Fog height", "Haze", "Exposure (EV)",
              "Rain", "Snow", "Hail", "Blowing sand", "Lightning", "Wind strength", "Wind direction" };
        var effects = settings.effects();
        float[] values = { settings.hour(), settings.clouds(), settings.fog(), settings.fogHeight(), settings.haze(),
              settings.exposure(), effects.rain(), effects.snow(), effects.hail(), effects.sand(), effects.lightning(),
              effects.wind(), effects.windDirection() };
        for (int index = 0; index < names.length; index++) {
            Slider slider = GpuBoardTestUi.stage().getRoot().findActor(names[index]);
            assertEquals(values[index], slider.getValue(), .001f, names[index]);
        }
    }
}
