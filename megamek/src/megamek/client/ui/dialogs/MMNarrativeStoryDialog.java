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
package megamek.client.ui.dialogs;

import megamek.client.ui.swing.util.FontHandler;
import megamek.server.scriptedevent.NarrativeDisplayProvider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

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
        txtDesc.setFont(FontHandler.notoFont());
        txtDesc.setEditable(false);
        txtDesc.setContentType("text/html");
        txtDesc.setText(getStoryPoint().text());
        txtDesc.setCaretPosition(0);
        txtDesc.setBorder(new EmptyBorder(5, 20, 5, 20));
        JScrollPane scrollPane = new JScrollPane(txtDesc) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(400, super.getPreferredSize().height);
            }
        };
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scrollPane, gbc);

        return mainPanel;
    }
}
