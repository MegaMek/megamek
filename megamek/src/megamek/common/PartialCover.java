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
import megamek.common.units.Mek;
import megamek.common.units.Targetable;

/**
 * Which unit types can receive partial cover at all.
 * <p>
 * Partial cover is a Mek rule: large support vehicles, grounded small craft and DropShips never receive it, even
 * though they rise more than one level, and ProtoMeks never do because they are only the height of vehicles (TW
 * p.102, p.171; TO:AR p.85). This class decides eligibility by unit type only. Whether the Mek is standing, and
 * whether the terrain, water or building actually covers it, is decided by the caller: {@link LosEffects} for terrain,
 * the building and water checks in the to-hit code and the bots for the rest.
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
}
