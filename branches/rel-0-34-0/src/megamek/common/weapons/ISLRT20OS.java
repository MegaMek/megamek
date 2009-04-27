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
public class ISLRT20OS extends LRTWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -8753051336468930345L;

    /**
     * 
     */
    public ISLRT20OS() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "LRT 20 (OS)";
        this.setInternalName(this.name);
        this.addLookupName("IS OS LRT-20");
        this.addLookupName("ISLRTorpedo20 (OS)");
        this.addLookupName("IS LRT 20 (OS)");
        this.heat = 6;
        this.rackSize = 20;
        this.minimumRange = 6;
        this.waterShortRange = 7;
        this.waterMediumRange = 14;
        this.waterLongRange = 21;
        this.waterExtremeRange = 28;
        this.tonnage = 10.5f;
        this.criticals = 5;
        this.bv = 36;
        this.flags |= F_ONESHOT;
        this.cost = 250000;
    }
}
