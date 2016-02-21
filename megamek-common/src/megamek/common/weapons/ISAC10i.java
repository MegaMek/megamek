/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.EquipmentType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISAC10i extends ACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 7447941274169853546L;

    /**
     *
     */
    public ISAC10i() {
        super();
        techLevel.put(3071, TechConstants.T_IS_UNOFFICIAL);
        name = "AC/10i";
        setInternalName("ISAutocannon10i");
        addLookupName("ISAC10i");
        addLookupName("ISAC/10i");
        heat = 3;
        damage = 10;
        rackSize = 10;
        minimumRange = 1;
        shortRange = 7;
        mediumRange = 15;
        longRange = 20;
        extremeRange = 28;
        tonnage = 12.0f;
        criticals = 7;
        bv = 167;
        cost = 410000;
        explosive = false;
        shortAV = 10;
        medAV = 10;
        longAV = 10;
        maxRange = RANGE_LONG;
        explosionDamage = damage;
        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_MECH_WEAPON)
                .or(F_AERO_WEAPON).or(F_TANK_WEAPON);
        ammoType = AmmoType.T_ACi;
        atClass = CLASS_AC;
        //Since this is an unoffical Weapon I'm using the Normal AC10 Stats
        availRating = new int[] { EquipmentType.RATING_C,
                EquipmentType.RATING_D, EquipmentType.RATING_D };
        introDate = 2460;
        techLevel.put(2460, techLevel.get(3071));
        techRating = RATING_C;
    }
}
