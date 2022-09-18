/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.strategicBattleSystems;

public enum SBFMovementMode {

    //TODO: Nah, not in SBF
    UNKNOWN("Unknown", "Unknown", Integer.MAX_VALUE),
    BIM_AEROSPACE_WALK("l", "Mek Walk", 50),
    LAM_AEROSPACE_WALK("l", "Mek Walk", 65),
    MEK_WALK("l", "Mek Walk", 60),
    WARSHIP("aw", "Warship Thrust", 31),
    BA_WALK("l", "BAttleArmor Walk", 50),
    WHEELED("w", "Wheeled", 20),
    VTOL("v", "VTOL", 80),
    INFANTRY_FOOT("f", "Infantry Walk", 52),
    AIRSHIP("i", "Airship", 41),
    RAIL("r", "Rail", 10),
    HOVER("h", "Hover", 30),
    WIGE("g", "WiGE", 81),
    SPHEROID("p", "Spheroid Craft", 21),
    AERODYNE("a", "Aerodyne Craft", 54),
    QUAD_TRACKED("qt", "Quad Tracked", 63),
    QUAD_WHEELED("qw", "Quad Wheeled", 64),
    STATION_KEEPING("k", "Station-keeping", 11),
    NAVAL("n", "Naval", 0),
    TRACKED("t", "Tracked", 40),
    SUBMARINE("s", "Submarine", 0),
    MEK_UMU("s", "Mek UMU", 62),
    BA_UMU("s", "BA UMU", 51),
    MEK_JUMP("j", "Mek Jump", 70),
    BA_JUMP("j", "BA Jump", 61),
    CI_JUMP("j", "CI Jump", 53);

    public final String mode;
    public final String name;
    public final int rank;

    SBFMovementMode(String mode, String modeName, int rank) {
        this.mode = mode;
        name = modeName;
        this.rank = rank;
    }

}
