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
import megamek.common.WeaponType;
import megamek.common.weapons.Weapon;

/**
 * @author Andrew Hunter
 * @since Sep 24, 2004
 */
public class CLBAGrenadeLauncherHeavy extends Weapon {
    private static final long serialVersionUID = 2728566278196446996L;

    public CLBAGrenadeLauncherHeavy() {
        super();
        name = "Grenade Launcher (Heavy)";
        setInternalName("CLBAHeavyGrenadeLauncher");
        addLookupName("CLBAHeavyGL");
        addLookupName("Heavy BA Grenade Launcher");      
      	addLookupName("ISBAHeavyGrenadeLauncher");
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
        bv = 1;
        cost = 4500;
        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_BURST_FIRE).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "256,TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_D, RATING_D, RATING_C)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3050, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2880, 2900, 3050, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);        
    }
}
