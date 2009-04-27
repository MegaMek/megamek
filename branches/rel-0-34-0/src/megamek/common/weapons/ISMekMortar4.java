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
public class ISMekMortar4 extends MekMortarWeapon{

    /**
     * 
     */
    private static final long serialVersionUID = 6803604562717710451L;

    /**
     * 
     */
    public ISMekMortar4() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Mortar 4";
        this.setInternalName("IS Mech Mortar-4");
        this.addLookupName("ISMekMortar4");
        this.addLookupName("IS Mek Mortar 4");
        this.rackSize = 4;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.bv = 26;
        this.heat = 5;
        this.criticals = 3;
        this.tonnage = 7;
        this.cost = 32000;
    }
}
