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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISTAG extends TAGWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -2474477168563228542L;

    public ISTAG() {
        super();
        techLevel.put(3071,TechConstants.T_IS_TW_NON_BOX);
        name = "TAG";
        setInternalName("ISTAG");
        addLookupName("IS TAG");
        tonnage = 1;
        criticals = 1;
        hittable = true;
        spreadable = false;
        heat = 0;
        damage = 0;
        shortRange = 5;
        mediumRange = 9;
        longRange = 15;
        extremeRange = 18;
        bv = 0;
        cost = 50000;
        introDate = 2600;
        techLevel.put(2600,techLevel.get(3071));
        extinctDate = 2835;
        reintroDate = 3044;
        availRating = new int[]{RATING_E,RATING_F,RATING_D};
        techRating = RATING_E;
    }
}
