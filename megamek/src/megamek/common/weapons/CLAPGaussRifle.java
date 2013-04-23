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
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import megamek.common.WeaponType;

/**
 * @author Sebastian Brocks
 */
public class CLAPGaussRifle extends GaussWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 3055904827702262063L;

    /**
     *
     */
    public CLAPGaussRifle() {
        super();
        techLevel = TechConstants.T_CLAN_TW;
        name = "AP Gauss Rifle";
        setInternalName("CLAPGaussRifle");
        addLookupName("Clan AP Gauss Rifle");
        heat = 1;
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        ammoType = AmmoType.T_APGAUSS;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 0.5f;
        criticals = 1;
        bv = 21;
        flags = flags.or(F_BURST_FIRE);
        cost = 8500;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        explosionDamage = 3;
        availRating = new int[]{RATING_X,RATING_X,RATING_E};
        this.introDate = 3069;
        techRating = RATING_F;
    }

}
