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
 * @author Sebastian Brocks
 */
public class InfantrySupportISLightSRMInfernoWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 7788576728727248931L;

    public InfantrySupportISLightSRMInfernoWeapon() {
        super();

        name = "SRM Launcher (Light) - Inferno";
        setInternalName(name);
        addLookupName("InfantrySRMLightInferno");
        addLookupName("Light SRM (Inferno)");
        ammoType = AmmoType.T_NA;
        cost = 1500;
        bv = 1.74;
        flags = flags.or(F_DIRECT_FIRE).or(F_INFERNO).or(F_MISSILE).or(F_INF_SUPPORT);
        infantryDamage = 0.34;
        infantryRange = 2;
        String[] modeStrings = { "Damage", "Heat" };
        setModes(modeStrings);
        introDate = 2360;
        techLevel.put(2360, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(2370, TechConstants.T_IS_ADVANCED);
        techLevel.put(2400, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_C,RATING_C ,RATING_D ,RATING_C};
        techRating = RATING_C;
        rulesRefs = "273, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(2360, 2370, 2400);
        techAdvancement.setTechRating(RATING_C);
        techAdvancement.setAvailability( new int[] { RATING_C, RATING_C, RATING_D, RATING_C });
    }
}
