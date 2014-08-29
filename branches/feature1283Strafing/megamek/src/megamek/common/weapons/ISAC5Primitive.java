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
import megamek.common.EquipmentType;
import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class ISAC5Primitive extends ACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 8826026540026351600L;

    public ISAC5Primitive() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "AC/5p";
        setInternalName("Autocannon/5 Primitive");
        addLookupName("IS Auto Cannon/5 Primitive");
        addLookupName("Auto Cannon/5 Primitive");
        addLookupName("AC/5p");
        addLookupName("AutoCannon/5 Primitive");
        addLookupName("ISAC5p");
        addLookupName("IS Autocannon/5 Primitive");
        ammoType = AmmoType.T_AC_PRIMITIVE;
        heat = 1;
        damage = 5;
        rackSize = 5;
        minimumRange = 3;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        tonnage = 8.0f;
        criticals = 4;
        bv = 70;
        cost = 125000;
        shortAV = 5;
        medAV = 5;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        //Per Blake Documents using normal weapon information and an introdate 10 years before the normal.
        availRating = new int[] { EquipmentType.RATING_C,
                EquipmentType.RATING_D, EquipmentType.RATING_D };
        introDate = 2450;
        techLevel.put(2450, techLevel.get(3071));
        techRating = RATING_C;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
                                              WeaponAttackAction waa, IGame game, Server server) {
        return new PrimitiveACWeaponHandler(toHit, waa, game, server);
    }
}
