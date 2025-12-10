/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

import java.util.EnumMap;

/**
 * Enumeration of all 6 possible facings. It provides methods for translation from old integer constants. Utility
 * methods for turning left, right and getting opposite direction are also provided.
 *
 * @author Saginatio
 */
public enum Facing {
    N(0), NE(1), SE(2), S(3), SW(4), NW(5), NONE(6);

    Facing(int intValue) {
        this.intValue = intValue;
    }

    private final int intValue;

    static private final Facing[] valuesOfInt = { N, NE, SE, S, SW, NW, NONE };
    static private final EnumMap<Facing, Facing> opposite = new EnumMap<>(Facing.class);
    static private final EnumMap<Facing, Facing> cw = new EnumMap<>(Facing.class);
    static private final EnumMap<Facing, Facing> ccw = new EnumMap<>(Facing.class);

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
     * @param i Integer constant. must be non-negative
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
     * @return Facing in degrees, e.g. N.getAngle() returns 0, NE.getAngle() returns 30 and S.getAngle() returns 180.
     */
    public int getAngle() {
        if (this == NONE) {
            return 0;
        }
        return intValue * 60;
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
     * @return the next facing in counterclockwise direction
     */
    public Facing getNextCounterClockwise() {
        return ccw.get(this);
    }
}
