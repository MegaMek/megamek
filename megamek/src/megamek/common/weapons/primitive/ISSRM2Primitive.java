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
import megamek.common.TechAdvancement;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.SRMHandler;
import megamek.common.weapons.SRMWeapon;
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
        //IO Doesn't strictly define when these weapons stop production so assigning a value of ten years.
        rulesRefs = "217, IO";
  
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(2439, DATE_NONE, DATE_NONE, 2470);
        techAdvancement.setTechRating(RATING_C);
        techAdvancement.setAvailability( new int[] { RATING_F, RATING_X, RATING_X, RATING_X });
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
                                              WeaponAttackAction waa, IGame game, Server server) {
        return new SRMHandler(toHit, waa, game, server, -2);
    }
}
