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
public class ISEHERPPC extends PPCWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 7175778897598535734L;

    /**
     *
     */
    public ISEHERPPC() {
        super();
        name = "Enhanced ER PPC";
        setInternalName("ISEHERPPC");
        addLookupName("IS EH ER PPC");
        heat = 15;
        damage = 12;
        shortRange = 7;
        mediumRange = 14;
        longRange = 23;
        extremeRange = 28;
        waterShortRange = 4;
        waterMediumRange = 10;
        waterLongRange = 16;
        waterExtremeRange = 20;
        tonnage = 7.0;
        criticals = 3;
        bv = 329;
        cost = 300000;
        shortAV = 12;
        medAV = 12;
        longAV = 12;
        maxRange = RANGE_LONG;
        //This is the Clan Wolverine PPC mentioned in Blake Documents
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(2828, DATE_NONE, DATE_NONE, 2828);
        techAdvancement.setTechRating(RATING_F);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_F, RATING_X, RATING_X });
    }
}

//Replaced Per IO

