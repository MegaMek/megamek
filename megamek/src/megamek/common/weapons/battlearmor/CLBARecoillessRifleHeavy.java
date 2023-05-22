/*
 * Copyright (c) 2004-2005 - Ben Mazur (bmazur@sev.org).
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

import megamek.common.AmmoType;
import megamek.common.WeaponType;
import megamek.common.weapons.Weapon;

/**
 * @author Andrew Hunter
 * @since Sep 24, 2004
 */
public class CLBARecoillessRifleHeavy extends Weapon {
    private static final long serialVersionUID = -4900393422594499114L;

    public CLBARecoillessRifleHeavy() {
        super();
        name = "BA Recoilless Rifle (Heavy)";
        setInternalName("CLBAHeavy Recoilless Rifle");
        addLookupName("CLBAHeavy Recoilless Rifle");
        addLookupName("CLBAHeavyRecoillessRifle");
        addLookupName("ISBAHeavyRecoillessRifle");
        addLookupName("ISHeavy Recoilless Rifle");
        addLookupName("ISBAHeavy Recoilless Rifle");
        sortingName = "Recoilless Rifle D";
        shortName = "Heavy Recoiless Rifle";
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        ammoType = AmmoType.T_NA;
        shortRange = 3;
        mediumRange = 5;
        longRange = 7;
        extremeRange = 10;
        bv = 22;
        tonnage = 0.325;
        criticals = 3;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_BA_WEAPON).or(F_BURST_FIRE).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        cost = 5000;
        rulesRefs = "268, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setISAdvancement(3052, 3054, 3056, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3062, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC);
    }
}
