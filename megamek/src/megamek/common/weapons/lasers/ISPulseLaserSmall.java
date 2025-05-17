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
package megamek.common.weapons.lasers;

import megamek.common.WeaponType;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class ISPulseLaserSmall extends PulseLaserWeapon {
    private static final long serialVersionUID = 2977404162226570144L;

    public ISPulseLaserSmall() {
        super();
        name = "Small Pulse Laser";
        setInternalName("ISSmallPulseLaser");
        addLookupName("IS Small Pulse Laser");
        addLookupName("ISSmall Pulse Laser");
        sortingName = "Laser Pulse B";
        heat = 2;
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        toHitModifier = -2;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 2;
        waterExtremeRange = 2;
        tonnage = 1.0;
        criticals = 1;
        bv = 12;
        cost = 16000;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        atClass = CLASS_POINT_DEFENSE;
        flags = flags.or(F_BURST_FIRE);
        rulesRefs = "226, TM";
        techAdvancement.setTechBase(TechBase.IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
                .setISAdvancement(2595, 2609, 3042, 2950, 3037)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.DC);
    }

    @Override
    public boolean isAlphaStrikePointDefense() {
        return true;
    }

}
