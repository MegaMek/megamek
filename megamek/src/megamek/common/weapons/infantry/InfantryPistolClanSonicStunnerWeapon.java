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
import megamek.common.TechProgression;

/**
 * @author Ben Grills
 */
public class InfantryPistolClanSonicStunnerWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryPistolClanSonicStunnerWeapon() {
        super();

        name = "Sonic Stunner [Clan]";
        setInternalName(name);
        addLookupName("CLInfantrySonicStunnerpistol");
        ammoType = AmmoType.T_NA;
        cost = 100;
        bv = 0.06;
        flags = flags.or(F_NO_FIRES).or(F_INF_NONPENETRATING).or(F_DIRECT_FIRE).or(F_ENERGY);
        infantryDamage = 0.07;
        infantryRange = 0;
        introDate = 2807;
        techLevel.put(2807, TechConstants.T_CLAN_TW);
        availRating = new int[] { RATING_X,RATING_C ,RATING_B ,RATING_B};
        techRating = RATING_D;
        rulesRefs =" 273, TM";
        techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        techProgression.setClanProgression(DATE_NONE, DATE_NONE, 2807);
        techProgression.setTechRating(RATING_D);
        techProgression.setAvailability( new int[] { RATING_X, RATING_C, RATING_B, RATING_B });
    }
}
