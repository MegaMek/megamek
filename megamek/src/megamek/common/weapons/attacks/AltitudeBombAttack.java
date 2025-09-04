/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2009-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.attacks;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponTypeFlag;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.IBomber;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.BombAttackHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jay Lawson
 */
public class AltitudeBombAttack extends Weapon {
    @Serial
    private static final long serialVersionUID = 1837670588683382376L;

    public AltitudeBombAttack() {
        name = "Altitude Bomb";
        setInternalName(IBomber.ALT_BOMB_ATTACK);
        heat = 0;
        damage = DAMAGE_SPECIAL;
        ammoType = AmmoType.AmmoTypeEnum.NA;
        minimumRange = WEAPON_NA;
        shortRange = 0;
        mediumRange = 0;
        longRange = 0;
        extremeRange = 0;
        maxRange = 0;
        tonnage = 0.0;
        criticalSlots = 0;
        bv = 0;
        cost = 0;
        flags = flags.or(F_ALT_BOMB).or(WeaponTypeFlag.INTERNAL_REPRESENTATION);
        hittable = false;
        techAdvancement.setAvailability(AvailabilityValue.B,
              AvailabilityValue.B,
              AvailabilityValue.B,
              AvailabilityValue.B);
    }

    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        try {
            return new BombAttackHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;

    }
}
