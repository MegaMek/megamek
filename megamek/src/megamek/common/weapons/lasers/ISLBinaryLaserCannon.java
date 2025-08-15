/*
  Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.lasers;

import megamek.common.SimpleTechLevel;

/**
 * @author Jason Tighe
 * @since Sep 12, 2004
 */
public class ISLBinaryLaserCannon extends LaserWeapon {
    private static final long serialVersionUID = -6849916948609019186L;

    public ISLBinaryLaserCannon() {
        super();
        name = "Light Blazer";
        setInternalName(name);
        addLookupName("ISLightBlazerr");
        heat = 6;
        damage = 7;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        waterShortRange = 2;
        waterMediumRange = 4;
        waterLongRange = 6;
        waterExtremeRange = 8;
        tonnage = 1.5;
        criticals = 2;
        bv = 148;
        cost = 15000;
        shortAV = 7;
        maxRange = RANGE_SHORT;
        flags = flags.andNot(F_PROTO_WEAPON);
        // Nothing to see here, move along
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(2852, DATE_NONE, 3077)
              .setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.WB).setStaticTechLevel(SimpleTechLevel.UNOFFICIAL);
    }
}
