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
public class CLHAG40 extends HAGWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -8369909187223849480L;

    public CLHAG40() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "HAG/40";
        this.setInternalName("CLHAG40");
        this.addLookupName("Clan HAG/40");
        this.heat = 8;
        this.rackSize = 40;
        this.minimumRange = 2;
        this.shortRange = 8;
        this.mediumRange = 16;
        this.longRange = 24;
        this.extremeRange = 32;
        this.tonnage = 16.0f;
        this.criticals = 10;
        this.bv = 535;
        this.flags |= F_SPLITABLE;
        this.cost = 480000;
        this.shortAV = 32;
        this.medAV = 24;
        this.longAV = 24;
        this.maxRange = RANGE_LONG;
        this.explosionDamage = rackSize/2;
    }

}
