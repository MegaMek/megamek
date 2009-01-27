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
        techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        name = "Rotary AC/5";
        setInternalName("CLRotaryAC5");
        addLookupName("Clan Rotary AC/5");
        addLookupName("Clan Rotary Assault Cannon/5");
        heat = 1;
        damage = 5;
        rackSize = 5;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 10.0f;
        criticals = 8;
        bv = 360;
        cost = 400000;
    }
}
