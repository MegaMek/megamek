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
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLHAG20 extends HAGWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -1150472287591805766L;

    public CLHAG20() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "HAG/20";
        this.setInternalName("CLHAG20");
        this.addLookupName("Clan HAG/20");
        this.heat = 4;
        this.rackSize = 20;
        this.minimumRange = 2;
        this.shortRange = 8;
        this.mediumRange = 16;
        this.longRange = 24;
        this.extremeRange = 32;
        this.tonnage = 10.0f;
        this.criticals = 6;
        this.bv = 267;
        this.cost = 480000;
        this.shortAV = 16;
        this.medAV = 12;
        this.longAV = 12;
        this.maxRange = RANGE_LONG;
        this.explosionDamage = rackSize/2;

    }

}
