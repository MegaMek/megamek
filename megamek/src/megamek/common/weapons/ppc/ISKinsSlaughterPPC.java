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
package megamek.common.weapons.ppc;

/**
 * @author Andrew Hunter
 * @since Sep 13, 2004
 */
public class ISKinsSlaughterPPC extends PPCWeapon {
    private static final long serialVersionUID = 6733393836643781374L;

    public ISKinsSlaughterPPC() {
        super();
        this.name = "Kinslaughter H ER PPC";
        this.setInternalName("ISKinHERPPC");
        this.addLookupName("IS Kinslaughter H ER PPC");
        sortingName = "PPC ER Kins";
        this.heat = 13;
        this.damage = 10;
        this.shortRange = 4;
        this.mediumRange = 10;
        this.longRange = 16;
        this.extremeRange = 20;
        this.waterShortRange = 4;
        this.waterMediumRange = 10;
        this.waterLongRange = 16;
        this.waterExtremeRange = 20;
        this.tonnage = 7.0;
        this.criticals = 3;
        this.bv = 229;
        this.cost = 450000;
        // Since this is a SL Era ER PPC variant mentioned in Spartan Fluff
        // This weapons was actually blended into IO's Enhanced PPC and should be considered non-canon
        // for IS factions
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                .setISAdvancement(2740, 2751, DATE_NONE, 2860, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2740, 2751, DATE_NONE, 2831, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
    }
}
