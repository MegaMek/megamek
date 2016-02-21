/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

/**
 *  This class represents parachute flares deployed by illumination artillery
 *  or mech mortars
 */
public class Flare implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 451911245389504483L;
    public Coords position;
    public int turnsToBurn;
    public int radius;
    public int flags = 0;

    public static int F_IGNITED = 1;
    public static int F_DRIFTING = 2;

    public Flare(Coords position, int turnsToBurn, int radius, int flags) {
        this.position = position;
        this.turnsToBurn = turnsToBurn;
        this.radius = radius;
        this.flags = flags;
    }

    public boolean illuminates(Coords c) {
        return ((flags & F_IGNITED) != 0 && position.distance(c) <= radius);
    }
}
