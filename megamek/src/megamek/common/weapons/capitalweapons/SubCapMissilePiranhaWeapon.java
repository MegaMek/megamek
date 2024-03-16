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
package megamek.common.weapons.capitalweapons;

import megamek.common.AmmoType;
import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.alphaStrike.AlphaStrikeElement;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class SubCapMissilePiranhaWeapon extends SubCapMissileWeapon {
    private static final long serialVersionUID = 3827228773282489872L;

    public SubCapMissilePiranhaWeapon() {
        super();
        name = "Sub-Capital Missile Launcher (Piranha)";
        setInternalName(name);
        addLookupName("Piranha");
        this.shortName = "Piranha";
        heat = 9;
        damage = 3;
        ammoType = AmmoType.T_PIRANHA;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 100.0;
        bv = 670;
        cost = 75000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        this.missileArmor = 30;
        maxRange = RANGE_LONG;
        flags = flags.or(F_AERO_WEAPON).or(F_MISSILE);
        this.atClass = CLASS_CAPITAL_MISSILE;
        rulesRefs = "345, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setISAdvancement(DATE_NONE, 3060, 3072, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_NONE, 3070, 3072, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_WB)
                .setProductionFactions(F_WB)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted linked) {
        return (range <= AlphaStrikeElement.LONG_RANGE) ? 3 : 0;
    }
}
