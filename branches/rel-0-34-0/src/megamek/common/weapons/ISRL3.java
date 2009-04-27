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
public class ISRL3 extends RLWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -5963869448761538363L;

    /**
     * 
     */
    public ISRL3() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Rocket Launcher 3";
        this.setInternalName("RL3");
        this.addLookupName("RL 3");
        this.addLookupName("ISRocketLauncher3");
        this.addLookupName("IS RLauncher-3");
        this.rackSize = 3;
        this.shortRange = 3;
        this.mediumRange = 7;
        this.longRange = 12;
        this.extremeRange = 14;
        this.bv = 4;
    }
}
