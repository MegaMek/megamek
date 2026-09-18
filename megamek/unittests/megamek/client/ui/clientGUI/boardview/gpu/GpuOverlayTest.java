/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.SwingUtilities;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.overlay.KeyBindingsOverlay;
import megamek.client.ui.util.StringDrawer;
import megamek.client.ui.util.UIUtil;
import org.junit.jupiter.api.Test;

class GpuOverlayTest {
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
                }
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                preferences.setValue(GUIPreferences.GUI_SCALE, originalScale);
                preferences.setValue(GUIPreferences.PLANETARY_CONDITIONS_BACKGROUND_TRANSPARENCY, originalAlpha);
            });
        }
    }
}
