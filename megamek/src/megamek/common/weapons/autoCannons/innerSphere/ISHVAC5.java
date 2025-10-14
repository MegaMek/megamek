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

package megamek.common.weapons.autoCannons.innerSphere;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.autoCannons.HVACWeapon;

/**
 * @author Jason Tighe
 * @since Sep 25, 2004
 */
public class ISHVAC5 extends HVACWeapon {
    @Serial
    private static final long serialVersionUID = -1116752747486372187L;

    public ISHVAC5() {
        super();
        name = "HVAC/5";
        setInternalName("Hyper Velocity Auto Cannon/5");
        addLookupName("IS Hyper Velocity Auto Cannon/5");
        addLookupName("ISHVAC5");
        addLookupName("IS Hyper Velocity Autocannon/5");
        sortingName = "HVAC/05";
        heat = 3;
        damage = 5;
        rackSize = 5;
        shortRange = 8;
        mediumRange = 16;
        longRange = 28;
        extremeRange = 42;
        tonnage = 12.0;
        criticalSlots = 4;
        bv = 109;
        cost = 160000;
        shortAV = 5;
        medAV = 5;
        longAV = 5;
        extAV = 5;
        maxRange = RANGE_EXT;
        explosionDamage = 5;
        rulesRefs = "285, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3059, 3079)
              .setPrototypeFactions(Faction.CC).setProductionFactions(Faction.CC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }
}
