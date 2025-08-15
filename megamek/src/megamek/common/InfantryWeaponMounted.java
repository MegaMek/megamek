/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import megamek.common.equipment.WeaponMounted;
import megamek.common.options.IGameOptions;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * Specialized mount used for infantry units with primary and secondary weapons. This handles special features of both
 * weapons. Infantry units that only have a single weapon should use {@link Mounted}.
 */
public class InfantryWeaponMounted extends WeaponMounted {

    transient private InfantryWeapon otherWeapon;
    private final String typeName;

    transient private List<EquipmentMode> modes;

    /**
     * Creates a Mounted that deals with combined feature of a primary and a secondary weapon. Infantry units with a
     * single type of weapon should use {@link Mounted}
     *
     * @param entity      The infantry unit
     * @param rangeWeapon The weapon used to calculate range.
     * @param otherWeapon The other weapon
     */
    public InfantryWeaponMounted(Entity entity, InfantryWeapon rangeWeapon, InfantryWeapon otherWeapon) {
        super(entity, rangeWeapon);
        this.typeName = otherWeapon.getInternalName();
        this.otherWeapon = otherWeapon;
        rebuildModeList();
    }

    public InfantryWeapon getOtherWeapon() {
        if (otherWeapon == null) {
            otherWeapon = (InfantryWeapon) EquipmentType.get(typeName);
        }
        return otherWeapon;
    }

    public void rebuildModeList() {
        modes = new ArrayList<>();
        for (Enumeration<EquipmentMode> e = getType().getModes(); e.hasMoreElements(); ) {
            modes.add(e.nextElement());
        }
        for (Enumeration<EquipmentMode> e = getOtherWeapon().getModes(); e.hasMoreElements(); ) {
            EquipmentMode mode = e.nextElement();
            if (!modes.contains(mode)) {
                modes.add(mode);
            }
        }
    }

    private List<EquipmentMode> getModes() {
        if (modes == null) {
            rebuildModeList();
        }
        return modes;
    }

    @Override
    public int getModesCount() {
        return getModes().size();
    }

    @Override
    protected EquipmentMode getMode(int mode) {
        return getModes().get(mode);
    }

    @Override
    public boolean hasModes() {
        return !getModes().isEmpty();
    }

    @Override
    public boolean hasModeType(String mode) {
        return modes.contains(EquipmentMode.getMode(mode));
    }

    @Override
    public boolean canInstantSwitch(int newMode) {
        if (getType().hasModes() && (getType().getMode(newMode) != null)) {
            return super.canInstantSwitch(newMode);
        } else {
            String newModeName = getOtherWeapon().getMode(newMode).getName();
            String curModeName = curMode().getName();
            return getType().hasInstantModeSwitch()
                  && !getOtherWeapon().isNextTurnModeSwitch(newModeName)
                  && !getOtherWeapon().isNextTurnModeSwitch(curModeName);
        }
    }

    @Override
    public void adaptToGameOptions(IGameOptions options) {
        ((Weapon) getOtherWeapon()).adaptToGameOptions(options);
        super.adaptToGameOptions(options);
        rebuildModeList();
    }
}
