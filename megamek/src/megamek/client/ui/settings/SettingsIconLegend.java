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

import static megamek.client.ui.util.FontHandler.symbolIcon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import org.apache.commons.text.StringEscapeUtils;

import megamek.client.ui.util.UIUtil;

/** Reference legend for resolved {@link SettingsBadge} markers. */
public class SettingsIconLegend extends JPanel {
    private static final int ICON_SIZE = 16;

    public SettingsIconLegend(List<SettingsBadge> badges) {
        super(new GridBagLayout());
        setName("settingsIconLegend");
        setOpaque(false);
        for (int index = 0; index < badges.size(); index++) {
            addEntry(index / 2, index % 2, badges.get(index));
        }
    }

    /** Creates a footer button that opens the supplied badge legend above itself. */
    public static JButton createLegendButton(String text, String tooltip, List<SettingsBadge> badges) {
        List<SettingsBadge> legendBadges = List.copyOf(badges);
        JButton button = new JButton(text);
        button.setName("btnSettingsIconLegend");
        button.setToolTipText(tooltip);
        button.setIcon(symbolIcon(0xE88E, button.getFont().getSize(), button.getForeground()));
        button.addActionListener(event -> showLegendPopupAbove(button, new SettingsIconLegend(legendBadges)));
        return button;
    }

    private void addEntry(int row, int column, SettingsBadge badge) {
        Color iconColor = badge.color() == null ? UIManager.getColor("Label.foreground") : badge.color();
        JLabel label = new JLabel("<html>" + StringEscapeUtils.escapeHtml4(badge.description()) + "</html>");
        label.setIcon(symbolIcon(badge.codePoint(), UIUtil.scaleForGUI(ICON_SIZE), iconColor));
        label.setIconTextGap(UIUtil.scaleForGUI(6));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.weightx = 0.5;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.insets = new Insets(UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(8));
        add(label, constraints);
    }

    private static void showLegendPopupAbove(JButton anchor, SettingsIconLegend legend) {
        int padding = UIUtil.scaleForGUI(8);
        legend.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        JPopupMenu popup = new JPopupMenu();
        popup.setName("settingsLegendPopup");
        popup.setLayout(new BorderLayout());
        popup.add(legend, BorderLayout.CENTER);
        Dimension popupSize = popup.getPreferredSize();
        popup.show(anchor, 0, -popupSize.height);
    }
}
