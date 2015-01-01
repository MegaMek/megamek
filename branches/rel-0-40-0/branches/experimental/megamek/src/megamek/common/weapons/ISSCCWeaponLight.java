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
public class ISSCCWeaponLight extends SubCapitalCannonWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public ISSCCWeaponLight() {
        super();
        this.techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        this.name = "Light Sub-Capital Cannon";
        this.setInternalName(this.name);
        this.addLookupName("LightSCC");
        this.heat = 12;
        this.damage = 2;
        this.rackSize = 2;
        this.shortRange = 11;
        this.mediumRange = 22;
        this.longRange = 33;
        this.extremeRange = 44;
        this.tonnage = 200.0f;
        this.bv = 379;
        this.cost = 330000;
        this.shortAV = 2;
        this.medAV = 2;
        this.longAV = 2;
        this.maxRange = RANGE_LONG;
        introDate = 3068;
        techLevel.put(3068, techLevel.get(3071));
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        techRating = RATING_E;
    }
}
