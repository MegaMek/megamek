/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
/*
 * Created on Sep 25, 2004
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
        name = "HVAC/5";
        setInternalName("Hyper Velocity Auto Cannon/5");
        addLookupName("IS Hyper Velocity Auto Cannon/5");
        addLookupName("ISHVAC5");
        addLookupName("IS Hyper Velocity Autocannon/5");
        heat = 3;
        damage = 5;
        rackSize = 5;
        shortRange = 8;
        mediumRange = 16;
        longRange = 28;
        extremeRange = 32;
        tonnage = 12.0f;
        criticals = 4;
        bv = 109;
        cost = 160000;
        shortAV = 5;
        medAV = 5;
        longAV = 5;
        extAV = 5;
        maxRange = RANGE_EXT;
        techRating = RATING_D;
        availRating = new int[]{RATING_X, RATING_X, RATING_F};
        introDate = 3059;
        techLevel.put(3059,techLevel.get(3071));
    }
}
