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
package megamek.common.weapons.tag;

/**
 * @author Sebastian Brocks
 * @since Sep 7, 2005
 */
public class CLLightTAG extends TAGWeapon {
    private static final long serialVersionUID = -6411290826952751265L;

    public CLLightTAG() {
        super();
        name = "Light TAG";
        setInternalName("CLLightTAG");
        addLookupName("Clan Light TAG");
        addLookupName("Light TAG [Clan]");
        tonnage = 0.5;
        criticals = 1;
        hittable = true;
        spreadable = false;
        heat = 0;
        damage = 0;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        bv = 0;
        cost = 40000;
        flags = flags.or(F_AERO_WEAPON).or(F_PROTO_WEAPON).andNot(F_BA_WEAPON);
        rulesRefs = "238, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setClanAdvancement(3051, 3054, 3059, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CWF)
                .setProductionFactions(F_CWF);
    }
}
