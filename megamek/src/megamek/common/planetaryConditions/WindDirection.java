/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.planetaryConditions;

import megamek.common.Messages;

public enum WindDirection {
    SOUTH("SOUTH", "PlanetaryConditions.DisplayableName.WindDirection.South", "\u2191"),
    SOUTHWEST("SOUTHWEST", "PlanetaryConditions.DisplayableName.WindDirection.Southwest", "\u2B08"),
    NORTHWEST("NORTHWEST", "PlanetaryConditions.DisplayableName.WindDirection.Northwest", "\u2B0A"),
    NORTH("NORTH", "PlanetaryConditions.DisplayableName.WindDirection.North", "\u2193"),
    NORTHEAST("NORTHEAST", "PlanetaryConditions.DisplayableName.WindDirection.Northeast", "\u2B0B"),
    SOUTHEAST("SOUTHEAST", "PlanetaryConditions.DisplayableName.WindDirection.Southeast", "\u2B09"),
    RANDOM("RANDOM", "PlanetaryConditions.DisplayableName.WindDirection.RandomWindDirection", "");
    private final String externalId;
    private final String name;
    private final String indicator;

    WindDirection(final String externalId, final String name, final String indicator) {
        this.externalId = externalId;
        this.name = name;
        this.indicator = indicator;
    }

    public String getIndicator() {
        return indicator;
    }

    public String getExternalId() {
        return externalId;
    }

    @Override
    public String toString() {
        return Messages.getString(name);
    }

    public WindDirection rotateClockwise() {
        switch (this) {
            case NORTH:
                return NORTHEAST;
            case NORTHEAST:
                return SOUTHEAST;
            case SOUTHEAST:
                return SOUTH;
            case SOUTH:
                return SOUTHWEST;
            case SOUTHWEST:
                return NORTHWEST;
            case NORTHWEST:
                return NORTH;
            default:
                return RANDOM;
        }
    }

    public WindDirection rotateCounterClockwise() {
        switch (this) {
            case NORTH:
                return NORTHWEST;
            case NORTHWEST:
                return SOUTHWEST;
            case SOUTHWEST:
                return SOUTH;
            case SOUTH:
                return SOUTHEAST;
            case SOUTHEAST:
                return NORTHEAST;
            case NORTHEAST:
                return NORTH;
            default:
                return RANDOM;
        }
    }

    public boolean isRandomWindDirection() {
        return this == RANDOM;
    }

    public static WindDirection getWindDirection(int i) {
        return WindDirection.values()[i];
    }

    public static WindDirection getWindDirection(String s) {
        for (WindDirection condition : WindDirection.values()) {
            if (condition.getExternalId().equals(s)) {
                return condition;
            }
        }
        return WindDirection.NORTH;
    }
}
