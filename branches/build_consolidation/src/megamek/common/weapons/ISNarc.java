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
public class ISNarc extends NarcWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 5562345335764812479L;

    /**
     * 
     */
    public ISNarc() {
        super();
        this.techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        this.name = "Narc";
        this.setInternalName("ISNarcBeacon");
        this.addLookupName("IS Narc Beacon");
        this.addLookupName("IS Narc Missile Beacon");
        this.heat = 0;
        this.rackSize = 1;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 3.0f;
        this.criticals = 2;
        this.bv = 30;
        this.cost = 100000;
        introDate = 2587;
        techLevel.put(2587, techLevel.get(3071));
        extinctDate = 2795;
        reintroDate = 3035;
        availRating = new int[] { RATING_E, RATING_F, RATING_D };
        techRating = RATING_E;
    }
}
