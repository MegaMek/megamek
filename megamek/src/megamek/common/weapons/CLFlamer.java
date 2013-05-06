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
public class CLFlamer extends FlamerWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8782512971175525221L;

    /**
     * 
     */
    public CLFlamer() {
        super();
        this.techLevel.put(3071,TechConstants.T_CLAN_TW);
        this.name = "Flamer";
        this.setInternalName("CLFlamer");
        this.addLookupName("Clan Flamer");
        this.heat = 3;
        this.damage = 2;
        this.infDamageClass = WeaponType.WEAPON_BURST_4D6;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.tonnage = 0.5f;
        this.criticals = 1;
        this.bv = 6;
        this.cost = 7500;
        this.shortAV = 2;
        this.maxRange = RANGE_SHORT;
        availRating = new int[]{RATING_B,RATING_B,RATING_B};
        introDate = 2827;
        techLevel.put(2827,techLevel.get(3071));
        techRating = RATING_C;
    }
    
}
