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

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class NL45Weapon extends NLWeapon {
    private static final long serialVersionUID = 8756042527483383101L;

    public NL45Weapon() {
        super();
        this.name = "Naval Laser 45";
        this.setInternalName(this.name);
        this.addLookupName("NL45");
        this.addLookupName("Naval Laser 45 (Clan)");
        this.shortName = "NL45";
        this.heat = 70;
        this.damage = 4;
        this.shortRange = 12;
        this.mediumRange = 24;
        this.longRange = 36;
        this.extremeRange = 48;
        this.tonnage = 900.0;
        this.bv = 1134;
        this.cost = 850000;
        this.shortAV = 4.5;
        this.medAV = 4.5;
        this.longAV = 4.5;
        this.extAV = 4.5;
        this.maxRange = RANGE_EXT;
        rulesRefs = "333, TO";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_D)
                .setAvailability(RATING_D, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(DATE_ES, 2305, DATE_NONE, 2950, 3051)
                .setISApproximate(false, true, false, true, false)
                .setClanAdvancement(DATE_ES, 2305, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setProductionFactions(F_TA)
                .setReintroductionFactions(F_FS, F_LC);
    }
}
