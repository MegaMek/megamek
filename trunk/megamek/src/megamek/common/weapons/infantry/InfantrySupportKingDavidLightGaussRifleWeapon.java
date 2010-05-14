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
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Infantry;
import megamek.common.TechConstants;

/**
 * @author Ben Grills
 */
public class InfantrySupportKingDavidLightGaussRifleWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySupportKingDavidLightGaussRifleWeapon() {
        super();
        techLevel = TechConstants.T_IS_TW_NON_BOX;
        name = "Infantry King David Light Gauss Rifle";
        setInternalName(name);
        addLookupName("InfantryKingDavid");
        addLookupName("InfantryKingDavidLightGaussRifle");
        ammoType = AmmoType.T_NA;
        cost = 8000;
        bv = 4.12;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_SUPPORT);
        infantryDamage = 0.68;
        infantryRange = 3;
        crew = 2;
    }
}