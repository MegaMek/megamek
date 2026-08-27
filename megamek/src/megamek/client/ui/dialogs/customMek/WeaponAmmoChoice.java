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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC2;
import megamek.client.ui.comboBoxes.SearchableComboBox;
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
    private final List<AmmoMounted> matchingAmmoBins = new ArrayList<>();
    /** Munitions chosen in the Carried Munitions panel but not yet applied to their bins, keyed by bin. */
    private final Map<AmmoMounted, AmmoType> pendingAmmoTypes = new HashMap<>();
    /** The bin chooser, or {@code null} when the weapon has no bins to choose from and the row is not shown. */
    private final SearchableComboBox<AmmoMounted> comboAmmoBins;
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
            comboAmmoBins = null;
            return;
        }

        comboAmmoBins = new SearchableComboBox<>("comboAmmoBins", matchingAmmoBins, this::displayName);
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
     * Shows the bins with their current names and selects the bin the weapon is loaded from.
     */
    public void refreshAmmoBinNames() {
        if (comboAmmoBins == null) {
            return;
        }
        comboAmmoBins.refreshDisplayTexts();
        boolean isLinkedBinListed = (weapon.getLinked() instanceof AmmoMounted linkedBin)
              && matchingAmmoBins.contains(linkedBin);
        if (isLinkedBinListed) {
            comboAmmoBins.setSelectedItem(weapon.getLinked());
        }
    }

    /**
     * Shows a bin under the name of the munition just chosen for it. Because the underlying ammo bin hasn't been
     * updated yet, the new name is remembered here and used until the choice is applied.
     *
     * @param ammoBin          The ammo bin whose ammo type has probably changed.
     * @param selectedAmmoType The new ammo type.
     */
    public void refreshAmmoBinName(Mounted<?> ammoBin, AmmoType selectedAmmoType) {
        if ((comboAmmoBins == null) || !(ammoBin instanceof AmmoMounted renamedBin)) {
            return;
        }
        if (!matchingAmmoBins.contains(renamedBin)) {
            return;
        }
        pendingAmmoTypes.put(renamedBin, selectedAmmoType);
        comboAmmoBins.refreshDisplayTexts();
    }

    /**
     * @param ammoBin the bin to describe
     *
     * @return the bin's location prefix and munition name, using a munition chosen but not yet applied when there
     *       is one
     */
    private String displayName(AmmoMounted ammoBin) {
        boolean isInternal = ammoBin.isOneShotAmmo() || ammoBin.isOneShot() || (ammoBin.getLocation() == -1);
        String prefix = isInternal ? "(Internal) " :
              "(" + ammoBin.getEntity().getLocationAbbr(ammoBin.getLocation()) + ") ";
        AmmoType pendingAmmoType = pendingAmmoTypes.get(ammoBin);
        String munitionName = (pendingAmmoType == null) ? ammoBin.getName() : pendingAmmoType.getName();
        return prefix + munitionName;
    }

    /**
     * Common functionality that applies the panel's current ammo bin choice to the panel's weapon.
     */
    public void applyChoice() {
        if (comboAmmoBins == null) {
            return;
        }
        AmmoMounted selectedBin = comboAmmoBins.getSelectedItem();
        if (selectedBin != null) {
            entity.loadWeapon(weapon, selectedBin);
        }
    }
}
