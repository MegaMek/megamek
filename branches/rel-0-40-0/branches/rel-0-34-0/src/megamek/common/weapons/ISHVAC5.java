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
public class ISHVAC5 extends HVACWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -1116752747486372187L;

    public ISHVAC5() {
        super();
        this.name = "Hyper Velocity Auto Cannon/5";
        this.setInternalName(this.name);
        this.addLookupName("IS Hyper Velocity Auto Cannon/5");
        this.addLookupName("ISHVAC5");
        this.addLookupName("IS Hyper Velocity Autocannon/5");
        this.heat = 3;
        this.damage = 5;
        this.rackSize = 5;
        this.shortRange = 8;
        this.mediumRange = 16;
        this.longRange = 28;
        this.extremeRange = 32;
        this.tonnage = 12.0f;
        this.criticals = 4;
        this.bv = 109;
        this.cost = 160000;
        this.shortAV = 5;
        this.medAV = 5;
        this.longAV = 5;
        this.extAV = 5;
        this.maxRange = RANGE_EXT;
    }
}
