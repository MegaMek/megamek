/* MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons.capitalweapons;

import megamek.common.TechAdvancement;

/**
 * @author Jay Lawson
 */
public class CLNL55Weapon extends NavalLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public CLNL55Weapon() {
        super();
        this.name = "Naval Laser 55 (Clan)";
        this.setInternalName(this.name);
        this.addLookupName("CLNL55");
        this.heat = 85;
        this.damage = 5;
        this.shortRange = 13;
        this.mediumRange = 26;
        this.longRange = 39;
        this.extremeRange = 52;
        this.tonnage = 1100.0f;
        this.bv = 1386;
        this.cost = 1250000;
        this.shortAV = 5.5;
        this.medAV = 5.5;
        this.longAV = 5.5;
        this.extAV = 5.5;
        this.maxRange = RANGE_EXT;
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(DATE_NONE, 2820, DATE_NONE);
        techAdvancement.setTechRating(RATING_D);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_D, RATING_C, RATING_X });
    }
}
//Commented out in Weapontype. Tech progression in IS weapon covers these.