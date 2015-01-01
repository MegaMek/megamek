/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Oct 15, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class CLLB5XAC extends LBXACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 722040764690180243L;

    /**
     * 
     */
    public CLLB5XAC() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "LB 5-X AC";
        this.setInternalName("CLLBXAC5");
        this.addLookupName("Clan LB 5-X AC");
        this.heat = 1;
        this.damage = 5;
        this.rackSize = 5;
        this.minimumRange = 3;
        this.shortRange = 8;
        this.mediumRange = 15;
        this.longRange = 24;
        this.extremeRange = 30;
        this.tonnage = 7.0f;
        this.criticals = 4;
        this.bv = 93;
        this.cost = 250000;
        this.shortAV = 5;
        this.medAV = 5;
        this.longAV = 5;
        this.maxRange = RANGE_LONG;
    }
}
