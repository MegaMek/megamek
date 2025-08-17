/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.common.equipment.AmmoType.F_BATTLEARMOR;
import static megamek.common.equipment.AmmoType.F_PROTOMEK;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;
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
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.interfaces.ITechnology;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;

class AmmoRowPanel extends JPanel implements ChangeListener {
    @Serial
    private static final long serialVersionUID = 7251618728823971065L;

    private final BayMunitionsChoicePanel bayMunitionsChoicePanel;
    private final JLabel lblTonnage = new JLabel();

    final WeaponMounted bay;
    private final AmmoType.AmmoTypeEnum at;
    private final int rackSize;
    private final ITechnology.TechBase techBase;
    final List<AmmoMounted> ammoMounts;

    final List<JSpinner> spinners;
    final List<AmmoType> munitions;

    double tonnage;

    AmmoRowPanel(BayMunitionsChoicePanel bayMunitionsChoicePanel, WeaponMounted bay, AmmoType.AmmoTypeEnum at,
          int rackSize, List<AmmoMounted> ammoMounts) {
        this.bayMunitionsChoicePanel = bayMunitionsChoicePanel;
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
        this.techBase = weaponType.map(EquipmentType::getTechBase).orElse(WeaponType.TechBase.ALL);

        munitions = AmmoType.getMunitionsFor(at).stream()
              .filter(this::includeMunition)
              .toList();
        tonnage = ammoMounts.stream().mapToDouble(Mounted::getSize).sum();
        Map<String, Integer> starting = new HashMap<>();
        ammoMounts.forEach(m -> starting.merge(m.getType().getInternalName(), m.getBaseShotsLeft(), Integer::sum));
        for (AmmoType ammoType : munitions) {
            JSpinner spn = new JSpinner(new SpinnerNumberModel(starting.getOrDefault(ammoType.getInternalName(), 0),
                  0, null, 1));
            spn.setName(ammoType.getInternalName());
            spn.addChangeListener(this);
            if (ammoType.getTonnage(bayMunitionsChoicePanel.getEntity()) > 1) {
                spn.setToolTipText(String.format(Messages.getString("CustomMekDialog.formatMissileTonnage"),
                      ammoType.getName(), ammoType.getTonnage(bayMunitionsChoicePanel.getEntity())));
            } else {
                spn.setToolTipText(String.format(Messages.getString("CustomMekDialog.formatShotsPerTon"),
                      ammoType.getName(), ammoType.getShots()));
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
        add(new JLabel("(" + bayMunitionsChoicePanel.getEntity().getLocationAbbr(bay.getLocation()) + ") "
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
     *
     * @param ammoType the type of munition to be asserted
     *
     * @return true means the munition should be included.
     */
    private boolean includeMunition(AmmoType ammoType) {
        if (!ammoType
              .canAeroUse(bayMunitionsChoicePanel.getGame().getOptions()
                    .booleanOption(OptionsConstants.ADVAERORULES_AERO_ARTILLERY_MUNITIONS))
              || (ammoType.getAmmoType() != at)
              || (ammoType.getRackSize() != rackSize)
              || ((ammoType.getTechBase() != techBase)
              && (ammoType.getTechBase() != AmmoType.TechBase.ALL)
              && (techBase != AmmoType.TechBase.ALL))
              || !ammoType.isLegal(bayMunitionsChoicePanel.getGame()
                    .getOptions()
                    .intOption(OptionsConstants.ALLOWED_YEAR),
              SimpleTechLevel.getGameTechLevel(bayMunitionsChoicePanel.getGame()),
              techBase == AmmoType.TechBase.CLAN,
              techBase == AmmoType.TechBase.ALL,
              bayMunitionsChoicePanel.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_SHOW_EXTINCT))) {
            return false;
        }
        if (ammoType.hasFlag(AmmoType.F_NUCLEAR)
              && !bayMunitionsChoicePanel.getGame().getOptions().booleanOption(
              OptionsConstants.ADVAERORULES_AT2_NUKES)) {
            return false;
        }
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE)) {
            return bayMunitionsChoicePanel.getEntity().hasWorkingMisc(MiscType.F_ARTEMIS)
                  || bayMunitionsChoicePanel.getEntity().hasWorkingMisc(MiscType.F_ARTEMIS_PROTO);
        }
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_V_CAPABLE)) {
            return bayMunitionsChoicePanel.getEntity().hasWorkingMisc(MiscType.F_ARTEMIS_V);
        }
        // A Bay should not load BA nor Protomek exclusive ammo/weapons
        return !ammoType.hasFlag(F_BATTLEARMOR) && !ammoType.hasFlag(F_PROTOMEK);
    }

