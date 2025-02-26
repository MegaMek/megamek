/*
 * MegaMek - Copyright (C) 2024 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.EnumSet;
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

import megamek.client.ui.Messages;
import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Game;
import megamek.common.LocationFullException;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.WeaponType;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;

import static megamek.common.AmmoType.F_BATTLEARMOR;
import static megamek.common.AmmoType.F_PROTOMEK;

/**
 * @author Neoancient
 */
public class BayMunitionsChoicePanel extends JPanel {
    private final static MMLogger logger = MMLogger.create(BayMunitionsChoicePanel.class);

    private static final long serialVersionUID = -7741380967676720496L;

    private final Entity entity;
    private final Game game;
    private final List<AmmoRowPanel> rows = new ArrayList<>();

    public BayMunitionsChoicePanel(Entity entity, Game game) {
        this.entity = entity;
        this.game = game;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 0, 10, 0);

        for (WeaponMounted bay : entity.getWeaponBayList()) {
            Map<List<Integer>, List<AmmoMounted>> ammoByType = new HashMap<>();
            for (AmmoMounted ammo : bay.getBayAmmo()) {
                List<Integer> key = new ArrayList<>(2);
                key.add(ammo.getType().getAmmoType());
                key.add(ammo.getType().getRackSize());
                ammoByType.putIfAbsent(key, new ArrayList<>());
                ammoByType.get(key).add(ammo);
            }
            for (List<Integer> key : ammoByType.keySet()) {
                AmmoRowPanel row = new AmmoRowPanel(bay, key.get(0), key.get(1), ammoByType.get(key));
                gbc.gridy++;
                add(row, gbc);
                rows.add(row);
            }
        }
    }

    /**
     * Change the munition types of the bay ammo mounts to the selected values. If
     * there are more
     * munition types than there were originally, additional ammo bin mounts will be
     * added. If fewer,
     * the unneeded ones will have their shot count zeroed.
     */
    public void apply() {
        for (AmmoRowPanel row : rows) {
            int mountIndex = 0;
            double remainingWeight = row.tonnage;
            for (int i = 0; i < row.munitions.size(); i++) {
                int shots = (Integer) row.spinners.get(i).getValue();
                if (shots > 0) {
                    AmmoMounted mounted;
                    if (mountIndex >= row.ammoMounts.size()) {
                        mounted = (AmmoMounted) Mounted.createMounted(entity, row.munitions.get(i));
                        try {
                            entity.addEquipment(mounted, row.bay.getLocation(), row.bay.isRearMounted());
                            row.bay.addAmmoToBay(entity.getEquipmentNum(mounted));
                        } catch (LocationFullException e) {
                            logger.error(e, "apply");
                        }

                    } else {
                        mounted = row.ammoMounts.get(mountIndex);
                        mounted.changeAmmoType(row.munitions.get(i));
                    }
                    mounted.setShotsLeft(shots);
                    int slots = (int) Math.ceil((double) shots / row.munitions.get(i).getShots());
                    mounted.setOriginalShots(slots * row.munitions.get(i).getShots());
                    mounted.setSize(slots * row.munitions.get(i).getTonnage(entity));
                    remainingWeight -= mounted.getSize();
                    mountIndex++;
                }
            }
            // Zero out any remaining unused bins.
            while (mountIndex < row.ammoMounts.size()) {
                AmmoMounted mount = row.ammoMounts.get(mountIndex);
                mount.setSize(0);
                mount.setOriginalShots(0);
                mount.setShotsLeft(0);
                mountIndex++;
            }
            // If the unit is assigned less ammo than the capacity, assign remaining weight
            // to first mount
            // and adjust original shots.
            if (remainingWeight > 0) {
                AmmoMounted m = row.ammoMounts.get(0);
                m.setSize(m.getSize() + remainingWeight);
                m.setOriginalShots((int) Math.floor(m.getSize() / (m.getType().getShots() * m.getTonnage())));
            }
        }
    }

    class AmmoRowPanel extends JPanel implements ChangeListener {
        private static final long serialVersionUID = 7251618728823971065L;

        private final JLabel lblTonnage = new JLabel();

        private final WeaponMounted bay;
        private final int at;
        private final int rackSize;
        private final int techBase;
        private final List<AmmoMounted> ammoMounts;

        private final List<JSpinner> spinners;
        private final List<AmmoType> munitions;

        private double tonnage;

        AmmoRowPanel(WeaponMounted bay, int at, int rackSize, List<AmmoMounted> ammoMounts) {
            this.bay = bay;
            this.at = at;
            this.rackSize = rackSize;
            this.ammoMounts = new ArrayList<>(ammoMounts);
            this.spinners = new ArrayList<>();

            final Optional<WeaponType> weaponType = bay.getBayWeapons().stream()
                    .map(Mounted::getType).findAny();

            // set the bay's tech base to that of any weapon in the bay
            // an assumption is made here that bays don't mix clan-only and IS-only tech
            // base
            this.techBase = weaponType.map(EquipmentType::getTechBase).orElse(WeaponType.TECH_BASE_ALL);

            munitions = AmmoType.getMunitionsFor(at).stream()
                .filter(this::includeMunition)
                .toList();
            tonnage = ammoMounts.stream().mapToDouble(Mounted::getSize).sum();
            Map<String, Integer> starting = new HashMap<>();
            ammoMounts.forEach(m -> starting.merge(m.getType().getInternalName(), m.getBaseShotsLeft(), Integer::sum));
            for (AmmoType atype : munitions) {
                JSpinner spn = new JSpinner(new SpinnerNumberModel(starting.getOrDefault(atype.getInternalName(), 0),
                        0, null, 1));
                spn.setName(atype.getInternalName());
                spn.addChangeListener(this);
                if (atype.getTonnage(entity) > 1) {
                    spn.setToolTipText(String.format(Messages.getString("CustomMekDialog.formatMissileTonnage"),
                            atype.getName(), atype.getTonnage(entity)));
                } else {
                    spn.setToolTipText(String.format(Messages.getString("CustomMekDialog.formatShotsPerTon"),
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
            add(new JLabel("(" + entity.getLocationAbbr(bay.getLocation()) + ") "
                    + (weaponType.isPresent() ? weaponType.get().getName() : "?")), gbc);
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

        /**
         * Assert if a specific ammo type should be included in the list of munitions for the ship.
         * @param ammoType  the type of munition to be asserted
         * @return true means the munition should be included.
         */
        private boolean includeMunition(AmmoType ammoType) {
            if (!ammoType
                    .canAeroUse(game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_ARTILLERY_MUNITIONS))
                    || (ammoType.getAmmoType() != at)
                    || (ammoType.getRackSize() != rackSize)
                    || ((ammoType.getTechBase() != techBase)
                            && (ammoType.getTechBase() != AmmoType.TECH_BASE_ALL)
                            && (techBase != AmmoType.TECH_BASE_ALL))
                    || !ammoType.isLegal(game.getOptions().intOption(OptionsConstants.ALLOWED_YEAR),
                            SimpleTechLevel.getGameTechLevel(game),
                            techBase == AmmoType.TECH_BASE_CLAN,
                            techBase == AmmoType.TECH_BASE_ALL,
                            game.getOptions().booleanOption(OptionsConstants.ALLOWED_SHOW_EXTINCT))) {
                return false;
            }
            if (ammoType.hasFlag(AmmoType.F_NUCLEAR)
                    && !game.getOptions().booleanOption(
                            OptionsConstants.ADVAERORULES_AT2_NUKES)) {
                return false;
            }
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE)) {
                return entity.hasWorkingMisc(MiscType.F_ARTEMIS)
                        || entity.hasWorkingMisc(MiscType.F_ARTEMIS_PROTO);
            }
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_V_CAPABLE)) {
                return entity.hasWorkingMisc(MiscType.F_ARTEMIS_V);
            }
            // A Bay should not load BA nor Protomek exclusive ammo/weapons
            if (ammoType.hasFlag(F_BATTLEARMOR) || ammoType.hasFlag(F_PROTOMEK)) {
                return false;
            }
            return true;
        }

        private String createMunitionLabel(AmmoType atype) {
            if (atype.getAmmoType() == AmmoType.T_MML) {
                EnumSet<AmmoType.Munitions> artemisCapable = EnumSet.of(
                        AmmoType.Munitions.M_ARTEMIS_CAPABLE,
                        AmmoType.Munitions.M_ARTEMIS_V_CAPABLE);
                if (atype.getMunitionType().stream().noneMatch(artemisCapable::contains)) {
                    return Messages.getString(atype.hasFlag(AmmoType.F_MML_LRM)
                            ? "CustomMekDialog.LRM"
                            : "CustomMekDialog.SRM");
                } else {
                    return Messages.getString(atype.hasFlag(AmmoType.F_MML_LRM)
                            ? "CustomMekDialog.LRMArtemis"
                            : "CustomMekDialog.SRMArtemis");
                }
            }

            if (atype.hasFlag(AmmoType.F_CAP_MISSILE)) {
                String tele = atype.hasFlag(AmmoType.F_TELE_MISSILE) ? "-T" : "";
                if (atype.hasFlag(AmmoType.F_PEACEMAKER)) {
                    return Messages.getString("CustomMekDialog.Peacemaker") + tele;
                } else if (atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
                    return Messages.getString("CustomMekDialog.SantaAnna") + tele;
                } else if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                    return Messages.getString("CustomMekDialog.KillerWhale") + tele;
                } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                    return Messages.getString("CustomMekDialog.WhiteShark") + tele;
                } else if (atype.hasFlag(AmmoType.F_AR10_BARRACUDA)) {
                    return Messages.getString("CustomMekDialog.Barracuda") + tele;
                }
            }

            if ((atype.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE))
                    || (atype.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_V_CAPABLE))) {
                return Messages.getString("CustomMekDialog.Artemis");
            }

            // ATM munitions
            if ((atype.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE))
                    || (atype.getMunitionType().contains(AmmoType.Munitions.M_EXTENDED_RANGE))) {
                return atype.getDesc();
            }

            if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_ARTILLERY_MUNITIONS)) {
                if (atype.getAmmoType() == AmmoType.T_ARROW_IV
                        || atype.getAmmoType() == AmmoType.T_LONG_TOM
                        || atype.getAmmoType() == AmmoType.T_SNIPER
                        || atype.getAmmoType() == AmmoType.T_THUMPER
                        || atype.getAmmoType() == AmmoType.T_CRUISE_MISSILE) {
                    if (atype.getMunitionType().contains(AmmoType.Munitions.M_STANDARD)) {
                        return Messages.getString("CustomMekDialog.StandardMunition");
                    }
                    return atype.getShortName();
                }
            }
            return Messages.getString("CustomMekDialog.StandardMunition");
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
            lblTonnage.setText(String.format(Messages.getString("CustomMekDialog.formatAmmoTonnage"),
                    tonnage - remaining, tonnage));
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            recalcMaxValues();
        }
    }

}
