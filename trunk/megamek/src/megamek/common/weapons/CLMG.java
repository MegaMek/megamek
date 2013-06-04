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
import megamek.common.WeaponType;

/**
 * @author Andrew Hunter
 */
public class CLMG extends MGWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 2557643305248678454L;

    /**
     * 
     */
    public CLMG() {
        super();
        this.techLevel.put(3071, TechConstants.T_CLAN_TW);
        this.name = "Machine Gun";
        this.setInternalName("CLMG");
        this.addLookupName("Clan Machine Gun");
        this.heat = 0;
        this.damage = 2;
        this.infDamageClass = WeaponType.WEAPON_BURST_2D6;
        this.rackSize = 2;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.tonnage = 0.25f;
        this.criticals = 1;
        this.bv = 5;
        this.cost = 5000;
        this.shortAV = 2;
        this.maxRange = RANGE_SHORT;
        introDate = 2825;
        techLevel.put(2825, techLevel.get(3071));
        availRating = new int[] { RATING_A, RATING_A, RATING_B };
        techRating = RATING_B;

    }

}
