/*
 * MegaMek - Copyright (C) 2017 - The MegaMek Team
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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.LocationFullException;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.logging.MMLogger;

/**
 * @author Neoancient
 *
 */
public class BayMunitionsChoicePanel extends JPanel {
    
    private final static int ATM_STD = 0;
    private final static int ATM_HE  = 1;
    private final static int ATM_ER  = 2;
    private final static int NUM_ATM = 3;
    
    private final static int MML_SRM = 0;
    private final static int MML_LRM = 1;
    private final static int NUM_MML = 2;

    private final static int AR10_KW  = 0;
    private final static int AR10_WS  = 1;
    private final static int AR10_B   = 2;
    private final static int NUM_AR10 = 3;

    private final Entity entity;
    private final Map<Integer,List<AmmoRow>> rows = new HashMap<>();
    
    public BayMunitionsChoicePanel(Entity entity) {
        this.entity = entity;
        rows.put(AmmoType.T_ATM, new ArrayList<>());
        rows.put(AmmoType.T_MML, new ArrayList<>());
        rows.put(AmmoType.T_AR10, new ArrayList<>());
        
        for (Mounted bay : entity.getWeaponBayList()) {
            Map<List<Integer>,List<Mounted>> ammoByType = new HashMap<>();
            for (Integer aNum : bay.getBayAmmo()) {
                final Mounted ammo = entity.getEquipment(aNum);
                if ((null != ammo) && (ammo.getType() instanceof AmmoType)) {
                    AmmoType atype = (AmmoType) ammo.getType();
                    if ((atype.getAmmoType() == AmmoType.T_ATM)
                            || (atype.getAmmoType() == AmmoType.T_IATM)
                            || (atype.getAmmoType() == AmmoType.T_MML)
                            || (atype.getAmmoType() == AmmoType.T_AR10)) {
                        List<Integer> key = new ArrayList<>(2);
                        key.add(atype.getAmmoType());
                        key.add(atype.getRackSize());
                        ammoByType.putIfAbsent(key, new ArrayList<>());
                        ammoByType.get(key).add(ammo);
                    }
                }
            }
            for (List<Integer> key : ammoByType.keySet()) {
                if (key.get(0) == AmmoType.T_IATM) {
                    rows.get(AmmoType.T_ATM).add(new AmmoRow(bay, key.get(0), key.get(1), ammoByType.get(key)));
                } else {
                    rows.get(key.get(0)).add(new AmmoRow(bay, key.get(0), key.get(1), ammoByType.get(key)));
                }
            }
        }
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        if (rows.get(AmmoType.T_ATM).size() > 0) {
            add(new JLabel("<html><b>ATM Bays</b></html>"), gbc);
            gbc.gridx++;
            gbc.anchor = GridBagConstraints.CENTER;
            add(new JLabel("<html><b>Std</b></html>"), gbc);
            gbc.gridx++;
            add(new JLabel("<html><b>HE</b></html>"), gbc);
            gbc.gridx++;
            add(new JLabel("<html><b>ER</b></html>"), gbc);
            gbc.anchor = GridBagConstraints.WEST;
            for (AmmoRow row : rows.get(AmmoType.T_ATM)) {
                final Optional<WeaponType> wtype = row.bay.getBayWeapons().stream()
                        .map(wNum -> entity.getEquipment(wNum))
                        .map(m -> (WeaponType) m.getType()).findAny();
                gbc.gridy++;
                gbc.gridx = 0;
                add(new JLabel("(" + entity.getLocationAbbr(row.bay.getLocation()) + ") "
                        + (wtype.isPresent()? wtype.get().getName() : "?")), gbc);
                gbc.gridx++;
                add(row.spinners[ATM_STD], gbc);
                gbc.gridx++;
                add(row.spinners[ATM_HE], gbc);
                gbc.gridx++;
                add(row.spinners[ATM_ER], gbc);
            }
        }
        if (rows.get(AmmoType.T_MML).size() > 0) {
            gbc.gridy++;
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.WEST;
            add(new JLabel("MML Bays"), gbc);
            gbc.gridx++;
            gbc.anchor = GridBagConstraints.CENTER;
            add(new JLabel("SRM"), gbc);
            gbc.gridx++;
            add(new JLabel("LRM"), gbc);
            gbc.anchor = GridBagConstraints.WEST;
            for (AmmoRow row : rows.get(AmmoType.T_MML)) {
                final Optional<WeaponType> wtype = row.bay.getBayWeapons().stream()
                        .map(wNum -> entity.getEquipment(wNum))
                        .map(m -> (WeaponType) m.getType()).findAny();
                gbc.gridy++;
                gbc.gridx = 0;
                add(new JLabel("(" + entity.getLocationAbbr(row.bay.getLocation()) + ") "
                        + (wtype.isPresent()? wtype.get().getName() : "?")), gbc);
                gbc.gridx++;
                add(row.spinners[MML_SRM], gbc);
                gbc.gridx++;
                add(row.spinners[MML_LRM], gbc);
            }
        }
        if (rows.get(AmmoType.T_AR10).size() > 0) {
            gbc.gridy++;
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.WEST;
            add(new JLabel("Capital Missile Bays"), gbc);
            gbc.gridx++;
            gbc.anchor = GridBagConstraints.CENTER;
            add(new JLabel("Killer Whale"), gbc);
            gbc.gridx++;
            add(new JLabel("White Shark"), gbc);
            gbc.gridx++;
            add(new JLabel("Barracuda"), gbc);
            gbc.anchor = GridBagConstraints.WEST;
            for (int i = 0; i < rows.get(AmmoType.T_AR10).size(); i++) {
                final AmmoRow row = rows.get(AmmoType.T_AR10).get(i);
                final Optional<WeaponType> wtype = row.bay.getBayWeapons().stream()
                        .map(wNum -> entity.getEquipment(wNum))
                        .map(m -> (WeaponType) m.getType()).findAny();
                gbc.gridy++;
                gbc.gridx = 0;;
                add(new JLabel("(" + entity.getLocationAbbr(row.bay.getLocation()) + ") "
                        + (wtype.isPresent()? wtype.get().getName() : "?")), gbc);
                gbc.gridx++;
                add(row.spinners[AR10_KW], gbc);
                gbc.gridx++;
                add(row.spinners[AR10_WS], gbc);
                gbc.gridx++;
                add(row.spinners[AR10_B], gbc);
            }
        }
    }
    
