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
package megamek.common.weapons.autocannons;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.Mounted;
/**
 * @author Andrew Hunter
 * @since Oct 15, 2004
 */
public class ISLB10XAC extends LBXACWeapon {
    private static final long serialVersionUID = -6873790245999096707L;

    public ISLB10XAC() {
        super();
        this.name = "LB 10-X AC";
        this.setInternalName("ISLBXAC10");
        this.addLookupName("IS LB 10-X AC");
        this.heat = 2;
        this.damage = 10;
        this.rackSize = 10;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.tonnage = 11.0;
        this.criticals = 6;
        this.bv = 148;
        this.cost = 400000;
        this.shortAV = 10;
        this.medAV = 10;
        this.maxRange = RANGE_MED;
        rulesRefs = "207, TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                .setISAdvancement(2590, 2595, 3040, 2840, 3035)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_FS);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        return (range <= AlphaStrikeElement.LONG_RANGE) ? 0.63 : 0;
    }
}
