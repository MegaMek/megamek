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
public class ISRL20 extends RLWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -1220608344459915265L;

    /**
     * 
     */
    public ISRL20() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Rocket Launcher 20";
        this.setInternalName("RL20");
        this.addLookupName("ISRocketLauncher20");
        this.addLookupName("RL 20");
        this.addLookupName("IS RLauncher-20");
        this.heat = 5;
        this.rackSize = 20;
        this.shortRange = 3;
        this.mediumRange = 7;
        this.longRange = 12;
        this.extremeRange = 14;
        this.tonnage = 1.5f;
        this.criticals = 3;
        this.bv = 24;
        this.cost = 45000;
        this.shortAV = 12;
        this.medAV = 12;
        this.maxRange = RANGE_MED;
    }
}
