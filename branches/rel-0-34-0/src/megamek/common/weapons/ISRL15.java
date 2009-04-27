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
public class ISRL15 extends RLWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -8464817815813827947L;

    /**
     * 
     */
    public ISRL15() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Rocket Launcher 15";
        this.setInternalName("RL15");
        this.addLookupName("ISRocketLauncher15");
        this.addLookupName("RL 15");
        this.addLookupName("IS RLauncher-15");
        this.heat = 5;
        this.rackSize = 15;
        this.shortRange = 4;
        this.mediumRange = 9;
        this.longRange = 15;
        this.extremeRange = 18;
        this.tonnage = 1.0f;
        this.criticals = 2;
        this.bv = 23;
        this.cost = 30000;
        this.shortAV = 9;
        this.medAV = 9;
        this.maxRange = RANGE_MED;
    }
}
