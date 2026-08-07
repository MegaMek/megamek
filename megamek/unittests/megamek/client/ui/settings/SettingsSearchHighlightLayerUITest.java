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
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

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
    void highlightUsesStrongThemeOverrideColor() throws Exception {
        runOnEdt(() -> {
            Object previous = UIManager.get(SettingsSearchHighlightLayerUI.HIGHLIGHT_COLOR_KEY);
            Color override = new Color(240, 80, 20);
            try {
                UIManager.put(SettingsSearchHighlightLayerUI.HIGHLIGHT_COLOR_KEY, override);
                JLabel label = new JLabel("Visible Match");
                HighlightFixture fixture = fixture(label);
                BufferedImage baseline = fixture.paint("");

                BufferedImage highlighted = fixture.paint("visible");

                Rectangle bounds = fixture.bounds().getFirst();
                Color baselineColor = new Color(baseline.getRGB(bounds.x + bounds.width / 2,
                      bounds.y + 1), true);
                Color highlightedColor = new Color(highlighted.getRGB(bounds.x + bounds.width / 2,
                      bounds.y + 1), true);
                assertTrue(colorDistance(baselineColor, highlightedColor) > 100,
                      baselineColor + " -> " + highlightedColor);
            } finally {
                UIManager.put(SettingsSearchHighlightLayerUI.HIGHLIGHT_COLOR_KEY, previous);
            }
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
    void htmlHighlightKeepsXpMatchInsideExperienceWord() throws Exception {
        runOnEdt(() -> {
            List<String> labels = List.of(
                  "experience",
                  "Experience");
            for (String text : labels) {
                JLabel label = new JLabel("<html><nobr>" + text + "</nobr></html>");
                HighlightFixture fixture = fixture(label);

                fixture.paint("experience");
                assertEquals(1, fixture.bounds().size(), text);
                Rectangle experienceBounds = fixture.bounds().getFirst();

                fixture.paint("XP");
                assertEquals(1, fixture.bounds().size(), text);
                Rectangle xpBounds = fixture.bounds().getFirst();
                assertTrue(xpBounds.x > experienceBounds.x, text);
                assertTrue(xpBounds.getMaxX() < experienceBounds.getMaxX(), text);
                assertTrue(xpBounds.width < experienceBounds.width, text);
            }
        });
    }

    @Test
    void highlightsWrappedHtmlOnItsPaintedLine() throws Exception {
        runOnEdt(() -> {
            JLabel label = new JLabel("<html>First line wraps Needle word</html>");
            label.setPreferredSize(new Dimension(110, 60));
            label.setVerticalAlignment(SwingConstants.TOP);
            JPanel constrainedWidth = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            constrainedWidth.add(label);
            HighlightFixture fixture = fixture(constrainedWidth);

            fixture.paint("needle");

            View htmlView = (View) label.getClientProperty(BasicHTML.propertyKey);
            int fontHeight = label.getFontMetrics(label.getFont()).getHeight();
            assertEquals(110, label.getWidth());
            assertTrue(htmlView.getPreferredSpan(View.X_AXIS) > label.getWidth());
            assertTrue(htmlView.getPreferredSpan(View.Y_AXIS) > fontHeight,
                "Test setup must wrap the HTML onto multiple lines");
            assertEquals(1, fixture.bounds().size());
            Rectangle labelBounds = SwingUtilities.convertRectangle(label,
                  new Rectangle(0, 0, label.getWidth(), label.getHeight()), fixture.layer());
            Rectangle highlight = fixture.bounds().getFirst();
            assertEquals(highlight, highlight.intersection(labelBounds));
            assertTrue(highlight.y >= labelBounds.y + fontHeight,
                  "Expected second-line highlight but got " + highlight + " in " + labelBounds);
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
    void tableCellAndHeaderHighlightsFitRendererText() throws Exception {
        runOnEdt(() -> {
            JTable table = new JTable(new Object[][] { { "Recruitment Bonus", 3 }, { "Private", 5 } },
                  new Object[] { "Rank Name", "XP Cost" });
            JScrollPane tableScrollPane = new JScrollPane(table);
            tableScrollPane.setColumnHeaderView(table.getTableHeader());
            tableScrollPane.setPreferredSize(new Dimension(300, 90));
            HighlightFixture fixture = fixture(tableScrollPane);

            fixture.paint("recruit");

            assertEquals(1, fixture.bounds().size());
            Rectangle cell = SwingUtilities.convertRectangle(table,
                  table.getCellRect(0, 0, true), fixture.layer());
            Rectangle cellHighlight = fixture.bounds().getFirst();
            assertEquals(cellHighlight, cellHighlight.intersection(cell));
            int expectedCellWidth = table.getFontMetrics(table.getFont()).stringWidth("Recruit");
            assertTrue(cellHighlight.width <= expectedCellWidth + 1,
                  "Expected at most " + (expectedCellWidth + 1) + "px but was " + cellHighlight.width + "px");

            fixture.paint("rank");

            assertEquals(1, fixture.bounds().size());
            Rectangle header = SwingUtilities.convertRectangle(table.getTableHeader(),
                  table.getTableHeader().getHeaderRect(0), fixture.layer());
            Rectangle headerHighlight = fixture.bounds().getFirst();
            assertEquals(headerHighlight, headerHighlight.intersection(header));
        });
    }

    @Test
    void tableHighlightSkipsRowsOutsideItsViewport() throws Exception {
        runOnEdt(() -> {
            Object[][] rows = new Object[20][1];
            for (int row = 0; row < rows.length; row++) {
                rows[row][0] = "Row " + row;
            }
            rows[0][0] = "Hidden Needle";
            rows[10][0] = "Visible Needle";
            JTable table = new JTable(rows, new Object[] { "Name" });
            table.setRowHeight(20);
            JScrollPane tableScrollPane = new JScrollPane(table);
            tableScrollPane.setColumnHeaderView(table.getTableHeader());
            tableScrollPane.setPreferredSize(new Dimension(300, 90));
            HighlightFixture fixture = fixture(tableScrollPane);
            table.scrollRectToVisible(table.getCellRect(10, 0, true));

            fixture.paint("hidden");
            assertTrue(fixture.bounds().isEmpty());

            fixture.paint("visible");
            assertEquals(1, fixture.bounds().size());
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

    @Test
    void nestedScrollPaneHidesOffscreenLabelHighlights() throws Exception {
        runOnEdt(() -> {
            JPanel innerContent = new JPanel(null);
            innerContent.setPreferredSize(new Dimension(300, 220));
            JLabel hidden = new JLabel("Hidden Needle");
            hidden.setBounds(0, 0, 160, 24);
            innerContent.add(hidden);
            JLabel visible = new JLabel("Visible Needle");
            visible.setBounds(0, 150, 160, 24);
            innerContent.add(visible);
            JScrollPane innerScrollPane = new JScrollPane(innerContent);
            innerScrollPane.setPreferredSize(new Dimension(300, 90));
            HighlightFixture fixture = fixture(innerScrollPane);
            innerScrollPane.getViewport().setViewPosition(new Point(0, 130));

            fixture.paint("hidden");
            assertTrue(fixture.bounds().isEmpty());

            fixture.paint("visible");
            assertEquals(1, fixture.bounds().size());
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

    private static int colorDistance(Color first, Color second) {
        return Math.abs(first.getRed() - second.getRed())
              + Math.abs(first.getGreen() - second.getGreen())
              + Math.abs(first.getBlue() - second.getBlue());
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