    /**
     * Change the munition types of the bay ammo mounts to the selected values. If there are more
     * munition types than there were originally, additional ammo bin mounts will be added. If fewer,
     * the unneeded ones will have their shot count zeroed.
     */
    public void apply() {
        for (List<AmmoRow> list : rows.values()) {
            for (AmmoRow row : list) {
                int mountIndex = 0;
                double remainingWeight = row.tonnage;
                for (int i = 0; i < row.munitions.length; i++) {
                    int shots = (Integer) row.spinners[i].getValue();
                    if (shots > 0) {
                        Mounted mounted = null;
                        if (mountIndex >= row.ammoMounts.size()) {
                            mounted = new Mounted(entity, row.munitions[i]);
                            try {
                                entity.addEquipment(mounted, row.bay.getLocation(), row.bay.isRearMounted());
                                row.bay.addAmmoToBay(entity.getEquipmentNum(mounted));
                            } catch (LocationFullException e) {
                                DefaultMmLogger.getInstance().log(BayMunitionsChoicePanel.class,
                                        "apply()", e);
                            }
                            
                        } else {
                            mounted = row.ammoMounts.get(mountIndex);
                            mounted.changeAmmoType(row.munitions[i]);
                        }
                        mounted.setShotsLeft(shots);
                        int slots = (int) Math.ceil((double) shots / row.munitions[i].getShots());
                        mounted.setOriginalShots(slots * row.munitions[i].getShots());
                        mounted.setAmmoCapacity(slots * row.munitions[i].getTonnage(entity));
                        remainingWeight -= mounted.getAmmoCapacity();
                        mountIndex++;
                    }
                }
                // Zero out any remaining unused bins.
                while (mountIndex < row.ammoMounts.size()) {
                    Mounted mount = row.ammoMounts.get(mountIndex);
                    mount.setAmmoCapacity(0);
                    mount.setOriginalShots(0);
                    mount.setShotsLeft(0);
                    mountIndex++;
                }
                // If the unit is assigned less ammo than the capacity, assign remaining weight to first mount
                // and adjust original shots.
                if (remainingWeight > 0) {
                    Mounted m = row.ammoMounts.get(0);
                    AmmoType at = (AmmoType) m.getType();
                    m.setAmmoCapacity(m.getAmmoCapacity() + remainingWeight);
                    m.setOriginalShots((int) Math.floor(m.getAmmoCapacity() / (at.getShots() * at.getTonnage(entity))));
                }
            }
        }
    }
    
    class AmmoRow implements ChangeListener {
        private final Mounted bay;
        private final int at;
        private final int rackSize;
        private final List<Mounted> ammoMounts;
        
        private JSpinner[] spinners;
        private AmmoType[] munitions;
        
        private double tonnage = 0;
        
        AmmoRow(Mounted bay, int at, int rackSize, List<Mounted> ammoMounts) {
            this.bay = bay;
            this.at = at;
            this.rackSize = rackSize;
            this.ammoMounts = new ArrayList<>(ammoMounts);
            
            switch (at) {
                case AmmoType.T_ATM:
                case AmmoType.T_IATM:
                    addATMSpinners();
                    break;
                case AmmoType.T_MML:
                    addMMLSpinners();
                    break;
                case AmmoType.T_AR10:
                    addAR10Spinners();
                    break;
            }
            tonnage = ammoMounts.stream().mapToDouble(m -> m.getAmmoCapacity()).sum();
            recalcMaxValues();
        }
        
