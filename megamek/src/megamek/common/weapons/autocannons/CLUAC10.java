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

/**
 * @author Andrew Hunter
 * @since Oct 2, 2004
 */
public class CLUAC10 extends UACWeapon {
    private static final long serialVersionUID = 6077697413308875802L;

    public CLUAC10() {
        super();

        this.name = "Ultra AC/10";
        this.setInternalName("CLUltraAC10");
        this.addLookupName("Clan Ultra AC/10");
        this.heat = 3;
        this.damage = 10;
        this.rackSize = 10;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.tonnage = 10.0;
        this.criticals = 4;
        this.bv = 210;
        this.cost = 320000;
        this.shortAV = 15;
        this.medAV = 15;
        this.maxRange = RANGE_MED;
        this.explosionDamage = damage;
        rulesRefs = "208,TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_D, RATING_D, RATING_C)
                .setClanAdvancement(2825, 2827, 2829, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false)
                .setPrototypeFactions(F_CLAN)
                .setProductionFactions(F_CLAN);
    }
}
