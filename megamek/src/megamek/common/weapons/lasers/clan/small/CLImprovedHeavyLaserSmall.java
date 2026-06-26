/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.lasers.clan.small;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.lasers.LaserWeapon;

/**
 * @author Jason Tighe
 * @since Sep 12, 2004
 */
public class CLImprovedHeavyLaserSmall extends LaserWeapon {
    @Serial
    private static final long serialVersionUID = 4467522144065588079L;

    public CLImprovedHeavyLaserSmall() {
        super();
        name = "Improved Heavy Small Laser";
        shortName = "Imp. Heavy Small Laser";
        setInternalName("CLImprovedSmallHeavyLaser");
        addLookupName("CLImprovedHeavySmallLaser");
        addLookupName("Clan Improved Small Heavy Laser");
        sortingName = "Laser Heavy Imp B";
        heat = 3;
        damage = 6;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 2;
        waterExtremeRange = 2;
        tonnage = 0.5;
        criticalSlots = 1;
        bv = 19;
        cost = 30000;
        shortAV = 6;
        maxRange = RANGE_SHORT;
        explosionDamage = 3;
        explosive = true;
        rulesRefs = "133, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(DATE_NONE, 3069, 3085, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.CGS)
              .setProductionFactions(Faction.RD)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
