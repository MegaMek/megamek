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

import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;

/** Header content for a settings page, using caller-resolved text and an optional caller-supplied icon. */
public class SettingsHeaderPanel extends JPanel {
    public static final int DEFAULT_BODY_TEXT_WIDTH = 750;
    private static final int COMPONENT_PADDING = 5;

    public SettingsHeaderPanel(String name, String headerText, @Nullable Icon icon) {
        this(name, headerText, icon, null, DEFAULT_BODY_TEXT_WIDTH);
    }

    public SettingsHeaderPanel(String name, String headerText, @Nullable Icon icon,
          @Nullable String bodyText, int bodyTextWidth) {
        setName("pnl" + name + "HeaderPanel");
        setOpaque(false);
        setLayout(new GridBagLayout());

        JLabel headerLabel = new JLabel("<html>" + headerText + "</html>", SwingConstants.CENTER);
        headerLabel.setName("lbl" + name);
        setFontScaling(headerLabel, true, 2);

        GridBagConstraints layout = centeredConstraints(0);
        add(headerLabel, layout);

        if (icon != null) {
            JLabel imageLabel = new JLabel(icon);
            layout = centeredConstraints(1);
            add(imageLabel, layout);
        }

        if (bodyText != null) {
            String bodyHtml = String.format("<html><div style='width: %s'>%s</div></html>",
                  UIUtil.scaleForGUI(bodyTextWidth), bodyText);
            JLabel bodyLabel = new JLabel(bodyHtml, SwingConstants.CENTER);
            bodyLabel.setName("lbl" + name + "Body");
            setFontScaling(bodyLabel, false, 1);
            layout = centeredConstraints(icon == null ? 1 : 2);
            add(bodyLabel, layout);
        }
    }

    private static GridBagConstraints centeredConstraints(int row) {
        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.gridy = row;
        layout.anchor = GridBagConstraints.CENTER;
        layout.fill = GridBagConstraints.HORIZONTAL;
        int padding = UIUtil.scaleForGUI(COMPONENT_PADDING);
        layout.insets = new Insets(padding, padding, padding, padding);
        return layout;
    }
}
