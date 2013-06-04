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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLAMS extends AmmoWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 7447941274169853546L;

    /**
     *
     */
    public CLAMS() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "AMS";
        setInternalName("CLAntiMissileSystem");
        addLookupName("Clan Anti-Missile Sys");
        addLookupName("Clan AMS");
        heat = 1;
        rackSize = 2;
        damage = 2; // # of d6 of missiles affected
        ammoType = AmmoType.T_AMS;
        tonnage = 0.5f;
        criticals = 1;
        bv = 32;
        flags = flags.or(F_AUTO_TARGET).or(F_AMS).or(F_BALLISTIC)
                .or(F_MECH_WEAPON).or(F_AERO_WEAPON).or(F_TANK_WEAPON)
                .or(F_PROTO_WEAPON);
        setModes(new String[] { "On", "Off" });
        setInstantModeSwitch(false);
        cost = 100000;
        introDate = 2831;
        techLevel.put(2831, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_D, RATING_C };
        techRating = RATING_F;
        atClass = CLASS_POINT_DEFENSE;
    }
}
