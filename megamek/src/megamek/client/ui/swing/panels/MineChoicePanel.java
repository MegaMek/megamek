/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.client.ui.swing.panels;

import java.awt.GridBagLayout;
import java.io.Serial;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.equipment.MiscMounted;

public class MineChoicePanel extends JPanel {
    @Serial
    private static final long serialVersionUID = -1868675102440527538L;

    private final JComboBox<String> comboChoices;

    private final MiscMounted miscMounted;

    public MineChoicePanel(MiscMounted miscMounted, Entity entity) {
        this.miscMounted = miscMounted;
        comboChoices = new JComboBox<>();
        comboChoices.addItem(Messages.getString("CustomMekDialog.Conventional"));
        comboChoices.addItem(Messages.getString("CustomMekDialog.Vibrabomb"));

        int miscMountedLocation = miscMounted.getLocation();
        String labelDescription = '(' + entity.getLocationAbbr(miscMountedLocation) + ')';
        JLabel labelLocation = new JLabel(labelDescription);

        GridBagLayout gridBagLayout = new GridBagLayout();
        setLayout(gridBagLayout);
        add(labelLocation, GBC.std());

        comboChoices.setSelectedIndex(miscMounted.getMineType());
        add(comboChoices, GBC.eol());
    }

    public void applyChoice() {
        miscMounted.setMineType(comboChoices.getSelectedIndex());
    }

    @Override
    public void setEnabled(boolean enabled) {
        comboChoices.setEnabled(enabled);
    }
}
