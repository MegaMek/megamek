/*
 * MegaMek - Copyright (C) 2011 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.weapons.other;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.WeaponType;
import megamek.common.weapons.CLIATMWeapon;

/**
 * @author beerockxs
 */
public class CLFussilade extends CLIATMWeapon {
    private static final long serialVersionUID = 1237937853765733086L;

    public CLFussilade() {
        super();
        //TODO Game Rules.
        this.name = "Fusillade Launcher";
        setInternalName("Fusillade");
        addLookupName("Fussilade");
        flags = flags.or(WeaponType.F_PROTO_WEAPON).or(WeaponType.F_MISSILE)
                .or(WeaponType.F_ONESHOT).or(WeaponType.F_DOUBLE_ONESHOT)
                .andNot(F_AERO_WEAPON).andNot(F_BA_WEAPON).andNot(F_MECH_WEAPON)
                .andNot(F_TANK_WEAPON);
        rackSize = 3;
        minimumRange = 4;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        damage = 6;
        shortAV = 6;
        medAV = 6;
        longAV = 6;
        maxRange = RANGE_MED;
        cost = 100000;
        tonnage = 1.5;
        criticals = 1;
        bv = 11;
        rulesRefs = "65, IO";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                .setClanAdvancement(3072,DATE_NONE,DATE_NONE, 3075,DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 0.45;
        } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
            return 0.3;
        } else {
            return 0;
        }
    }

}
