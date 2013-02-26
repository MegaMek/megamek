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
 * Created on Oct 2, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISTHBUAC20 extends UACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -5199793409417838142L;

    /**
     *
     */
    public ISTHBUAC20() {
        super();
        techLevel = TechConstants.T_IS_UNOFFICIAL;
        name = "Ultra AC/20 (THB)";
        setInternalName("ISUltraAC20 (THB)");
        addLookupName("IS Ultra AC/20 (THB)");
        heat = 10;
        damage = 20;
        rackSize = 20;
        ammoType = AmmoType.T_AC_ULTRA_THB;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 15.0f;
        criticals = 11;
        bv = 335;
        cost = 600000;
    }
}
