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
 * Created on Sep 13, 2004
 *
 */
package megamek.common.weapons.ppc;

import megamek.common.TechAdvancement;

/**
 * @author Andrew Hunter
 */
public class ISPPC extends PPCWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 5775665622863346537L;

    /**
     *
     */
    public ISPPC() {
        super();
        name = "PPC";
        setInternalName(name);
        addLookupName("Particle Cannon");
        addLookupName("IS PPC");
        addLookupName("ISPPC");
        heat = 10;
        damage = 10;
        minimumRange = 3;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        waterShortRange = 4;
        waterMediumRange = 7;
        waterLongRange = 10;
        waterExtremeRange = 14;
        setModes(new String[] { "Field Inhibitor ON", "Field Inhibitor OFF" });
        tonnage = 7.0f;
        criticals = 3;
        bv = 176;
        cost = 200000;
        shortAV = 10;
        medAV = 10;
        maxRange = RANGE_MED;
        // with a capacitor
        explosive = true;
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_ALL);
        techAdvancement.setISAdvancement(2435, 2460, 2500);
        techAdvancement.setClanAdvancement(DATE_NONE, DATE_NONE, 2500, 2825);
        techAdvancement.setIntroLevel(true);
        techAdvancement.setTechRating(RATING_D);
        techAdvancement.setAvailability( new int[] { RATING_C, RATING_C, RATING_C, RATING_C });
    }

}
