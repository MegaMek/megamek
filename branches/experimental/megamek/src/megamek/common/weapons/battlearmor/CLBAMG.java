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
package megamek.common.weapons.battlearmor;

import megamek.common.TechConstants;
import megamek.common.WeaponType;

/**
 * @author Sebastian Brocks
 */
public class CLBAMG extends BAMGWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -5021714235121936669L;

    /**
     * 
     */
    public CLBAMG() {
        super();
        this.techLevel.put(3071, TechConstants.T_CLAN_TW);
        this.name = "Machine Gun";
        this.setInternalName("CLBAMG");
        this.addLookupName("Clan BA Machine Gun");
        this.heat = 0;
        this.damage = 2;
        this.infDamageClass = WeaponType.WEAPON_BURST_1D6;
        this.rackSize = 2;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.tonnage = 0.1f;
        this.criticals = 1;
        this.bv = 5;
        this.cost = 5000;
        flags = flags.or(F_BA_WEAPON);
        introDate = 1950;
        techLevel.put(1950, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_B };
        techRating = RATING_C;
    }

}
