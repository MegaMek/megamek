/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.client.ui.GBC2;
import megamek.client.ui.Messages;
import megamek.common.units.Entity;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.exceptions.LocationFullException;
import megamek.common.equipment.Mounted;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * Panel that allows splitting ammo between standard and inferno for light and medium weapons that have inferno
 * variants.
 */
class SmallSVMunitionsChoice {

    private final List<AmmoRow> rows = new ArrayList<>();

    public SmallSVMunitionsChoice(Entity entity, JPanel parentPanel, GBC2 gbc) {
        for (Mounted<?> weapon : entity.getWeaponList()) {
            if ((weapon.getType() instanceof InfantryWeapon infantryWeapon) && (infantryWeapon.hasInfernoAmmo())) {
                rows.add(new AmmoRow(entity, weapon, parentPanel, gbc));
            }
        }
    }

    boolean isEmpty() {
        return rows.isEmpty();
    }

    /**
     * Distribute the ammo between the standard and inferno bins. Original shots in each bin will be set to the number
     * of shots rounded up to full clips. Any completely empty clips will be assigned to the standard bin.
     */
    public void apply() {
        for (AmmoRow row : rows) {
            row.stdAmmo.setShotsLeft((Integer) row.spnStandard.getValue());
            row.infernoAmmo.setShotsLeft((Integer) row.spnInferno.getValue());
            int infernoClips = (int) Math.ceil((double) row.infernoAmmo.getBaseShotsLeft() / row.shotsPerClip);
            int stdClips = (int) row.weapon.getSize() - infernoClips;
            row.infernoAmmo.setOriginalShots(infernoClips * row.shotsPerClip);
            row.stdAmmo.setOriginalShots(stdClips * row.shotsPerClip);
        }
    }

    private static class AmmoRow {

        private final Mounted<?> weapon;
        private final Mounted<?> stdAmmo;
        private final Mounted<?> infernoAmmo;
        private final int shotsPerClip;
        private final int maxShots;

        private final JSpinner spnStandard;
        private final JSpinner spnInferno;

        AmmoRow(Entity entity, Mounted<?> weapon, JPanel parentPanel, GBC2 gbc) {
            this.weapon = weapon;
            InfantryWeapon weaponType = (InfantryWeapon) weapon.getType();
            shotsPerClip = weaponType.getShots();
            int clips = (int) weapon.getSize();
            maxShots = clips * shotsPerClip;
            if (weapon.getLinked() == null) {
                Mounted<?> ammo = addAmmoMount(EquipmentType.get(EquipmentTypeLookup.INFANTRY_AMMO), maxShots);
                weapon.setLinked(ammo);
            }
            stdAmmo = weapon.getLinked();
            if (stdAmmo.getLinked() == null) {
                Mounted<?> ammo = addAmmoMount(EquipmentType.get(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO), 0);
                stdAmmo.setLinked(ammo);
            }
            infernoAmmo = stdAmmo.getLinked();
            spnStandard = new JSpinner(new SpinnerNumberModel(stdAmmo.getBaseShotsLeft(), 0, maxShots, 1));
            spnInferno = new JSpinner(new SpinnerNumberModel(infernoAmmo.getBaseShotsLeft(), 0, maxShots, 1));

            parentPanel.add(new JLabel("(%s) %s:".formatted(
                  entity.getLocationAbbr(weapon.getLocation()),
                  weapon.getShortName())), gbc.forLabel());
            parentPanel.add(new JLabel(Messages.getString("CustomMekDialog.formatSmSVAmmoShots",
                  shotsPerClip, clips)), gbc.eol());

            parentPanel.add(new JLabel("Standard"), gbc.forLabel());
            parentPanel.add(spnStandard, gbc.oneColumn());
            parentPanel.add(new JLabel("Inferno"), gbc.oneColumn());
            parentPanel.add(spnInferno, gbc.eol());

            spnStandard.addChangeListener(ev -> recalcMaxValues());
            spnInferno.addChangeListener(ev -> recalcMaxValues());
            recalcMaxValues();
        }

        private Mounted<?> addAmmoMount(EquipmentType ammo, int shots) {
            Mounted<?> mount = Mounted.createMounted(weapon.getEntity(), ammo);
            mount.setOmniPodMounted(mount.isOmniPodMounted());
            mount.setShotsLeft(shots);
            mount.setOriginalShots(shots);
            try {
                weapon.getEntity().addEquipment(mount, Entity.LOC_NONE, false);
            } catch (LocationFullException ignored) {
                // Added to LOC_NONE
            }
            return mount;
        }

        private void recalcMaxValues() {
            int stdClips = (int) Math.ceil(((Number) spnStandard.getValue()).doubleValue() / shotsPerClip);
            int infernoClips = (int) Math.ceil(((Number) spnInferno.getValue()).doubleValue() / shotsPerClip);
            int remainingClips = maxShots / shotsPerClip - stdClips - infernoClips;

            ((SpinnerNumberModel) spnStandard.getModel()).setMaximum((stdClips + remainingClips) * shotsPerClip);
            ((SpinnerNumberModel) spnInferno.getModel()).setMaximum((infernoClips + remainingClips) * shotsPerClip);
        }
    }
}
