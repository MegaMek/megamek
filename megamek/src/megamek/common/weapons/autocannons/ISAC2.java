/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.autocannons;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.Mounted;

/**
 * @author Andrew Hunter
 * @since Sep 25, 2004
 */
public class ISAC2 extends ACWeapon {
    private static final long serialVersionUID = 49211848611799265L;

    public ISAC2() {
        super();
        name = "AC/2";
        setInternalName("Autocannon/2");
        addLookupName("IS Auto Cannon/2");
        addLookupName("Auto Cannon/2");
        addLookupName("AutoCannon/2");
        addLookupName("AC/2");
        addLookupName("ISAC2");
        addLookupName("IS Autocannon/2");
        sortingName = "AC/02";
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 4;
        shortRange = 8;
        mediumRange = 16;
        longRange = 24;
        extremeRange = 32;
        tonnage = 6.0;
        criticals = 1;
        bv = 37;
        cost = 75000;
        explosive = true; // when firing incendiary ammo
        shortAV = 2;
        medAV = 2;
        longAV = 2;
        maxRange = RANGE_LONG;
        explosionDamage = damage;
        rulesRefs = "208, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(true)
                .setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(2290, 2300, 2305, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2290, 2300, 2305, 2850, DATE_NONE)
                .setClanApproximate(false, false, false, true, false)
                .setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted ignore) {
        return range == AlphaStrikeElement.SHORT_RANGE ? 0.132 : 0.2;
    }
}
