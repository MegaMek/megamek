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
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class ISSRM2Primitive extends SRMWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 8488734998051278663L;

    public ISSRM2Primitive() {
        super();
        techLevel.put(3071, TechConstants.T_IS_UNOFFICIAL);
        name = "SRM 2p";
        setInternalName(name);
        addLookupName("IS SRM-2 Primitive");
        addLookupName("ISSRM2p");
        addLookupName("IS SRM 2 Primitive");
        heat = 2;
        rackSize = 2;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 1.0f;
        criticals = 1;
        bv = 21;
        flags = flags.or(F_NO_FIRES);
        cost = 10000;
        shortAV = 2;
        maxRange = RANGE_SHORT;
        ammoType = AmmoType.T_SRM_PRIMITIVE;
        //Per Blake Documents Intro Date is 10 years early, with same tech levels
        introDate = 2360;
        techLevel.put(2360, TechConstants.T_IS_UNOFFICIAL);
        availRating = new int[] { RATING_C, RATING_C, RATING_C };
        techRating = RATING_C;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
                                              WeaponAttackAction waa, IGame game, Server server) {
        return new SRMHandler(toHit, waa, game, server, -2);
    }
}
