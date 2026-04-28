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
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.dialogs.customMek;

import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC2;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * A panel representing the option to choose a particular ammo bin for an individual weapon.
 *
 * @author NickAragua
 */
public class WeaponAmmoChoice {

    // the weapon being displayed in this row
    private final WeaponMounted weapon;
    private final ArrayList<AmmoMounted> matchingAmmoBins = new ArrayList<>();
    private final JComboBox<String> comboAmmoBins = new JComboBox<>();
    private final Entity entity;

    /**
     * Constructor
     *
     * @param weapon The mounted weapon. Assumes that the weapon uses ammo.
     */
    public WeaponAmmoChoice(WeaponMounted weapon, Entity entity, JPanel parentPanel, GBC2 gbc) {
        this.entity = entity;
        this.weapon = weapon;

        if (weapon.isOneShot() ||
              (entity.isSupportVehicle() && (weapon.getType() instanceof InfantryWeapon))) {
            // One-shot weapons can only access their own bin
            matchingAmmoBins.add(weapon.getLinkedAmmo());
            // Fusillade and some small SV weapons are treated like one-shot weapons but may have a second munition
            // type available.
            AmmoMounted firstBin = (AmmoMounted) weapon.getLinked();
            if ((firstBin.getLinked() instanceof AmmoMounted secondBin) &&
                  (firstBin.getType().getMunitionType() != secondBin.getType().getMunitionType())) {
                matchingAmmoBins.add(secondBin);
            }
        } else if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAPON_NEG_STATIC_FEED)
              && (weapon.getLinkedAmmo() != null)) {
            // Static Ammo Feed weapons are locked to their specific ammo bin (CamOps p.235/BMM p.89)
            // Only use this path if the weapon already has linked ammo; otherwise fall through to
            // the regular logic which uses canSwitchToAmmo() to find compatible bins
            matchingAmmoBins.add(weapon.getLinkedAmmo());
        } else {
            entity.getAmmo().stream()
                  .filter(ammo -> ammo.getLocation() != Entity.LOC_NONE)
                  .filter(ammo -> AmmoType.canSwitchToAmmo(weapon, ammo.getType()))
                  .forEach(matchingAmmoBins::add);
        }

        // don't bother displaying the row if there's no ammo to be swapped
        if (matchingAmmoBins.isEmpty()) {
            return;
        }

        String weaponName = "(%s) %s:"
              .formatted(weapon.getEntity().getLocationAbbr(weapon.getLocation()), weapon.getShortName());
        parentPanel.add(new JLabel(weaponName), gbc.forLabel());
        parentPanel.add(comboAmmoBins, gbc.eol());
        refreshAmmoBinNames();
        comboAmmoBins.setEnabled(matchingAmmoBins.size() > 1);
    }

    boolean isEmpty() {
        return matchingAmmoBins.isEmpty();
    }

    /**
     * Worker function that refreshes the combo box with "up-to-date" ammo names.
     */
    public void refreshAmmoBinNames() {
        int selectedIndex = comboAmmoBins.getSelectedIndex();
        comboAmmoBins.removeAllItems();

        int currentIndex = 0;
        for (Mounted<?> ammoBin : matchingAmmoBins) {
            boolean isInternal = ammoBin.isOneShotAmmo() || ammoBin.isOneShot() || (ammoBin.getLocation() == -1);
            String prefix = isInternal ? "(Internal) " :
                  "(" + ammoBin.getEntity().getLocationAbbr(ammoBin.getLocation()) + ") ";
            String ammoBinName = prefix + ammoBin.getName();
            comboAmmoBins.addItem(ammoBinName);

            if (weapon.getLinked() == ammoBin) {
                selectedIndex = currentIndex;
            }

            currentIndex++;
        }

        if (selectedIndex >= 0) {
            comboAmmoBins.setSelectedIndex(selectedIndex);
        }
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
        }
    }

    /**
     * Common functionality that applies the panel's current ammo bin choice to the panel's weapon.
     */
    public void applyChoice() {
        int selectedIndex = comboAmmoBins.getSelectedIndex();
        if ((selectedIndex >= 0) && (selectedIndex < matchingAmmoBins.size())) {
            entity.loadWeapon(weapon, matchingAmmoBins.get(selectedIndex));
        }
    }
}
