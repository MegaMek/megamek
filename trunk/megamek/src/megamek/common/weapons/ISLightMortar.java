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
 * @author Sebastian Brocks
 */
public class ISLightMortar extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = -141763207003813118L;

    /**
     *
     */
    public ISLightMortar() {
        super();
        techLevel = TechConstants.T_IS_TW_NON_BOX;
        name = "Light Mortar";
        setInternalName(name);
        addLookupName("ISLightMortar");
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        ammoType = AmmoType.T_NA;
        minimumRange = 1;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        bv = 9;
        flags = flags.or(F_BALLISTIC);
    }

}
