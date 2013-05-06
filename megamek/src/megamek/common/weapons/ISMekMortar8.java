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
 * @author Jason Tighe
 */
public class ISMekMortar8 extends MekMortarWeapon{

    /**
     * 
     */
    private static final long serialVersionUID = -3352749710661515958L;

    /**
     * 
     */
    public ISMekMortar8() {
        super();
        this.techLevel.put(3071,TechConstants.T_IS_ADVANCED);
        this.name = "Mortar 8";
        this.setInternalName("IS Mech Mortar-8");
        this.addLookupName("ISMekMortar8");
        this.addLookupName("IS Mek Mortar 8");
        this.rackSize = 8;
        this.minimumRange = 6;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.bv = 50;
        this.heat = 10;
        this.criticals = 5;
        this.tonnage = 10;
        this.cost = 70000;
        this.techRating = RATING_B;
        this.availRating = new int[]{RATING_D, RATING_F, RATING_E};
        this.introDate = 2531;	
        this.extinctDate = 2819;	
        this.reintroDate = 3043;
    }
}
