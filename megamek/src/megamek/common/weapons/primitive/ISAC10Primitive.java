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
public class ISAC10Primitive extends ACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 4614699958561953757L;

    public ISAC10Primitive() {
        super();

        name = "Primitive Prototype Autocannon/10";
        setInternalName("Autocannon/10 Primitive");
        addLookupName("IS Auto Cannon/10 Primitive");
        addLookupName("Auto Cannon/10 Primitive");
        addLookupName("AutoCannon/10 Primitive");
        addLookupName("AC/10p");
        addLookupName("ISAC10p");
        addLookupName("IS Autocannon/10 Primitive");
        ammoType = AmmoType.T_AC_PRIMITIVE;
        heat = 3;
        damage = 10;
        rackSize = 10;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 12.0f;
        criticals = 7;
        bv = 123;
        cost = 200000;
        shortAV = 10;
        medAV = 10;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        //IO Doesn't strictly define when these weapons stop production so assigning a value of ten years.
        rulesRefs = "118, IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
        .setTechRating(RATING_C)
        .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
        .setISAdvancement(2443, DATE_NONE, DATE_NONE, 2460, DATE_NONE)
        .setISApproximate(true, false, false, true, false)
        .setPrototypeFactions(F_TH)
        .setProductionFactions(F_TH);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
                                              WeaponAttackAction waa, IGame game, Server server) {
        return new PrimitiveACWeaponHandler(toHit, waa, game, server);
    }
}
