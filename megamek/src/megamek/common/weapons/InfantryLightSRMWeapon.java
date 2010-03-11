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
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class InfantryLightSRMWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -5311681183178942222L;

    public InfantryLightSRMWeapon() {
        super();
        techLevel = TechConstants.T_ALLOWED_ALL;
        name = "Light SRM Launcher";
        setInternalName(name);
        addLookupName("InfantrySRMLight");
        addLookupName("InfantrySRM");
        ammoType = AmmoType.T_SRM;
        cost = 1500;
        bv = 1.71;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_MISSILE);
        infantryDamage = 0.41;
        infantryRange = 2;
    }
}