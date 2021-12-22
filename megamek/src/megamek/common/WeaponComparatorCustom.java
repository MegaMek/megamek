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

import java.util.Comparator;

/**
 * Comparator for sorting Weapons (Mounteds that have WeaponTypes) by a custom
 * ordering.  The ordering is stored in the Entity instance, using a map that
 * maps the weapon's ID to it's custom order index.
 * 
 * @author arlith
 */
public class WeaponComparatorCustom implements Comparator<Mounted> {
    
    private Entity entity;
    
    public WeaponComparatorCustom(Entity e) {
        entity = e;
    }

    @Override
    public int compare(Mounted obj1, Mounted obj2) {
        if (obj1.getType() instanceof WeaponType
                && obj2.getType() instanceof WeaponType) {
            int weapNum1 = entity.getCustomWeaponOrder(obj1);
            int weapNum2 = entity.getCustomWeaponOrder(obj2);
            return weapNum1 - weapNum2;
        }
        throw new ClassCastException("Passed Mounteds are not Weapons");
    }
}