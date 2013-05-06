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

/**
 * @author Ben Grills
 */
public class InfantrySupportFiredrakeNeedlerWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySupportFiredrakeNeedlerWeapon() {
        super();
        techLevel.put(3071,TechConstants.T_IS_TW_NON_BOX);
        name = "Infantry Firedrake Needler";
        setInternalName(name);
        addLookupName("InfantryFiredrake");
        addLookupName("InfantrySupportNeedler");
        ammoType = AmmoType.T_NA;
        cost = 500;
        bv = 2.24;
        // TM and its errata don't say this has the (N) property, but the fluff text does, as does its original entry in Combat Equipment, so I have included it here.
        //Taharqa: I think we should ask on the forums about this before we make assumptions, removing
        flags = flags.or(F_INCENDIARY_NEEDLES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_SUPPORT);
        String[] modeStrings = { "Damage", "Heat" };
        setModes(modeStrings);
        infantryDamage = 0.91;
        infantryRange = 1;
        crew = 2;
        introDate = 3061;
        techLevel.put(3061,techLevel.get(3071));
        availRating = new int[]{RATING_X,RATING_X,RATING_C};
        techRating = RATING_D;
    }
}