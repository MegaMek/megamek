/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common.enums;

import java.util.Arrays;

/**
 * A hazardous liquid pool can be wind-blown, pushed by water flow, both, or neither.
 */
public enum HazardousLiquidPoolType {
    // Wind blown hazardous is for MapPack Alien Worlds' Wind Blown Hazardous Liquid rules
    // Wind blown (MP: Alien Rules) isn't implemented
    // Water flow (TO:AR 47 for Hazardous Pools rules) isn't implemented
    NORMAL(0),
    WIND_BLOWN(1),
    FLOWS(2),
    FLOWS_AND_WIND_BLOWN(3);

    private final Integer terrainLevel;

    HazardousLiquidPoolType(final Integer terrainLevel) {
        this.terrainLevel = terrainLevel;
    }

    public int getTerrainLevel() {
        return this.terrainLevel;
    }

    public static HazardousLiquidPoolType getType(final int ordinal) {
        return Arrays.stream(HazardousLiquidPoolType.values())
              .filter(type -> type.ordinal() == ordinal)
              .findFirst()
              .orElse(NORMAL);
    }
}
