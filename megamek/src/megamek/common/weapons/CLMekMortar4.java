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
public class CLMekMortar4 extends MekMortarWeapon{

    /**
     * 
     */
    private static final long serialVersionUID = -7326848486069567891L;

    /**
     * 
     */
    public CLMekMortar4() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Mortar 4";
        this.setInternalName("Clan Mech Mortar-4");
        this.addLookupName("CLMekMortar4");
        this.addLookupName("Clan Mek Mortar 4");
        this.rackSize = 1;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.bv = 86;
        this.heat = 5;
        this.criticals = 2;
        this.tonnage = 3.5f;
        this.cost = 32000;
    }
}
