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
public class InfantrySupportMediumRecoillessRifleInfernoWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySupportMediumRecoillessRifleInfernoWeapon() {
        super();
        techLevel = TechConstants.T_ALLOWED_ALL;
        name = "Infantry InfernoMediumRecoillessRifle";
        setInternalName(name);
        addLookupName("InfantryInfernoMRR");
        addLookupName("InfantryMediumRecoillessRifleInferno");
        ammoType = AmmoType.T_NA;
        cost = 2000;
        bv = 1.25;
        flags = flags.or(F_INFERNO).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_SUPPORT);
        infantryDamage = 0.21;
        infantryRange = 2;
        crew = 2;
    }
}