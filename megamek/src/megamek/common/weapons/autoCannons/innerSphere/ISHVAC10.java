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
import megamek.common.equipment.Mounted;
import megamek.common.weapons.autoCannons.HVACWeapon;

/**
 * @author Jason Tighe
 * @since Sep 25, 2004
 */
public class ISHVAC10 extends HVACWeapon {
    @Serial
    private static final long serialVersionUID = 4958849713169213573L;

    public ISHVAC10() {
        super();
        name = "HVAC/10";
        setInternalName("Hyper Velocity Auto Cannon/10");
        addLookupName("IS Hyper Velocity Auto Cannon/10");
        addLookupName("ISHVAC10");
        addLookupName("IS Hyper Velocity Autocannon/10");
        heat = 7;
        damage = 10;
        rackSize = 10;
        shortRange = 6;
        mediumRange = 12;
        longRange = 20;
        extremeRange = 30;
        tonnage = 14.0;
        criticalSlots = 6;
        bv = 158;
        cost = 230000;
        shortAV = 10;
        medAV = 10;
        longAV = 10;
        maxRange = RANGE_LONG;
        explosionDamage = 10;
        rulesRefs = "97, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3059, 3079)
              .setPrototypeFactions(Faction.CC).setProductionFactions(Faction.CC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> ignore) {
        return 1;
    }

}
