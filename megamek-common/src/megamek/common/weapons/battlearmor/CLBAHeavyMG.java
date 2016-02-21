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
public class CLBAHeavyMG extends BAMGWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 7184744610192773285L;

    /**
     *
     */
    public CLBAHeavyMG() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "Heavy Machine Gun";
        setInternalName("CLBAHeavyMG");
        addLookupName("Clan BA Heavy Machine Gun");
        heat = 0;
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        rackSize = 3;
        shortRange = 1;
        mediumRange = 2;
        longRange = 2;
        extremeRange = 2;
        tonnage = 0.15f;
        criticals = 1;
        bv = 6;
        cost = 7500;
        introDate = 3059;
        techLevel.put(3059, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_C };
        techRating = RATING_C;
    }

}
