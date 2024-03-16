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
package megamek.common.weapons.lrms;

import static megamek.common.MountedHelper.*;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.Mounted;

/**
 * @author Sebastian Brocks
 */
public class CLLRM5 extends LRMWeapon {
    private static final long serialVersionUID = -2860859814228145513L;

    public CLLRM5() {
        super();
        name = "LRM 5";
        setInternalName("CLLRM5");
        addLookupName("Clan LRM-5");
        addLookupName("Clan LRM 5");
        heat = 2;
        rackSize = 5;
        minimumRange = WEAPON_NA;
        tonnage = 1.0;
        criticals = 1;
        bv = 55;
        cost = 30000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        maxRange = RANGE_LONG;
        rulesRefs = "229, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
                .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
    }
    
    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        if (isArtemisIV(fcs) || isArtemisProto(fcs)) {
            return (range <= AlphaStrikeElement.LONG_RANGE) ? 0.4 : 0;
        } else if (isArtemisV(fcs)) {
            return (range <= AlphaStrikeElement.LONG_RANGE) ? 0.42 : 0;
        } else {
            return (range <= AlphaStrikeElement.LONG_RANGE) ? 0.3 : 0;
        }
    }
}
