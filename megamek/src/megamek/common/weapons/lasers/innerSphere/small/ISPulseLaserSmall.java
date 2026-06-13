/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons.lasers.innerSphere.small;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.lasers.PulseLaserWeapon;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class ISPulseLaserSmall extends PulseLaserWeapon {
    @Serial
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
        criticalSlots = 1;
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
