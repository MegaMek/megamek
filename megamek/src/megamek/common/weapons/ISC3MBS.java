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
 * @author Jason Tighe
 */
public class ISC3MBS extends TAGWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6402667441307181946L;

    public ISC3MBS() {
        super();
        techLevel.put(3071,TechConstants.T_IS_EXPERIMENTAL);
        name = "C3 Master Boosted with TAG";
        setInternalName("ISC3MasterBoostedSystemUnit");
        addLookupName("IS C3 Computer Boosted");
        addLookupName("ISC3MasterComputerBoosted");
        addLookupName("C3 Master Boosted System with TAG");
        tonnage = 6;
        criticals = 6;
        hittable = true;
        spreadable = false;
        cost = 3000000;
        bv = 0;
        flags = flags.or(F_C3MBS);
        heat = 0;
        damage = 0;
        shortRange = 5;
        mediumRange = 9;
        longRange = 15;
        extremeRange = 18;
        techRating = RATING_E;
        availRating = new int[]{RATING_X, RATING_X, RATING_F};
        introDate = 3073;
        techLevel.put(3073,techLevel.get(3071));
    }
}
