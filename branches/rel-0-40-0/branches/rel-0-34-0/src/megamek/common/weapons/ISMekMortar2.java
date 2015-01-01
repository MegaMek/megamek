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
public class ISMekMortar2 extends MekMortarWeapon{

    /**
     * 
     */
    private static final long serialVersionUID = -6644886866545312980L;

    /**
     * 
     */
    public ISMekMortar2() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Mortar 2";
        this.setInternalName("IS Mech Mortar-2");
        this.addLookupName("ISMekMortar2");
        this.addLookupName("IS Mek Mortar 2");
        this.rackSize = 2;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.bv = 14;
        this.heat = 2;
        this.criticals = 2;
        this.tonnage = 5;
        this.cost = 15000;
    }
}
