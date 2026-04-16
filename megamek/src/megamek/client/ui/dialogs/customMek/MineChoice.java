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
package megamek.client.ui.dialogs.customMek;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC2;
import megamek.client.ui.Messages;
import megamek.common.units.Entity;
import megamek.common.equipment.MiscMounted;

public class MineChoice {

    private final JComboBox<String> comboChoices;

    private final MiscMounted miscMounted;

    public MineChoice(MiscMounted miscMounted, Entity entity, JPanel parentPanel, GBC2 gbc) {
        this.miscMounted = miscMounted;
        comboChoices = new JComboBox<>();
        comboChoices.addItem(Messages.getString("CustomMekDialog.Conventional"));
        comboChoices.addItem(Messages.getString("CustomMekDialog.Vibrabomb"));
        comboChoices.setSelectedIndex(miscMounted.getMineType());

        parentPanel.add(new JLabel(entity.getLocationName(miscMounted.getLocation()) + ":"), gbc.forLabel());
        parentPanel.add(comboChoices, gbc.eol());
    }

    public void applyChoice() {
        miscMounted.setMineType(comboChoices.getSelectedIndex());
    }

    public void setEnabled(boolean enabled) {
        comboChoices.setEnabled(enabled);
    }
}
