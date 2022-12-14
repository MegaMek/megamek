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

import megamek.common.alphaStrike.ASUnitType;
import megamek.common.alphaStrike.AlphaStrikeElement;

import static megamek.common.alphaStrike.BattleForceSUA.*;

public enum SBFMovementMode {

    UNKNOWN("Unknown", "Unknown", Integer.MAX_VALUE),
    BIM_AEROSPACE_WALK("", "Mek Walk", 50),
    LAM_AEROSPACE_WALK("", "Mek Walk", 65),
    MEK_WALK("", "Mek Walk", 60),
    WARSHIP("aw", "Warship Thrust", 31),
    BA_WALK("", "BAttleArmor Walk", 50),
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
    MEK_JUMP("", "Mek Jump", 70),
    BA_JUMP("", "BA Jump", 61),
    CI_JUMP("", "CI Jump", 53);

    public final String code;
    public final String name;
    public final int rank;

    SBFMovementMode(String mode, String modeName, int rank) {
        code = mode;
        name = modeName;
        this.rank = rank;
    }

    public static SBFMovementMode modeForElement(SBFUnit unit, AlphaStrikeElement element) {
        if (unit.isAerospace() && element.isGround()) {
            if (element.hasSUA(BIM)) {
                return BIM_AEROSPACE_WALK;
            } else if (element.hasSUA(LAM)) {
                return LAM_AEROSPACE_WALK;
            } else if (element.hasSUA(SOA)) {
                return STATION_KEEPING;
            } else {
                return UNKNOWN;
            }
        }
        switch (element.getPrimaryMovementMode()) {
            case "":
                if (element.isType(ASUnitType.BM, ASUnitType.PM, ASUnitType.IM)) {
                    return MEK_WALK;
                } else if (element.isType(ASUnitType.WS)) {
                    return WARSHIP;
                } else if (element.isType(ASUnitType.BA)) {
                    return BA_WALK;
                }
                break;
            case "w":
            case "w(b)":
            case "w(m)":
            case "m":
                return WHEELED;
            case "v":
                return VTOL;
            case "f":
                return INFANTRY_FOOT;
            case "i":
                return AIRSHIP;
            case "r":
                return RAIL;
            case "h":
                return HOVER;
            case "g":
                return WIGE;
            case "p":
                return SPHEROID;
            case "a":
                return AERODYNE;
            case "qt":
                return QUAD_TRACKED;
            case "qw":
                return QUAD_WHEELED;
            case "k":
                return STATION_KEEPING;
            case "n":
                return NAVAL;
            case "t":
                return TRACKED;
            case "s":
                if (element.isType(ASUnitType.CV, ASUnitType.SV)) {
                    return SUBMARINE;
                } else if (element.isType(ASUnitType.BM, ASUnitType.PM)) {
                    return MEK_UMU;
                } else if (element.isType(ASUnitType.BA)) {
                    return BA_UMU;
                }
                break;
            case "j":
                if (element.isType(ASUnitType.BM, ASUnitType.PM)) {
                    return MEK_JUMP;
                } else if (element.isType(ASUnitType.BA)) {
                    return BA_JUMP;
                } else if (element.isType(ASUnitType.CI)) {
                    return CI_JUMP;
                }
                break;
        }
        return UNKNOWN;
    }
}