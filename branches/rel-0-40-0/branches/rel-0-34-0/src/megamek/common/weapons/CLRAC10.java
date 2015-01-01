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
public class CLRAC10 extends RACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 7945585759921446908L;

    /**
     * 
     */
    public CLRAC10() {
        super();
        this.techLevel = TechConstants.T_CLAN_UNOFFICIAL;
        this.name = "Rotary AC/10";
        this.setInternalName("CLRotaryAC10");
        this.addLookupName("Clan Rotary AC/10");
        this.addLookupName("Clan Rotary Assault Cannon/10");
        this.heat = 3;
        this.damage = 10;
        this.rackSize = 10;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.tonnage = 14.0f;
        this.criticals = 7;
        this.bv = 617;
        this.cost = 640000;
    }
}
