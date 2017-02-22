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
public class InfantryRifleRadiumLaserSniperWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryRifleRadiumLaserSniperWeapon() {
        super();

        name = "Rifle (Radium Sniper)";
        setInternalName(name);
        addLookupName("InfantryRadiumSniper");
        addLookupName("Radium Sniper Rifle");
        ammoType = AmmoType.T_NA;
        cost = 9500;
        bv = 2.58;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
        infantryDamage = 0.36;
        infantryRange = 3;
        introDate = 2577;
        extinctDate = 2607;
        reintroDate = 3062;
        techLevel.put(2577, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(2583, TechConstants.T_IS_ADVANCED);
        techLevel.put(3062, TechConstants.T_IS_EXPERIMENTAL);
        availRating = new int[] { RATING_F,RATING_X ,RATING_F ,RATING_F};
        techRating = RATING_E;
        rulesRefs = "273, TM";
        techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        techProgression.setISProgression(2577, 2583, DATE_NONE, 2607, 3062);
        techProgression.setTechRating(RATING_E);
        techProgression.setAvailability( new int[] { RATING_F, RATING_X, RATING_F, RATING_F });
    }
}
