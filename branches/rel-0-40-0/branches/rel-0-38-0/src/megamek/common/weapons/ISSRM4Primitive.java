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
public class ISSRM4Primitive extends SRMWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 4581498300717143202L;

    public ISSRM4Primitive() {
        super();
        this.techLevel.put(3071, TechConstants.T_IS_UNOFFICIAL);
        this.name = "SRM 4p";
        this.setInternalName(this.name);
        this.addLookupName("IS SRM-4 Primitive");
        this.addLookupName("ISSRM4p");
        this.addLookupName("IS SRM 4 Primitive");
        this.heat = 3;
        this.rackSize = 4;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 39;
        this.cost = 60000;
        this.shortAV = 4;
        this.maxRange = RANGE_SHORT;
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
