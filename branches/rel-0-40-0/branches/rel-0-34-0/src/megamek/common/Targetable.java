/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common;

import java.io.Serializable;

public interface Targetable extends Serializable {
    public static final int TYPE_ENTITY = 0;
    public static final int TYPE_HEX_CLEAR = 1;
    public static final int TYPE_HEX_IGNITE = 2;
    public static final int TYPE_BUILDING = 3;
    public static final int TYPE_BLDG_IGNITE = 4;
    public static final int TYPE_MINEFIELD_CLEAR = 5;
    public static final int TYPE_MINEFIELD_DELIVER = 6;
    public static final int TYPE_HEX_ARTILLERY = 7;
    public static final int TYPE_HEX_EXTINGUISH = 8;
    public static final int TYPE_INARC_POD = 11;
    public static final int TYPE_SEARCHLIGHT = 12;
    public static final int TYPE_FLARE_DELIVER = 13;
    public static final int TYPE_HEX_BOMB = 14;
    public static final int TYPE_FUEL_TANK = 15;
    public static final int TYPE_FUEL_TANK_IGNITE = 16;
    public static final int TYPE_HEX_SCREEN = 17;

    public int getTargetType();

    public int getTargetId();

    /** @return the coordinates of the hex containing the target */
    public Coords getPosition();

    /**
     * @return elevation of the top (e.g. torso) of the target relative to
     *         surface
     */
    public int absHeight();

    /** @return height of the target in elevation levels */
    public int getHeight();

    /**
     * @return elevation of the bottom (e.g. legs) of the target relative to
     *         surface
     */
    public int getElevation();

    /** @return true if the target is immobile (-4 to hit) */
    public boolean isImmobile();

    /** @return name of the target for ui purposes */
    public String getDisplayName();

    /** @return side hit from location */
    public int sideTable(Coords src);

    /** @return if this is off the board */
    public boolean isOffBoard();
}
