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
 * @author Sebastian Brocks
 */
public class ISBAFiredrakeNeedler extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = -8852176757815947141L;

    /**
     *
     */
    public ISBAFiredrakeNeedler() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "Firedrake Needler";
        setInternalName(name);
        addLookupName("ISFiredrakeIncendiaryNeedler");
        damage = 1;
        infDamageClass = WeaponType.WEAPON_BURST_3D6;
        ammoType = AmmoType.T_NA;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        bv = 2;
        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC)
                .or(F_INCENDIARY_NEEDLES).or(F_BURST_FIRE).or(F_BA_WEAPON);
    }

}
