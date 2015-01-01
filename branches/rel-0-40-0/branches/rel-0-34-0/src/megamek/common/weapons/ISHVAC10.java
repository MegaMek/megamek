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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

/**
 * @author Jason Tighe
 */
public class ISHVAC10 extends HVACWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 4958849713169213573L;

    public ISHVAC10() {
        super();
        this.name = "Hyper Velocity Auto Cannon/10";
        this.setInternalName(this.name);
        this.addLookupName("IS Hyper Velocity Auto Cannon/10");
        this.addLookupName("ISHVAC10");
        this.addLookupName("IS Hyper Velocity Autocannon/10");
        this.heat = 7;
        this.damage = 10;
        this.rackSize = 10;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 20;
        this.extremeRange = 24;
        this.tonnage = 14.0f;
        this.criticals = 6;
        this.bv = 158;
        this.cost = 230000;
        this.shortAV = 10;
        this.medAV = 10;
        this.longAV = 10;
        this.maxRange = RANGE_LONG;
    }
    
}
