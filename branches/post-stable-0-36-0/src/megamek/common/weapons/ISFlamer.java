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
import megamek.common.WeaponType;

/**
 * @author Andrew Hunter
 */
public class ISFlamer extends FlamerWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 1414639280093120062L;

    /**
     * 
     */
    public ISFlamer() {
        super();
        this.techLevel.put(3071, TechConstants.T_INTRO_BOXSET);
        this.name = "Flamer";
        this.setInternalName(this.name);
        this.addLookupName("IS Flamer");
        this.addLookupName("ISFlamer");
        this.heat = 3;
        this.damage = 2;
        this.infDamageClass = WeaponType.WEAPON_BURST_4D6;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.tonnage = 1f;
        this.criticals = 1;
        this.bv = 6;
        this.cost = 7500;
        this.shortAV = 2;
        this.maxRange = RANGE_SHORT;
        introDate = 2025;
        techLevel.put(2025, techLevel.get(3071));
        availRating = new int[] { RATING_B, RATING_B, RATING_B };
        techRating = RATING_C;
    }
}
