/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.flamers;

import megamek.common.WeaponType;
import megamek.common.alphaStrike.AlphaStrikeElement;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class ISVehicleFlamer extends VehicleFlamerWeapon {
    private static final long serialVersionUID = -5209851790302913451L;

    public ISVehicleFlamer() {
        super();

        this.name = "Flamer (Vehicle)";
        this.setInternalName(this.name);
        this.addLookupName("IS Vehicle Flamer");
        this.addLookupName("ISVehicleFlamer");
        this.addLookupName("CLVehicleFlamer");
        this.addLookupName("Clan Vehicle Flamer");
        this.addLookupName("Vehicle Flamer");
        this.heat = 3;
        this.damage = 2;
        this.infDamageClass = WeaponType.WEAPON_BURST_4D6;
        this.rackSize = 2;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.tonnage = 0.5;
        this.criticals = 1;
        this.bv = 5;
        this.cost = 7500;
        this.shortAV = 2;
        this.maxRange = RANGE_SHORT;
        rulesRefs = "218, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(true)
                .setUnofficial(false)
                .setTechRating(RATING_B)
                .setAvailability(RATING_A, RATING_A, RATING_B, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
    }

    @Override
    public int getAlphaStrikeHeatDamage(int rangeband) {
        if (rangeband <= AlphaStrikeElement.RANGE_BAND_SHORT) {
            return 2;
        } else {
            return 0;
        }
    }

    @Override
    public boolean isAlphaStrikePointDefense() {
        return true;
    }
}
