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
import java.awt.event.ItemListener;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.codeUtilities.MathUtility;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.ProtoMek;
import megamek.common.equipment.AmmoMounted;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;

public class MunitionChoicePanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 3401106035583965326L;

    private final List<AmmoType> ammoTypes;

    private final JComboBox<AmmoType> comboAmmoTypes;

    private final JComboBox<String> comboNumberOfShots;
    private final ItemListener numShotsListener;
    private final GameOptions gameOptions;
    private final AmmoMounted ammoMounted;
    boolean numShotsChanged = false;

    JLabel labDump = new JLabel(Messages.getString("CustomMekDialog.labDump"));

    JCheckBox chDump = new JCheckBox();

    JLabel labHotLoad = new JLabel(Messages.getString("CustomMekDialog.switchToHotLoading"));

    JCheckBox chHotLoad = new JCheckBox();

    public MunitionChoicePanel(AmmoMounted ammoMounted, ArrayList<AmmoType> vTypes,
          List<WeaponAmmoChoicePanel> weaponAmmoChoicePanels, Entity entity, ClientGUI clientGUI) {
        ammoTypes = vTypes;
        this.ammoMounted = ammoMounted;
        gameOptions = clientGUI.getClient().getGame().getOptions();

        AmmoType ammoType = ammoMounted.getType();
        comboAmmoTypes = new JComboBox<>();

        Iterator<AmmoType> e = ammoTypes.iterator();
        for (int x = 0; e.hasNext(); x++) {
            AmmoType at = e.next();
            comboAmmoTypes.addItem(at);
            if (at.equals(ammoType)) {
                comboAmmoTypes.setSelectedIndex(x);
            }
        }

        numShotsListener = evt -> numShotsChanged = true;
        comboNumberOfShots = new JComboBox<>();

        int shotsPerTon = ammoType.getShots();
        // BattleArmor always have a certain number of shots per slot
        int stepSize = 1;
        // ProtoMeks and BattleArmor are limited to the number of shots allocated in construction
        if ((entity instanceof BattleArmor) || (entity instanceof ProtoMek)) {
            shotsPerTon = ammoMounted.getOriginalShots();
            // BA tube artillery always comes in pairs
            if (ammoType.getAmmoType() == AmmoType.T_BA_TUBE) {
                stepSize = 2;
            }
        }

        for (int i = 0; i <= shotsPerTon; i += stepSize) {
            comboNumberOfShots.addItem(String.valueOf(i));
        }

        comboNumberOfShots.setSelectedItem(this.ammoMounted.getBaseShotsLeft());
        comboNumberOfShots.addItemListener(numShotsListener);

        comboAmmoTypes.addItemListener(evt -> {
            comboNumberOfShots.removeItemListener(numShotsListener);

            int currShots = 0;

            if (comboNumberOfShots.getSelectedItem() instanceof String value) {
                currShots = MathUtility.parseInt(value, currShots);
            }

            comboNumberOfShots.removeAllItems();
            int numberOfShotsPerTon = ammoTypes.get(comboAmmoTypes.getSelectedIndex()).getShots();

            // ProtoMeks are limited to number of shots added during construction
            if ((entity instanceof BattleArmor) || (entity instanceof ProtoMek)) {
                numberOfShotsPerTon = ammoMounted.getOriginalShots();
            }

            for (int i = 0; i <= numberOfShotsPerTon; i++) {
                comboNumberOfShots.addItem(String.valueOf(i));
            }

            // If the shots selection was changed, try to set that value, unless it's too large
            if (numShotsChanged && currShots <= numberOfShotsPerTon) {
                comboNumberOfShots.setSelectedItem(currShots);
            } else {
                comboNumberOfShots.setSelectedItem(numberOfShotsPerTon);
            }

            for (WeaponAmmoChoicePanel weaponAmmoChoicePanel : weaponAmmoChoicePanels) {
                weaponAmmoChoicePanel.refreshAmmoBinName(this.ammoMounted,
                      ammoTypes.get(comboAmmoTypes.getSelectedIndex()));
            }

            comboNumberOfShots.addItemListener(numShotsListener);
        });

        int ammoMountedLocation = ammoMounted.getLocation();
        boolean isOneShot = false;

        if (ammoMountedLocation == Entity.LOC_NONE) {
            // one shot weapons don't have a location of their own some weapons (e.gridBagLayout. fusillade) use the one-shot
            // mechanic but have an extra reload which is chained to the first
            Mounted<?> linkedBy = ammoMounted.getLinkedBy();

            while (linkedBy.getLinkedBy() != null) {
                linkedBy = linkedBy.getLinkedBy();
            }

            ammoMountedLocation = linkedBy.getLocation();
            isOneShot = linkedBy.isOneShot();
        } else {
            ammoMountedLocation = ammoMounted.getLocation();
        }

        comboNumberOfShots.setVisible(!isOneShot);
        String stringDescription = '(' + entity.getLocationAbbr(ammoMountedLocation) + ')';
        JLabel labelLocation = new JLabel(stringDescription);
        GridBagLayout gridBagLayout = new GridBagLayout();
        setLayout(gridBagLayout);
        add(labelLocation, GBC.std());
        add(comboAmmoTypes, GBC.std());
        add(comboNumberOfShots, GBC.eol());
        chHotLoad.setSelected(this.ammoMounted.isHotLoaded());

        if (gameOptions.booleanOption(OptionsConstants.BASE_LOBBY_AMMO_DUMP)) {
            add(labDump, GBC.std());
            add(chDump, GBC.eol());
        }

        if (gameOptions.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD) &&
                  ammoType.hasFlag(AmmoType.F_HOTLOAD)) {
            add(labHotLoad, GBC.std());
            add(chHotLoad, GBC.eol());
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
        if (ammoMounted.getLocation() != Entity.LOC_NONE &&
                  comboNumberOfShots.getSelectedItem() instanceof String value) {
            ammoMounted.setShotsLeft(MathUtility.parseInt(value, 0));
        }

        if (chDump.isSelected()) {
            ammoMounted.setShotsLeft(0);
        }

        if (gameOptions.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD)) {
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

    @Override
    public void setEnabled(boolean enabled) {
        comboAmmoTypes.setEnabled(enabled);
    }

    /**
     * Get the number of shots in the mount.
     *
     * @return the <code>int</code> number of shots in the mount.
     *
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    int getShotsLeft() {
        return ammoMounted.getBaseShotsLeft();
    }

    /**
     * Set the number of shots in the mount.
     *
     * @param shots the <code>int</code> number of shots for the mount.
     *
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    void setShotsLeft(int shots) {
        ammoMounted.setShotsLeft(shots);
    }
}
