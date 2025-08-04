/*
  Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.unofficial;

import megamek.common.AmmoType;
import megamek.common.weapons.autocannons.ACWeapon;

/**
 * @author Sebastian Brocks
 * @since Sep 25, 2004
 */
public class ISAC10i extends ACWeapon {
    private static final long serialVersionUID = 7447941274169853546L;

    public ISAC10i() {
        super();
        name = "AC/10i";
        setInternalName("ISAutocannon10i");
        addLookupName("ISAC10i");
        addLookupName("ISAC/10i");
        heat = 3;
        damage = 10;
        rackSize = 10;
        minimumRange = 1;
        shortRange = 7;
        mediumRange = 15;
        longRange = 20;
        extremeRange = 28;
        tonnage = 12.0;
        criticals = 7;
        bv = 167;
        cost = 410000;
        explosive = false;
        shortAV = 10;
        medAV = 10;
        longAV = 10;
        maxRange = RANGE_LONG;
        explosionDamage = damage;
        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_MEK_WEAPON)
              .or(F_AERO_WEAPON).or(F_TANK_WEAPON);
        ammoType = AmmoType.AmmoTypeEnum.ACi;
        atClass = CLASS_AC;
        // Since this is an unofficial Weapon I'm using the Normal AC10 Stats
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TechBase.ALL)
              .setUnofficial(true)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(2443, 2460, 2465, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2443, 2460, 2465, 2850, DATE_NONE)
              .setClanApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
    }
}
