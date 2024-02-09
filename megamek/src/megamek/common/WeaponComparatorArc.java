/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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
package megamek.common;

import megamek.common.equipment.WeaponMounted;

import java.util.Comparator;

/**
 * Comparator for sorting Weapons (Mounteds that have WeaponTypes) by arcs with
 * ties arbitrated based on damage (high to low).
 * 
 * @author arlith
 */
public class WeaponComparatorArc implements Comparator<WeaponMounted> {
    private final Entity entity;

    public WeaponComparatorArc(Entity e) {
        entity = e;
    }

    @Override
    public int compare(WeaponMounted obj1, WeaponMounted obj2) {
        int wnum1 = entity.getEquipmentNum(obj1);
        int wnum2 = entity.getEquipmentNum(obj2);
        WeaponType weap1 = (WeaponType) obj1.getType();
        WeaponType weap2 = (WeaponType) obj2.getType();

        // Pick the weapon with the lowest arc
        if (entity.getWeaponArc(wnum1) > entity.getWeaponArc(wnum2)) {
            return 1;
        } else if (entity.getWeaponArc(wnum1) < entity.getWeaponArc(wnum2)) {
            return -1;
        } else {
            // Break ties with damage
            // If types are equal, pick front facing first
            if (weap1 == weap2) {
                if (obj1.isRearMounted()) {
                    return -1;
                } else if (obj2.isRearMounted()) {
                    return 1;
                } else {
                    return 0;
                }
            }
            // Pick the weapon with the highest damage
            if (weap1.getDamage() > weap2.getDamage()) {
                return 1;
            } else if (weap1.getDamage() < weap2.getDamage()) {
                return -1;
            } else { // Break ties with heat
                return Integer.compare(weap1.getHeat(), weap2.getHeat());
            }
        }
    }
}
