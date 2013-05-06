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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISMRM40OS extends MRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5383621160269655212L;

    /**
     *
     */
    public ISMRM40OS() {
        super();
        techLevel.put(3071,TechConstants.T_IS_TW_NON_BOX);
        name = "MRM 40 (OS)";
        setInternalName(name);
        addLookupName("OS MRM-40");
        addLookupName("ISMRM40 (OS)");
        addLookupName("IS MRM 40 (OS)");
        heat = 12;
        rackSize = 40;
        shortRange = 3;
        mediumRange = 8;
        longRange = 15;
        extremeRange = 16;
        tonnage = 12.5f;
        criticals = 7;
        bv = 45;
        flags = flags.or(F_ONESHOT);
        cost = 175000;
        shortAV = 24;
        medAV = 24;
        maxRange = RANGE_MED;
        introDate = 3058;
        techLevel.put(3058,techLevel.get(3071));
        availRating = new int[]{RATING_X,RATING_X,RATING_E};
        techRating = RATING_C;
    }
}
