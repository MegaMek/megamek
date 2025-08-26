/*
 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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
 * Created on Sep 23, 2004
 *
 */
package megamek.common.weapons.flamers;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.VehicleFlamerCoolHandler;
import megamek.common.weapons.handlers.VehicleFlamerHandler;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Andrew Hunter
 */
public abstract class VehicleFlamerWeapon extends AmmoWeapon {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -8729838198434670197L;

    /**
     *
     */
    public VehicleFlamerWeapon() {
        super();
        flags = flags.or(F_MEK_WEAPON).or(F_TANK_WEAPON).or(F_PROTO_WEAPON)
              .or(F_FLAMER).or(F_ENERGY).or(F_BURST_FIRE);
        ammoType = AmmoType.AmmoTypeEnum.VEHICLE_FLAMER;
        atClass = CLASS_POINT_DEFENSE;
    }

    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager manager) {
        try {
            Entity entity = game.getEntity(waa.getEntityId());

            if (entity != null) {
                Object ammo = entity.getEquipment(waa.getWeaponId()).getLinked().getType();

                if (ammo instanceof AmmoType ammoType) {
                    if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_COOLANT)) {
                        return new VehicleFlamerCoolHandler(toHit, waa, game, manager);
                    }

                }
            }

            return new VehicleFlamerHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }

}
