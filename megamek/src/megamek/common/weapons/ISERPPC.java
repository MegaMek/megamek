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
 * Created on Sep 13, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISERPPC extends PPCWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 7175778897598535734L;

    /**
     * 
     */
    public ISERPPC() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "ER PPC";
        this.setInternalName("ISERPPC");
        this.addLookupName("IS ER PPC");
        this.heat = 15;
        this.damage = 10;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 23;
        this.extremeRange = 28;
        this.waterShortRange = 4;
        this.waterMediumRange = 10;
        this.waterLongRange = 16;
        this.waterExtremeRange = 20;
        this.tonnage = 7.0f;
        this.criticals = 3;
        this.bv = 229;
        this.cost = 300000;
        this.shortAV = 10;
        this.medAV = 10;
        this.longAV = 10;
        this.maxRange = RANGE_LONG;
    }
}
