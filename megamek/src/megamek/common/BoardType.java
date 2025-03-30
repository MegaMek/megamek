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
import java.util.Arrays;
import java.util.Optional;

/**
 * Represents the type of a game map. Note that low atmosphere (aka atmospheric) maps may come with or without
 * terrain and space maps may be high atmospheric maps when they are near a planet.
 * Radar and Capital Radar maps may be used in the Abstract Aerospace Combat rules of SO:AA.
 */
public enum BoardType implements Serializable {
    GROUND("G"),
    
    // The following are intentionally not named as in the rules so the temptation to compare directly with them is 
    // low. Compare using the is... methods
    FAR_SPACE("FS"), // space without planetary surface
    NEAR_SPACE("NS"), // space with planetary surface, aka high atmospheric
    SKY("SA"), // low atmosphere without ground terrain
    SKY_WITH_TERRAIN("GA"), // low atmosphere with ground terrain
    
    RADAR("R"),
    CAPITAL_RADAR("C");


    private final String code;

    BoardType(String code) {
        this.code = code;
    }

    public boolean isSpace() {
        return this == FAR_SPACE || this == NEAR_SPACE;
    }

    public boolean isLowAtmo() {
        return this == SKY_WITH_TERRAIN || this == SKY;
    }

    public boolean isGround() {
        return this == GROUND;
    }

    public boolean isHighAltitude() {
        return this == NEAR_SPACE;
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
     * menus. Use {@link #boardTypeForCode(String)} to reconstruct the MapType.
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
    public static Optional<BoardType> boardTypeForCode(final String code) {
        return Arrays.stream(values()).filter(t -> t.code.equals(code)).findAny();
    }

    @Override
    public String toString() {
        return switch (this) {
            case CAPITAL_RADAR -> "Board Type: Capital Radar";
            case SKY -> "Board Type: Low Atmosphere (Sky)";
            case SKY_WITH_TERRAIN -> "Board Type: Low Atmosphere (Terrain)";
            case FAR_SPACE -> "Board Type: Space";
            case NEAR_SPACE -> "Board Type: High Atmosphere";
            case RADAR -> "Board Type: Radar";
            case GROUND -> "Board Type: Ground";
        };
    }
}
