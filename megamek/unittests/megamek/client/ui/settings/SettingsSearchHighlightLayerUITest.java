/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class SettingsSearchHighlightLayerUITest {
    @Test
    void paintsEveryMatchingTokenWithoutMutatingTextOrLayout() throws Exception {
        runOnEdt(() -> {
            JLabel label = new JLabel("Heat and Fire");
            HighlightFixture fixture = fixture(label);
            String originalText = label.getText();
            Dimension originalSize = label.getPreferredSize();
            BufferedImage baseline = fixture.paint("");

            BufferedImage highlighted = fixture.paint("heat fire");

            assertEquals(2, fixture.bounds().size());
            assertTrue(countDifferentPixels(baseline, highlighted) > 0);
            assertEquals(originalText, label.getText());
            assertEquals(originalSize, label.getPreferredSize());
        });
    }

    @Test
    void paintsMatchesInSwingHtmlText() throws Exception {
        runOnEdt(() -> {
            JLabel label = new JLabel("<html><b>Heat &amp; Fire</b><br>Rules</html>");
            HighlightFixture fixture = fixture(label);

            fixture.paint("heat rules");

            assertEquals(2, fixture.bounds().size());
            assertEquals("<html><b>Heat &amp; Fire</b><br>Rules</html>", label.getText());
        });
    }

    @Test
    void paintsMatchesInReadOnlyHtmlPane() throws Exception {
        runOnEdt(() -> {
            JEditorPane textPane = new JEditorPane("text/html", "<html><b>Heat &amp; Fire</b></html>");
            textPane.setEditable(false);
            textPane.setFocusable(false);
            HighlightFixture fixture = fixture(textPane);

            fixture.paint("heat");

            assertFalse(fixture.bounds().isEmpty());
            assertTrue(textPane.getText().contains("Heat &amp; Fire"), textPane.getText());
        });
    }

    @Test
    void checkboxHighlightStartsAfterIndicator() throws Exception {
        runOnEdt(() -> {
            JCheckBox checkBox = new JCheckBox("Enable Option");
            HighlightFixture fixture = fixture(checkBox);

            fixture.paint("enable");

            assertEquals(1, fixture.bounds().size());
            assertTrue(fixture.bounds().getFirst().x > checkBox.getInsets().left);
        });
    }

    @Test
    void readsCurrentTextOnEveryPaintInsteadOfRestoringABaseline() throws Exception {
        runOnEdt(() -> {
            JLabel label = new JLabel("Original Label");
            HighlightFixture fixture = fixture(label);
            fixture.paint("original");
            assertFalse(fixture.bounds().isEmpty());

            label.setText("Updated Label");
            fixture.layout();
            fixture.paint("original");
            assertTrue(fixture.bounds().isEmpty());

            fixture.paint("updated");
            assertFalse(fixture.bounds().isEmpty());
            assertEquals("Updated Label", label.getText());
        });
    }

    @Test
    void ignoresHiddenComponentsAndClearsOnBlankFilter() throws Exception {
        runOnEdt(() -> {
            JCheckBox checkBox = new JCheckBox("Hidden Match");
            HighlightFixture fixture = fixture(checkBox);
            checkBox.setVisible(false);

            fixture.paint("hidden");

            assertTrue(fixture.bounds().isEmpty());
            checkBox.setVisible(true);
            fixture.paint("");
            assertTrue(fixture.bounds().isEmpty());
        });
    }

    @Test
    void clipsPartiallyVisibleHighlightToViewport() throws Exception {
        runOnEdt(() -> {
            JLabel label = new JLabel("Clipped Match");
            HighlightFixture fixture = fixture(label);
            fixture.content().setPreferredSize(new Dimension(320, 240));
            fixture.layout();
            fixture.layer().getView().getViewport().setViewPosition(new Point(0, label.getHeight() / 2));

            fixture.paint("clipped");

            Rectangle viewport = SwingUtilities.convertRectangle(fixture.layer().getView().getViewport(),
                  new Rectangle(0, 0, fixture.layer().getView().getViewport().getWidth(),
                        fixture.layer().getView().getViewport().getHeight()), fixture.layer());
            assertFalse(fixture.bounds().isEmpty());
            fixture.bounds().forEach(bounds -> assertEquals(bounds, bounds.intersection(viewport)));
        });
    }

    private static HighlightFixture fixture(Component component) {
        JPanel content = new JPanel(new BorderLayout());
        content.add(component, BorderLayout.NORTH);
        content.setPreferredSize(new Dimension(320, 120));
        JScrollPane scrollPane = new JScrollPane(content);
        SettingsSearchHighlightLayerUI layerUI = new SettingsSearchHighlightLayerUI();
        JLayer<JScrollPane> layer = new JLayer<>(scrollPane, layerUI);
        HighlightFixture fixture = new HighlightFixture(layer, layerUI, content);
        fixture.layout();
        return fixture;
    }

    private static int countDifferentPixels(BufferedImage first, BufferedImage second) {
        int differences = 0;
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) {
                    differences++;
                }
            }
        }
        return differences;
    }

    private static void layoutTree(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) {
            if (child instanceof Container container) {
                layoutTree(container);
            }
        }
    }

    private static void runOnEdt(Runnable test) throws Exception {
        try {
            SwingUtilities.invokeAndWait(test);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof Error error) {
                throw error;
            }
            if (exception.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw exception;
        }
    }

    private record HighlightFixture(JLayer<JScrollPane> layer, SettingsSearchHighlightLayerUI layerUI,
                                    JPanel content) {
        private void layout() {
            layer.setSize(320, 120);
            layer.getView().setSize(320, 120);
            content.setSize(content.getPreferredSize());
            layoutTree(layer);
        }

        private BufferedImage paint(String filter) {
            layerUI.setFilter(SettingsRoute.normalizeSearchText(filter), layer);
            BufferedImage image = new BufferedImage(layer.getWidth(), layer.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                layer.paint(graphics);
            } finally {
                graphics.dispose();
            }
            return image;
        }

        private List<Rectangle> bounds() {
            return layerUI.highlightBounds(layer);
        }
    }
}
