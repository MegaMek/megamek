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
 * @author Sebastian Brocks, edited by Greg
 */
public class CLIATM9 extends CLIATMWeapon {
    private static final long serialVersionUID = 1L;

    public CLIATM9() {
        super();
        this.name = "Improved ATM 9";
        this.setInternalName("CLiATM9");
        this.addLookupName("Clan iATM-9");
        this.heat = 6;
        this.rackSize = 9;
        this.minimumRange = 4;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.tonnage = 5.0;
        this.criticals = 4;
        this.bv = 231; // Ammo BV is 54
        this.cost = 450000;
        this.shortAV = 18;
        this.medAV = 18;
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
            return 2.7;
        } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
            return 1.8;
        } else {
            return 0.9;
        }
    }
}
