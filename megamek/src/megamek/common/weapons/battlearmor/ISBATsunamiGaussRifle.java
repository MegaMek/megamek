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
import megamek.common.weapons.Weapon;

/**
 * @author Sebastian Brocks
 */
public class ISBATsunamiGaussRifle extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = -4179313979730970060L;

    /**
     *
     */
    public ISBATsunamiGaussRifle() {
        super();
        name = "Gauss Rifle [Tsunami]";
        setInternalName("ISBATsunamiHeavyGaussRifle");
        addLookupName("BA-ISTsunamiHeavyGaussRifle");
        addLookupName("IS BA Tsunami Heavy Gauss Rifle");
        heat = 0;
        damage = 1;
        ammoType = AmmoType.T_NA;
        shortRange = 2;
        mediumRange = 4;
        longRange = 5;
        extremeRange = 8;
        tonnage = 0.125f;
        criticals = 2;
        cost = 9500;
        bv = 6;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        introDate = 3049;
        techLevel.put(3049, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3056, TechConstants.T_IS_ADVANCED);
        techLevel.put(3058, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X ,RATING_X ,RATING_E ,RATING_E};
        techRating = RATING_E;
        rulesRefs = "255, TM";
        techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        techProgression.setISProgression(3049, 3056, 3058);
        techProgression.setTechRating(RATING_E);
        techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_E, RATING_E });
    }
}
