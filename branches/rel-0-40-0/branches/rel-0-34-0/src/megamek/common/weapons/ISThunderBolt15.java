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
public class ISThunderBolt15 extends ThunderBoltWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -5466726857144417393L;

    /**
     * 
     */
    public ISThunderBolt15() {
        super();
        this.techLevel = TechConstants.T_IS_ADVANCED;
        this.name = "Thunderbolt 15";
        this.setInternalName(this.name);
        this.addLookupName("IS Thunderbolt-15");
        this.addLookupName("ISThunderbolt15");
        this.addLookupName("IS Thunderbolt 15");
        this.addLookupName("ISTBolt15");
        this.ammoType = AmmoType.T_TBOLT_15;
        this.heat = 7;
        this.minimumRange = 5;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.tonnage = 11.0f;
        this.criticals = 3;
        this.bv = 229;
        this.cost = 325000;
    }
}
