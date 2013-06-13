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
 * Created on May 29, 2004
 *
 */
package megamek.common.weapons;

/**
 * @author Jason Tighe
 */
public class CLSmallChemicalLaser extends CLChemicalLaserWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 322396740172378519L;

    public CLSmallChemicalLaser() {
        name = "Small Chem Laser";
        setInternalName("CLSmallChemicalLaser");
        setInternalName("CLSmallChemLaser");
        heat = 1;
        rackSize = 3;
        damage = 3;
        minimumRange = WEAPON_NA;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 2;
        waterExtremeRange = 2;
        tonnage = 0.5f;
        criticals = 1;
        bv = 7;
        cost = 10000;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        atClass = CLASS_LASER;
        techRating = RATING_E;
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        introDate = 3059;
        techLevel.put(3059, techLevel.get(3071));
    }

}
