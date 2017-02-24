/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
/*
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;
import megamek.common.TechConstants;
import megamek.common.TechAdvancement;

/**
 * @author Ben Grills
 */
public class InfantryArchaicMedusaWhipWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryArchaicMedusaWhipWeapon() {
        super();

        name = "Whip (Medusa)";
        setInternalName(name);
        addLookupName("InfantryClanMedusaWhip");
        addLookupName("Medusa Whip");
        ammoType = AmmoType.T_NA;
        cost = 2200;
        bv = 0.15;
        flags = flags.or(F_NO_FIRES).or(F_INF_POINT_BLANK).or(F_INF_NONPENETRATING).or(F_INF_ARCHAIC);
        infantryDamage = 0.16;
        infantryRange = 0;
        introDate = 2815;
        techLevel.put(2815, TechConstants.T_CLAN_EXPERIMENTAL);
        techLevel.put(2820, TechConstants.T_CLAN_ADVANCED);
        availRating = new int[] { RATING_X,RATING_F ,RATING_E ,RATING_E};
        techRating = RATING_E;
        rulesRefs = "272, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(2815, 2820, DATE_NONE);
        techAdvancement.setTechRating(RATING_E);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_F, RATING_E, RATING_E });
    }
}
