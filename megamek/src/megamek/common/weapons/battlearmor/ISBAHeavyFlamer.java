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
import megamek.common.WeaponType;
import megamek.common.alphaStrike.AlphaStrikeElement;


/**
 * Commented out in WeaponType. Clan version is same stats as IS one. And Clan versions captures
 * Tech progression for both.
 * @since Sep 24, 2004
 */
public class ISBAHeavyFlamer extends BAFlamerWeapon {
    private static final long serialVersionUID = -2482256971276582340L;

    public ISBAHeavyFlamer() {
        super();
        name = "Heavy Flamer [BA]";
        setInternalName("ISBAHeavyFlamer");
        addLookupName("IS BA Heavy Flamer");
        sortingName = "Flamer D";
        heat = 5;
        damage = 4;
        infDamageClass = WeaponType.WEAPON_BURST_6D6;
        shortRange = 2;
        mediumRange = 3;
        longRange = 4;
        extremeRange = 6;
        tonnage = .35;
        criticals = 1;
        bv = 15;
        cost = 11250;
        flags = flags.or(F_FLAMER).or(F_ENERGY).or(F_BA_WEAPON)
                .or(F_BURST_FIRE).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "312, TO";

        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(3065, 3073, 3075);
        techAdvancement.setTechRating(RATING_C);
        techAdvancement.setAvailability(RATING_X, RATING_X, RATING_E, RATING_D);
    }

    @Override
    public int getAlphaStrikeHeatDamage(int rangeband) {
        if (rangeband <= AlphaStrikeElement.RANGE_BAND_SHORT) {
            return 4;
        } else {
            return 0;
        }
    }
}
