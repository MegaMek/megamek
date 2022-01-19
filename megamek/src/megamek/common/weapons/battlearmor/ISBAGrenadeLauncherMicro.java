/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.battlearmor;

import megamek.common.WeaponType;
import megamek.common.weapons.Weapon;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class ISBAGrenadeLauncherMicro extends Weapon {
    private static final long serialVersionUID = 5856065014622975919L;

    public ISBAGrenadeLauncherMicro() {
        super();
        name = "Grenade Launcher (Micro)";
        setInternalName("ISBAMicroGrenadeLauncher");
        addLookupName("IS BA Micro Grenade Launcher");
        addLookupName("CL BA Micro Grenade Launcher");
        addLookupName("CLBAMicroGrenadeLauncher");
        sortingName = "Grenade Launcher A";
        heat = 0;
        damage = 1;
        infDamageClass = WeaponType.WEAPON_BURST_HALFD6;
        minimumRange = WEAPON_NA;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        tonnage = 0.075;
        criticals = 1;
        bv = 1;
        cost = 1950;
        flags = flags.or(F_BALLISTIC).or(F_BA_WEAPON).or(F_BURST_FIRE).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "256, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_ES, DATE_ES, 3050, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, 3050, DATE_NONE, DATE_NONE);
    }
}
