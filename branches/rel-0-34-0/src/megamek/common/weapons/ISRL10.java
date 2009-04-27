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
public class ISRL10 extends RLWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 3437644808445570760L;

    /**
     * 
     */
    public ISRL10() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Rocket Launcher 10";
        this.setInternalName("RL10");
        this.addLookupName("RL 10");
        this.addLookupName("ISRocketLauncher10");
        this.addLookupName("IS RLauncher-10");
        this.heat = 3;
        this.rackSize = 10;
        this.shortRange = 5;
        this.mediumRange = 11;
        this.longRange = 18;
        this.extremeRange = 22;
        this.tonnage = .5f;
        this.criticals = 1;
        this.bv = 18;
        this.cost = 15000;
        this.shortAV = 6;
        this.medAV = 6;
        this.maxRange = RANGE_MED;
    }
}
