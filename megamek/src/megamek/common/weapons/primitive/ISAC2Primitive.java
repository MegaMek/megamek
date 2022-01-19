/*
 * MegaMek -
 * Copyright (C) 2000-2007 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.primitive;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PrimitiveACWeaponHandler;
import megamek.common.weapons.autocannons.ACWeapon;
import megamek.server.Server;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class ISAC2Primitive extends ACWeapon {
    private static final long serialVersionUID = 3540956033197287540L;

    public ISAC2Primitive() {
        super();

        name = "Primitive Prototype Autocannon/2";
        setInternalName("Autocannon/2p");
        addLookupName("IS Auto Cannon/2 Primitive");
        addLookupName("Auto Cannon/2 Primitive");
        addLookupName("AutoCannon/2 Primitive");
        addLookupName("AC/2p");
        addLookupName("ISAC2p");
        addLookupName("IS Autocannon/2 Primitive");
        sortingName = "Primitive Prototype Autocannon/02";
        shortName = "AC/2p";
        ammoType = AmmoType.T_AC_PRIMITIVE;
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 4;
        shortRange = 8;
        mediumRange = 16;
        longRange = 24;
        extremeRange = 32;
        tonnage = 6.0;
        criticals = 1;
        bv = 37;
        cost = 75000;
        explosive = true; // when firing incendiary ammo
        shortAV = 2;
        medAV = 2;
        longAV = 2;
        extAV = 2;
        maxRange = RANGE_LONG;
        flags = flags.or(F_PROTOTYPE);
        explosionDamage = damage;
        // IO Doesn't strictly define when these weapons stop production. Checked with Herb, and
        // they would always be around. This to cover some of the back worlds in the Periphery.
        rulesRefs = "118, IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2290, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              Server server) {
        return new PrimitiveACWeaponHandler(toHit, waa, game, server);
    }
}
