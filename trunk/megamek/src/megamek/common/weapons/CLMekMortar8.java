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
public class CLMekMortar8 extends MekMortarWeapon{

    /**
     * 
     */
    private static final long serialVersionUID = 7757701625628311696L;

    /**
     * 
     */
    public CLMekMortar8() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Mortar 8";
        this.setInternalName("Clan Mech Mortar-8");
        this.addLookupName("CLMekMortar8");
        this.addLookupName("Clan Mek Mortar 8");
        this.rackSize = 8;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.bv = 114;
        this.heat = 10;
        this.criticals = 3;
        this.tonnage = 5;
        this.cost = 70000;
    }
}
