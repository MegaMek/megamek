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
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;
import megamek.common.WeaponType;

/**
 * @author Andrew Hunter
 */
public class ISHeavyRecoillessRifle extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = 1717246683401964562L;

    /**
     *
     */
    public ISHeavyRecoillessRifle() {
        super();
        techLevel.put(3071,TechConstants.T_IS_TW_NON_BOX);
        name = "Heavy Recoilless Rifle";
        setInternalName(name);
        addLookupName("ISHeavy Recoilless Rifle");
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        ammoType = AmmoType.T_NA;
        shortRange = 3;
        mediumRange = 5;
        longRange = 7;
        extremeRange = 10;
        bv = 22;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_BA_WEAPON)
                .or(F_BURST_FIRE);
        cost = 5000;
        introDate = 1950;
        techLevel.put(1950,techLevel.get(3071));
        availRating = new int[]{RATING_X,RATING_X,RATING_A};
        techRating = RATING_C;
    }

}
