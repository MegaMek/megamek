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
public class ISThunderBolt10 extends ThunderBoltWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 6781882739979127656L;

    /**
     * 
     */
    public ISThunderBolt10() {
        super();
        this.techLevel = TechConstants.T_IS_ADVANCED;
        this.name = "Thunderbolt 10";
        this.setInternalName(this.name);
        this.addLookupName("IS Thunderbolt-10");
        this.addLookupName("ISThunderbolt10");
        this.addLookupName("IS Thunderbolt 10");
        this.addLookupName("ISTBolt10");
        this.ammoType = AmmoType.T_TBOLT_10;
        this.heat = 5;
        this.minimumRange = 5;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.tonnage = 7.0f;
        this.criticals = 2;
        this.bv = 127;
        this.cost = 175000;
    }
}
