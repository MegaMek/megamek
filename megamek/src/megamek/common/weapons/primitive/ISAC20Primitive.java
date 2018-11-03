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
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PrimitiveACWeaponHandler;
import megamek.common.weapons.autocannons.ACWeapon;
import megamek.server.Server;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class ISAC20Primitive extends ACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 4703572491922950865L;

    public ISAC20Primitive() {
        super();

        name = "Primitive Prototype Autocannon/20";
        setInternalName("Autocannon/20 Primitive");
        addLookupName("IS Auto Cannon/20 Primitive");
        addLookupName("Auto Cannon/20 Primitive");
        addLookupName("AutoCannon/20 Primitive");
        addLookupName("ISAC20p");
        addLookupName("IS Autocannon/20 Primitive");
        ammoType = AmmoType.T_AC_PRIMITIVE;
        heat = 7;
        damage = 20;
        rackSize = 20;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 14.0;
        criticals = 10;
        bv = 178;
        cost = 300000;
        shortAV = 20;
        maxRange = RANGE_SHORT;
        explosionDamage = damage;
        //IO Doesn't strictly define when these weapons stop production. Checked with Herb and they would always be around
        //This to cover some of the back worlds in the Periphery.
        rulesRefs = "118, IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
            .setIntroLevel(false)
            .setUnofficial(false)
            .setTechRating(RATING_C)
            .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
            .setISAdvancement(2488, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(true, false, false,false, false)
            .setPrototypeFactions(F_TA)
            .setProductionFactions(F_TA);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
                                              WeaponAttackAction waa, IGame game, Server server) {
        return new PrimitiveACWeaponHandler(toHit, waa, game, server);
    }
}
