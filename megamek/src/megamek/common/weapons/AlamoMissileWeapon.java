/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons;

import megamek.common.TechAdvancement;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.BombType.BombTypeEnum;
import megamek.common.weapons.capitalWeapons.CapitalMissileWeapon;

/**
 * @author Jay Lawson
 */
public class AlamoMissileWeapon extends CapitalMissileWeapon {
    private static final long serialVersionUID = 3672430739887768960L;

    public AlamoMissileWeapon() {
        super();
        name = "Alamo Missile";
        setInternalName(BombTypeEnum.ALAMO.getWeaponName());
        flags = flags.or(F_BOMB_WEAPON).or(F_MISSILE);
        heat = 0;
        damage = 10;
        rackSize = 1;
        shortRange = 6;
        mediumRange = 12;
        longRange = 24;
        extremeRange = 40;
        tonnage = 0;
        criticals = 0;
        hittable = false;
        bv = 0.0;
        cost = 0;
        shortAV = 10;
        medAV = 10;
        missileArmor = 20;
        maxRange = RANGE_MED;
        ammoType = AmmoType.AmmoTypeEnum.ALAMO;
        capital = true;
        techAdvancement.setTechBase(TechAdvancement.TechBase.IS)
              .setISAdvancement(3071, DATE_NONE, DATE_NONE)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.E);
    }
}
