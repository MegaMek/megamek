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
import megamek.common.weapons.lasers.PulseLaserWeapon;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class ISPulseLaserLarge extends PulseLaserWeapon {
    @Serial
    private static final long serialVersionUID = 94533476706680275L;

    public ISPulseLaserLarge() {
        super();
        name = "Large Pulse Laser";
        setInternalName("ISLargePulseLaser");
        addLookupName("IS Pulse Large Laser");
        addLookupName("IS Large Pulse Laser");
        sortingName = "Laser Pulse D";
        heat = 10;
        damage = 9;
        toHitModifier = -2;
        shortRange = 3;
        mediumRange = 7;
        longRange = 10;
        extremeRange = 15;
        waterShortRange = 2;
        waterMediumRange = 5;
        waterLongRange = 7;
        waterExtremeRange = 10;
        tonnage = 7.0;
        criticalSlots = 2;
        bv = 119;
        cost = 175000;
        shortAV = 9;
        medAV = 9;
        maxRange = RANGE_MED;
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
}
