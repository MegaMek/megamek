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
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import megamek.client.ui.Messages;
import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.LocationFullException;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.WeaponType;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.options.OptionsConstants;

/**
 * @author Neoancient
 *
 */
public class BayMunitionsChoicePanel extends JPanel {
    
    /**
     * 
     */
    private static final long serialVersionUID = -7741380967676720496L;
    
    private final Entity entity;
    private final IGame game;
    private final List<AmmoRowPanel> rows = new ArrayList<>();
    
    public BayMunitionsChoicePanel(Entity entity, IGame game) {
        this.entity = entity;
        this.game = game;
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 0, 10, 0);

        for (Mounted bay : entity.getWeaponBayList()) {
            Map<List<Integer>,List<Mounted>> ammoByType = new HashMap<>();
            for (Integer aNum : bay.getBayAmmo()) {
                final Mounted ammo = entity.getEquipment(aNum);
                if ((null != ammo) && (ammo.getType() instanceof AmmoType)) {
                    AmmoType atype = (AmmoType) ammo.getType();
                    List<Integer> key = new ArrayList<>(2);
                    key.add(atype.getAmmoType());
                    key.add(atype.getRackSize());
                    key.add(atype.getTechBase());
                    ammoByType.putIfAbsent(key, new ArrayList<>());
                    ammoByType.get(key).add(ammo);
                }
            }
            for (List<Integer> key : ammoByType.keySet()) {
                AmmoRowPanel row = new AmmoRowPanel(bay, key.get(0), key.get(1), key.get(2), ammoByType.get(key));
                gbc.gridy++;
                add(row, gbc);
                rows.add(row);
            }
        }
    }
    
    /**
     * Change the munition types of the bay ammo mounts to the selected values. If there are more
     * munition types than there were originally, additional ammo bin mounts will be added. If fewer,
     * the unneeded ones will have their shot count zeroed.
     */
    public void apply() {
        for (AmmoRowPanel row : rows) {
            int mountIndex = 0;
            double remainingWeight = row.tonnage;
            for (int i = 0; i < row.munitions.size(); i++) {
                int shots = (Integer) row.spinners.get(i).getValue();
                if (shots > 0) {
                    Mounted mounted = null;
                    if (mountIndex >= row.ammoMounts.size()) {
                        mounted = new Mounted(entity, row.munitions.get(i));
                        try {
                            entity.addEquipment(mounted, row.bay.getLocation(), row.bay.isRearMounted());
                            row.bay.addAmmoToBay(entity.getEquipmentNum(mounted));
                        } catch (LocationFullException e) {
                            DefaultMmLogger.getInstance().log(BayMunitionsChoicePanel.class,
                                    "apply()", e);
                        }

                    } else {
                        mounted = row.ammoMounts.get(mountIndex);
                        mounted.changeAmmoType(row.munitions.get(i));
                    }
                    mounted.setShotsLeft(shots);
                    int slots = (int) Math.ceil((double) shots / row.munitions.get(i).getShots());
                    mounted.setOriginalShots(slots * row.munitions.get(i).getShots());
                    mounted.setAmmoCapacity(slots * row.munitions.get(i).getTonnage(entity));
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
    
    class AmmoRowPanel extends JPanel implements ChangeListener {
        /**
         * 
         */
        private static final long serialVersionUID = 7251618728823971065L;
        
        private final JLabel lblTonnage = new JLabel();
        
        private final Mounted bay;
        private final int at;
        private final int rackSize;
        private final int techBase;
        private final List<Mounted> ammoMounts;
        
        private final List<JSpinner> spinners;
        private final List<AmmoType> munitions;
        
        private double tonnage = 0;
        
        AmmoRowPanel(Mounted bay, int at, int rackSize, int techBase, List<Mounted> ammoMounts) {
            this.bay = bay;
            this.at = at;
            this.rackSize = rackSize;
            this.techBase = techBase;
            this.ammoMounts = new ArrayList<>(ammoMounts);
            this.spinners = new ArrayList<>();
            Dimension spinnerSize =new Dimension(55, 25);
            
            munitions = AmmoType.getMunitionsFor(at).stream()
                    .filter(this::includeMunition).collect(Collectors.toList());
            tonnage = ammoMounts.stream().mapToDouble(m -> m.getAmmoCapacity()).sum();
            Map<String,Integer> starting = new HashMap<>();
            ammoMounts.forEach(m -> starting.merge(m.getType().getInternalName(), m.getBaseShotsLeft(), Integer::sum));
            for (AmmoType atype : munitions) {
                JSpinner spn = new JSpinner(new SpinnerNumberModel(starting.getOrDefault(atype.getInternalName(), 0),
                        0, null, 1));
                spn.setPreferredSize(spinnerSize);
                spn.setName(atype.getInternalName());
                spn.addChangeListener(this);
                if (atype.getTonnage(entity) > 1) {
                    spn.setToolTipText(String.format(Messages.getString("CustomMechDialog.formatMissileTonnage"),
                            atype.getName(), atype.getTonnage(entity)));
                } else {
                    spn.setToolTipText(String.format(Messages.getString("CustomMechDialog.formatShotsPerTon"),
                            atype.getName(), atype.getShots()));
                }
                spinners.add(spn);
            }
            
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(0, 5, 0, 5);
            gbc.gridwidth = 5;
            final Optional<WeaponType> wtype = bay.getBayWeapons().stream()
                    .map(wNum -> entity.getEquipment(wNum))
                    .map(m -> (WeaponType) m.getType()).findAny();
            add(new JLabel("(" + entity.getLocationAbbr(bay.getLocation()) + ") "
                    + (wtype.isPresent()? wtype.get().getName() : "?")), gbc);
            gbc.gridx = 5;
            gbc.gridwidth = 1;
            gbc.weightx = 1.0;
            add(lblTonnage, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 1;
            gbc.weightx = 0.0;
            for (int i = 0; i < munitions.size(); i++) {
                add(new JLabel(createMunitionLabel(munitions.get(i))), gbc);
                gbc.gridx++;
                add(spinners.get(i), gbc);
                gbc.gridx++;
                if (gbc.gridx > 5) {
                    gbc.gridx = 0;
                    gbc.gridy++;
                }
            }
            recalcMaxValues();
        }
        
        private boolean includeMunition(AmmoType atype) {
            if (!atype.canAeroUse()
                    || (atype.getAmmoType() != at)
                    || (atype.getRackSize() != rackSize)
                    || ((atype.getTechBase() != techBase)
                            && (atype.getTechBase() != AmmoType.TECH_BASE_ALL)
                            && (techBase != AmmoType.TECH_BASE_ALL))
                    || !atype.isLegal(game.getOptions().intOption(OptionsConstants.ALLOWED_YEAR),
                            SimpleTechLevel.getGameTechLevel(game), techBase == AmmoType.TECH_BASE_CLAN, false)) {
                return false;
            }
            if (atype.hasFlag(AmmoType.F_NUCLEAR)
                    && !game.getOptions().booleanOption(
                            OptionsConstants.ADVAERORULES_AT2_NUKES)) {
                return false;
            }
            if ((atype.getMunitionType() & AmmoType.M_ARTEMIS_CAPABLE) != 0) {
                return entity.hasWorkingMisc(MiscType.F_ARTEMIS)
                        || entity.hasWorkingMisc(MiscType.F_ARTEMIS_PROTO);
            }
            if ((atype.getMunitionType() & AmmoType.M_ARTEMIS_V_CAPABLE) != 0) {
                return entity.hasWorkingMisc(MiscType.F_ARTEMIS_V);
            }
            return true;
        }
        
        private String createMunitionLabel(AmmoType atype) {
            if (atype.getAmmoType() == AmmoType.T_MML) {
                if ((atype.getMunitionType() & (AmmoType.M_ARTEMIS_CAPABLE | AmmoType.M_ARTEMIS_V_CAPABLE))
                        == 0) {
                    return Messages.getString(atype.hasFlag(AmmoType.F_MML_LRM)?
                            "CustomMechDialog.LRM" : "CustomMechDialog.SRM"); //$NON-NLS-1$  //$NON-NLS-2$
                } else {
                    return Messages.getString(atype.hasFlag(AmmoType.F_MML_LRM)?
                            "CustomMechDialog.LRMArtemis" : "CustomMechDialog.SRMArtemis"); //$NON-NLS-1$  //$NON-NLS-2$
                }
            }
            
            if (atype.hasFlag(AmmoType.F_CAP_MISSILE)) {
                String tele = atype.hasFlag(AmmoType.F_TELE_MISSILE)? "-T":"";
                if (atype.hasFlag(AmmoType.F_PEACEMAKER)) {
                    return Messages.getString("CustomMechDialog.Peacemaker") + tele; //$NON-NLS-1$
                } else if (atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
                    return Messages.getString("CustomMechDialog.SantaAnna") + tele; //$NON-NLS-1$
                } else if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                    return Messages.getString("CustomMechDialog.KillerWhale") + tele; //$NON-NLS-1$
                } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                    return Messages.getString("CustomMechDialog.WhiteShark") + tele; //$NON-NLS-1$
                } else if (atype.hasFlag(AmmoType.F_AR10_BARRACUDA)) {
                    return Messages.getString("CustomMechDialog.Barracuda") + tele; //$NON-NLS-1$
                }
            }
            
            if ((atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE)
                    || (atype.getMunitionType() == AmmoType.M_ARTEMIS_V_CAPABLE)) {
                return Messages.getString("CustomMechDialog.Artemis"); //$NON-NLS-1$
            }
            
            return Messages.getString("CustomMechDialog.StandardMunition"); //$NON-NLS-1$
        }
        
        private void recalcMaxValues() {
            double[] currentWeight = new double[spinners.size()];
            double remaining = tonnage;
            for (int i = 0; i < spinners.size(); i++) {
                currentWeight[i] += Math.ceil(munitions.get(i).getTonnage(entity)
                        * ((Integer) spinners.get(i).getValue() / (double) munitions.get(i).getShots()));
                remaining -= currentWeight[i];
            }
            for (int i = 0; i < spinners.size(); i++) {
                int max = (int) Math.floor((currentWeight[i] + remaining)
                        / munitions.get(i).getTonnage(entity) * munitions.get(i).getShots());
                spinners.get(i).removeChangeListener(this);
                ((SpinnerNumberModel) spinners.get(i).getModel()).setMaximum(max);
                spinners.get(i).addChangeListener(this);
            }
            lblTonnage.setText(String.format(Messages.getString("CustomMechDialog.formatAmmoTonnage"), //$NON-NLS-1$
                    tonnage - remaining, tonnage));
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            recalcMaxValues();
        }
    }

}
