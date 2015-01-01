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

import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISUAC20 extends UACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -8297688910484314546L;

    /**
     * 
     */
    public ISUAC20() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Ultra AC/20";
        this.setInternalName("ISUltraAC20");
        this.addLookupName("IS Ultra AC/20");
        this.heat = 8;
        this.damage = 20;
        this.rackSize = 20;
        this.shortRange = 3;
        this.mediumRange = 7;
        this.longRange = 10;
        this.extremeRange = 14;
        this.tonnage = 15.0f;
        this.criticals = 10;
        this.bv = 281;
        this.cost = 480000;
        this.flags |= F_SPLITABLE;
        this.shortAV = 30;
        this.medAV = 30;
        this.maxRange = RANGE_MED;
        this.explosionDamage = damage;
    }
}
