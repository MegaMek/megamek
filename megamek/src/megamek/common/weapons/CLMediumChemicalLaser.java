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
public class CLMediumChemicalLaser extends CLChemicalLaserWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 322396740172378519L;

    public CLMediumChemicalLaser() {
        name = "Medium Chem Laser";
        setInternalName("CLMediumChemicalLaser");
        setInternalName("CLMediumChemLaser");
        heat = 2;
        rackSize = 5;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 3;
        waterExtremeRange = 3;
        tonnage = 1.0f;
        criticals = 1;
        bv = 37;
        cost = 300000;
        shortAV = 7;
        maxRange = RANGE_SHORT;
    }
}
