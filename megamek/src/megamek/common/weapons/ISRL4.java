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
public class ISRL4 extends RLWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 5679355637948305939L;

    /**
     * 
     */
    public ISRL4() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Rocket Launcher 4";
        this.setInternalName("RL4");
        this.addLookupName("RL 4");
        this.addLookupName("ISRocketLauncher4");
        this.addLookupName("IS RLauncher-4");
        this.rackSize = 4;
        this.shortRange = 3;
        this.mediumRange = 7;
        this.longRange = 12;
        this.extremeRange = 14;
        this.bv = 5;
    }
}
