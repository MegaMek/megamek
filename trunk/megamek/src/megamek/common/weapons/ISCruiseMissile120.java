/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISCruiseMissile120 extends ArtilleryWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 5323886711682442495L;

    /**
     * 
     */
    public ISCruiseMissile120() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "Cruise Missile/120";
        this.setInternalName("ISCruiseMissile120");
        this.heat = 120;
        this.rackSize = 120;
        this.ammoType = AmmoType.T_CRUISE_MISSILE;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 150;
        this.extremeRange = 150; // No extreme range.
        this.tonnage = 135f;
        this.criticals = 120;
        this.bv = 2281;
        this.cost = 3000000;
    }

}
