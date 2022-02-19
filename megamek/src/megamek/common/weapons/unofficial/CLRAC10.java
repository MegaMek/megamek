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
package megamek.common.weapons.unofficial;

import megamek.common.weapons.autocannons.RACWeapon;

/**
 * @author Sebastian Brocks
 * @since Oct 19, 2004
 */
public class CLRAC10 extends RACWeapon {
    private static final long serialVersionUID = 7945585759921446908L;

    public CLRAC10() {
        super();

        this.name = "Rotary AC/10";
        this.setInternalName("CLRotaryAC10");
        this.addLookupName("Clan Rotary AC/10");
        this.addLookupName("Clan Rotary Assault Cannon/10");
        this.heat = 3;
        this.damage = 10;
        this.rackSize = 10;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.tonnage = 14.0;
        this.criticals = 7;
        this.bv = 617;
        this.cost = 640000;
        rulesRefs = "Unofficial";
        flags = flags.andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3073, 3104, 3145, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
    }
}
