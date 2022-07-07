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
public class SubCapMissileStingrayWeapon extends SubCapMissileWeapon {
    private static final long serialVersionUID = 3827228773281489872L;

    public SubCapMissileStingrayWeapon() {
        super();
        this.name = "Sub-Capital Missile Launcher (Stingray)";
        this.setInternalName(this.name);
        this.addLookupName("Stingray");
        this.addLookupName("CLStingray");
        this.shortName = "Stingray";
        this.heat = 9;
        this.damage = 3;
        this.ammoType = AmmoType.T_STINGRAY;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 120.0;
        this.bv = 496;
        this.cost = 85000;
        this.flags = flags.or(F_MISSILE);
        this.atClass = CLASS_CAPITAL_MISSILE;
        this.shortAV = 3.5;
        this.medAV = 3.5;
        this.missileArmor = 35;
        this.maxRange = RANGE_MED;
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
        return (range <= AlphaStrikeElement.CAPITAL_RANGES[1]) ? 3.5 : 0;
    }
}
