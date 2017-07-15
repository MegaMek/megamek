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
import megamek.common.TechAdvancement;
import megamek.common.WeaponType;
import megamek.common.weapons.Weapon;

/**
 * @author Andrew Hunter
 */
public class ISBARecoillessRifleMedium extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = -2795333414856085616L;

    /**
     *
     */
    public ISBARecoillessRifleMedium() {
        super();
        name = "Medium Recoilless Rifle";
        setInternalName("ISBAMediumRecoillessRifle");
        addLookupName("IS BA Medium Recoilless Rifle");
        addLookupName("ISBAMedium Recoilless Rifle");
        addLookupName("ISBAMediumRecoillessRifle");
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        ammoType = AmmoType.T_NA;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        bv = 19;
        tonnage = 0.25;
        cost = 3000;
        criticals = 2;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC)
                .or(F_BA_WEAPON).or(F_BURST_FIRE);
        cost = 3000;
        rulesRefs = "268,TM";

        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(3047, 3054, 3056);
        techAdvancement.setTechRating(RATING_C);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_X, RATING_D, RATING_D });
    }

}

//Commented out in Weapontype. Clan version is same stats as IS one. And Clan versions captures Tech progression for both.