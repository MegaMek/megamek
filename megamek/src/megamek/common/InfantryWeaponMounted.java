/*
 * MegaMek - Copyright (C) 2023 - The MegaMek Team
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

package megamek.common;

import megamek.common.options.GameOptions;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.infantry.InfantryWeapon;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Specialized mount used for infantry units with primary and secondary weapons.
 * This handles special features of both weapons. Infantry units that only have
 * a single weapon should use {@link Mounted}.
 */
public class InfantryWeaponMounted extends Mounted {

    transient private InfantryWeapon otherWeapon;
    private final String typeName;

    transient private List<EquipmentMode> modes;

    /**
     * Creates a Mounted that deals with combined feature of a primary and a secondary weapon.
     * Infantry units with a single type of weapon should use {@link Mounted}
     *
     * @param entity      The infantry unit
     * @param rangeWeapon The weapon used to calculate range.
     * @param otherWeapon The other weapon
     */
    public InfantryWeaponMounted(Entity entity, EquipmentType rangeWeapon, InfantryWeapon otherWeapon) {
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
    public void adaptToGameOptions(GameOptions options) {
        ((Weapon) getOtherWeapon()).adaptToGameOptions(options);
        super.adaptToGameOptions(options);
        rebuildModeList();
    }
}
