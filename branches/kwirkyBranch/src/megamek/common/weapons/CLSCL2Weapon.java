/* MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Jay Lawson
 */
public class CLSCL2Weapon extends SubCapitalLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public CLSCL2Weapon() {
        super();
        this.techLevel.put(3071, TechConstants.T_CLAN_ADVANCED);
        this.name = "Sub-Capital Laser 2 (Clan)";
        this.setInternalName(this.name);
        this.addLookupName("CLSCL2");
        this.heat = 28;
        this.damage = 2;
        this.shortRange = 11;
        this.mediumRange = 22;
        this.longRange = 33;
        this.extremeRange = 44;
        this.tonnage = 200.0f;
        this.bv = 354;
        this.cost = 335000;
        this.shortAV = 2;
        this.medAV = 2;
        this.maxRange = RANGE_MED;
        introDate = 3068;
        techLevel.put(3068, techLevel.get(3071));
        techLevel.put(3073, TechConstants.T_CLAN_TW);
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        techRating = RATING_E;
    }
}
