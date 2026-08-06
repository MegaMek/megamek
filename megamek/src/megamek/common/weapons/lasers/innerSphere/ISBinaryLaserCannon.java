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

package megamek.common.weapons.lasers.innerSphere;

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
public class ISBinaryLaserCannon extends LaserWeapon {
    @Serial
    private static final long serialVersionUID = -6849916948609019186L;

    public ISBinaryLaserCannon() {
        super();
        name = "Binary Laser (Blazer) Cannon";
        setInternalName(name);
        shortName = "Blazer Cannon";
        addLookupName("IS Binary Laser Cannon");
        addLookupName("ISBlazer");
        addLookupName("ISBinaryLaserCannon");
        addLookupName("ISBinaryLaser");
        addLookupName("Blazer Cannon");
        heat = 16;
        damage = 12;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 9.0;
        criticalSlots = 4;
        bv = 222;
        cost = 200000;
        shortAV = 12;
        medAV = 12;
        maxRange = RANGE_MED;
        flags = flags.andNot(F_PROTO_WEAPON);
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        rulesRefs = "131, TO:AUE";
        techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(2812, DATE_NONE, 3077)
              .setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.WB).setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
