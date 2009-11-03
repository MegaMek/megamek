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
public class CLLargeChemicalLaser extends CLChemicalLaserWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 322396740172378519L;

    public CLLargeChemicalLaser() {
        name = "Large Chem Laser";
        setInternalName("CLLargeChemicalLaser");
        setInternalName("CLLargeChemLaser");
        heat = 6;
        rackSize = 1;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        waterShortRange = 4;
        waterMediumRange = 9;
        waterLongRange = 14;
        waterExtremeRange = 16;
        tonnage = 5.0f;
        criticals = 2;
        bv = 99;
        cost = 750000;
        shortAV = 16;
        medAV = 16;
        maxRange = RANGE_MED;
    }
}
