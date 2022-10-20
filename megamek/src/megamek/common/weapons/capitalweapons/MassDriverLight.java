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
package megamek.common.weapons.capitalweapons;

import megamek.common.AmmoType;
import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.alphaStrike.AlphaStrikeElement;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class MassDriverLight extends MassDriverWeapon {
    private static final long serialVersionUID = 8756042527483383101L;

    public MassDriverLight() {
        super();
        this.name = "Mass Driver (Light)";
        this.setInternalName(this.name);
        this.addLookupName("LightMassDriver");
        this.shortName = "Light Mass Driver";
        this.heat = 30;
        this.damage = 60;
        this.ammoType = AmmoType.T_LMASS;
        this.shortRange = 12;
        this.mediumRange = 24;
        this.longRange = 40;
        this.tonnage = 30000;
        this.bv = 7056;
        this.cost = 150000000;
        this.shortAV = 60;
        this.medAV = 60;
        this.longAV = 60;
        this.maxRange = RANGE_LONG;
        rulesRefs = "323, TO";
        techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_D)
                .setAvailability(RATING_F, RATING_X, RATING_F, RATING_F)
                .setISAdvancement(2715, DATE_NONE, DATE_NONE, 2855, 3066)
                .setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2715, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, true, false)
                .setPrototypeFactions(F_TH)
                .setReintroductionFactions(F_WB)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted linked) {
        return (range <= AlphaStrikeElement.LONG_RANGE) ? 54 : 0;
    }
}
