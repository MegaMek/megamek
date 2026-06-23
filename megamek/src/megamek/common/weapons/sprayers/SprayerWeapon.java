/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons.sprayers;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.FluidMunitionHandlers;
import megamek.common.weapons.handlers.SprayerHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Base class for Sprayers (TM pp.248-249). A Sprayer "fires" as a weapon - dispensing Fluid Gun
 * ammunition at an adjacent target - but is an industrial fitting rather than a true weapon: it
 * needs no gunner, cannot benefit from a fire control system or targeting computer (every attack
 * takes the +2 no-fire-control penalty), and inflicts direct damage to conventional infantry only,
 * equivalent to a battle-armor flamer at a range of 1 hex (TW p.217). Its battlefield value comes
 * from the fluid effects of its loaded ammunition.
 *
 * @author The MegaMek Team
 */
public abstract class SprayerWeapon extends AmmoWeapon {
    @Serial
    private static final long serialVersionUID = -6427152922142655350L;

    protected SprayerWeapon() {
        super();
        ammoType = AmmoType.AmmoTypeEnum.FLUID_GUN;
        heat = 0;
        damage = 0;
        rackSize = 1;
        shortRange = 1;
        mediumRange = 1;
        longRange = 1;
        extremeRange = 1;
        infDamageClass = WeaponType.WEAPON_BURST_3D6;
        // F_SPRAYER marks it as a non-true weapon (no gunner / no fire control); F_BURST_FIRE delivers
        // full burst damage to infantry; F_NO_FIRES because the spray itself does not ignite targets.
        flags = flags.or(F_SPRAYER).or(F_BURST_FIRE).or(F_NO_FIRES);
    }

    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        try {
            Entity attacker = game.getEntity(waa.getEntityId());
            if (attacker != null) {
                Mounted<?> linkedAmmo = attacker.getEquipment(waa.getWeaponId()).getLinked();
                // A Sprayer's effect is governed by the loaded fluid, not its base BA-flamer profile
                // (TO:AUE p.172). It feeds from Fluid Gun ammo, so it resolves every fluid the same way a
                // Fluid Gun does (Water is the default round); only Inferno Fuel is barred (handled by the
                // to-hit eligibility check). Anything else falls back to the plain Sprayer attack.
                if ((linkedAmmo != null) && (linkedAmmo.getType() instanceof AmmoType ammoType)) {
                    AttackHandler fluidHandler =
                          FluidMunitionHandlers.forFluidGunOrSprayer(ammoType, toHit, waa, game, manager);
                    if (fluidHandler != null) {
                        return fluidHandler;
                    }
                }
            }
            return new SprayerHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Sprayer Handler received a null entity.");
        }
        return null;
    }
}
