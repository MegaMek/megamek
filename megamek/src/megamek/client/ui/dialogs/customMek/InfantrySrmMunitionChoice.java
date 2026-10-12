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

import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC2;
import megamek.client.ui.Messages;
import megamek.client.ui.comboBoxes.SearchableComboBox;
import megamek.common.equipment.WeaponType;
import megamek.common.units.ConvInfantry;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * Lobby row for declaring which munitions a conventional infantry platoon's SRM launchers carry. Per TW p. 143 and
 * the TechManual pp. 350-352 errata, an SRM platoon declares standard or Inferno munitions before the battle, and the
 * choice holds for the whole battle. This plays the same part for the platoon that a Carried Munitions row plays for
 * a Mek's SRM ammo bin.
 */
public class InfantrySrmMunitionChoice {

    /** The munitions an infantry SRM launcher can be loaded with. */
    enum SrmMunition {
        STANDARD("CustomMekDialog.srmMunitionStandard"),
        INFERNO("CustomMekDialog.srmMunitionInferno");

        private final String messageKey;

        SrmMunition(String messageKey) {
            this.messageKey = messageKey;
        }

        String displayName() {
            return Messages.getString(messageKey);
        }
    }

    private final ConvInfantry infantry;
    private final SearchableComboBox<SrmMunition> comboMunitions;

    public InfantrySrmMunitionChoice(ConvInfantry infantry, JPanel parentPanel, GBC2 gbc) {
        this.infantry = infantry;

        comboMunitions = new SearchableComboBox<>("comboInfantrySrmMunitions", List.of(SrmMunition.values()),
              SrmMunition::displayName);
        comboMunitions.setSelectedItem(infantry.isInfernoSrmsDeclared() ? SrmMunition.INFERNO : SrmMunition.STANDARD);
        comboMunitions.setToolTipText(Messages.getString("CustomMekDialog.srmMunition.tooltip"));

        JLabel launcherLabel = new JLabel(srmLauncherName() + ":");
        parentPanel.add(launcherLabel, gbc.forLabel());
        parentPanel.add(comboMunitions, gbc.eol());
    }

    /**
     * @return the name of the platoon's SRM launcher; the secondary weapon when both are launchers, as that is the
     *       support weapon the platoon is built around
     */
    private String srmLauncherName() {
        InfantryWeapon secondary = infantry.getSecondaryWeapon();
        if ((secondary != null) && secondary.hasFlag(WeaponType.F_SRM)) {
            return secondary.getName();
        }
        return infantry.getPrimaryWeapon().getName();
    }

    /**
     * Applies the selected munitions to the platoon.
     */
    public void applyChoice() {
        SrmMunition selected = comboMunitions.getSelectedItem();
        if (selected != null) {
            infantry.setInfernoSrmsDeclared(selected == SrmMunition.INFERNO);
        }
    }

    public void setEnabled(boolean enabled) {
        comboMunitions.setEnabled(enabled);
    }
}
