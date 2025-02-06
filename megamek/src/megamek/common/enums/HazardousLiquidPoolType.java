/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
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
        return Arrays.stream(HazardousLiquidPoolType.values()).filter(type -> type.ordinal() == ordinal).findFirst().orElse(NORMAL);
    }
}
