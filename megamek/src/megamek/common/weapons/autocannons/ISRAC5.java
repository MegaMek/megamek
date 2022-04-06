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

import megamek.common.SimpleTechLevel;

/**
 * @author Andrew Hunter
 * @since Oct 19, 2004
 */
public class ISRAC5 extends RACWeapon {
    private static final long serialVersionUID = 1212976417295270466L;

    public ISRAC5() {
        super();
        this.name = "Rotary AC/5";
        this.setInternalName("ISRotaryAC5");
        this.addLookupName("IS Rotary AC/5");
        this.addLookupName("ISRAC5");
        this.heat = 1;
        this.damage = 5;
        this.rackSize = 5;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.tonnage = 10.0;
        this.criticals = 6;
        this.bv = 247;
        this.cost = 275000;
        this.shortAV = 20;
        this.medAV = 20;
        this.maxRange = RANGE_MED;
        this.explosionDamage = damage;
        rulesRefs = "207, TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3060, 3062, 3071, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
    }
}
