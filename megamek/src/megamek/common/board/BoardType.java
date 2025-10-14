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

package megamek.common.board;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Optional;

import megamek.common.Messages;

/**
 * Represents the type of game board (map). Note that low altitude (aka atmospheric) maps may come with or without
 * terrain and space maps may be high altitude maps when they are near a planet. Radar and Capital Radar maps may be
 * used in the Abstract Aerospace Combat rules of SO:AA and SBF.
 */
public enum BoardType implements Serializable {
    GROUND("G"),

    // The following are intentionally not named as in the rules so the temptation to compare directly with them is
    // low. Compare using the isXYZ() methods

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

    /**
     * @return True if this board type is a space map, either close to a planet with some atmospheric hexes ("high
     *       altitude") or in deeper space.
     */
    public boolean isSpace() {
        return this == FAR_SPACE || this == NEAR_SPACE;
    }

    /**
     * @return True if this board type is an atmospheric map, either with or without terrain ("sky"). This has nothing
     *       to do with planetary conditions, only the type of board.
     */
    public boolean isLowAltitude() {
        return this == SKY_WITH_TERRAIN || this == SKY;
    }

    /**
     * @return True if this board type is an atmospheric map without terrain ("sky"). This has nothing to do with
     *       planetary conditions, only the type of board.
     */
    public boolean isSky() {
        return this == SKY;
    }

    /**
     * @return True if this board type is a ground map.
     */
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
     * Returns a single character code identifying the MapType (e.g. "G" for GROUND). Can be used e.g. in context menus.
     * Use {@link #boardTypeForCode(String)} to reconstruct the MapType.
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
            case CAPITAL_RADAR -> "Capital Radar";
            case SKY -> "Low Atmosphere (Sky)";
            case SKY_WITH_TERRAIN -> "Low Atmosphere (Terrain)";
            case FAR_SPACE -> "Space";
            case NEAR_SPACE -> "High Atmosphere";
            case RADAR -> "Radar";
            case GROUND -> "Ground";
        };
    }

    /**
     * @return A value to use for ordering boards according to ground - atmospheric - space.
     */
    public int orderValue() {
        return switch (this) {
            case GROUND -> 0;
            case SKY, SKY_WITH_TERRAIN -> 1;
            case FAR_SPACE, NEAR_SPACE -> 2;
            case RADAR -> 100;
            case CAPITAL_RADAR -> 101;
        };
    }
}
