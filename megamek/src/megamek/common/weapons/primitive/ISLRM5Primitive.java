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
import megamek.common.weapons.LRMHandler;
import megamek.common.weapons.lrms.LRMWeapon;
import megamek.server.Server;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class ISLRM5Primitive extends LRMWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 176095314320974740L;

    public ISLRM5Primitive() {
        super();

        name = "Primitive Prototype LRM 5";
        setInternalName(name);
        addLookupName("IS LRM-5 Primitive");
        addLookupName("ISLRM5p");
        addLookupName("IS LRM 5 Primitive");
        heat = 2;
        rackSize = 5;
        minimumRange = 6;
        tonnage = 2.0f;
        criticals = 1;
        bv = 45;
        cost = 30000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        maxRange = RANGE_LONG;
        ammoType = AmmoType.T_LRM_PRIMITIVE;
        //IO Doesn't strictly define when these weapons stop production so assigning a value of ten years.
        rulesRefs = "217, IO";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(2295, DATE_NONE, DATE_NONE, 2320);
        techAdvancement.setTechRating(RATING_C);
        techAdvancement.setAvailability( new int[] { RATING_F, RATING_X, RATING_X, RATING_X });
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
                                              WeaponAttackAction waa, IGame game, Server server) {
        return new LRMHandler(toHit, waa, game, server, -2);
    }
}
