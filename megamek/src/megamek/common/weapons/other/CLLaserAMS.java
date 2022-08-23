/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.other;

import megamek.common.AmmoType;
import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.weapons.lasers.LaserWeapon;

/**
 * @author Sebastian Brocks
 * @since Sep 25, 2004
 */
public class CLLaserAMS extends LaserWeapon {
    private static final long serialVersionUID = 3262387868757752971L;

    public CLLaserAMS() {
        super();
        name = "Laser AMS";
        setInternalName("CLLaserAntiMissileSystem");
        addLookupName("Clan Laser Anti-Missile Sys");
        addLookupName("Clan Laser AMS");
        sortingName = "Anti-Missile System Laser";
        heat = 5;
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
        tonnage = 1;
        criticals = 1;
        bv = 45;
        atClass = CLASS_AMS;
        // we need to remove the direct fire flag again, so TC weight is not affected
        flags = flags.or(F_MECH_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON)
                .andNot(F_PROTO_WEAPON).or(F_AUTO_TARGET).or(F_AMS).or(F_ENERGY)
                .and(F_DIRECT_FIRE.not());
        setModes(new String[] { "On", "Off" });
        setInstantModeSwitch(false);
        cost = 100000;
        rulesRefs = "322, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(DATE_NONE, 3048, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CWF)
                .setProductionFactions(F_CWF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        return 0;
    }
}