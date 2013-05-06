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
        techLevel.put(3071,TechConstants.T_CLAN_UNOFFICIAL);
        name = "Rotary AC/20";
        setInternalName("CLRotaryAC20");
        addLookupName("Clan Rotary AC/20");
        addLookupName("Clan Rotary Assault Cannon/20");
        heat = 7;
        damage = 20;
        rackSize = 20;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 16.0f;
        criticals = 10;
        bv = 823;
        cost = 960000;
        techRating = RATING_F;
    }
}
