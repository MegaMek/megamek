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

/*
 * Created on March 20, 2022
 * @author Hammer
 */

package megamek.common.weapons.infantry.smg;

import megamek.common.equipment.AmmoType;
import megamek.common.weapons.infantry.InfantryWeapon;

public class InfantrySMGWC6 extends InfantryWeapon {

    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySMGWC6() {
        super();

        name = "SMG (WC-6)";
        setInternalName(name);
        addLookupName("WC-6");
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
        bv = .27;
        tonnage = 0.0022;
        infantryDamage = 0.27;
        infantryRange = 2;
        ammoWeight = 0.0022;
        cost = 800;
        ammoCost = 20;
        shots = 30;
        bursts = 5;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        rulesRefs = "Shrapnel #5";
        techAdvancement
              .setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, DATE_NONE, 2830, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setProductionFactions(Faction.CLAN);
    }
}
