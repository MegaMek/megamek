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
package megamek.common.equipment.enums;

/**
 * Where a nuclear device goes off over a ground map (IO:AE p.164). An air burst detonates above the Ground Zero hex: it
 * makes no crater, uses the weapon's air-burst blast figures, and only a surviving structure or water shields a unit
 * from it (IO:AE p.165).
 */
public enum NuclearBurstMode {
    /** A direct hit on the terrain: digs the weapon's crater; hills and buildings beside a unit can shield it. */
    GROUND,
    /** Set off above Ground Zero at the best height for ground damage: no crater, wider blast. */
    AIR;

    /**
     * @return {@code true} for an air burst
     */
    public boolean isAirBurst() {
        return this == AIR;
    }
}
