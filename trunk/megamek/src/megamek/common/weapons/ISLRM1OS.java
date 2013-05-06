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
public class ISLRM1OS extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -5976936994611000430L;

    /**
     *
     */
    public ISLRM1OS() {
        super();
        techLevel.put(3071,TechConstants.T_IS_TW_NON_BOX);
        name = "LRM 1 (OS)";
        setInternalName(name);
        addLookupName("ISLRM1OS");
        rackSize = 1;
        minimumRange = 6;
        bv = 3;
        flags = flags.or(F_ONESHOT);
        this.introDate = 2676;
        this.extinctDate = 2800;
        this.reintroDate = 3030;
    }
}
