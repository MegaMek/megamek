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

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISThunderBolt5 extends ThunderBoltWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 5295837076559643763L;

    /**
     * 
     */
    public ISThunderBolt5() {
        super();
        this.techLevel = TechConstants.T_IS_ADVANCED;
        this.name = "Thunderbolt 5";
        this.setInternalName(this.name);
        this.addLookupName("IS Thunderbolt-5");
        this.addLookupName("ISThunderbolt5");
        this.addLookupName("IS Thunderbolt 5");
        this.ammoType = AmmoType.T_TBOLT_5;
        this.heat = 3;
        this.minimumRange = 5;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.tonnage = 3.0f;
        this.criticals = 1;
        this.bv = 64;
        this.cost = 50000;
    }
}
