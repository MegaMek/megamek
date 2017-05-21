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
package megamek.common.weapons.missiles;

/**
 * @author Sebastian Brocks
 */
public class ISMRM30OS extends MRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 7118245780649534184L;

    /**
     *
     */
    public ISMRM30OS() {
        super();

        name = "MRM 30 (OS)";
        setInternalName(name);
        addLookupName("OS MRM-30");
        addLookupName("ISMRM30 (OS)");
        addLookupName("IS MRM 30 (OS)");
        heat = 10;
        rackSize = 30;
        shortRange = 3;
        mediumRange = 8;
        longRange = 15;
        extremeRange = 16;
        tonnage = 10.5f;
        criticals = 5;
        bv = 34;
        flags = flags.or(F_ONESHOT);
        cost = 112500;
        shortAV = 18;
        medAV = 18;
        maxRange = RANGE_MED;
        rulesRefs = "229, TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_C)
        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
        .setISAdvancement(3052, 3058, 3063, DATE_NONE, DATE_NONE)
        .setISApproximate(true, false, false,false, false)
        .setPrototypeFactions(F_DC)
        .setProductionFactions(F_DC);
    }
}
