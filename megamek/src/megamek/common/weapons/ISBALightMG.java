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
package megamek.common.weapons;

import megamek.common.TechConstants;

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
        techLevel = TechConstants.T_IS_TW_NON_BOX;
        name = "Light Machine Gun";
        setInternalName("BA Light Machine Gun");
        addLookupName("IS BA Light Machine Gun");
        addLookupName("ISBALightMG");
        heat = 0;
        damage = 1;
        rackSize = 1;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 4;
        tonnage = 0.5f;
        criticals = 1;
        bv = 5;
        cost = 5000;
    }

}
