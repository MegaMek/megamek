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

import megamek.common.AmmoType;
import megamek.common.TechAdvancement;
import megamek.common.WeaponType;
import megamek.common.weapons.Weapon;

/**
 * Commented out in WeaponType. Clan version is same stats as IS one. And IS versions captures Tech
 * progression for both.
 * @author Andrew Hunter
 * @since Sep 24, 2004
 */
public class ISBAGrenadeLauncherHeavy extends Weapon {
    private static final long serialVersionUID = -5514157095037913844L;

    public ISBAGrenadeLauncherHeavy() {
        super();
        name = "Grenade Launcher(Heavy)";
        setInternalName("ISBAHeavyGrenadeLauncher");
        addLookupName("BA Heavy Grenade Launcher");
        addLookupName("ISBAAutoGL");
        addLookupName("ISBAHeavyGL");
        //Per TM Errata the original Grenade Launcher becomes the Heavy. Lookups below to keep unit files consistent.
        addLookupName("ISBAGrenadeLauncher");
      	addLookupName("IS BA Grenade Launcher");
        sortingName = "Grenade Launcher D";
        heat = 0;
        damage = 1;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        ammoType = AmmoType.T_NA;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        tonnage = 0.1;
        criticals = 1;
        bv = 2;
        cost = 4500;
        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_BURST_FIRE).or(F_BA_WEAPON)
                .andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
		rulesRefs = "256, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, 3050);
        techAdvancement.setTechRating(RATING_C);
        techAdvancement.setAvailability(RATING_X, RATING_D, RATING_D, RATING_C);
    }

}
