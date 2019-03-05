/* MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.capitalweapons;

import megamek.common.AmmoType;

/**
 * @author Jay Lawson
 */
public class SubCapMissilePiranhaWeapon extends SubCapMissileWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 3827228773282489872L;

    /**
     * 
     */
    public SubCapMissilePiranhaWeapon() {
        super();
        name = "Sub-Capital Missile Launcher (Piranha)";
        setInternalName(name);
        addLookupName("Piranha");
        heat = 9;
        damage = 3;
        ammoType = AmmoType.T_PIRANHA;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 100.0;
        bv = 670;
        cost = 75000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        this.missileArmor = 30;
        maxRange = RANGE_LONG;
        flags = flags.or(F_AERO_WEAPON).or(F_MISSILE);
        this.atClass = CLASS_CAPITAL_MISSILE;
        rulesRefs = "345,TO";
        techAdvancement.setTechBase(TECH_BASE_ALL)
            .setIntroLevel(false)
            .setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
            .setISAdvancement(3066, 3072, 3145, DATE_NONE, DATE_NONE)
            .setISApproximate(true, false, false, false, false)
            .setClanAdvancement(DATE_NONE,DATE_NONE,3073,DATE_NONE,DATE_NONE)
            .setISApproximate(true, false, false, false, false)
            .setPrototypeFactions(F_WB)
            .setProductionFactions(F_WB);
    }
}
