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
public class CLRAC20 extends RACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -6413635622423390328L;

    /**
     * 
     */
    public CLRAC20() {
        super();
        this.techLevel = TechConstants.T_CLAN_UNOFFICIAL;
        this.name = "Rotary AC/20";
        this.setInternalName("CLRotaryAC20");
        this.addLookupName("Clan Rotary AC/20");
        this.addLookupName("Clan Rotary Assault Cannon/20");
        this.heat = 7;
        this.damage = 20;
        this.rackSize = 20;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.extremeRange = 16;
        this.tonnage = 16.0f;
        this.criticals = 10;
        this.bv = 823;
        this.cost = 960000;
    }
}
