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
 * @author Sebastian Brocks
 */
public class CLBALightMG extends BAMGWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 1810341654439496432L;

    /**
     * 
     */
    public CLBALightMG() {
        super();
        this.techLevel.put(3071, TechConstants.T_CLAN_TW);
        this.name = "Light Machine Gun";
        this.setInternalName("CLBALightMG");
        this.addLookupName("Clan BA Light Machine Gun");
        this.heat = 0;
        this.damage = 1;
        this.infDamageClass = WeaponType.WEAPON_BURST_HALFD6;
        this.rackSize = 1;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.tonnage = 0.25f;
        this.criticals = 1;
        this.bv = 5;
        this.cost = 5000;
        introDate = 3060;
        techLevel.put(3060, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_C };
        techRating = RATING_C;
    }

}
