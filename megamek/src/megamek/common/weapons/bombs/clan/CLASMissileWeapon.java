/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.bombs.clan;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.weapons.missiles.thuunderbolt.ThunderboltWeapon;

/**
 * @author Jay Lawson
 */
public class CLASMissileWeapon extends ThunderboltWeapon {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 8263429182520693147L;

    public CLASMissileWeapon() {
        super();
        name = "AS Missile";
        setInternalName(BombTypeEnum.AS.getWeaponName());
        heat = 0;
        damage = 30;
        rackSize = 1;
        minimumRange = 9;
        shortRange = 17;
        mediumRange = 25;
        longRange = 32;
        extremeRange = 50;
        tonnage = 2;
        criticalSlots = 0;
        hittable = false;
        bv = 0;
        cost = 15000;
        shortAV = 30;
        medAV = 30;
        longAV = 30;
        flags = flags.or(F_ANTI_SHIP)
              .or(F_MISSILE)
              .or(F_LARGE_MISSILE)
              .or(F_BOMB_WEAPON)
              .andNot(F_MEK_WEAPON)
              .andNot(F_TANK_WEAPON);
        maxRange = RANGE_LONG;
        ammoType = AmmoType.AmmoTypeEnum.AS_MISSILE;
        capital = false;
        this.missileArmor = 30;
        rulesRefs = "170, TO:AUE";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(DATE_NONE, DATE_NONE, 3076, DATE_NONE, DATE_NONE)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

}
