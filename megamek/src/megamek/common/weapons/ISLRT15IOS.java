/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISLRT15IOS extends LRTWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 106526906717711956L;

    /**
     *
     */
    public ISLRT15IOS() {
        super();
        techLevel = TechConstants.T_IS_TW_NON_BOX;
        name = "LRT 15 (I-OS)";
        setInternalName(name);
        addLookupName("IS IOS LRT-15");
        addLookupName("ISLRTorpedo15 (IOS)");
        addLookupName("IS LRT 15 (IOS)");
        addLookupName("ISLRT15IOS");
        heat = 5;
        rackSize = 15;
        minimumRange = 6;
        waterShortRange = 7;
        waterMediumRange = 14;
        waterLongRange = 21;
        waterExtremeRange = 28;
        tonnage = 7.0f;
        criticals = 3;
        bv = 27;
        flags = flags.or(F_ONESHOT);
        cost = 175000;
    }
}
