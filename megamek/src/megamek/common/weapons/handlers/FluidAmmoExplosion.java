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

import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.units.Entity;

/**
 * Applies the per-fluid critical-hit explosion side-effects of Flamer / Fluid Gun / Sprayer fluid
 * ammunition (TO:AUE pp.173-174), keeping that rule out of the (already very large) explosion code in
 * the game manager.
 *
 * @author The MegaMek Team
 */
public final class FluidAmmoExplosion {

    private FluidAmmoExplosion() {
    }

    /**
     * Applies the critical-hit explosion side-effects of a fluid ammunition bin to its carrier and returns
     * the explosion damage to deal (TO:AUE pp.173-174):
     * <ul>
     *     <li>Coolant removes 3 heat (then explodes for a flat 2 internal).</li>
     *     <li>Flame-Retardant Foam removes 2 heat (then explodes for a flat 2 internal).</li>
     *     <li>Corrosive deals an extra 1D6 internal now and queues 1D6/2 (round up) internal for the End
     *         Phase of the following turn.</li>
     *     <li>Every other fluid (and all non-fluid ammo) keeps its base explosion damage.</li>
     * </ul>
     *
     * @param entity     the unit whose ammo is exploding
     * @param ammoType   the exploding ammunition
     * @param baseDamage the explosion damage already computed for the bin
     *
     * @return the explosion damage to actually deal
     */
    public static int applyCriticalEffects(Entity entity, AmmoType ammoType, int baseDamage) {
        boolean isFluidAmmo = (ammoType.getAmmoType() == AmmoTypeEnum.FLUID_GUN)
              || (ammoType.getAmmoType() == AmmoTypeEnum.VEHICLE_FLAMER)
              || (ammoType.getAmmoType() == AmmoTypeEnum.HEAVY_FLAMER);
        if (!isFluidAmmo) {
            return baseDamage;
        }

        EnumSet<Munitions> munitions = ammoType.getMunitionType();
        if (munitions.contains(Munitions.M_COOLANT)) {
            entity.coolFromExternal += 3;
            return 2;
        }
        if (munitions.contains(Munitions.M_ANTI_FLAME_FOAM)) {
            entity.coolFromExternal += 2;
            return 2;
        }
        if (munitions.contains(Munitions.M_CORROSIVE)) {
            entity.addNextTurnCorrosiveDamage((int) Math.ceil(Compute.d6() / 2.0));
            return baseDamage + Compute.d6();
        }
        return baseDamage;
    }
}
