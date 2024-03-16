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
package megamek.common.weapons.battlearmor;

import megamek.common.weapons.missiles.MRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISBAMRM4 extends MRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 5084851020651390032L;

    /**
     * 
     */
    public ISBAMRM4() {
        super();
        this.name = "MRM 4";
        this.setInternalName("ISBAMRM4");
        this.addLookupName("BA MRM-4");
        this.addLookupName("IS BA MRM 4");
        this.rackSize = 4;
        this.shortRange = 3;
        this.mediumRange = 8;
        this.longRange = 15;
        this.extremeRange = 16;
        this.bv = 23;
        cost = 20000;
        tonnage = .24;
        criticals = 3;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "261, TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_E)
        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_B)
        .setISAdvancement(3058, 3060, 3067, DATE_NONE, DATE_NONE)
        .setISApproximate(true, false, false, false, false)
        .setPrototypeFactions(F_DC)
        .setProductionFactions(F_DC);
    }
}
