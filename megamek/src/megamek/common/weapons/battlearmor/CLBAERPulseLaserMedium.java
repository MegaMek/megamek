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
import megamek.common.weapons.lasers.PulseLaserWeapon;

/**
 * @author Sebastian Brocks
 * @since Sep 12, 2004
 */
public class CLBAERPulseLaserMedium extends PulseLaserWeapon {
    private static final long serialVersionUID = 7816191920104768204L;

    public CLBAERPulseLaserMedium() {
        super();
        name = "ER Medium Pulse Laser";
        setInternalName("BACLERMediumPulseLaser");
        addLookupName("CLBAERMediumPulseLaser");
        addLookupName("BA Clan ER Pulse Med Laser");
        addLookupName("BA Clan ER Medium Pulse Laser");
        sortingName = "Laser Pulse ER C";
        heat = 6;
        damage = 7;
        toHitModifier = -1;
        shortRange = 5;
        mediumRange = 9;
        longRange = 14;
        extremeRange = 21;
        waterShortRange = 3;
        waterMediumRange = 5;
        waterLongRange = 8;
        waterExtremeRange = 12;
        tonnage = .8;
        criticals = 4;
        bv = 117;
        cost = 150000;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MEK_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "258, TM";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.CLAN)
                .setIntroLevel(false).setUnofficial(false).setTechRating(TechRating.F)
                .setAvailability(TechRating.X, TechRating.X, TechRating.E, TechRating.D)
                .setClanAdvancement(DATE_NONE, 3057, 3082, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.CWF)
                .setProductionFactions(Faction.CWF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
