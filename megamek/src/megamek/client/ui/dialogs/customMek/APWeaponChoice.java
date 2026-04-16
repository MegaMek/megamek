/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
import java.util.Objects;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC2;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.BaConstructionUtil;
import megamek.common.units.ConstructionUtil;
import megamek.common.units.Entity;

/**
 * A panel that houses a label and a combo box that allows for selecting which anti-personnel weapon is mounted in an AP
 * mount.
 *
 * @author arlith
 */
class APWeaponChoice {

    private final Entity entity;
    private final List<WeaponType> weaponTypes;
    private final JComboBox<String> comboChoices = new JComboBox<>();
    private final Mounted<?> apMount;

    APWeaponChoice(Entity entity, Mounted<?> apMount, List<WeaponType> suitableWeapons, JPanel parentPanel,
                   GBC2 gbc) {
        Objects.requireNonNull(apMount);
        this.entity = entity;
        weaponTypes = suitableWeapons;
        this.apMount = apMount;
        EquipmentType equipmentType = null;

        if (apMount.getLinked() != null) {
            equipmentType = apMount.getLinked().getType();
        }

        Vector<String> agWeaponNames = new Vector<>();
        agWeaponNames.add("None");
        agWeaponNames.addAll(suitableWeapons.stream().map(EquipmentType::getName).toList());
        comboChoices.setModel(new DefaultComboBoxModel<>(agWeaponNames));
        if (equipmentType != null) {
            comboChoices.setSelectedItem(equipmentType.getName());
        }

        String location = BattleArmor.MOUNT_LOC_NAMES[apMount.getBaMountLoc()] + ":";
        parentPanel.add(new JLabel(location), gbc.forLabel());
        parentPanel.add(comboChoices, gbc.eol());
    }

    void applyChoice() {
        int selectedIndex = comboChoices.getSelectedIndex();
        WeaponType weaponType = null;
        if ((selectedIndex > 0) && (selectedIndex <= weaponTypes.size())) {
            // Need to account for the "None" item; also, treat index -1 as "None"
            weaponType = weaponTypes.get(selectedIndex - 1);
        }

        // Remove the currently mounted AP weapon, if it is not the selected weapon
        if (apMount.getLinked() != null && apMount.getLinked().getType() != weaponType) {
            ConstructionUtil.removeMounted(entity, apMount.getLinked());
        }

        // If a weapon is selected but the mount now has none, add it
        if (weaponType != null && apMount.getLinked() == null) {
            try {
                Mounted<?> newWeapon = entity.addEquipment(weaponType, apMount.getLocation());
                BaConstructionUtil.mountOnApm(newWeapon, apMount);
            } catch (LocationFullException ex) {
                // this is not thrown for BA
            }
        }
    }

//    @Override
    public void setEnabled(boolean enabled) {
        comboChoices.setEnabled(enabled);
    }
}
