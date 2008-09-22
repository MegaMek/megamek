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
public class CLRAC2 extends RACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -2134880724662962943L;

    /**
     * 
     */
    public CLRAC2() {
        super();
        this.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        this.name = "Rotary AC/2";
        this.setInternalName("CLRotaryAC2");
        this.addLookupName("Clan Rotary AC/2");
        this.addLookupName("Clan Rotary Assault Cannon/2");
        this.heat = 1;
        this.damage = 2;
        this.rackSize = 2;
        this.minimumRange = 2;
        this.shortRange = 9;
        this.mediumRange = 18;
        this.longRange = 27;
        this.extremeRange = 36;
        this.tonnage = 7.0f;
        this.criticals = 4;
        this.bv = 185;
        this.cost = 240000;
    }
}
