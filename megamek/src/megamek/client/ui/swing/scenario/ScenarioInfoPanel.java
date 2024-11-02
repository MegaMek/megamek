/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.scenario;

import megamek.client.ui.MMMarkdownRenderer;
import megamek.client.ui.swing.util.*;
import megamek.common.scenario.Scenario;

import javax.swing.*;
import java.awt.*;

/**
 * This panel displays a single {@link Scenario} object in a well-formatted manner for display in the {@link ScenarioChooser}.
 */
public class ScenarioInfoPanel extends JPanel {

    static final int BASE_MINIMUM_WIDTH = 600;
    static final int BASE_MINIMUM_HEIGHT = 100;

    private final JLabel lblTitle = new JLabel();
    private final JTextPane textDescription2 = new DescriptionPane();

    public ScenarioInfoPanel() {
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 5, 10, 5),
                new LocationBorder(UIManager.getColor("Separator.foreground"), 3)));
        setName("ScenarioInfoPanel");
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        lblTitle.setName("lblTitle");
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitle.setForeground(UIUtil.uiLightGreen());
        new FlatLafStyleBuilder().font("Exo 2").bold().size(1.4).apply(lblTitle);
        add(lblTitle);
        add(Box.createVerticalStrut(10));
        add(new DashedSeparator(UIUtil.uiLightGreen(), 0.9f, 2f));
        new FlatLafStyleBuilder().font(FontHandler.notoFont()).apply(textDescription2);
        add(textDescription2);
    }

    protected void updateFromPreset(final Scenario preset) {
        lblTitle.setText(preset.getName());
        String description = MMMarkdownRenderer.getRenderedHtml(preset.getDescription());
        // bring the paragraph top margin in line (and scale it)
        int margin = UIUtil.scaleForGUI(8);
        textDescription2.setText("<HTML><HEAD><STYLE>p { margin-top: %d; }</STYLE></HEAD><BODY>%s".formatted(margin, description));
    }

    private static class DescriptionPane extends JTextPane {

        public DescriptionPane() {
            setEditable(false);
            setContentType("text/html");
            setCaretPosition(0);
            setAlignmentX(0.5f);
            setBackground(null);
            setMargin(new Insets(0, 10, 0, 10));
        }

        @Override
        public Dimension getMaximumSize() {
            // Fixed sizes do not get scaled by FlatLaf
            return UIUtil.scaleForGUI(BASE_MINIMUM_WIDTH, BASE_MINIMUM_HEIGHT);
        }

        @Override
        public Dimension getPreferredSize() {
            return getMaximumSize();
        }
    }
}
