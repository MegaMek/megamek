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

package megamek.common;

import java.io.Serializable;

/**
 * Represents the type of a game map. Note that low atmosphere (aka atmospheric) maps may come with or without
 * terrain and space maps may be high atmospheric maps when they are near a planet.
 * Radar and Capital Radar maps may be used in the Abstract Aerospace Combat rules of SO:AA.
 */
public enum BoardType implements Serializable {
    GROUND("G"),
    ATMOSPHERIC("A"), // aka "low atmosphere"
    SPACE("S"), // includes "high atmosphere"
    RADAR("R"),
    CAPITAL_RADAR("C");

    private final String code;

    BoardType(String code) {
        this.code = code;
    }

    public boolean isSpace() {
        return this == SPACE;
    }

    public boolean isLowAtmo() {
        return this == ATMOSPHERIC;
    }

    public boolean isGround() {
        return this == GROUND;
    }

    public boolean isRadarMap() {
        return this == RADAR;
    }

    public boolean isCapitalRadarMap() {
        return this == CAPITAL_RADAR;
    }

    public String getDisplayName() {
        return Messages.getString("MapType." + name());
    }

    /**
     * Returns a single character code identifying the this MapType (e.g. "G" for GROUND). Can be used e.g. in context
     * menus. Use {@link #mapTypeForCode(String)} to reconstruct the MapType.
     *
     * @return the code character for this MapType
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the MapType represented by the given code. (e.g. GROUND for "G"). Can be used e.g. in context menus.
     *
     * @return the MapType for the given code
     *
     * @throws IllegalArgumentException When the given code has no corresponding MapType
     */
    public static BoardType mapTypeForCode(String code) {
        for (BoardType mapType : values()) {
            if (mapType.code.equals(code)) {
                return mapType;
            }
        }
        throw new IllegalArgumentException("No MapType exists for the code " + code);
    }

    @Override
    public String toString() {
        return switch (this) {
            case CAPITAL_RADAR -> "Map Type: Capital Radar";
            case ATMOSPHERIC -> "Map Type: Low Atmosphere";
            case SPACE -> "Map Type: Space";
            case RADAR -> "Map Type: Radar";
            case GROUND -> "Map Type: Ground";
        };
    }
}
