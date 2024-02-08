/*
 * MegaMek - Copyright (C) 2020 The MegaMek Team
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client.ui.swing;

import megamek.client.ui.Messages;
import megamek.common.*;
import megamek.common.weapons.infantry.InfantryWeapon;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Panel that allows splitting ammo between standard and inferno for light and medium weapons
 * that have inferno variants.
 */
public class SmallSVMunitionsChoicePanel extends JPanel {
    private final Entity entity;
    private final List<AmmoRowPanel> rows = new ArrayList<>();

    public SmallSVMunitionsChoicePanel(Entity entity) {
        this.entity = entity;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 0, 10, 0);

        for (Mounted weapon : entity.getWeaponList()) {
            if ((weapon.getType() instanceof InfantryWeapon)
                    && ((InfantryWeapon) weapon.getType()).hasInfernoAmmo()) {
                AmmoRowPanel row = new AmmoRowPanel(weapon);
                gbc.gridy++;
                add(row, gbc);
                rows.add(row);
            }
        }
    }

    /**
     * Distribute the ammo between the standard and inferno bins. Original shots in each bin will
     * be set to the number of shots rounded up to full clips. Any completely empty clips will
     * be assigned to the standard bin.
     */
    public void apply() {
        for (AmmoRowPanel row : rows) {
            row.stdAmmo.setShotsLeft((Integer) row.spnStandard.getValue());
            row.infernoAmmo.setShotsLeft((Integer) row.spnInferno.getValue());
            int infernoClips = (int) Math.ceil((double) row.infernoAmmo.getBaseShotsLeft() / row.shotsPerClip);
            int stdClips = (int) row.weapon.getSize() - infernoClips;
            row.infernoAmmo.setOriginalShots(infernoClips * row.shotsPerClip);
            row.stdAmmo.setOriginalShots(stdClips * row.shotsPerClip);
        }
    }

    class AmmoRowPanel extends JPanel {
        private final Mounted weapon;
        private final Mounted stdAmmo;
        private final Mounted infernoAmmo;
        private final int shotsPerClip;
        private final int maxShots;

        private final JSpinner spnStandard;
        private final JSpinner spnInferno;

        AmmoRowPanel(Mounted weapon) {
            this.weapon = weapon;
            shotsPerClip = ((InfantryWeapon) weapon.getType()).getShots();
            maxShots = (int) weapon.getSize() * ((InfantryWeapon) weapon.getType()).getShots();
            if (weapon.getLinked() == null) {
                Mounted ammo = addAmmoMount(EquipmentType.get(EquipmentTypeLookup.INFANTRY_AMMO), maxShots);
                weapon.setLinked(ammo);
            }
            stdAmmo = weapon.getLinked();
            if (stdAmmo.getLinked() == null) {
                Mounted ammo = addAmmoMount(EquipmentType.get(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO), 0);
                stdAmmo.setLinked(ammo);
            }
            infernoAmmo = stdAmmo.getLinked();
            spnStandard = new JSpinner(new SpinnerNumberModel(stdAmmo.getBaseShotsLeft(), 0, maxShots, 1));
            spnInferno = new JSpinner(new SpinnerNumberModel(infernoAmmo.getBaseShotsLeft(), 0, maxShots, 1));

            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(0, 5, 0, 5);
            gbc.gridwidth = 5;
            add(new JLabel(String.format("(%s) %s", entity.getLocationAbbr(weapon.getLocation()),
                    weapon.getName())), gbc);
            gbc.gridx = 5;
            gbc.gridwidth = 1;
            gbc.weightx = 1.0;
            add(new JLabel(String.format(Messages.getString("CustomMechDialog.formatSmSVAmmoShots"),
                    shotsPerClip, (int) weapon.getSize())), gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 1;
            gbc.weightx = 0.0;
            add(new JLabel("Standard"), gbc);
            gbc.gridx++;
            add(spnStandard, gbc);
            gbc.gridx++;
            add(new JLabel("Inferno"), gbc);
            gbc.gridx++;
            add(spnInferno, gbc);
            spnStandard.addChangeListener(ev -> recalcMaxValues());
            spnInferno.addChangeListener(ev -> recalcMaxValues());
            recalcMaxValues();
        }

        private Mounted addAmmoMount(EquipmentType ammo, int shots) {
            Mounted mount = Mounted.createMounted(weapon.getEntity(), ammo);
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
