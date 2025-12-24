/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.weapons.missiles.thuunderbolt.ThunderboltWeapon;

/**
 * @author Jay Lawson
 * @author Dave Nawton
 */
public class CLAAAMissileWeapon extends ThunderboltWeapon {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -2043203178614130517L;

    public CLAAAMissileWeapon() {
        super();

        this.name = "AAA Missile";
        this.setInternalName(BombTypeEnum.AAA.getWeaponName());
        this.heat = 0;
        this.damage = 20;
        this.rackSize = 1;
        this.minimumRange = 6;
        this.shortRange = 12;
        this.mediumRange = 18;
        this.longRange = 24;
        this.extremeRange = 36;
        this.tonnage = 1;
        this.criticalSlots = 0;
        this.hittable = false;
        this.bv = 0;
        this.cost = 9000;
        this.flags = flags.or(F_MISSILE)
              .or(F_LARGE_MISSILE)
              .or(F_BOMB_WEAPON)
              .andNot(F_MEK_WEAPON)
              .andNot(F_TANK_WEAPON);
        this.shortAV = 20;
        this.medAV = 20;
        this.maxRange = RANGE_MED;
        this.ammoType = AmmoType.AmmoTypeEnum.AAA_MISSILE;
        this.capital = false;
        this.missileArmor = 20;
        cost = 9000;
        rulesRefs = "169, TO:AUE";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3069, 3072, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CWX);
    }
}
