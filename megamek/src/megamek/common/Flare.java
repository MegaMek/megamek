/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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
 *  This class represents parachute flares deployed by illumination artillery
 *  or mech mortars.
 */
public class Flare implements Serializable {
    private static final long serialVersionUID = 451911245389504483L;
    public Coords position;
    public int turnsToBurn;
    public final int radius;
    public int flags;

    public static int F_IGNITED = 1;
    public static int F_DRIFTING = 2;

    public Flare(Coords position, int turnsToBurn, int radius, int flags) {
        this.position = position;
        this.turnsToBurn = turnsToBurn;
        this.radius = radius;
        this.flags = flags;
    }

    public boolean illuminates(Coords c) {
        return isIgnited() && (position.distance(c) <= radius);
    }

    public boolean isIgnited() {
        return (flags & F_IGNITED) != 0;
    }

    public boolean isDrifting() {
        return (flags & F_DRIFTING) != 0;
    }

    public void ignite() {
        flags |= Flare.F_IGNITED;
    }
}
