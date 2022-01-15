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
 * @since Oct 15, 2004
 */
public class CLLB10XAC extends LBXACWeapon {
    private static final long serialVersionUID = 3315625878431308444L;

    public CLLB10XAC() {
        super();

        this.name = "LB 10-X AC";
        this.setInternalName("CLLBXAC10");
        this.addLookupName("Clan LB 10-X AC");
        this.heat = 2;
        this.damage = 10;
        this.rackSize = 10;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.tonnage = 10.0;
        this.criticals = 5;
        this.bv = 148;
        this.cost = 400000;
        this.shortAV = 10;
        this.medAV = 10;
        this.maxRange = RANGE_MED;
        rulesRefs = "207,TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
                .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false)
                .setProductionFactions(F_CLAN)
                .setReintroductionFactions(F_CLAN);
    }
}
