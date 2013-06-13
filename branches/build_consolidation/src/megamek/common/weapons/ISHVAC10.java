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
public class ISHVAC10 extends HVACWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 4958849713169213573L;

    public ISHVAC10() {
        super();
        name = "HVAC/10";
        setInternalName("Hyper Velocity Auto Cannon/10");
        addLookupName("IS Hyper Velocity Auto Cannon/10");
        addLookupName("ISHVAC10");
        addLookupName("IS Hyper Velocity Autocannon/10");
        heat = 7;
        damage = 10;
        rackSize = 10;
        shortRange = 6;
        mediumRange = 12;
        longRange = 20;
        extremeRange = 24;
        tonnage = 14.0f;
        criticals = 6;
        bv = 158;
        cost = 230000;
        shortAV = 10;
        medAV = 10;
        longAV = 10;
        maxRange = RANGE_LONG;
        techRating = RATING_D;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3060;
        techLevel.put(3060, techLevel.get(3071));
    }

}
