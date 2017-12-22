package megamek.common.weapons.tag;
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
 * Created on Sep 7, 2005
 *
 */

/**
 * @author Jason Tighe
 */
public class ISC3MBS extends TAGWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6402667441307181946L;

    public ISC3MBS() {
        super();
        name = "C3 Boosted System (C3BS) [Master]";
        setInternalName("ISC3MasterBoostedSystemUnit");
        addLookupName("IS C3 Computer Boosted");
        addLookupName("ISC3MasterComputerBoosted");
        addLookupName("C3 Master Boosted System with TAG");
        tonnage = 6;
        criticals = 6;
        tankslots = 1;
        hittable = true;
        spreadable = false;
        cost = 3000000;
        bv = 0;
        flags = flags.or(F_C3MBS).or(F_MECH_WEAPON).or(F_TANK_WEAPON).andNot(F_AERO_WEAPON);
        heat = 0;
        damage = 0;
        shortRange = 5;
        mediumRange = 9;
        longRange = 15;
        extremeRange = 18;
        rulesRefs = "298,TO";
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setISAdvancement(3071, 3100, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, false,false, false)
            .setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS);
    }
}
