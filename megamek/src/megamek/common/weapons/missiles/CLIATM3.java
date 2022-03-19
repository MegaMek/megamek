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

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.Mounted;
import megamek.common.weapons.CLIATMWeapon;

/**
 * @author Sebastian Brocks, modified by Greg
 */
public class CLIATM3 extends CLIATMWeapon {
    private static final long serialVersionUID = 1L;

    public CLIATM3() {
        super();
        this.name = "Improved ATM 3";
        this.setInternalName("CLiATM3");
        this.addLookupName("Clan iATM-3");
        this.heat = 2;
        this.rackSize = 3;
        this.minimumRange = 4;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.tonnage = 1.5;
        this.criticals = 2;
        this.bv = 83; // Ammo BV is 21
        this.cost = 100000;
        this.shortAV = 6; // Seems to be for aero
        this.medAV = 6; // Seems to be for aero
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
    
    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 0.9;
        } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
            return 0.6;
        } else {
            return 0.3;
        }
    }
}
