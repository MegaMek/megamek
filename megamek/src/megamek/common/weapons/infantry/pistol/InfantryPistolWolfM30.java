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

package megamek.common.weapons.infantry.pistol;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.weapons.infantry.InfantryWeapon;


public class InfantryPistolWolfM30 extends InfantryWeapon {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryPistolWolfM30() {
        super();

        name = "Pistol (Wolf M30)";
        setInternalName(name);
        addLookupName("Wolf M30");
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
        bv = .35;
        tonnage = 0.00095;
        infantryDamage = 0.35;
        infantryRange = 0;
        ammoWeight = 0.000025;
        cost = 250;
        ammoCost = 7;
        shots = 17;
        bursts = 1;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        rulesRefs = "Shrapnel #3";
        techAdvancement
              .setTechBase(TechBase.IS)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
              .setISAdvancement(DATE_NONE, DATE_NONE, 2100, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, true, false, false)
              .setProductionFactions(Faction.FW);

    }
}
