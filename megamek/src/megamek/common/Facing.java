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
package megamek.common;

import java.util.EnumMap;

/**
 * Enumeration of all 6 possible facings. It provides methods for translation
 * from old integer constants. Utility methods for turning left, right and
 * getting opposite direction are also provided.
 *
 *
 * @author Saginatio
 *
 */
public enum Facing {
    N(0), NE(1), SE(2), S(3), SW(4), NW(5), NONE(6);

    private Facing(int intValue) {
        this.intValue = intValue;
    }

    private final int intValue;

    static private final Facing[] valuesOfInt = { N, NE, SE, S, SW, NW, NONE };
    static private EnumMap<Facing, Facing> opposite = new EnumMap<>(Facing.class);
    static private EnumMap<Facing, Facing> cw = new EnumMap<>(Facing.class);
    static private EnumMap<Facing, Facing> ccw = new EnumMap<>(Facing.class);

    static {
        for (Facing f : values()) {
            int i = f.intValue;
            opposite.put(f, valuesOfInt[(i + 3) % 6]);
            cw.put(f, valuesOfInt[(i + 1) % 6]);
            ccw.put(f, valuesOfInt[(i + 5) % 6]);
        }
        opposite.put(NONE, NONE);
        cw.put(NONE, NONE);
        ccw.put(NONE, NONE);
    }

    /**
     * Method provided for backward compatibility with old integer constants.
     *
     * @param i Integer constant. must be non negative
     */
    public static Facing valueOfInt(final int i) {
        return valuesOfInt[i % 6];
    }

    /**
     * Method provided for backward compatibility with old integer constants.
     */
    public int getIntValue() {
        return intValue;
    }

    /**
     * @return Facing in the opposite direction e.g. N.getOpposite returns S
     */
    public Facing getOpposite() {
        return opposite.get(this);
    }

    /**
     * @return the next facing in clockwise direction
     */
    public Facing getNextClockwise() {
        return cw.get(this);
    }

    /**
     * @return the next facing in counter clockwise direction
     */
    public Facing getNextCounterClockwise() {
        return ccw.get(this);
    }
}
