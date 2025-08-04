/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.scenario;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import megamek.client.ui.MMMarkdownRenderer;
import megamek.client.ui.util.DashedSeparator;
import megamek.client.ui.util.FlatLafStyleBuilder;
import megamek.client.ui.util.FontHandler;
import megamek.client.ui.util.LocationBorder;
import megamek.client.ui.util.UIUtil;
import megamek.common.scenario.Scenario;

/**
 * This panel displays a single {@link Scenario} object in a well-formatted manner for display in the
 * {@link ScenarioChooserDialog}.
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
        textDescription2.setText("<HTML><HEAD><STYLE>p { margin-top: %d; }</STYLE></HEAD><BODY>%s".formatted(margin,
              description));
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
