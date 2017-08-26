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
package megamek.common.weapons.unofficial;

import megamek.common.AmmoType;
import megamek.common.weapons.lasers.LaserWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISLaserAMSTHB extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -1940059603781427515L;

    /**
     *
     */
    public ISLaserAMSTHB() {
        super();
        name = "Laser AMS (THB)";
        setInternalName("ISLaserAntiMissileSystemTHB");
        addLookupName("IS Laser Anti-Missile System (THB)");
        addLookupName("IS Laser AMS (THB)");
        heat = 3;
        rackSize = 2;
        damage = 2; // # of d6 of missiles affected
        ammoType = AmmoType.T_NA;
        tonnage = 1.5f;
        criticals = 2;
        bv = 105;
        // we need to remove the direct fire flag again, so TC weight is not
        // affected
        flags = flags.or(F_MECH_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON)
                .or(F_AUTO_TARGET).or(F_HEATASDICE).or(F_AMS)
                .and(F_DIRECT_FIRE.not());
        setModes(new String[] { "On", "Off" });
        setInstantModeSwitch(false);
        cost = 300000;
        //Since this are the Tactical Handbook Weapons I'm using the TM Stats.
        rulesRefs = "322,TO";
        atClass = CLASS_AMS;
        shortAV = 3; // StratOps Advanced Point Defense Damage
        maxRange = RANGE_SHORT; //TODO: add point defense range of 1.
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setUnofficial(true)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setISAdvancement(3059, 3079, 3145, DATE_NONE, DATE_NONE)
            .setISApproximate(false, true, false,false, false)
            .setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS);
    }
}
