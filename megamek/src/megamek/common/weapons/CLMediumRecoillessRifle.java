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
public class CLMediumRecoillessRifle extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = -8244299318168866609L;

    /**
     *
     */
    public CLMediumRecoillessRifle() {
        super();
        techLevel.put(3071,TechConstants.T_CLAN_TW);
        name = "Medium Recoilless Rifle";
        setInternalName("CLMedium Recoilless Rifle");
        addLookupName("CLMedium Recoilless Rifle");
        heat = 0;
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        ammoType = AmmoType.T_NA;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        tonnage = 0.0f;
        criticals = 0;
        bv = 19;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC)
                .or(F_BA_WEAPON).or(F_BURST_FIRE);
        cost = 3000;
        introDate = 1950;
        techLevel.put(1950,techLevel.get(3071));
        availRating = new int[]{RATING_X,RATING_X,RATING_A};
        techRating = RATING_C;
        
    }

}
