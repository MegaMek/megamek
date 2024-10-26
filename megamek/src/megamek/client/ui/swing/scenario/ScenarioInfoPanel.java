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

import megamek.client.ui.swing.util.*;
import megamek.common.scenario.ScenarioV1;
import megamek.common.scenario.Scenario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * This panel displays a single {@link ScenarioV1} object in a well-formatted manner for display in the
 * {@link ScenarioChooser}.
 */
public class ScenarioInfoPanel extends JPanel {

    static final int BASE_MINIMUM_WIDTH = 600;
    static final int BASE_MINIMUM_HEIGHT = 100;

    private final JLabel lblTitle = new JLabel();
    private final DescriptionLabel textDescription2 = new DescriptionLabel();

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
        add(Box.createVerticalStrut(10));

        new FlatLafStyleBuilder().font(FontHandler.notoFont()).apply(textDescription2);
        textDescription2.setAlignmentX(0.5f);
        textDescription2.setVerticalAlignment(SwingConstants.TOP);
        textDescription2.setBorder(new EmptyBorder(0, 10, 0, 10));
        add(textDescription2);
    }

    protected void updateFromPreset(final Scenario preset) {
        lblTitle.setText(preset.getName());
        textDescription2.setText("<HTML>" + preset.getDescription());
    }

    private static class DescriptionLabel extends JLabel {
        @Override
        public Dimension getMaximumSize() {
            // Fixed sizes do not get scaled by FlatLaf
            return new Dimension(UIUtil.scaleForGUI(BASE_MINIMUM_WIDTH), UIUtil.scaleForGUI(BASE_MINIMUM_HEIGHT));
        }

        @Override
        public Dimension getPreferredSize() {
            return getMaximumSize();
        }
    }
}
