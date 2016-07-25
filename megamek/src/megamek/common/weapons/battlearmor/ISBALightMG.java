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
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons.battlearmor;

import megamek.common.AmmoType;
import megamek.common.TechConstants;
import megamek.common.WeaponType;

/**
 * @author Sebastian Brocks
 */
public class ISBALightMG extends BAMGWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -1314457483959053741L;

    /**
     *
     */
    public ISBALightMG() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "Light Machine Gun";
        setInternalName("ISBALightMachineGun");
        addLookupName("IS BA Light Machine Gun");
        addLookupName("ISBALightMG");
        ammoType = AmmoType.T_NA;
        heat = 0;
        damage = 1;
        infDamageClass = WeaponType.WEAPON_BURST_HALFD6;
        rackSize = 1;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        tonnage = 0.075;
        criticals = 1;
        bv = 5;
        cost = 5000;
        introDate = 3068;
        techLevel.put(3068, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_C };
        techRating = RATING_C;
    }

}
