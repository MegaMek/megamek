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

package megamek.common.weapons.lasers.innerSphere.large;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.lasers.LaserWeapon;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class ISLaserLarge extends LaserWeapon {
    @Serial
    private static final long serialVersionUID = 7184746139915905374L;

    public ISLaserLarge() {
        super();
        name = "Large Laser";
        setInternalName(this.name);
        addLookupName("IS Large Laser");
        addLookupName("ISLargeLaser");
        sortingName = "Laser AA D";
        heat = 8;
        damage = 8;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 5.0;
        criticalSlots = 2;
        bv = 123;
        cost = 100000;
        shortAV = 8;
        medAV = 8;
        maxRange = RANGE_MED;
        rulesRefs = "227, TM";
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(true)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(2306, 2316, 2320, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2306, 2316, 2320, 2850, DATE_NONE)
              .setClanApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TH);
    }
}
