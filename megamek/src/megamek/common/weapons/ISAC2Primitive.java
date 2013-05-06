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
public class ISAC2Primitive extends ACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 3540956033197287540L;

    public ISAC2Primitive() {
        super();
        techLevel.put(3071,TechConstants.T_IS_EXPERIMENTAL);
        name = "AC/2p";
        setInternalName("Autocannon/2p");
        addLookupName("IS Auto Cannon/2 Primitive");
        addLookupName("Auto Cannon/2 Primitive");
        addLookupName("AutoCannon/2 Primitive");
        addLookupName("AC/2p");
        addLookupName("ISAC2p");
        addLookupName("IS Autocannon/2 Primitive");
        ammoType = AmmoType.T_AC_PRIMITIVE;
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 4;
        shortRange = 8;
        mediumRange = 16;
        longRange = 24;
        extremeRange = 32;
        tonnage = 6.0f;
        criticals = 1;
        bv = 37;
        cost = 75000;
        explosive = true; // when firing incendiary ammo
        shortAV = 2;
        medAV = 2;
        longAV = 2;
        extAV = 2;
        maxRange = RANGE_LONG;
        explosionDamage = damage;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, IGame game, Server server) {
        return new PrimitiveACWeaponHandler(toHit, waa, game, server);
    }
}
