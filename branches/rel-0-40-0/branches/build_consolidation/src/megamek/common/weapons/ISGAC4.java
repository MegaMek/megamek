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
public class ISGAC4 extends ACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 49211848611799265L;

    /**
     * 
     */
    public ISGAC4() {
        super();
        techLevel.put(3071, TechConstants.T_IS_UNOFFICIAL);
        name = "GAC/4";
        setInternalName(name);
        addLookupName("IS Gatling Auto Cannon/4");
        addLookupName("Gatling Auto Cannon/4");
        addLookupName("Gatling AutoCannon/4");
        addLookupName("ISGAC4");
        addLookupName("IS Gatling Autocannon/4");
        heat = 4;
        damage = 8;
        rackSize = 4;
        minimumRange = 4;
        shortRange = 8;
        mediumRange = 16;
        longRange = 24;
        extremeRange = 32;
        tonnage = 12.0f;
        criticals = 4;
        bv = 175;
        cost = 200000;
        explosive = true; // when firing incendiary ammo
        shortAV = 8;
        medAV = 8;
        longAV = 8;
        extAV = 8;
        maxRange = RANGE_LONG;
        explosionDamage = damage;
        toHitModifier = -1;
        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_MECH_WEAPON)
                .or(F_AERO_WEAPON).or(F_TANK_WEAPON).or(F_PULSE);
        ammoType = AmmoType.T_AC;
        techRating = RATING_X;
        atClass = CLASS_AC;
    }
}
