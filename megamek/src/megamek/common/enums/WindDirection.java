/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.MegaMek;
import megamek.common.Messages;

import java.util.EnumSet;
import java.util.ResourceBundle;

public enum WindDirection {
    NORTH("NORTH", "PlanetaryConditions.DisplayableName.WindDirection.North", "PlanetaryConditions.Indicator.WindDirection.North"),
    NORTHEAST("NORTHEAST", "PlanetaryConditions.DisplayableName.WindDirection.Northeast", "PlanetaryConditions.Indicator.WindDirection.Northeast"),
    SOUTHEAST("SOUTHEAST", "PlanetaryConditions.DisplayableName.WindDirection.Southeast", "PlanetaryConditions.Indicator.WindDirection.Southeast"),
    SOUTH("SOUTH", "PlanetaryConditions.DisplayableName.WindDirection.South", "PlanetaryConditions.Indicator.WindDirection.South"),
    SOUTHWEST("SOUTHWEST", "PlanetaryConditions.DisplayableName.WindDirection.Southwest", "PlanetaryConditions.Indicator.WindDirection.Southwest"),
    NORTHWEST("NORTHWEST", "PlanetaryConditions.DisplayableName.WindDirection.Northwest", "PlanetaryConditions.Indicator.WindDirection.Northwest"),
    RANDOM("RANDOM", "PlanetaryConditions.DisplayableName.WindDirection.RandomWindDirection", "PlanetaryConditions.Indicator.WindDirection.RandomWindDirection");
    private final String externalId;
    private final String name;
    private final String indicator;

    WindDirection(final String externalId, final String name, final String indicator) {
        final ResourceBundle resources = ResourceBundle.getBundle("megamek.common.messages", MegaMek.getMMOptions().getLocale());
        this.externalId = externalId;
        this.name = resources.getString(name);
        this.indicator = resources.getString(indicator);
    }

    public String getIndicator() {
        return indicator;
    }

    public String getExternalId() {
        return externalId;
    }

    @Override
    public String toString() {
        return name;
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

    public static WindDirection getWindDirection(int i) {
        return WindDirection.values()[i];
    }

    public static boolean isRandomWindDirection(WindDirection windDirection) {
        return windDirection == RANDOM;
    }
}
