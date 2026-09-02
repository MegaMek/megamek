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
package megamek.common;

import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;

/**
 * Which unit types can receive partial cover at all.
 * <p>
 * Partial cover is a Mek rule: large support vehicles, grounded small craft and DropShips never receive it, even
 * though they rise more than one level, and ProtoMeks never do because they are only the height of vehicles (TW
 * p.102, p.171; Core p.66, p.136; TO:AR p.85). This class decides eligibility by unit type only. Whether the Mek is
 * standing, and whether the terrain, water or building actually covers it, is decided by the caller:
 * {@link LosEffects} for terrain, the building and water checks in the to-hit code and the bots for the rest.
 */
public final class PartialCover {

    private PartialCover() {
        // static use only
    }

    /**
     * @param target the unit being shot at
     *
     * @return {@code true} if the target's unit type can receive partial cover at all. This says nothing about
     *       whether it currently does - a prone Mek is eligible by type but is not covered.
     */
    public static boolean canReceive(@Nullable Targetable target) {
        return target instanceof Mek;
    }

    /**
     * Whether water in the target's own hex covers its lower half.
     * <p>
     * The water has to be exactly as deep as the Mek is tall, and the Mek has to be standing on the bottom so that
     * its top is level with the surface: Depth 1 for a standard Mek, Depth 2 for a superheavy, which stands three
     * levels high (TW p.102; Core p.66, p.240). Shallower water covers nothing, deeper water submerges the Mek and
     * blocks line of sight altogether, and a Mek held above the surface - on a bridge, or jumping - is in the open.
     * Unlike cover from a neighbouring hex, this applies even when the attacker is higher than the target, because
     * the water surrounds the Mek.
     *
     * @param target                the unit being shot at
     * @param targetHex             the hex the target occupies
     * @param targetRelativeHeight  the height of the target's top above the surface of its own hex, as
     *                              {@link Entity#relHeight()} reports it: 0 when the top is level with the surface
     *
     * @return {@code true} if the target is standing in water that covers its lower half
     */
    public static boolean isInPartialWater(@Nullable Entity target, @Nullable Hex targetHex,
          int targetRelativeHeight) {
        if (!canReceive(target) || (targetHex == null) || !targetHex.containsTerrain(Terrains.WATER)) {
            return false;
        }
        // A superheavy Mek stands three levels high, so Depth 2 is what reaches its waist (Core p.240)
        int coveringDepth = target.isSuperHeavy() ? 2 : 1;
        return (target.height() > 0)
              && (targetHex.terrainLevel(Terrains.WATER) == coveringDepth)
              && (targetRelativeHeight == 0);
    }
}
