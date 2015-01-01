/**
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006,2007 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Deric Page (deric.page@usa.net)
 */
public class ISLRM20Primitive extends LRMWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 1127913710413265729L;

    public ISLRM20Primitive() {
        super();
        //techLevel.put(3071, TechConstants.T_IS_UNOFFICIAL);
        name = "LRM 20p";
        setInternalName(name);
        addLookupName("IS LRM-20 Primitive");
        addLookupName("ISLRM20p");
        addLookupName("IS LRM 20 Primitive");
        heat = 6;
        rackSize = 20;
        minimumRange = 6;
        tonnage = 10.0f;
        criticals = 5;
        bv = 181;
        cost = 250000;
        shortAV = 12;
        medAV = 12;
        longAV = 12;
        maxRange = RANGE_LONG;
        ammoType = AmmoType.T_LRM_PRIMITIVE;
        //Per Blake Documents Intro Date is 10 years early, with same tech levels
        introDate = 2305;
        techLevel.put(2305, TechConstants.T_IS_UNOFFICIAL);
        availRating = new int[] { RATING_C, RATING_C, RATING_C };
        techRating = RATING_C;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new LRMHandler(toHit, waa, game, server, -2);
    }
}
