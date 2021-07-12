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
import megamek.common.IGame;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.SRMHandler;
import megamek.common.weapons.srms.SRMWeapon;
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

        name = "Primitive Prototype SRM 2";
        setInternalName(name);
        addLookupName("IS SRM-2 Primitive");
        addLookupName("ISSRM2p");
        addLookupName("IS SRM 2 Primitive");
        this.shortName = "SRM/2p";
        flags = flags.andNot(F_ARTEMIS_COMPATIBLE);
        heat = 2;
        rackSize = 2;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 1.0;
        criticals = 1;
        bv = 21;
        flags = flags.or(F_NO_FIRES);
        cost = 10000;
        shortAV = 2;
        maxRange = RANGE_SHORT;
        ammoType = AmmoType.T_SRM_PRIMITIVE;
        //IO Doesn't strictly define when these weapons stop production. Checked with Herb and they would always be around
        //This to cover some of the back worlds in the Periphery.
        rulesRefs = "118, IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
            .setIntroLevel(false)
            .setUnofficial(false)
            .setTechRating(RATING_C)
            .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
            .setISAdvancement(2365, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, false,false, false)
            .setPrototypeFactions(F_TA)
            .setProductionFactions(F_TA)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
                                              WeaponAttackAction waa, IGame game, Server server) {
        return new SRMHandler(toHit, waa, game, server, -2);
    }
}
