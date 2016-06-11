/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons.battlearmor;

import megamek.common.AmmoType;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.weapons.Weapon;

/**
 * @author Andrew Hunter
 */
public class ISBAHeavyRecoillessRifle extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = 1717246683401964562L;

    /**
     *
     */
    public ISBAHeavyRecoillessRifle() {
        super();

        name = "Heavy Recoilless Rifle";
        setInternalName("ISBAHeavyRecoillessRifle");
        addLookupName("ISHeavy Recoilless Rifle");
        addLookupName("ISBAHeavy Recoilless Rifle");
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        ammoType = AmmoType.T_NA;
        shortRange = 3;
        mediumRange = 5;
        longRange = 7;
        extremeRange = 10;
        bv = 22;
        tonnage = 0.325f;
        criticals = 3;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC)
                .or(F_BA_WEAPON).or(F_BURST_FIRE).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        cost = 5000;
        introDate = 3047;
        techLevel.put(3047, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3054, TechConstants.T_IS_ADVANCED);
        techLevel.put(3056, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X ,RATING_X ,RATING_D ,RATING_D};
        techRating = RATING_C;
        rulesRefs = "268, TM";

    }

}
