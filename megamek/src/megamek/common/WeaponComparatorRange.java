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
 * Comparator for sorting Weapons (Mounteds that have WeaponTypes) by Range.
 * 
 * @author arlith
 */
public class WeaponComparatorRange implements Comparator<WeaponMounted> {

    /**
     * Value used to change order from ascending to descending. If descending,
     * value will be -1 and orders will be multiplied by -1.
     */
    private int ascending = 1;
    
    public WeaponComparatorRange(boolean ascending) {
        if (!ascending) {
            this.ascending = -1;
        }
    }

    @Override
    public int compare(WeaponMounted obj1, WeaponMounted obj2) {
        WeaponType weap1 = obj1.getType();
        WeaponType weap2 = obj2.getType();

        // If types are equal, pick front facing first
        if (weap1 == weap2) {
            if (obj1.isRearMounted()) {
                return -1 * ascending;
            } else if (obj2.isRearMounted()) {
                return ascending;
            } else {
                return 0;
            }
        }
        int[] ranges1 = weap1.getRanges(obj1);
        int[] ranges2 = weap2.getRanges(obj2);
        // Start by comparing the short range brackets (*not* the minimum
        // ranges at index 0), then work outwards from there as needed.
        for (int r = 1; r < ranges1.length; r++) {
            if (ranges1[r] < ranges2[r]) {
                return -1 * ascending;
            } else if (ranges1[r] > ranges2[r]) {
                return ascending;
            }
        }
        // If we get here, all ranges are equals, arbitrate with heat
        if (weap1.getHeat() > weap2.getHeat()) {
            return ascending;
        } else if (weap1.getHeat() < weap2.getHeat()) {
            return -1 * ascending;
        } else {
            return 0;
        }
    }
}
