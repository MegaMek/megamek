/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.dialog.imageChooser;

import megamek.common.icons.AbstractIcon;

import javax.swing.*;
import java.awt.*;

public abstract class AbstractIconPanel extends JPanel {
    private AbstractIcon icon;

    private JLabel lblImage;

    protected AbstractIconPanel(AbstractIcon icon, String panelName) {
        setIcon(icon);

        initComponents(panelName);
        updatePanel();
    }

    //region Initialization
    private void initComponents(String panelName) {
        setName(panelName);
        setLayout(new GridBagLayout());

        lblImage = new JLabel();
        lblImage.setName("lblImage");

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        add(lblImage, gridBagConstraints);
    }
    //endregion Initialization

    //region Getters/Setters
    public AbstractIcon getIcon() {
        return icon;
    }

    public void setIcon(AbstractIcon icon) {
        this.icon = icon;
    }

    public void setText(String text) {
        lblImage.setText(text);
    }
    //endregion Getters/Setters

    public void updatePanel() {
        lblImage.setIcon(getIcon().getImageIcon());
    }
}
