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
public class ISSRM5 extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 2564548381701365334L;

    /**
     *
     */
    public ISSRM5() {
        super();
        techLevel.put(3071,TechConstants.T_IS_TW_NON_BOX);
        name = "SRM 5";
        setInternalName(name);
        addLookupName("IS SRM-5");
        addLookupName("ISSRM5");
        addLookupName("IS SRM 5");
        rackSize = 5;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        bv = 47;
        flags = flags.or(F_NO_FIRES);
        introDate = 3050;
        techLevel.put(3050,techLevel.get(3071));
        availRating = new int[]{RATING_X,RATING_X,RATING_D};
        techRating = RATING_E;
    }
}
