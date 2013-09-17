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
 * @author Jason Tighe
 */
public class CLProtoMechAC2 extends ProtoMechACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 4371171653960292873L;

    /**
     *
     */
    public CLProtoMechAC2() {
        super();
        
        name = "ProtoMech AC/2";
        setInternalName("CLProtoMechAC2");
        addLookupName("Clan ProtoMech AC/2");
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 0;
        shortRange = 7;
        mediumRange = 14;
        longRange = 20;
        extremeRange = 28;
        tonnage = 3.5f;
        criticals = 2;
        bv = 34;
        cost = 95000;
        shortAV = 7;
        medAV = 7;
        longAV = 7;
        maxRange = RANGE_LONG;
        explosionDamage = damage;
        techRating = RATING_F;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3070;
        techLevel.put(3070, techLevel.get(3071));
        techLevel.put(3070, TechConstants.T_CLAN_ADVANCED);
        techLevel.put(3073, TechConstants.T_CLAN_TW);
    }
}
