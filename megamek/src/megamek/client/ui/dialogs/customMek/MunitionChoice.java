/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import java.awt.Component;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Vector;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import megamek.client.ui.GBC2;
import megamek.client.ui.Messages;
import megamek.codeUtilities.MathUtility;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.ProtoMek;

public class MunitionChoice {

    private final List<AmmoType> ammoTypes;
    private final JComboBox<AmmoType> comboAmmoTypes;
    private final JComboBox<String> comboNumberOfShots = new JComboBox<>();
    private final ItemListener numShotsListener;
    private final GameOptions gameOptions;
    private final AmmoMounted ammoMounted;
    private final JCheckBox chDump = new JCheckBox(Messages.getString("CustomMekDialog.labDump"));
    private final JCheckBox chHotLoad = new JCheckBox(Messages.getString("CustomMekDialog.switchToHotLoading"));

    private boolean numShotsChanged = false;

    public MunitionChoice(AmmoMounted ammoMounted, Vector<AmmoType> ammoTypes,
                          List<WeaponAmmoChoice> weaponAmmoChoices, Entity entity, Game game, JPanel parentPanel, GBC2 gbc) {

        this.ammoTypes = ammoTypes;
        this.ammoMounted = ammoMounted;
        gameOptions = game.getOptions();
        AmmoType ammoType = ammoMounted.getType();
        comboAmmoTypes = new JComboBox<>(ammoTypes);
        comboAmmoTypes.setRenderer(new AmmoComboRenderer());
        comboAmmoTypes.setSelectedItem(ammoType);

        numShotsListener = evt -> numShotsChanged = true;

        int shotsPerTon = ammoType.getShots();
        // BattleArmor always have a certain number of shots per slot
        int stepSize = 1;
        // ProtoMeks and BattleArmor are limited to the number of shots allocated in construction
        if ((entity instanceof BattleArmor) || (entity instanceof ProtoMek)) {
            shotsPerTon = ammoMounted.getOriginalShots();
            // BA tube artillery always comes in pairs
            if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.BA_TUBE) {
                stepSize = 2;
            }
        }

        for (int i = 0; i <= shotsPerTon; i += stepSize) {
            comboNumberOfShots.addItem(String.valueOf(i));
        }

        comboNumberOfShots.setSelectedItem(String.valueOf(ammoMounted.getBaseShotsLeft()));
        comboNumberOfShots.addItemListener(numShotsListener);

        comboAmmoTypes.addItemListener(evt -> {
            comboNumberOfShots.removeItemListener(numShotsListener);

            int currShots = 0;

            if (comboNumberOfShots.getSelectedItem() instanceof String value) {
                currShots = MathUtility.parseInt(value, currShots);
            }

            comboNumberOfShots.removeAllItems();
            int numberOfShotsPerTon = this.ammoTypes.get(comboAmmoTypes.getSelectedIndex()).getShots();

            // ProtoMeks are limited to number of shots added during construction
            if ((entity instanceof BattleArmor) || (entity instanceof ProtoMek)) {
                numberOfShotsPerTon = ammoMounted.getOriginalShots();
            }

            for (int i = 0; i <= numberOfShotsPerTon; i++) {
                comboNumberOfShots.addItem(String.valueOf(i));
            }

            // If the shots selection was changed, try to set that value, unless it's too large
            if (numShotsChanged && currShots <= numberOfShotsPerTon) {
                comboNumberOfShots.setSelectedItem(String.valueOf(currShots));
            } else {
                comboNumberOfShots.setSelectedItem(String.valueOf(numberOfShotsPerTon));
            }

            for (WeaponAmmoChoice weaponAmmoChoice : weaponAmmoChoices) {
                weaponAmmoChoice.refreshAmmoBinName(this.ammoMounted,
                      this.ammoTypes.get(comboAmmoTypes.getSelectedIndex()));
            }

            comboNumberOfShots.addItemListener(numShotsListener);
        });

        int ammoMountedLocation = ammoMounted.getLocation();
        boolean isOneShot = false;

        if (ammoMountedLocation == Entity.LOC_NONE) {
            // one shot weapons don't have a location of their own some weapons (e.g. fusillade) use the one-shot
            // mechanic but have an extra reload which is chained to the first
            Mounted<?> linkedBy = ammoMounted.getLinkedBy();

            while (linkedBy.getLinkedBy() != null) {
                linkedBy = linkedBy.getLinkedBy();
            }

            ammoMountedLocation = linkedBy.getLocation();
            isOneShot = linkedBy.isOneShot();
        }

        String stringDescription = entity.getLocationName(ammoMountedLocation) + ":";
        JLabel labelLocation = new JLabel(stringDescription);
        boolean gameUsesHotLoad = gameOptions.booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HOT_LOAD);
        boolean ammoAllowsHotLoad = ammoType.hasFlag(AmmoType.F_HOTLOAD);
        chHotLoad.setSelected(ammoMounted.isHotLoaded());
        comboAmmoTypes.setEnabled(ammoTypes.size() > 1);

        parentPanel.add(labelLocation, gbc.forLabel());
        parentPanel.add(comboAmmoTypes, gbc.oneColumn());

        if (!isOneShot) {
            parentPanel.add(comboNumberOfShots, gameUsesHotLoad ? gbc.oneColumn() : gbc.eol());
        }

        if (ammoAllowsHotLoad) {
            parentPanel.add(chHotLoad, gbc.eol());
        } else {
            parentPanel.add(new JLabel(), gbc.eol()); // make sure to end the line
        }
    }

    public void applyChoice() {
        int selectedIndex = comboAmmoTypes.getSelectedIndex();

        // If there's no selection, there's nothing we can do
        if (selectedIndex == -1) {
            return;
        }

        AmmoType ammoType = ammoTypes.get(selectedIndex);
        ammoMounted.changeAmmoType(ammoType);

        // set # shots only for non-one shot weapons
        if (ammoMounted.getLocation() != Entity.LOC_NONE
              && comboNumberOfShots.getSelectedItem() instanceof String value) {
            ammoMounted.setShotsLeft(MathUtility.parseInt(value, 0));
        }

        if (chDump.isSelected()) {
            ammoMounted.setShotsLeft(0);
        }

        if (gameOptions.booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HOT_LOAD)) {
            if (chHotLoad.isSelected() != ammoMounted.isHotLoaded()) {
                ammoMounted.setHotLoad(chHotLoad.isSelected());
                // Set the mode too, so vehicles can switch back
                for (int mode = 0; mode < ammoMounted.getModesCount(); mode++) {
                    if (ammoMounted.getType().getMode(mode).getName().equals("HotLoad")) {
                        ammoMounted.setMode(mode);
                    }
                }
            }
        }
    }

    public void setEnabled(boolean enabled) {
        comboAmmoTypes.setEnabled(enabled);
    }

    private static class AmmoComboRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {

            if (value instanceof AmmoType ammoType) {
                value = ammoType.getName().replace(" Ammo", "");

            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }
}
