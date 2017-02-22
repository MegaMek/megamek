/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.BombType;
import megamek.common.TechConstants;
import megamek.common.TechProgression;

/**
 * @author Jay Lawson
 */
public class ISASEWMissileWeapon extends CapitalMissileWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -2094737986722961212L;

    public ISASEWMissileWeapon() {
        super();

        this.name = "ASEW Missile";
        this.setInternalName("IS " + BombType.getBombWeaponName(BombType.B_ASEW));
        this.heat = 0;
        this.damage = 0;
        this.rackSize = 1;
        this.minimumRange = 7;
        this.shortRange = 14;
        this.mediumRange = 21;
        this.longRange = 28;
        this.extremeRange = 42;
        this.tonnage = 2;
        this.criticals = 0;
        this.hittable = false;
        this.bv = 0;
        this.cost = 20000;
        this.shortAV = 0;
        this.medAV = 0;
        this.longAV = 0;
        this.maxRange = RANGE_MED;
        this.ammoType = AmmoType.T_ASEW_MISSILE;
        this.capital = false;
        introDate = 3067;
        techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3073, TechConstants.T_IS_ADVANCED);
        availRating = new int[] { RATING_X ,RATING_X ,RATING_E ,RATING_E};
        techRating = RATING_E;
        rulesRefs = "358, TO";
        techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        techProgression.setISProgression(3067, 3073, DATE_NONE);
        techProgression.setTechRating(RATING_E);
        techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_E, RATING_E });
    }
}
