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

import megamek.client.ui.swing.util.DashedSeparator;
import megamek.client.ui.swing.util.LocationBorder;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.scenario.ScenarioFullInfo;

import javax.swing.*;
import java.awt.*;

/**
 * This panel displays a single {@link ScenarioFullInfo} object in a well-formatted manner for display in the
 * {@link ScenarioChooser}.
 */
public class ScenarioInfoPanel extends JPanel {

    static final int BASE_MINIMUM_WIDTH = 600;
    static final int BASE_MINIMUM_HEIGHT = 100;

    private final JLabel lblTitle = new HeaderLabel();
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
        lblTitle.setFont(new Font("Exo 2", Font.BOLD, UIUtil.FONT_SCALE1));
        add(lblTitle);
        add(UIUtil.scaledVerticalSpacer(10));
        add(new DashedSeparator(UIUtil.uiLightGreen(), 0.9f, 2f));
        add(UIUtil.scaledVerticalSpacer(10));

        textDescription2.setFont(new Font("Noto Sans", Font.PLAIN, UIUtil.FONT_SCALE1));
        textDescription2.setAlignmentX(0.5f);
        textDescription2.setVerticalAlignment(SwingConstants.TOP);
        textDescription2.setBorder(new UIUtil.ScaledEmptyBorder(0, 10, 0, 10));
        add(textDescription2);
    }

    protected void updateFromPreset(final ScenarioFullInfo preset) {
        lblTitle.setText(preset.getName());
        textDescription2.setText("<HTML>" + preset.getDescription());
    }

    private static class DescriptionLabel extends JLabel {
        @Override
        public Dimension getMaximumSize() {
            return UIUtil.scaleForGUI(BASE_MINIMUM_WIDTH, BASE_MINIMUM_HEIGHT);
        }

        @Override
        public Dimension getPreferredSize() {
            return UIUtil.scaleForGUI(BASE_MINIMUM_WIDTH, BASE_MINIMUM_HEIGHT);
        }
    }

    private static class HeaderLabel extends JLabel {
        @Override
        public void setFont(Font font) {
            // Keep a higher font size; UIUtil.adjustDialog sets everything to the same absolute font size
            font = font.deriveFont(1.4f * font.getSize());
            super.setFont(font);
        }
    }
}