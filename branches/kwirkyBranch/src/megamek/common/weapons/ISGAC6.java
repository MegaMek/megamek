/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
import megamek.common.TechConstants;

/**
 * @author BATTLEMASTER
 */
public class ISGAC6 extends ACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 49211848611799265L;

    /**
     * 
     */
    public ISGAC6() {
        super();
        techLevel.put(3071, TechConstants.T_IS_UNOFFICIAL);
        name = "GAC/6";
        setInternalName(name);
        addLookupName("IS Gatling Auto Cannon/6");
        addLookupName("Gatling Auto Cannon/6");
        addLookupName("Gatling AutoCannon/6");
        addLookupName("ISGAC6");
        addLookupName("IS Gatling Autocannon/6");
        heat = 6;
        damage = 12;
        rackSize = 6;
        minimumRange = 4;
        shortRange = 8;
        mediumRange = 16;
        longRange = 24;
        extremeRange = 32;
        tonnage = 14.0f;
        criticals = 4;
        bv = 316;
        cost = 300000;
        explosive = true; // when firing incendiary ammo
        shortAV = 12;
        medAV = 12;
        longAV = 12;
        extAV = 12;
        maxRange = RANGE_LONG;
        explosionDamage = damage;
        toHitModifier = -1;
        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_MECH_WEAPON)
                .or(F_AERO_WEAPON).or(F_TANK_WEAPON).or(F_PULSE);
        ammoType = AmmoType.T_AC;
        techRating = RATING_X;
        atClass = CLASS_AC;
        //Going to Assume these are like IS RACs
        introDate = 3062;
        techLevel.put(3062, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        techRating = RATING_E;
    }
}
