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
public class ISCruiseMissile70 extends ArtilleryWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 5323886711682442495L;

    /**
     * 
     */
    public ISCruiseMissile70() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "Cruise Missile/70";
        this.setInternalName("ISCruiseMissile70");
        this.heat = 70;
        this.rackSize = 70;
        this.ammoType = AmmoType.T_CRUISE_MISSILE;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 90;
        this.extremeRange = 90; // No extreme range.
        this.tonnage = 80f;
        this.criticals = 80;
        this.bv = 1031;
        this.cost = 1250000;
    }

}
