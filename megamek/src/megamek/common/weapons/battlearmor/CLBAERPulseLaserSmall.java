/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import megamek.common.SimpleTechLevel;
import megamek.common.WeaponType;
import megamek.common.weapons.lasers.PulseLaserWeapon;

/**
 * @author Sebastian Brocks
 * @since Sep 12, 2004
 */
public class CLBAERPulseLaserSmall extends PulseLaserWeapon {
    private static final long serialVersionUID = -273231806790327505L;

    public CLBAERPulseLaserSmall() {
        super();
        name = "ER Small Pulse Laser";
        setInternalName("CLBAERSmallPulseLaser");
        addLookupName("Clan BA ER Pulse Small Laser");
        addLookupName("Clan BA ER Small Pulse Laser");
        addLookupName("Clan BA ERSmallPulseLaser");
        sortingName = "Laser Pulse ER B";
        heat = 3;
        damage = 5;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        toHitModifier = -1;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 4;
        waterExtremeRange = 4;
        tonnage = .55;
        criticals = 2;
        bv = 36;
        cost = 30000;
        flags = flags.or(F_BURST_FIRE).or(F_NO_FIRES).or(F_BA_WEAPON).
        		andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "258, TM";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(DATE_NONE, 3057, 3082, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_CWF)
                .setProductionFactions(F_CWF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
