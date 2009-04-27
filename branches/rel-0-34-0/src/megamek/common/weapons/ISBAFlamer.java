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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISBAFlamer extends FlamerWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 1414639280093120062L;

    /**
     * 
     */
    public ISBAFlamer() {
        super();
        this.techLevel = TechConstants.T_INTRO_BOXSET;
        this.name = "Flamer";
        this.setInternalName("IS BA Flamer");
        this.addLookupName("ISBAFlamer");
        this.heat = 3;
        this.damage = 2;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.tonnage = 1f;
        this.criticals = 1;
        this.bv = 5;
        this.cost = 7500;
        this.shortAV = 2;
        this.maxRange = RANGE_SHORT;
        this.flags |= F_BA_WEAPON;
    }
}
