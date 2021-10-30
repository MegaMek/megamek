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
package megamek.common.weapons.primitive;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.LRMHandler;
import megamek.common.weapons.lrms.LRMWeapon;
import megamek.server.Server;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class ISLRM15Primitive extends LRMWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -3387192703164511275L;

    public ISLRM15Primitive() {
        super();

        name = "Primitive Prototype LRM 15";
        setInternalName(name);
        addLookupName("IS LRM-15 Primitive");
        addLookupName("ISLRM15p");
        addLookupName("IS LRM 15 Primitive");
        this.shortName = "LRM/15 p";
        flags = flags.andNot(F_ARTEMIS_COMPATIBLE);
        heat = 5;
        rackSize = 15;
        minimumRange = 6;
        tonnage = 7.0;
        criticals = 3;
        bv = 136;
        cost = 175000;
        shortAV = 9;
        medAV = 9;
        longAV = 9;
        maxRange = RANGE_LONG;
        ammoType = AmmoType.T_LRM_PRIMITIVE;
        //IO Doesn't strictly define when these weapons stop production. Checked with Herb and they would always be around
        //This to cover some of the back worlds in the Periphery.
        rulesRefs = "118, IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
            .setIntroLevel(false)
            .setUnofficial(false)
            .setTechRating(RATING_C)
            .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
            .setISAdvancement(2295, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, false,false, false)
            .setPrototypeFactions(F_TA)
            .setProductionFactions(F_TA)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
                                              WeaponAttackAction waa, Game game, Server server) {
        return new LRMHandler(toHit, waa, game, server, -2);
    }
}
