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

package megamek.common.weapons.infantry.shotgun;

import megamek.common.equipment.AmmoType;
import megamek.common.weapons.infantry.InfantryWeapon;


public class InfantryShotgunSGS9E extends InfantryWeapon {

    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryShotgunSGS9E() {
        super();

        name = "Shotgun (SGS-9E)";
        setInternalName(name);
        addLookupName("SGS-9E");
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
        bv = 1.375;
        tonnage = 0.0045;
        infantryDamage = 0.69;
        infantryRange = 1;
        ammoWeight = 0.0045;
        cost = 1500;
        ammoCost = 50;
        shots = 21;
        bursts = 3;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_ENCUMBER);
        rulesRefs = "Shrapnel #7";
        techAdvancement
              .setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, DATE_NONE, 2850, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setProductionFactions(Faction.CLAN);
    }
}
