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
package megamek.client.ui.dialogs.MMDialogs;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.util.FlatLafStyleBuilder;
import megamek.client.ui.util.FontHandler;
import megamek.client.ui.util.UIUtil;
import megamek.server.scriptedEvent.NarrativeDisplayProvider;

public class MMNarrativeStoryDialog extends MMStoryDialog {

    public MMNarrativeStoryDialog(final JFrame parent, NarrativeDisplayProvider sEvent) {
        super(parent, sEvent);
        initialize();
    }

    @Override
    protected Container getMainPanel() {

        GridBagConstraints gbc = new GridBagConstraints();
        JPanel mainPanel = new JPanel(new GridBagLayout());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 0.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(getImagePanel(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JTextPane txtDesc = new JTextPane();
        new FlatLafStyleBuilder(FontHandler.notoFont()).apply(txtDesc);
        txtDesc.setEditable(false);
        txtDesc.setContentType("text/html");
        txtDesc.setText(getStoryPoint().text());
        txtDesc.setCaretPosition(0);
        txtDesc.setBorder(new EmptyBorder(5, 20, 5, 20));
        JScrollPane scrollPane = new JScrollPane(txtDesc) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(UIUtil.scaleForGUI(400), super.getPreferredSize().height);
            }
        };
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scrollPane, gbc);

        return mainPanel;
    }
}
