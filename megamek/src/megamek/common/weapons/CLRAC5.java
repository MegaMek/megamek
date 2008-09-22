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
/*
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLRAC5 extends RACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -9054458663836717481L;

    /**
     * 
     */
    public CLRAC5() {
        super();
        this.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        this.name = "Rotary AC/5";
        this.setInternalName("CLRotaryAC5");
        this.addLookupName("Clan Rotary AC/5");
        this.addLookupName("Clan Rotary Assault Cannon/5");
        this.heat = 1;
        this.damage = 5;
        this.rackSize = 5;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 10.0f;
        this.criticals = 5;
        this.bv = 360;
        this.cost = 400000;
    }
}