    private String createMunitionLabel(AmmoType ammoType) {
        if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.MML) {
            EnumSet<AmmoType.Munitions> artemisCapable = EnumSet.of(
                  AmmoType.Munitions.M_ARTEMIS_CAPABLE,
                  AmmoType.Munitions.M_ARTEMIS_V_CAPABLE);
            if (ammoType.getMunitionType().stream().noneMatch(artemisCapable::contains)) {
                return Messages.getString(ammoType.hasFlag(AmmoType.F_MML_LRM)
                      ? "CustomMekDialog.LRM"
                      : "CustomMekDialog.SRM");
            } else {
                return Messages.getString(ammoType.hasFlag(AmmoType.F_MML_LRM)
                      ? "CustomMekDialog.LRMArtemis"
                      : "CustomMekDialog.SRMArtemis");
            }
        }

        if (ammoType.hasFlag(AmmoType.F_CAP_MISSILE)) {
            String tele = ammoType.hasFlag(AmmoType.F_TELE_MISSILE) ? "-T" : "";
            if (ammoType.hasFlag(AmmoType.F_PEACEMAKER)) {
                return Messages.getString("CustomMekDialog.Peacemaker") + tele;
            } else if (ammoType.hasFlag(AmmoType.F_SANTA_ANNA)) {
                return Messages.getString("CustomMekDialog.SantaAnna") + tele;
            } else if (ammoType.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                return Messages.getString("CustomMekDialog.KillerWhale") + tele;
            } else if (ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                return Messages.getString("CustomMekDialog.WhiteShark") + tele;
            } else if (ammoType.hasFlag(AmmoType.F_AR10_BARRACUDA)) {
                return Messages.getString("CustomMekDialog.Barracuda") + tele;
            }
        }

        if ((ammoType.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE))
              || (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_V_CAPABLE))) {
            return Messages.getString("CustomMekDialog.Artemis");
        }

        // ATM munitions
        if ((ammoType.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE))
              || (ammoType.getMunitionType().contains(AmmoType.Munitions.M_EXTENDED_RANGE))) {
            return ammoType.getDesc();
        }

        if (bayMunitionsChoicePanel.getGame().getOptions()
              .booleanOption(OptionsConstants.ADVAERORULES_AERO_ARTILLERY_MUNITIONS)) {
            if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.ARROW_IV
                  || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.LONG_TOM
                  || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.SNIPER
                  || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.THUMPER
                  || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.CRUISE_MISSILE) {
                if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_STANDARD)) {
                    return Messages.getString("CustomMekDialog.StandardMunition");
                }
                return ammoType.getShortName();
            }
        }
        return Messages.getString("CustomMekDialog.StandardMunition");
    }

    private void recalcMaxValues() {
        double[] currentWeight = new double[spinners.size()];
        double remaining = tonnage;
        for (int i = 0; i < spinners.size(); i++) {
            currentWeight[i] += Math.ceil(munitions.get(i).getTonnage(bayMunitionsChoicePanel.getEntity())
                  * ((Integer) spinners.get(i).getValue() / (double) munitions.get(i).getShots()));
            remaining -= currentWeight[i];
        }
        for (int i = 0; i < spinners.size(); i++) {
            int max = (int) Math.floor((currentWeight[i] + remaining)
                  / munitions.get(i).getTonnage(bayMunitionsChoicePanel.getEntity()) * munitions.get(i).getShots());
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
