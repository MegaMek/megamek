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

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Jay Lawson
 */
public class MediumMassDriver extends MassDriverWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public MediumMassDriver() {
        super();
        this.name = "Medium Mass Driver";
        this.setInternalName(this.name);
        this.addLookupName("MediumMassDriver");
        this.heat = 60;
        this.damage = 100;
        this.ammoType = AmmoType.T_MMASS;
        this.shortRange = 12;
        this.mediumRange = 24;
        this.longRange = 40;
        this.tonnage = 50000;
        this.bv = 0;
        this.cost = 280000000;
        this.shortAV = 100;
        this.medAV = 100;
        this.longAV = 100;
        techRating = RATING_D;
        availRating = new int[] { RATING_E, RATING_X, RATING_E };
        introDate = 2715;
        techLevel.put(2715, techLevel.get(3071));
        this.techLevel.put(2715, TechConstants.T_IS_EXPERIMENTAL);
        this.maxRange = RANGE_LONG;
        
    }
}
