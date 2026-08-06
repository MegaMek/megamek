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

public class InfantryLaserPistolAWAWilibyMk4LaserPistol extends InfantryWeapon {

    @Serial
    private static final long serialVersionUID = 1L; // Update for each unique class

    public InfantryLaserPistolAWAWilibyMk4LaserPistol() {
        super();

        name = "Laser Pistol (AWA Wiliby MK4 LASER PISTOL)";
        setInternalName(name);
        addLookupName("WILIBYMK4");
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
        cost = 1500;
        bv = 0.028;
        tonnage = 0.0018;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
        infantryDamage = 0.09;
        infantryRange = 1;
        // ammoWeight is not applicable (NA)
        shots = 1;
        // ammoCost is not applicable (NA)
        bursts = 1;
        rulesRefs = "Shrapnel #9";
        techAdvancement.setTechBase(TechBase.IS);
        techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, DATE_ES, DATE_NONE, DATE_NONE);
        techAdvancement.setTechRating(TechRating.D);
        techAdvancement.setAvailability(AvailabilityValue.C,
              AvailabilityValue.C,
              AvailabilityValue.C,
              AvailabilityValue.C);
        techAdvancement.setISApproximate(false, false, true, false, false);
        techAdvancement.setProductionFactions(Faction.FS);
    }
}
