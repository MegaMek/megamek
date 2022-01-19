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

/**
 * @author Andrew Hunter
 * @since Sep 24, 2004
 */
public class CLBAFlamer extends BAFlamerWeapon {
    private static final long serialVersionUID = 8782512971175525221L;

    public CLBAFlamer() {
        super();
        name = "Flamer [BA]";
        setInternalName("CLBAFlamer");
        addLookupName("Clan BA Flamer");
        addLookupName("ISBAFlamer");
        sortingName = "Flamer C";
        heat = 3;
        damage = 2;
        infDamageClass = WeaponType.WEAPON_BURST_3D6;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        tonnage = 0.15;
        criticals = 1;
        bv = 5;
        cost = 7500;
        shortAV = 2;
        maxRange = RANGE_SHORT;
        rulesRefs = "255, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_ALL);
        techAdvancement.setClanAdvancement(2860, 2868, 3050);
        techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, 3050);
        techAdvancement.setIntroLevel(false);
        techAdvancement.setPrototypeFactions(F_CWF).setProductionFactions(F_CWF);
        techAdvancement.setTechRating(RATING_C);
        techAdvancement.setAvailability(RATING_X, RATING_D, RATING_B, RATING_B);
    }
}
