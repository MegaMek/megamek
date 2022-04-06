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
import megamek.common.SimpleTechLevel;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class MassDriverMedium extends MassDriverWeapon {
    private static final long serialVersionUID = 8756042527483383101L;

    public MassDriverMedium() {
        super();
        this.name = "Mass Driver (Medium)";
        this.setInternalName(this.name);
        this.addLookupName("MediumMassDriver");
        this.shortName = "Medium Mass Driver";
        this.heat = 60;
        this.damage = 100;
        this.ammoType = AmmoType.T_MMASS;
        this.shortRange = 12;
        this.mediumRange = 24;
        this.longRange = 40;
        this.tonnage = 50000;
        this.bv = 11760;
        this.cost = 280000000;
        this.shortAV = 100;
        this.medAV = 100;
        this.longAV = 100;
        this.maxRange = RANGE_LONG;
        rulesRefs = "323, TO";
        techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_D)
                .setAvailability(RATING_F, RATING_X, RATING_F, RATING_F)
                .setISAdvancement(2715, DATE_NONE, DATE_NONE, 2855, 3066)
                .setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2715, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, true, false)
                .setPrototypeFactions(F_TH).setReintroductionFactions(F_WB)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
