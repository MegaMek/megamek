/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;
import megamek.common.WeaponType;

/**
 * @author Sebastian Brocks
 */
public class CLVehicleFlamer extends VehicleFlamerWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 1550030929314637199L;

    /**
     * 
     */
    public CLVehicleFlamer() {
        super();
        this.techLevel.put(3071, TechConstants.T_CLAN_TW);
        this.name = "Vehicle Flamer";
        this.setInternalName("CLVehicleFlamer");
        this.addLookupName("Clan Vehicle Flamer");
        this.heat = 3;
        this.damage = 2;
        this.infDamageClass = WeaponType.WEAPON_BURST_4D6;
        this.rackSize = 2;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.tonnage = 0.5f;
        this.criticals = 1;
        this.bv = 5;
        this.cost = 7500;
        this.shortAV = 2;
        this.maxRange = RANGE_SHORT;
        introDate = 1950;
        techLevel.put(1950, techLevel.get(3071));
        availRating = new int[] { RATING_A, RATING_A, RATING_B };
        techRating = RATING_B;
    }
}
