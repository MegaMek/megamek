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
package megamek.common.alphaStrike;

public enum ASRange {
    SHORT, MEDIUM, LONG, EXTREME, HORIZON;

    public boolean insideRange(int distance) {
        return switch (this) {
            case SHORT -> distance <= 6;
            case MEDIUM -> distance <= 24;
            case LONG -> distance <= 42;
            case EXTREME -> distance <= 60;
            case HORIZON -> distance > 60;
        };
    }

    public static ASRange fromDistance(int distance) {
        if (distance <= 6) {
            return SHORT;
        } else if (distance <= 24) {
            return MEDIUM;
        } else if (distance <= 42) {
            return LONG;
        } else if (distance <= 60) {
            return EXTREME;
        } else {
            return HORIZON;
        }
    }
}
