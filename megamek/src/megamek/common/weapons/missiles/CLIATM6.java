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
package megamek.common.weapons.missiles;

import megamek.common.weapons.CLIATMWeapon;

/**
 * @author Sebastian Brocks, edited by Greg
 */
public class CLIATM6 extends CLIATMWeapon {
    private static final long serialVersionUID = 1L;

    public CLIATM6() {
        super();
        this.name = "Improved ATM 6";
        this.setInternalName("CLiATM6");
        this.addLookupName("Clan iATM-6");
        this.heat = 4;
        this.rackSize = 6;
        this.minimumRange = 4;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.tonnage = 3.5;
        this.criticals = 3;
        this.bv = 165; // Ammo BV is 39
        this.cost = 250000;
        this.shortAV = 12;
        this.medAV = 12;
        this.maxRange = RANGE_MED;
        rulesRefs = "65, IO";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
    }
}
