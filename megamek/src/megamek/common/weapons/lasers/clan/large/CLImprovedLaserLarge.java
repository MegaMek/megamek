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

package megamek.common.weapons.lasers.clan.large;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.lasers.LaserWeapon;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class CLImprovedLaserLarge extends LaserWeapon {
    @Serial
    private static final long serialVersionUID = 7184746139915905374L;

    public CLImprovedLaserLarge() {
        super();
        this.name = "Improved Large Laser";
        this.setInternalName(this.name);
        this.addLookupName("Improved Large Laser");
        this.addLookupName("ImpLargeLaser");
        sortingName = "Laser Imp D";
        this.heat = 8;
        this.damage = 8;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.waterShortRange = 3;
        this.waterMediumRange = 6;
        this.waterLongRange = 9;
        this.waterExtremeRange = 12;
        this.tonnage = 4.0;
        this.criticalSlots = 1;
        this.bv = 123;
        this.cost = 100000;
        this.shortAV = 8;
        this.medAV = 8;
        this.maxRange = RANGE_MED;
        rulesRefs = "89, IO:AE";
        techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(2812, 2815, 2818, 2830, 3080)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.CNC).setProductionFactions(Faction.CNC)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
