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
public class ISHVAC2 extends HVACWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 4958849713169213573L;

    public ISHVAC2() {
        super();
        this.name = "Hyper Velocity Auto Cannon/2";
        this.setInternalName(this.name);
        this.addLookupName("IS Hyper Velocity Auto Cannon/2");
        this.addLookupName("ISHVAC2");
        this.addLookupName("IS Hyper Velocity Autocannon/2");
        this.heat = 1;
        this.damage = 2;
        this.rackSize = 2;
        this.minimumRange = 3;
        this.shortRange = 10;
        this.mediumRange = 20;
        this.longRange = 35;
        this.extremeRange = 40;
        this.tonnage = 8.0f;
        this.criticals = 2;
        this.bv = 53;
        this.cost = 100000;
        this.shortAV = 2;
        this.medAV = 2;
        this.longAV = 2;
        this.extAV = 2;
        this.maxRange = RANGE_EXT;
    }
}
