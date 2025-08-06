/*
 * Copyright (C) 2004-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.infantry;

import megamek.common.AmmoType;

/**
 * @author Hammer
 * @since March 20, 2022
 */
public class InfantrySniperRifleWilimtonRS17Stripped extends InfantryWeapon {
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySniperRifleWilimtonRS17Stripped() {
        super();

        name = "Sniper Rifle (Wilimton RS-17 (Stripped))";
        setInternalName(name);
        addLookupName("Wilimton RS-17 (Stripped)");
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
        bv = 1.05;
        tonnage = 0.009;
        infantryDamage = 0.53;
        infantryRange = 6;
        ammoWeight = 0.009;
        cost = 2000;
        ammoCost = 50;
        shots = 10;
        bursts = 1;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        rulesRefs = "Shrapnel #1";
        techAdvancement
              .setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(DATE_NONE, DATE_NONE, 3080, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, true, false, false)
              .setProductionFactions(Faction.WB);
    }
}
