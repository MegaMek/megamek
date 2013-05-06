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
public class ISLRM10Primitive extends LRMWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 6236976752458107991L;

    public ISLRM10Primitive() {
        super();
        techLevel.put(3071,TechConstants.T_IS_UNOFFICIAL);
        name = "LRM 10p";
        setInternalName(name);
        addLookupName("IS LRM-10 Primitive");
        addLookupName("ISLRM10p");
        addLookupName("IS LRM 10 Primitive");
        heat = 4;
        rackSize = 10;
        minimumRange = 6;
        tonnage = 5.0f;
        criticals = 2;
        bv = 90;
        cost = 100000;
        shortAV = 6;
        medAV = 6;
        longAV = 6;
        maxRange = RANGE_LONG;
        ammoType = AmmoType.T_LRM_PRIMITIVE;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, IGame game, Server server) {
        return new LRMHandler(toHit, waa, game, server, -2);
    }
}
