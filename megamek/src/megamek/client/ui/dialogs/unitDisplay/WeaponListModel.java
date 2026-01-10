/*
 * Copyright (C) 2015-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.unitDisplay;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import javax.swing.AbstractListModel;

import megamek.client.ui.Messages;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;

/**
 * ListModel implementation that supports keeping track of a list of Mounted instantiations, how to display them in the
 * JList, and an ability to sort the Mounted given WeaponComparators.
 *
 * @author arlith
 */
class WeaponListModel extends AbstractListModel<String> {
    @Serial
    private static final long serialVersionUID = 6312003196674512339L;

    private final WeaponPanel weaponPanel;
    /**
     * A collection of Mounted instantiations.
     */
    private final List<WeaponMounted> weapons;

    /**
     * The Entity that owns the collection of Mounted.
     */
    private final Entity entity;

    WeaponListModel(WeaponPanel weaponPanel, Entity entity) {
        this.weaponPanel = weaponPanel;
        this.entity = entity;
        weapons = new ArrayList<>();
    }

    /**
     * Add a new weapon to the list.
     *
     */
    public void addWeapon(WeaponMounted weaponMounted) {
        weapons.add(weaponMounted);
        fireIntervalAdded(this, weapons.size() - 1, weapons.size() - 1);
    }

    /**
     * Given the equipment (weapon) id, return the index in the (possibly sorted) list of Mounted.
     *
     */
    public int getIndex(int weaponId) {
        Mounted<?> mount = entity.getEquipment(weaponId);
        for (int i = 0; i < weapons.size(); i++) {
            if (weapons.get(i).equals(mount)) {
                return i;
            }
        }
        return -1;
    }

    public int getIndex(WeaponMounted weapon) {
        for (int i = 0; i < weapons.size(); i++) {
            if (weapons.get(i).equals(weapon)) {
                return i;
            }
        }
        return -1;
    }

    public void removeAllElements() {
        int numWeapons = weapons.size() - 1;
        weapons.clear();
        fireIntervalRemoved(this, 0, numWeapons);
    }

    /**
     * Swap the Mounted at the two specified index values.
     *
     */
    public void swapIdx(int idx1, int idx2) {
        // Bounds checking
        if ((idx1 >= weapons.size()) || (idx2 >= weapons.size())
              || (idx1 < 0) || (idx2 < 0)) {
            return;
        }
        WeaponMounted m1 = weapons.get(idx1);
        weapons.set(idx1, weapons.get(idx2));
        weapons.set(idx2, m1);
        fireContentsChanged(this, idx1, idx1);
        fireContentsChanged(this, idx2, idx2);
    }

    public WeaponMounted getWeaponAt(int index) {
        if (index < 0 || index >= weapons.size()) {
            return null;
        }
        return weapons.get(index);
    }

    /**
     * Given an index into the (possibly sorted) list of Mounted, return a text description. This consists of the
     * Mounted's description, as well as additional information like location, whether the Mounted is
     * shot/jammed/destroyed, etc. This is what the JList will display.
     */
    @Override
    public String getElementAt(int index) {
        final WeaponMounted mounted = weapons.get(index);
        final WeaponType weaponType = mounted.getType();
        Game game = null;
        if (weaponPanel.unitDisplayPanel.getClientGUI() != null) {
            game = weaponPanel.unitDisplayPanel.getClientGUI().getClient().getGame();
        }

        Entity entityMounted = mounted.getEntity();
        StringBuilder wn = new StringBuilder(mounted.getDesc());
        if ((mounted.getLinkedBy() != null)
              && (mounted.getLinkedBy().getType() instanceof MiscType)
              && (mounted.getLinkedBy().getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE))) {
            wn.append("+").append(mounted.getLinkedBy().getShortName());
        }
        wn.append(" [");
        wn.append(entityMounted.getLocationAbbr(mounted.getLocation()));
        //Check if mixedTech and add Clan or IS tag
        if (entityMounted.isMixedTech()) {
            wn.insert(0, weaponType.isClan() ? "(C) " : "(IS) ");
        }
        if (mounted.isSplit()) {
            wn.append('/');
            wn.append(entityMounted.getLocationAbbr(mounted.getSecondLocation()));
        }
        wn.append(']');
        // determine shots left & total shots left
        if ((weaponType.getAmmoType() != AmmoType.AmmoTypeEnum.NA)
              && (!weaponType.hasFlag(WeaponType.F_ONE_SHOT)
              || weaponType.hasFlag(WeaponType.F_BA_INDIVIDUAL))
              && (weaponType.getAmmoType() != AmmoType.AmmoTypeEnum.INFANTRY)) {
            int shotsLeft = 0;
            if ((mounted.getLinked() != null)
                  && !mounted.getLinked().isDumping()) {
                shotsLeft = mounted.getLinked().getUsableShotsLeft();
            }

            int totalShotsLeft = entityMounted.getTotalMunitionsOfType(mounted);

            wn.append(" (");
            wn.append(shotsLeft);
            wn.append('/');
            wn.append(totalShotsLeft);
            wn.append(')');
        } else if (weaponType.hasFlag(WeaponType.F_DOUBLE_ONE_SHOT)
              || (entityMounted.isSupportVehicle() && (weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.INFANTRY))) {
            int shotsLeft = 0;
            int totalShots = 0;
            EnumSet<AmmoType.Munitions> munition = ((AmmoType) mounted.getLinked().getType()).getMunitionType();
            for (Mounted<?> current = mounted.getLinked(); current != null; current = current.getLinked()) {
                if (((AmmoType) current.getType()).getMunitionType().equals(munition)) {
                    shotsLeft += current.getUsableShotsLeft();
                    totalShots += current.getOriginalShots();
                }
            }
            wn.append(" (").append(shotsLeft)
                  .append("/").append(totalShots).append(")");
        }

        // MG rapid fire
        if (mounted.isRapidFire()) {
            wn.append(Messages.getString("MekDisplay.rapidFire"));
        }

        // Hot loaded Missile Launchers
        if (mounted.isHotLoaded()) {
            wn.append(Messages.getString("MekDisplay.isHotLoaded"));
        }

        // Fire Mode - lots of things have variable modes
        if (mounted.hasModes()) {
            wn.append(' ');

            wn.append(mounted.curMode().getDisplayableName());
            if (!mounted.pendingMode().equals("None")) {
                wn.append(" (next turn, ");
                wn.append(mounted.pendingMode().getDisplayableName());
                wn.append(')');
            }
        }
        if ((game != null)
              && game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CALLED_SHOTS)) {
            wn.append(' ');
            wn.append(mounted.getCalledShot().getDisplayableName());
        }
        return wn.toString();
    }

    /**
     * Returns the number of Mounted in the list.
     */
    @Override
    public int getSize() {
        return weapons.size();
    }

    /**
     * Sort the Mounted, generally using a WeaponComparator.
     *
     */
    public void sort(Comparator<WeaponMounted> comparator) {
        weapons.sort(comparator);
        fireContentsChanged(this, 0, weapons.size() - 1);
    }
}
