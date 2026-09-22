/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import megamek.client.ui.clientGUI.boardview.BoardFieldOfView;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Native visual comparison: identical rules and camera, three choices of presentation. */
@Tag("on-demand")
class GpuFieldOfViewSmokeTest {
    private record Appearance(float visible, float blocked, float visibleChroma, float blockedChroma) { }

    @Test
    void allStylesShareVisibilityAndMaskCacheButRenderDifferentObscuration() throws Exception {
        Map<GpuFieldOfView.Style, Appearance> results = new EnumMap<>(GpuFieldOfView.Style.class);
        BoardFieldOfView expected = null;
        try (var options = new GpuFieldOfViewTest.Options()) {
            for (GpuFieldOfView.Style style : GpuFieldOfView.Style.values()) {
                GpuFieldOfView.Style sensorStyle = GpuFieldOfView.Style.values()[(style.ordinal() + 1) % 3];
                AtomicReference<Throwable> failure = new AtomicReference<>();
                try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
                    SwingUtilities.invokeAndWait(() -> {
                        fixture.view.select(new Coords(1, 6));
                        fixture.source.setVisibleArea(new Rectangle(0, 0, 16, 17));
                        fixture.source.refresh();
                    });
                    BoardFieldOfView mask = fixture.source.takeFrame().scene().fieldOfView();
                    assertTrue(mask.active());
                    if (expected != null) {
                        assertEquals(expected, mask, "Changing style must not change any visibility result");
                    }
                    expected = mask;
                    new Lwjgl3Application(new GpuBattleView(fixture.source) {
                        private int tick;
                        private GpuFieldOfView probe;
                        private Appearance noDarkening;

                        @Override
                        public void render() {
                            try {
                                super.render();
                                tick++;
                                if (tick == 1) {
                                    boardCamera.setIsometric(false);
                                    GpuBoardTestUi.click("tuning");
                                    assertDefaults();
                                    // Compare FoV at one hour, independently of each window's sampled scenario time.
                                    Slider time = GpuBoardTestUi.stage().getRoot().findActor("Time of day");
                                    time.setValue(13);
                                    GpuBoardTestUi.click("fov-style-" + style.name());
                                    GpuBoardTestUi.click("fov-style-" + style.name());
                                    assertTrue(modeButton("sensor", GpuFieldOfView.SENSOR_STYLE).isChecked(),
                                          "FoV controls must leave the sensor style unchanged");
                                    GpuBoardTestUi.click("sensor-style-" + sensorStyle.name());
                                    GpuBoardTestUi.click("sensor-style-" + sensorStyle.name());
                                    for (GpuFieldOfView.Style mode : GpuFieldOfView.Style.values()) {
                                        assertEquals(mode == style, modeButton("fov", mode).isChecked(),
                                              "Exactly one FoV mode must remain selected, even when clicked twice");
                                        assertEquals(mode == sensorStyle, modeButton("sensor", mode).isChecked(),
                                              "Exactly one sensor mode must remain selected, independently of FoV");
                                    }
                                    GpuBoardTestUi.click("tuning");
                                    probe = new GpuFieldOfView();
                                    probe.configure(style, GpuFieldOfView.FOV_DARKNESS, sensorStyle, GpuFieldOfView.SENSOR_DARKNESS);
                                    probe.update(mask);
                                    assertEquals(1, probe.uploads());
                                } else if (tick == 12) {
                                    capture("top");
                                    results.put(style, appearance(fixture.source.takeFrame().scene()));
                                    darknessSlider("FoV").setValue(0);
                                    probe.configure(style, 0, sensorStyle, GpuFieldOfView.SENSOR_DARKNESS);
                                } else if (tick == 16) {
                                    noDarkening = appearance(fixture.source.takeFrame().scene());
                                    capture("no-darkening");
                                    darknessSlider("FoV").setValue(100);
                                    probe.configure(style, 1, sensorStyle, GpuFieldOfView.SENSOR_DARKNESS);
                                } else if (tick == 20) {
                                    Appearance darkened = appearance(fixture.source.takeFrame().scene());
                                    assertTrue(darkened.blocked() < noDarkening.blocked() * 0.9f,
                                          "The darkness slider must darken blocked content in " + style);
                                    assertEquals(noDarkening.visible(), darkened.visible(), 0.01f,
                                          "The darkness slider must leave visible content unchanged");
                                    assertEquals(GpuFieldOfView.SENSOR_DARKNESS * 100, darknessSlider("Sensor").getValue(), 0.001f,
                                          "FoV darkness must not change sensor darkness");
                                    darknessSlider("FoV").setValue(GpuFieldOfView.FOV_DARKNESS * 100);
                                    boardCamera.setIsometric(true);
                                    boardCamera.fit(fixture.source.takeFrame().scene());
                                } else if (tick == 24) {
                                    capture("isometric");
                                    darknessSlider("FoV").setValue(50);
                                    darknessSlider("Sensor").setValue(50);
                                    GpuBoardTestUi.click("tuning");
                                    GpuBoardTestUi.click("tuning-defaults");
                                } else if (tick == 28) {
                                    assertDefaults();
                                    if (style == GpuFieldOfView.Style.GRAYSCALE) {
                                        capture("tuning");
                                    }
                                    assertEquals(mask, fixture.source.takeFrame().scene().fieldOfView(),
                                          "Presentation controls must not alter LOS/sensor data");
                                    probe.configure(GpuFieldOfView.FOV_STYLE, GpuFieldOfView.FOV_DARKNESS,
                                          GpuFieldOfView.SENSOR_STYLE, GpuFieldOfView.SENSOR_DARKNESS);
                                    probe.update(mask);
                                    assertEquals(1, probe.uploads(), "Camera and tuning changes must reuse an unchanged mask");
                                    probe.update(BoardFieldOfView.EMPTY);
                                    assertFalse(probe.active(), "Disabling FoV must immediately stop shading");
                                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                                    Gdx.app.exit();
                                }
                            } catch (Throwable error) {
                                failure.set(error);
                                Gdx.app.exit();
                            }
                        }

                        private void capture(String camera) {
                            File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
                            GpuBoardTestUi.capture(new File(output, "fov-" + style.name().toLowerCase(java.util.Locale.ROOT)
                                  + "-" + camera + ".png"));
                        }

                        private void assertDefaults() {
                            assertTrue(modeButton("fov", GpuFieldOfView.FOV_STYLE).isChecked());
                            assertTrue(modeButton("sensor", GpuFieldOfView.SENSOR_STYLE).isChecked());
                            assertEquals(GpuFieldOfView.FOV_DARKNESS * 100, darknessSlider("FoV").getValue(), 0.001f);
                            assertEquals(GpuFieldOfView.SENSOR_DARKNESS * 100, darknessSlider("Sensor").getValue(), 0.001f);
                        }

                        private Slider darknessSlider(String effect) {
                            return GpuBoardTestUi.stage().getRoot().findActor(effect + " darkness");
                        }

                        private TextButton modeButton(String effect, GpuFieldOfView.Style mode) {
                            return GpuBoardTestUi.stage().getRoot().findActor(effect + "-style-" + mode.name());
                        }

                        private Appearance appearance(BoardScene scene) {
                            Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0,
                                  Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
                            try {
                                float visible = 0, blocked = 0;
                                float visibleChroma = 0, blockedChroma = 0;
                                int visibleCount = 0, blockedCount = 0;
                                float scale = new GpuDisplayScale().read(fixture.source.uiPreferences.scale());
                                for (BoardScene.Tile tile : scene.tiles()) {
                                    // Unit HUD annotations cover nearby hexes and deliberately keep their colors.
                                    if (tile.water() || !tile.features().isEmpty()
                                          || tile.coords().distance(fixture.entity.getPosition()) <= 2) {
                                        continue;
                                    }
                                    Vector3 point = BoardGeometry.center(tile.coords(), tile.elevation())
                                          .add(BoardGeometry.WIDTH * 0.10f, BoardGeometry.HEIGHT * 0.08f, 0);
                                    boardCamera.camera.project(point, 0, Math.round(GpuBoardUi.TURN_HEIGHT * scale),
                                          boardCamera.camera.viewportWidth, boardCamera.camera.viewportHeight);
                                    // The camera can retain a zoom that puts edge hexes behind the toolbar or turn panel.
                                    float bottom = Math.round(GpuBoardUi.TURN_HEIGHT * scale);
                                    if (point.y < bottom || point.y >= bottom + boardCamera.camera.viewportHeight) {
                                        continue;
                                    }
                                    int x = Math.round(point.x * pixels.getWidth() / Gdx.graphics.getWidth());
                                    int y = Math.round(point.y * pixels.getHeight() / Gdx.graphics.getHeight());
                                    if (x < 0 || y < 0 || x >= pixels.getWidth() || y >= pixels.getHeight()) {
                                        continue;
                                    }
                                    int rgb = pixels.getPixel(x, y);
                                    int red = (rgb >>> 24) & 255, green = (rgb >>> 16) & 255, blue = (rgb >>> 8) & 255;
                                    float value = (0.2126f * red + 0.7152f * green + 0.0722f * blue) / 255f;
                                    float chroma = (Math.max(red, Math.max(green, blue))
                                          - Math.min(red, Math.min(green, blue))) / 255f;
                                    var visibility = GpuFieldOfViewTest.at(scene.fieldOfView(), tile.coords()).visibility();
                                    if (visibility == BoardFieldOfView.Visibility.VISIBLE) {
                                        visible += value;
                                        visibleChroma += chroma;
                                        visibleCount++;
                                    } else if (visibility == BoardFieldOfView.Visibility.BLOCKED) {
                                        blocked += value;
                                        blockedChroma += chroma;
                                        blockedCount++;
                                        if (style == GpuFieldOfView.Style.GRAYSCALE) {
                                            assertTrue(chroma <= 1 / 255f,
                                                  "Blocked hex content must be grayscale at " + tile.coords()
                                                        + "; pixel " + x + "," + y + "; RGB " + red + "," + green + "," + blue);
                                        }
                                    }
                                }
                                assertTrue(visibleCount > 10 && blockedCount > 10,
                                      "The comparison must sample both clear and blocked terrain");
                                return new Appearance(visible / visibleCount, blocked / blockedCount,
                                      visibleChroma / visibleCount, blockedChroma / blockedCount);
                            } finally {
                                pixels.dispose();
                            }
                        }

                        @Override
                        public void dispose() {
                            if (probe != null) {
                                probe.dispose();
                            }
                            super.dispose();
                        }
                    }, GpuBoardWindow.configuration(false));
                    if (failure.get() != null) {
                        throw new AssertionError("Native FoV comparison failed for " + style, failure.get());
                    }
                }
            }
        }
        Appearance dimmed = results.get(GpuFieldOfView.Style.DIMMED), fog = results.get(GpuFieldOfView.Style.FOG_OF_WAR);
        Appearance grayscale = results.get(GpuFieldOfView.Style.GRAYSCALE);
        assertEquals(dimmed.visible(), fog.visible(), 0.04, "Visible terrain must remain consistent between styles");
        assertEquals(dimmed.visible(), grayscale.visible(), 0.04, "Grayscale must leave visible terrain unchanged");
        assertEquals(dimmed.visibleChroma(), grayscale.visibleChroma(), 0.01, "Visible terrain must retain its color");
        assertTrue(grayscale.visibleChroma() > 0.01f, "Visible terrain must remain colored in grayscale mode");
        assertTrue(dimmed.blockedChroma() > 0.01f, "Dimmed terrain must retain its color");
        assertEquals(dimmed.blocked(), grayscale.blocked(), 0.02, "Dimmed and grayscale must share darkening strength");
        assertTrue(fog.blocked() < dimmed.blocked() * 0.8f, "Fog style must obscure blocked terrain more strongly");
    }
}
