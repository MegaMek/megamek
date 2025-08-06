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

import java.io.Serial;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.IBomber;
import megamek.common.ToHitData;
import megamek.common.WeaponTypeFlag;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Jay Lawson
 */
public class SpaceBombAttack extends Weapon {
    @Serial
    private static final long serialVersionUID = -7842514353177676459L;

    public SpaceBombAttack() {
        name = "Space Bomb";
        setInternalName(IBomber.SPACE_BOMB_ATTACK);
        heat = 0;
        damage = DAMAGE_SPECIAL;
        ammoType = AmmoType.AmmoTypeEnum.NA;
        minimumRange = WEAPON_NA;
        shortRange = 0;
        mediumRange = 0;
        longRange = 0;
        extremeRange = 0;
        tonnage = 0.0;
        criticals = 0;
        bv = 0;
        cost = 0;
        flags = flags.or(F_SPACE_BOMB).or(F_SOLO_ATTACK).or(WeaponTypeFlag.INTERNAL_REPRESENTATION);
        hittable = false;
        techAdvancement.setAvailability(AvailabilityValue.C,
              AvailabilityValue.C,
              AvailabilityValue.C,
              AvailabilityValue.C);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
          WeaponAttackAction waa, Game game, TWGameManager manager) {
        return new SpaceBombAttackHandler(toHit, waa, game, manager);
    }
}
