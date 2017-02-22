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
public class InfantrySMGClanGaussWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySMGClanGaussWeapon() {
        super();

        name = "Gauss Submachinegun";
        setInternalName(name);
        addLookupName("InfantryGaussSMG");
        addLookupName("InfantryClanGaussSMG");
        ammoType = AmmoType.T_NA;
        cost = 2000;
        bv = 1.38;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.45;
        infantryRange = 1;
        introDate = 3046;
        techLevel.put(3046, TechConstants.T_CLAN_EXPERIMENTAL);
        techLevel.put(3055, TechConstants.T_CLAN_ADVANCED);
        techLevel.put(3060, TechConstants.T_CLAN_TW);
        availRating = new int[] { RATING_X,RATING_X ,RATING_D ,RATING_D};
        techRating = RATING_E;
        rulesRefs = "273, TM";
        techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        techProgression.setClanProgression(3046, 3055, 3060);
        techProgression.setTechRating(RATING_E);
        techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_D, RATING_D });
    }
}
