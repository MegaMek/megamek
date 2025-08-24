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

package megamek.common.weapons.unofficial.innerSphere.ac;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.weapons.autoCannons.ACWeapon;

/**
 * @author BATTLEMASTER
 * @since Sep 25, 2004
 */
public class ISGAC6 extends ACWeapon {
    private static final long serialVersionUID = 49211848611799265L;

    public ISGAC6() {
        super();
        name = "GAC/6";
        setInternalName(name);
        addLookupName("IS Gatling Auto Cannon/6");
        addLookupName("Gatling Auto Cannon/6");
        addLookupName("Gatling AutoCannon/6");
        addLookupName("ISGAC6");
        addLookupName("IS Gatling Autocannon/6");
        heat = 6;
        damage = 12;
        rackSize = 6;
        minimumRange = 4;
        shortRange = 8;
        mediumRange = 16;
        longRange = 24;
        extremeRange = 32;
        tonnage = 14.0;
        criticalSlots = 4;
        bv = 316;
        cost = 300000;
        explosive = true; // when firing incendiary ammo
        shortAV = 12;
        medAV = 12;
        longAV = 12;
        extAV = 12;
        maxRange = RANGE_LONG;
        explosionDamage = damage;
        toHitModifier = -1;
        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_MEK_WEAPON)
              .or(F_AERO_WEAPON).or(F_TANK_WEAPON).or(F_PULSE);
        ammoType = AmmoType.AmmoTypeEnum.AC;
        atClass = CLASS_AC;
        // Going to Assume these are like IS RACs
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3060, 3062, 3071, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
    }
}
