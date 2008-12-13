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
public class ISRL1 extends RLWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 9080214985232453233L;

    /**
     * 
     */
    public ISRL1() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Rocket Launcher 1";
        this.setInternalName("RL1");
        this.addLookupName("RL 1");
        this.addLookupName("ISRocketLauncher1");
        this.addLookupName("IS RLauncher-1");
        this.rackSize = 1;
        this.shortRange = 3;
        this.mediumRange = 7;
        this.longRange = 12;
        this.extremeRange = 14;
        this.bv = 2;
    }
}
