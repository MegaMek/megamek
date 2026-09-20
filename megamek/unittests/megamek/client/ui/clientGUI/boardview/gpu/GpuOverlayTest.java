/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.SwingUtilities;

import megamek.client.ui.IDisplayable;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.overlay.KeyBindingsOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.OverlayImage;
import megamek.client.ui.clientGUI.boardview.overlay.PlanetaryConditionsOverlay;
import megamek.client.ui.util.StringDrawer;
import megamek.client.ui.util.UIUtil;
import megamek.common.KeyBindParser;
import org.junit.jupiter.api.Test;

class GpuOverlayTest {
    @Test
    void nativeLayersKeepWidgetsAboveAndBelowTextInPainterOrder() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                KeyBindingsOverlay keys = new KeyBindingsOverlay(fixture.view) {
                    @Override
                    protected boolean getVisibilityGUIPreference() {
                        return true;
                    }
                };
                try {
                    fixture.view.addOverlay(solidWidget(Color.RED, new Rectangle(0, 0, 500, 700)));
                    fixture.view.addOverlay(keys);
                    fixture.view.addOverlay(solidWidget(Color.CYAN, new Rectangle(20, 40, 400, 10)));
                    Dimension size = new Dimension(500, 700);
                    BufferedImage expected = fixture.view.captureOverlayImage(size, size);
                    List<OverlayImage> layers = fixture.view.captureOverlayLayers(size, size);
                    assertEquals(3, layers.size());
                    BufferedImage actual = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D painter = actual.createGraphics();
                    try {
                        for (OverlayImage layer : layers) {
                            painter.drawImage(layer.image(), layer.x(), layer.y(), null);
                        }
                    } finally {
                        painter.dispose();
                    }
                    assertArrayEquals(expected.getRGB(0, 0, size.width, size.height, null, 0, size.width),
                          actual.getRGB(0, 0, size.width, size.height, null, 0, size.width));
                } finally {
                    GUIPreferences.getInstance().removePreferenceChangeListener(keys);
                    KeyBindParser.removePreferenceChangeListener(keys);
                }
            });
        }
    }

    private static IDisplayable solidWidget(Color color, Rectangle bounds) {
        return new IDisplayable() {
            @Override
            public void draw(Graphics graphics, Rectangle clip) {
                graphics.setColor(color);
                graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        };
    }

    @Test
    void cachedOverlayTextRetainsNativeGlyphDetailWhenPixelDensityChanges() throws Exception {
        GUIPreferences preferences = GUIPreferences.getInstance();
        float originalScale = preferences.getGUIScale();
        int originalAlpha = preferences.getPlanetaryConditionsBackgroundTransparency();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                preferences.setValue(GUIPreferences.GUI_SCALE, 1f);
                preferences.setValue(GUIPreferences.PLANETARY_CONDITIONS_BACKGROUND_TRANSPARENCY, 0);
                String text = "Native text: rivers, 25\u00b0C, 1.0g";
                fixture.view.addOverlay(new KeyBindingsOverlay(fixture.view) {
                    @Override
                    protected boolean getVisibilityGUIPreference() {
                        return true;
                    }

                    @Override
                    protected List<String> assembleTextLines() {
                        return List.of("#FFFFFF" + text);
                    }

                    @Override
                    protected int getDistSide(Rectangle bounds, int width) {
                        return 30;
                    }

                    @Override
                    protected int getDistTop(Rectangle bounds, int height) {
                        return 30;
                    }
                });
                Dimension layout = new Dimension(400, 100);
                // Reuse the same overlay without dirtying its contents, including a return to the original density.
                for (double density : new double[] { 1, 2, 1.5, 1.25, 1 }) {
                    Dimension pixels = new Dimension((int) (layout.width * density), (int) (layout.height * density));
                    BufferedImage actual = fixture.view.captureOverlayImage(layout, pixels);
                    BufferedImage expected = new BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics = expected.createGraphics();
                    try {
                        graphics.translate(Math.round(30 * density), Math.round(30 * density));
                        graphics.scale(density, density);
                        UIUtil.setHighQualityRendering(graphics);
                        Font font = new Font("SansSerif", Font.PLAIN, 13);
                        new StringDrawer(text).font(font).color(Color.WHITE)
                              .at(10, 5 + graphics.getFontMetrics(font).getAscent()).draw(graphics);
                    } finally {
                        graphics.dispose();
                    }
                    assertArrayEquals(expected.getRGB(0, 0, pixels.width, pixels.height, null, 0, pixels.width),
                          actual.getRGB(0, 0, pixels.width, pixels.height, null, 0, pixels.width),
                          "Cached text must match vector text drawn at native density " + density);
                    BufferedImage layered = new BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D compositor = layered.createGraphics();
                    try {
                        for (OverlayImage layer : fixture.view.captureOverlayLayers(layout, pixels)) {
                            compositor.drawImage(layer.image(), layer.x(), layer.y(), null);
                        }
                    } finally {
                        compositor.dispose();
                    }
                    assertArrayEquals(actual.getRGB(0, 0, pixels.width, pixels.height, null, 0, pixels.width),
                          layered.getRGB(0, 0, pixels.width, pixels.height, null, 0, pixels.width),
                          "Native layers must preserve the classic painter's pixels and placement");
                }
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                preferences.setValue(GUIPreferences.GUI_SCALE, originalScale);
                preferences.setValue(GUIPreferences.PLANETARY_CONDITIONS_BACKGROUND_TRANSPARENCY, originalAlpha);
            });
        }
    }

    @Test
    void fadesStartHiddenReverseContinuouslyAndKeepTheSameArtwork() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                KeyBindingsOverlay keys = new KeyBindingsOverlay(fixture.view) {
                    @Override
                    protected boolean getVisibilityGUIPreference() {
                        return false;
                    }
                };
                PlanetaryConditionsOverlay conditions = new PlanetaryConditionsOverlay(fixture.view) {
                    @Override
                    protected boolean getVisibilityGUIPreference() {
                        return false;
                    }
                };
                try {
                    fixture.view.addOverlay(keys);
                    fixture.view.addOverlay(conditions);
                    fixture.source.setViewport(1400, 900, 1400, 900);
                    fixture.source.refresh();
                    GpuBoardSource.Hud hidden = fixture.source.takeFrame().hud();
                    assertEquals(2, hidden.layers().size());
                    hidden.layers().forEach(layer -> assertEquals(0, layer.fade().opacity(System.nanoTime())));
                    keys.setVisible(true);
                    conditions.setVisible(true);
                    fixture.source.refresh();
                    GpuBoardSource.Hud shown = fixture.source.takeFrame().hud();
                    for (int index = 0; index < shown.layers().size(); index++) {
                        var layer = shown.layers().get(index);
                        assertSame(hidden.layers().get(index).pixels(), layer.pixels(),
                              "Toggling visibility must not require new texture pixels");
                        OverlayImage.Fade fade = layer.fade();
                        long start = fade.transition().startedNanos();
                        assertEquals(0, fade.opacity(start));
                        assertEquals(0.5f, fade.opacity(start + 100_000_000));
                        assertEquals(1, fade.opacity(start + 200_000_000));
                        assertFalse(fade.isAnimating(start + 200_000_000));
                        float previous = 0;
                        for (int sample = 1; sample <= 12; sample++) {
                            float opacity = fade.opacity(start + sample * 16_666_667L);
                            assertTrue(opacity > previous, "Opacity advances between the 100 ms scene captures");
                            previous = opacity;
                        }
                    }
                    keys.setVisible(false);
                    fixture.source.refresh();
                    GpuBoardSource.Hud reversed = fixture.source.takeFrame().hud();
                    var fade = reversed.layers().getFirst().fade();
                    assertEquals(shown.layers().getFirst().fade().opacity(fade.transition().startedNanos()),
                          fade.transition().from());
                    assertEquals(0, fade.opacity(fade.transition().startedNanos() + 200_000_000));
                    assertSame(shown.layers().getFirst().pixels(), reversed.layers().getFirst().pixels());
                    assertEquals(shown.layers().get(1).fade(), reversed.layers().get(1).fade(),
                          "Reversing one overlay must not restart the other");
                } finally {
                    GUIPreferences.getInstance().removePreferenceChangeListener(keys);
                    GUIPreferences.getInstance().removePreferenceChangeListener(conditions);
                    KeyBindParser.removePreferenceChangeListener(keys);
                    KeyBindParser.removePreferenceChangeListener(conditions);
                }
            });
        }
    }
}
