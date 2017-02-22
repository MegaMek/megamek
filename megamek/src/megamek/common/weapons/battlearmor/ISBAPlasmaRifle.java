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
import megamek.common.TechProgression;
import megamek.common.WeaponType;
import megamek.common.weapons.Weapon;

/**
 * @author Sebastian Brocks
 */
public class ISBAPlasmaRifle extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = 4885473724392214253L;

    /**
     *
     */
    public ISBAPlasmaRifle() {
        super();
        name = "Plasma Rifle";
        setInternalName("ISBAPlasmaRifle");
        addLookupName("IS BA Plasma Rifle");
        damage = 2;
        baDamageClass = WeaponType.WEAPON_PLASMA;
        ammoType = AmmoType.T_NA;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        bv = 12;
        tonnage = 0.3f;
        criticals = 2;
        cost = 28000;
        flags = flags.or(F_BA_WEAPON).or(F_DIRECT_FIRE).or(F_PLASMA).or(F_ENERGY).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        introDate = 3058;
        techLevel.put(3058, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3065, TechConstants.T_IS_ADVANCED);
        techLevel.put(3074, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X ,RATING_X ,RATING_D ,RATING_C};
        techRating = RATING_E;
        rulesRefs = "267, TM";
        techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        techProgression.setISProgression(3058, 3065, 3074);
        techProgression.setTechRating(RATING_E);
        techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_D, RATING_C });
    }
}
