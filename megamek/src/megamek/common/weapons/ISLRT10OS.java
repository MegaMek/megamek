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
public class ISLRT10OS extends LRTWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 2674309948460871883L;

    /**
     * 
     */
    public ISLRT10OS() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "LRT 10 (OS)";
        this.setInternalName(this.name);
        this.addLookupName("IS OS LRT-10");
        this.addLookupName("ISLRTorpedo10 (OS)");
        this.addLookupName("IS LRT 10 (OS)");
        this.heat = 4;
        this.rackSize = 10;
        this.minimumRange = 6;
        this.waterShortRange = 7;
        this.waterMediumRange = 14;
        this.waterLongRange = 21;
        this.waterExtremeRange = 28;
        this.tonnage = 5.5f;
        this.criticals = 2;
        this.bv = 18;
        this.flags |= F_ONESHOT;
        this.cost = 100000;
    }
}
