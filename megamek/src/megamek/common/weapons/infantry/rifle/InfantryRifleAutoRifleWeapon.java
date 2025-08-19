/*
 * Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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

/*
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons.infantry.rifle;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * @author Ben Grills
 */
public class InfantryRifleAutoRifleWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryRifleAutoRifleWeapon() {
        super();

        name = "Auto-Rifle (Modern, Generic)";
        setInternalName(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);
        addLookupName(name);
        addLookupName("Auto Rifle");
        addLookupName("Auto-Rifle");
        addLookupName("Infantry Automatic Rifle");
        addLookupName("InfantryAutoRifle");
        addLookupName("Infantry Auto Rifle");
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
        cost = 80;
        bv = 1.59;
        tonnage = .004;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
        infantryDamage = 0.52;
        infantryRange = 1;
        ammoWeight = 0.00048;
        ammoCost = 2;
        shots = 30;
        bursts = 2;
        damage = 1;
        minimumRange = 0;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        rulesRefs = " 273, TM";
        techAdvancement.setTechBase(TechBase.ALL).setISAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(1950, 1950, 1950, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false).setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A);

    }
}
