/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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

import megamek.common.equipment.WeaponMounted;

import java.util.Comparator;

/**
 * Comparator for sorting Weapons (Mounteds that have WeaponTypes) by weapon
 * number.
 * 
 * @author arlith
 */
public class WeaponComparatorNum implements Comparator<WeaponMounted> {
    
    private final Entity entity;
    
    public WeaponComparatorNum(Entity e) {
        entity = e;
    }

    @Override
    public int compare(WeaponMounted obj1, WeaponMounted obj2) {
        int weapNum1 = entity.getEquipmentNum(obj1);
        int weapNum2 = entity.getEquipmentNum(obj2);
        return weapNum1 - weapNum2;
    }
}