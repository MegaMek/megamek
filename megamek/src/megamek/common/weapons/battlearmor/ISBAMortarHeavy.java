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
import megamek.common.Mounted;
import megamek.common.TechAdvancement;
import megamek.common.WeaponType;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.Weapon;

/**
 * Commented out in WeaponType. Clan version is same stats as IS one. And Clan versions captures
 * Tech progression for both.
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class ISBAMortarHeavy extends Weapon {
    private static final long serialVersionUID = 7081695747408312441L;

    public ISBAMortarHeavy() {
        super();
        name = "Heavy Mortar";
        setInternalName("ISBAHeavyMortar");
        addLookupName("IS BA Heavy Mortar");
        sortingName = "Mortar D";
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        ammoType = AmmoType.T_NA;
        minimumRange = 2;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        bv = 17;
        cost = 7500;
        tonnage = 0.4;
        criticals = 2;
        flags = flags.or(F_BALLISTIC).or(F_BURST_FIRE).or(F_BA_WEAPON)
                .andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "263, TM";

        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(3049, 3057, 3063);
        techAdvancement.setTechRating(RATING_B);
        techAdvancement.setAvailability(RATING_X, RATING_X, RATING_C, RATING_C);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted linked) {
        if (range <= AlphaStrikeElement.SHORT_RANGE) {
            return 0.249;
        } else if (range <= AlphaStrikeElement.MEDIUM_RANGE) {
            return 0.3;
        } else {
            return 0;
        }
    }

    @Override
    public boolean isAlphaStrikeIndirectFire() {
        return true;
    }
}