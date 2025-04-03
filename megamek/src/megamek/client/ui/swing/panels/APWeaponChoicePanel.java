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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC;
import megamek.common.BattleArmor;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LocationFullException;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import megamek.common.equipment.WeaponMounted;
import megamek.logging.MMLogger;

/**
 * A panel that houses a label and a combo box that allows for selecting which anti-personnel weapon is mounted in an AP
 * mount.
 *
 * @author arlith
 */
public class APWeaponChoicePanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 6189888202192403704L;

    private static final MMLogger LOGGER = MMLogger.create(APWeaponChoicePanel.class);

    private final Entity entity;

    private final ArrayList<WeaponType> weaponTypes;

    private final JComboBox<String> comboChoices;

    private final Mounted<?> apMounted;

    public APWeaponChoicePanel(Entity entity, Mounted<?> mounted, ArrayList<WeaponType> weapons) {
        this.entity = entity;
        weaponTypes = weapons;
        apMounted = mounted;
        EquipmentType equipmentType = null;

        if ((mounted != null) && (mounted.getLinked() != null)) {
            equipmentType = mounted.getLinked().getType();
        }

        comboChoices = new JComboBox<>();
        comboChoices.addItem("None");
        comboChoices.setSelectedIndex(0);

        Iterator<WeaponType> it = weaponTypes.iterator();
        for (int x = 1; it.hasNext(); x++) {
            WeaponType weaponType = it.next();
            comboChoices.addItem(weaponType.getName());
            if ((equipmentType != null) &&
                      Objects.equals(weaponType.getInternalName(), equipmentType.getInternalName())) {
                comboChoices.setSelectedIndex(x);
            }
        }

        String labelDescription = "";
        if ((mounted != null) && (mounted.getBaMountLoc() != BattleArmor.MOUNT_LOC_NONE)) {
            labelDescription += " (" + BattleArmor.MOUNT_LOC_NAMES[mounted.getBaMountLoc()] + ')';
        } else {
            labelDescription = "None";
        }
        JLabel labelLocation = new JLabel(labelDescription);
        GridBagLayout gridBagLayout = new GridBagLayout();
        setLayout(gridBagLayout);
        add(labelLocation, GBC.std());
        add(comboChoices, GBC.std());
    }

    public void applyChoice() {
        int selectedIndex = comboChoices.getSelectedIndex();
        // If there's no selection, there's nothing we can do
        if (selectedIndex == -1) {
            return;
        }

        WeaponType weaponType = null;
        if ((selectedIndex > 0) && (selectedIndex <= weaponTypes.size())) {
            // Need to account for the "None" selection
            weaponType = weaponTypes.get(selectedIndex - 1);
        }

        // Remove any currently mounted AP weapon
        if (apMounted.getLinked() != null && apMounted.getLinked().getType() != weaponType) {
            Mounted<?> mAPMountedLinked = apMounted.getLinked();
            entity.getEquipment().remove(mAPMountedLinked);

            if (mAPMountedLinked instanceof WeaponMounted weaponMounted) {
                entity.getWeaponList().remove(weaponMounted);
                entity.getTotalWeaponList().remove(weaponMounted);
            }

            // We need to make sure that the weapon has been removed from the critical slots, otherwise it can cause
            // issues
            for (int location = 0; location < entity.locations(); location++) {
                for (int locationCritical = 0;
                      locationCritical < entity.getNumberOfCriticals(location);
                      locationCritical++) {
                    CriticalSlot criticalSlot = entity.getCritical(location, locationCritical);
                    if (criticalSlot != null &&
                              criticalSlot.getMount() != null &&
                              criticalSlot.getMount().equals(mAPMountedLinked)) {
                        entity.setCritical(location, locationCritical, null);
                    }
                }
            }
        }

        // Did the selection not change, or no weapon was selected
        if ((apMounted.getLinked() != null && apMounted.getLinked().getType() == weaponType) || selectedIndex == 0) {
            return;
        }

        // Add the newly mounted weapon
        try {
            Mounted<?> newWeapon = entity.addEquipment(weaponType, apMounted.getLocation());
            apMounted.setLinked(newWeapon);
            newWeapon.setLinked(apMounted);
            newWeapon.setAPMMounted(true);
        } catch (LocationFullException ex) {
            // This shouldn't happen for BA...
            LOGGER.error(ex, "Location Full");
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        comboChoices.setEnabled(enabled);
    }
}
