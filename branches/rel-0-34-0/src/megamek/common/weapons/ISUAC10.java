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
 * Created on Oct 1, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISUAC10 extends UACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 6937673199956551674L;

    /**
     * 
     */
    public ISUAC10() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Ultra AC/10";
        this.setInternalName("ISUltraAC10");
        this.addLookupName("IS Ultra AC/10");
        this.heat = 4;
        this.damage = 10;
        this.rackSize = 10;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.tonnage = 13.0f;
        this.criticals = 7;
        this.bv = 210;
        this.cost = 320000;
        this.shortAV = 15;
        this.medAV = 15;
        this.maxRange = RANGE_MED;
        this.explosionDamage = damage;
    }
}
