/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.unofficial;

import megamek.common.AmmoType;
import megamek.common.weapons.autocannons.ACWeapon;

/**
 * @author BATTLEMASTER
 * @since Sep 25, 2004
 */
public class ISGAC8 extends ACWeapon {
    private static final long serialVersionUID = 49211848611799265L;

    public ISGAC8() {
        super();
        name = "GAC/8";
        setInternalName(name);
        addLookupName("IS Gatling Auto Cannon/8");
        addLookupName("Gatling Auto Cannon/8");
        addLookupName("Gatling AutoCannon/8");
        addLookupName("ISGAC8");
        addLookupName("IS Gatling Autocannon/8");
        heat = 8;
        damage = 16;
        rackSize = 8;
        minimumRange = 4;
        shortRange = 8;
        mediumRange = 16;
        longRange = 24;
        extremeRange = 32;
        tonnage = 16.0;
        criticals = 4;
        bv = 421;
        cost = 400000;
        explosive = true; // when firing incendiary ammo
        shortAV = 16;
        medAV = 16;
        longAV = 16;
        extAV = 16;
        maxRange = RANGE_LONG;
        explosionDamage = damage;
        toHitModifier = -1;
        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_MECH_WEAPON)
                .or(F_AERO_WEAPON).or(F_TANK_WEAPON).or(F_PULSE);
        ammoType = AmmoType.T_AC;
        atClass = CLASS_AC;
        // Going to Assume these are like IS RACs
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3060, 3062, 3071, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
    }
}
