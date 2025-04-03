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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC;
import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * A panel representing the option to choose a particular ammo bin for an individual weapon.
 *
 * @author NickAragua
 */
public class WeaponAmmoChoicePanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 604670659251519188L;
    // the weapon being displayed in this row
    private final WeaponMounted weaponMounted;
    private final ArrayList<AmmoMounted> matchingAmmoBins = new ArrayList<>();
    private final JComboBox<String> comboAmmoBins = new JComboBox<>();

    private final Entity entity;

    /**
     * Constructor
     *
     * @param weapon The mounted weapon. Assumes that the weapon uses ammo.
     */
    public WeaponAmmoChoicePanel(WeaponMounted weapon, Entity entity) {
        this.entity = entity;
        weaponMounted = weapon;

        this.setLayout(new GridBagLayout());
        
        if (weaponMounted.isOneShot() ||
                  (entity.isSupportVehicle() && (weaponMounted.getType() instanceof InfantryWeapon))) {
            // One-shot weapons can only access their own bin
            matchingAmmoBins.add(weaponMounted.getLinkedAmmo());
            // Fusillade and some small SV weapons are treated like one-shot weapons but may have a second munition
            // type available.
            if ((weaponMounted.getLinked().getLinked() != null) &&
                      (((AmmoType) weaponMounted.getLinked().getType()).getMunitionType() !=
                             (((AmmoType) weaponMounted.getLinked().getLinked().getType()).getMunitionType()))) {
                matchingAmmoBins.add((AmmoMounted) weaponMounted.getLinked().getLinked());
            }
        } else {
            for (AmmoMounted ammoBin : weapon.getEntity().getAmmo()) {
                if ((ammoBin.getLocation() != Entity.LOC_NONE) && AmmoType.canSwitchToAmmo(weapon, ammoBin.getType())) {
                    matchingAmmoBins.add(ammoBin);
                }
            }
        }

        // don't bother displaying the row if there's no ammo to be swapped
        if (matchingAmmoBins.isEmpty()) {
            return;
        }

        JLabel weaponName = new JLabel();
        weaponName.setText("(" + weapon.getEntity().getLocationAbbr(weapon.getLocation()) + ") " + weapon.getName());
        add(weaponName, GBC.std());

        add(comboAmmoBins, GBC.eol());
        refreshAmmoBinNames();
    }

    /**
     * Worker function that refreshes the combo box with "up-to-date" ammo names.
     */
    public void refreshAmmoBinNames() {
        int selectedIndex = comboAmmoBins.getSelectedIndex();
        comboAmmoBins.removeAllItems();

        int currentIndex = 0;
        for (Mounted<?> ammoBin : matchingAmmoBins) {
            comboAmmoBins.addItem("(" +
                                        ammoBin.getEntity().getLocationAbbr(ammoBin.getLocation()) +
                                        ") " +
                                        ammoBin.getName());
            if (weaponMounted.getLinked() == ammoBin) {
                selectedIndex = currentIndex;
            }

            currentIndex++;
        }

        if (selectedIndex >= 0) {
            comboAmmoBins.setSelectedIndex(selectedIndex);
        }

        validate();
    }

    /**
     * Refreshes a single item in the ammo type combo box to display the correct ammo type name. Because the underlying
     * ammo bin hasn't been updated yet, we carry out the name swap "in-place".
     *
     * @param ammoBin          The ammo bin whose ammo type has probably changed.
     * @param selectedAmmoType The new ammo type.
     */
    public void refreshAmmoBinName(Mounted<?> ammoBin, AmmoType selectedAmmoType) {
        int index;
        boolean matchFound = false;

        for (index = 0; index < matchingAmmoBins.size(); index++) {
            if (matchingAmmoBins.get(index) == ammoBin) {
                matchFound = true;
                break;
            }
        }

        if (matchFound) {
            int currentBinIndex = comboAmmoBins.getSelectedIndex();

            comboAmmoBins.removeItemAt(index);
            comboAmmoBins.insertItemAt("(" +
                                             ammoBin.getEntity().getLocationAbbr(ammoBin.getLocation()) +
                                             ") " +
                                             selectedAmmoType.getName(), index);

            if (currentBinIndex == index) {
                comboAmmoBins.setSelectedIndex(index);
            }

            validate();
        }
    }

    /**
     * Common functionality that applies the panel's current ammo bin choice to the panel's weapon.
     */
    public void applyChoice() {
        int selectedIndex = comboAmmoBins.getSelectedIndex();
        if ((selectedIndex >= 0) && (selectedIndex < matchingAmmoBins.size())) {
            entity.loadWeapon(weaponMounted, matchingAmmoBins.get(selectedIndex));
        }
    }
}
