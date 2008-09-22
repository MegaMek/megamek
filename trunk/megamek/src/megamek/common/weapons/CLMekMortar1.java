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
public class CLMekMortar1 extends MekMortarWeapon{

    /**
     * 
     */
    private static final long serialVersionUID = -2449264496450109574L;

    /**
     * 
     */
    public CLMekMortar1() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Mortar 1";
        this.setInternalName("Clan Mech Mortar-1");
        this.addLookupName("CLMekMortar1");
        this.addLookupName("Clan Mek Mortar 1");
        this.rackSize = 1;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.bv = 29;
        this.heat = 1;
        this.criticals = 1;
        this.tonnage = 1;
        this.cost = 7000;
    }
}
