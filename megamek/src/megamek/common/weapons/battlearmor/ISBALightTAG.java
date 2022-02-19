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
package megamek.common.weapons.battlearmor;

import megamek.common.TechAdvancement;
import megamek.common.weapons.tag.TAGWeapon;

/**
 * This serves both as the Fa-Shih's Light TAG and the Kage's IS Compact TAG, as the stats are the same.
 * Commented out in WeaponType. Clan version is same stats as IS one. And Clan versions captures
 * Tech progression for both.
 *
 * @author Sebastian Brocks
 * @since Sep 7, 2005
 */
public class ISBALightTAG extends TAGWeapon {
    private static final long serialVersionUID = 3038539726901030186L;

    public ISBALightTAG() {
        super();
        this.name = "TAG (Light)";
        setInternalName("ISBALightTAG");
        this.addLookupName("IS BA Light TAG");
        this.tonnage = 0.035;
        this.criticals = 1;
        this.hittable = true;
        this.spreadable = false;
        this.heat = 0;
        this.damage = 0;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.bv = 0;
        this.cost = 40000;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON)
                .andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "270, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(3046, 3053, 3057);
        techAdvancement.setTechRating(RATING_E);
        techAdvancement.setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);
    }
}