        private void addATMSpinners() {
            munitions = new AmmoType[NUM_ATM];
            int[] starting = new int[NUM_ATM];
            spinners = new JSpinner[NUM_ATM];
            for (AmmoType atype : AmmoType.getMunitionsFor(at)) {
                if (atype.getRackSize() == rackSize) {
                    if (atype.getMunitionType() == AmmoType.M_STANDARD) {
                        munitions[ATM_STD] = atype;
                    } else if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                        munitions[ATM_HE] = atype;
                    } else if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                        munitions[ATM_ER] = atype;
                    }
                }
            }
            for (Mounted m : ammoMounts) {
                if (((AmmoType) m.getType()).getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                    starting[ATM_HE] += m.getBaseShotsLeft();
                } else if (((AmmoType) m.getType()).getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                    starting[ATM_ER] += m.getBaseShotsLeft();
                } else {
                    starting[ATM_STD] += m.getBaseShotsLeft();
                }
            }
            for (int i = 0; i < starting.length; i++) {
                spinners[i] = new JSpinner(new SpinnerNumberModel(starting[i],
                        0, null, munitions[i].getShots()));
                spinners[i].setPreferredSize(new Dimension(55, 25));
                spinners[i].setName(String.valueOf(i));
                spinners[i].addChangeListener(this);
            }
        }

        private void addMMLSpinners() {
            munitions = new AmmoType[NUM_MML];
            int[] starting = new int[NUM_MML];
            spinners = new JSpinner[NUM_MML];
            for (AmmoType atype : AmmoType.getMunitionsFor(at)) {
                if ((atype.getRackSize() == rackSize)
                        && (atype.getMunitionType() == ((AmmoType) ammoMounts.get(0).getType()).getMunitionType())){
                    if (atype.hasFlag(AmmoType.F_MML_LRM)) {
                        munitions[MML_LRM] = atype;
                    } else {
                        munitions[MML_SRM] = atype;
                    }
                }
            }
            for (Mounted m : ammoMounts) {
                if (m.getType().hasFlag(AmmoType.F_MML_LRM)) {
                    starting[MML_LRM] += m.getBaseShotsLeft();
                } else {
                    starting[MML_SRM] += m.getBaseShotsLeft();
                }
            }
            for (int i = 0; i < starting.length; i++) {
                spinners[i] = new JSpinner(new SpinnerNumberModel(starting[i],
                        0, null, munitions[i].getShots()));
                spinners[i].setPreferredSize(new Dimension(55, 25));
                spinners[i].setName(String.valueOf(i));
                spinners[i].addChangeListener(this);
            }
        }

        private void addAR10Spinners() {
            munitions = new AmmoType[NUM_AR10];
            int[] starting = new int[NUM_AR10];
            spinners = new JSpinner[NUM_AR10];
            for (AmmoType atype : AmmoType.getMunitionsFor(at)) {
                if ((atype.getRackSize() == rackSize)
                        && (atype.getMunitionType() == ((AmmoType) ammoMounts.get(0).getType()).getMunitionType())){
                    if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                        munitions[AR10_KW] = atype;
                    } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                        munitions[AR10_WS] = atype;
                    } else if (atype.hasFlag(AmmoType.F_AR10_BARRACUDA)) {
                        munitions[AR10_B] = atype;
                    }
                }
            }
            for (Mounted m : ammoMounts) {
                if (m.getType().hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                    starting[AR10_KW] += m.getBaseShotsLeft();
                } else if (m.getType().hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                    starting[AR10_WS] += m.getBaseShotsLeft();
                } else if (m.getType().hasFlag(AmmoType.F_AR10_BARRACUDA)) {
                    starting[AR10_B] += m.getBaseShotsLeft();
                }
            }
            for (int i = 0; i < starting.length; i++) {
                spinners[i] = new JSpinner(new SpinnerNumberModel(starting[i],
                        0, null, munitions[i].getShots()));
                spinners[i].setPreferredSize(new Dimension(55, 25));
                spinners[i].setName(String.valueOf(i));
                spinners[i].addChangeListener(this);
            }
        }
        
        private void recalcMaxValues() {
            double[] currentWeight = new double[spinners.length];
            double remaining = tonnage;
            for (int i = 0; i < spinners.length; i++) {
                currentWeight[i] += munitions[i].getTonnage(entity)
                        * ((Integer) spinners[i].getValue() / munitions[i].getShots());
                remaining -= currentWeight[i];
            }
            for (int i = 0; i < spinners.length; i++) {
                int max = (int) Math.floor((currentWeight[i] + remaining)
                        / munitions[i].getTonnage(entity) * munitions[i].getShots());
                spinners[i].removeChangeListener(this);
                ((SpinnerNumberModel) spinners[i].getModel()).setMaximum(max);
                spinners[i].addChangeListener(this);
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            recalcMaxValues();
        }
    }

}
