/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Oct 2, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Jason Tighe
 */
public class CLProtoMechAC4 extends ACWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 4371171653960292873L;

    /**
     *
     */
    public CLProtoMechAC4() {
        super();
        techLevel = TechConstants.T_CLAN_ADVANCED;
        name = "ProtoMech AC/4";
        setInternalName("CLProtoMechAC4");
        addLookupName("Clan ProtoMech AC/4");
        heat = 1;
        damage = 4;
        rackSize = 4;
        minimumRange = 0;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 4.5f;
        criticals = 3;
        bv = 49;
        cost = 133000;
        shortAV = 7;
        medAV = 7;
        longAV = 7;
        maxRange = RANGE_MED;
        explosionDamage = damage;
    }
}
