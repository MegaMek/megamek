/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common.weapons.infantry.laser.pistol;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.weapons.infantry.InfantryWeapon;

public class InfantryLaserPistolBR25 extends InfantryWeapon {

    @Serial
    private static final long serialVersionUID = 1L; // Update for each unique class

    public InfantryLaserPistolBR25() {
        super();

        name = "Laser Pistol (BR-25)";
        setInternalName(name);
        addLookupName("BR25");
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
        cost = 950;
        bv = 0.01575;
        tonnage = 0.0013;
        infantryDamage = 0.05;
        infantryRange = 1;
        shots = 1;
        bursts = 1; // Bursts value is now always shown
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
        rulesRefs = "Shrapnel #9";
        techAdvancement.setTechBase(TechBase.IS);
        techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, DATE_ES, DATE_NONE, DATE_NONE);
        techAdvancement.setTechRating(TechRating.D);
        techAdvancement.setAvailability(AvailabilityValue.D,
              AvailabilityValue.D,
              AvailabilityValue.D,
              AvailabilityValue.C);
        techAdvancement.setISApproximate(false, false, true, false, false);
        techAdvancement.setProductionFactions(Faction.TC);
    }
}
