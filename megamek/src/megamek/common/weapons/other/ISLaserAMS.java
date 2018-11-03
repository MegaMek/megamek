/*
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
 */
package megamek.common.weapons.other;

import megamek.common.AmmoType;
import megamek.common.weapons.lasers.LaserWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISLaserAMS extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -7448728413011101076L;

    public ISLaserAMS() {
        super();
        name = "Laser AMS";
        setInternalName("ISLaserAntiMissileSystem");
        addLookupName("IS Laser Anti-Missile System");
        addLookupName("IS Laser AMS");
        addLookupName("ISLaserAMS");
        heat = 7;
        rackSize = 2;
        damage = 3; // for manual operation
        minimumRange = 0; 
        shortRange = 1;
        mediumRange = 1;
        longRange = 1;
        extremeRange = 1;
        maxRange = RANGE_SHORT;
        shortAV = 3;
        ammoType = AmmoType.T_NA;
        tonnage = 1.5;
        criticals = 2;
        bv = 45;
        atClass = CLASS_AMS;
        // we need to remove the direct fire flag again, so TC weight is not
        // affected
        flags = flags.or(F_MECH_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON)
                .or(F_AUTO_TARGET).or(F_AMS).or(F_ENERGY).and(F_DIRECT_FIRE.not());
        setModes(new String[] { "On", "Off" });
        setInstantModeSwitch(false);
        cost = 225000;
        rulesRefs = "322,TO";
        techAdvancement.setTechBase(TECH_BASE_IS)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setISAdvancement(3059, 3079, 3145, DATE_NONE, DATE_NONE)
            .setISApproximate(false, true, false,false, false)
            .setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS);
    }
}
