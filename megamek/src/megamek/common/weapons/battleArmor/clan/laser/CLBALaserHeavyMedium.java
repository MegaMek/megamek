/*
 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.battleArmor.clan.laser;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.lasers.LaserWeapon;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class CLBALaserHeavyMedium extends LaserWeapon {
    private static final long serialVersionUID = -3836305728245548205L;

    public CLBALaserHeavyMedium() {
        super();
        name = "Heavy Medium Laser";
        setInternalName("CLBAHeavyMediumLaser");
        addLookupName("Clan BA Medium Heavy Laser");
        sortingName = "Laser Heavy C";
        heat = 7;
        damage = 10;
        toHitModifier = 1;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        waterShortRange = 2;
        waterMediumRange = 4;
        waterLongRange = 6;
        waterExtremeRange = 8;
        tonnage = 1.0;
        criticalSlots = 4;
        bv = 76;
        cost = 100000;
        shortAV = 10;
        maxRange = RANGE_SHORT;
        flags = flags.or(F_NO_FIRES)
              .or(F_BA_WEAPON)
              .andNot(F_MEK_WEAPON)
              .andNot(F_TANK_WEAPON)
              .andNot(F_AERO_WEAPON)
              .andNot(F_PROTO_WEAPON);
        rulesRefs = "258, TM";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3057, 3059, 3062, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSA)
              .setProductionFactions(Faction.CSA);
    }
}
