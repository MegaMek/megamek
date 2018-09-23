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
 * Created on Sep 12, 2004
 *
 */
package megamek.common.weapons.lasers;

import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;

/**
 * @author Andrew Hunter
 */
public class ISERLaserLarge extends LaserWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -4487405793320900805L;

    public ISERLaserLarge() {
        super();
        this.name = "ER Large Laser";
        this.setInternalName("ISERLargeLaser");
        this.addLookupName("IS ER Large Laser");
        this.heat = 12;
        this.damage = 8;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 19;
        this.extremeRange = 28;
        this.waterShortRange = 3;
        this.waterMediumRange = 9;
        this.waterLongRange = 12;
        this.waterExtremeRange = 18;
        this.tonnage = 5.0;
        this.criticals = 2;
        this.bv = 163;
        this.cost = 200000;
        this.shortAV = 8;
        this.medAV = 8;
        this.longAV = 8;
        this.maxRange = RANGE_LONG;
        rulesRefs = "226,TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
            .setISAdvancement(2610, 2620, 3045, 2950, 3037)
            .setISApproximate(true, false, false,false, false)
            .setPrototypeFactions(F_TH)
            .setProductionFactions(F_TH)
            .setReintroductionFactions(F_DC);
    }

    @Override
    public int getLongRange() {
        GameOptions options = getGameOptions();
        if (options == null) {
            return super.getLongRange();
        } else if (options.getOption(OptionsConstants.ADVCOMBAT_INCREASED_ISERLL_RANGE) == null) {
            return super.getLongRange();
        }
        if (options.getOption(OptionsConstants.ADVCOMBAT_INCREASED_ISERLL_RANGE).booleanValue()) {
            return 21;
        }
        return super.getLongRange();
    }
}
