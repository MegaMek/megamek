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
package megamek.common.weapons.handlers;

import java.util.EnumSet;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Selects the {@link AttackHandler} for the fluid ammunitions shared by Fluid Guns and Sprayers, which
 * both feed from Fluid Gun ammo and resolve those fluids identically (TO:AUE pp.172-174). Keeping this
 * mapping in one place avoids duplicating it in each weapon's {@code getCorrectHandler}. Inferno Fuel
 * (Fluid Guns only) and each weapon's default attack are handled by the calling weapon.
 *
 * @author The MegaMek Team
 */
public final class FluidMunitionHandlers {

    private FluidMunitionHandlers() {
    }

    /**
     * @param ammoType the fluid ammunition loaded in the weapon
     * @param toHit    the to-hit data for the attack
     * @param waa      the weapon attack action
     * @param game     the game
     * @param manager  the game manager
     *
     * @return the handler for Coolant, Water, Corrosive, Flame-Retardant Foam, Oil Slick or
     *       Paint/Obscurant ammo, or {@code null} if {@code ammoType} is none of these (so the caller
     *       should fall back to Inferno Fuel handling or its default attack)
     *
     * @throws EntityLoadingException if the attacking entity cannot be loaded
     */
    @Nullable
    public static AttackHandler forFluidGunOrSprayer(AmmoType ammoType, ToHitData toHit, WeaponAttackAction waa,
          Game game, TWGameManager manager) throws EntityLoadingException {
        EnumSet<Munitions> munitions = ammoType.getMunitionType();
        if (munitions.contains(Munitions.M_COOLANT)) {
            return new FluidGunCoolHandler(toHit, waa, game, manager);
        }
        if (munitions.contains(Munitions.M_WATER)) {
            return new WaterHandler(toHit, waa, game, manager);
        }
        if (munitions.contains(Munitions.M_CORROSIVE)) {
            return new CorrosiveHandler(toHit, waa, game, manager);
        }
        if (munitions.contains(Munitions.M_ANTI_FLAME_FOAM)) {
            return new FoamHandler(toHit, waa, game, manager);
        }
        if (munitions.contains(Munitions.M_OIL_SLICK)) {
            return new OilSlickHandler(toHit, waa, game, manager);
        }
        if (munitions.contains(Munitions.M_PAINT_OBSCURANT)) {
            return new PaintObscurantHandler(toHit, waa, game, manager);
        }
        return null;
    }
}
