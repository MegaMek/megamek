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
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class ISAC20Primitive extends ACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 4703572491922950865L;

    public ISAC20Primitive() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "AC/20p";
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
        tonnage = 14.0f;
        criticals = 10;
        bv = 178;
        cost = 300000;
        shortAV = 20;
        maxRange = RANGE_SHORT;
        explosionDamage = damage;
        //Per Blake Documents using normal weapon information and an introdate 10 years before the normal.
        availRating = new int[] { EquipmentType.RATING_C,
                EquipmentType.RATING_D, EquipmentType.RATING_D };
        introDate = 2488;
        techLevel.put(2488, techLevel.get(3071));
        techRating = RATING_C;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
                                              WeaponAttackAction waa, IGame game, Server server) {
        return new PrimitiveACWeaponHandler(toHit, waa, game, server);
    }
}
