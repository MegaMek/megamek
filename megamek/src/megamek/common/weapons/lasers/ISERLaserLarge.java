/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.lasers;

import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class ISERLaserLarge extends LaserWeapon {
    private static final long serialVersionUID = -4487405793320900805L;

    public ISERLaserLarge() {
        super();
        name = "ER Large Laser";
        setInternalName("ISERLargeLaser");
        addLookupName("IS ER Large Laser");
        sortingName = "ER Laser D";
        heat = 12;
        damage = 8;
        shortRange = 7;
        mediumRange = 14;
        longRange = 19;
        extremeRange = 28;
        waterShortRange = 3;
        waterMediumRange = 9;
        waterLongRange = 12;
        waterExtremeRange = 18;
        tonnage = 5.0;
        criticals = 2;
        bv = 163;
        cost = 200000;
        shortAV = 8;
        medAV = 8;
        longAV = 8;
        maxRange = RANGE_LONG;
        rulesRefs = "226, TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                .setISAdvancement(2610, 2620, 3045, 2950, 3037)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_DC);
    }

    @Override
    public int getLongRange() {
        if (Server.getServerInstance() == null) {
            return super.getLongRange();
        }
        final GameOptions options = Server.getServerInstance().getGame().getOptions();
        if (options.getOption(OptionsConstants.ADVCOMBAT_INCREASED_ISERLL_RANGE) == null) {
            return super.getLongRange();
        } else if (options.getOption(OptionsConstants.ADVCOMBAT_INCREASED_ISERLL_RANGE).booleanValue()) {
            return 21;
        } else {
            return super.getLongRange();
        }
    }
}
