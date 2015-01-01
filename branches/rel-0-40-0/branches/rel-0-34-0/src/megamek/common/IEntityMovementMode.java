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

/**
 * This interface represents the Entity Movement Types
 */
public interface IEntityMovementMode {

    public static final int NONE = 0; // Future expansion. Turrets?
    public static final int BIPED = 1;
    public static final int QUAD = 2;
    public static final int TRACKED = 3;
    public static final int WHEELED = 4;
    public static final int HOVER = 5;
    public static final int VTOL = 6;
    public static final int NAVAL = 7;
    public static final int HYDROFOIL = 8;
    public static final int SUBMARINE = 9;
    public static final int INF_LEG = 10;
    public static final int INF_MOTORIZED = 11;
    public static final int INF_JUMP = 12;
    public static final int BIPED_SWIM = 13;
    public static final int QUAD_SWIM = 14;
    public static final int WIGE = 15;
    public static final int AERODYNE = 16;
    public static final int SPHEROID = 17;
    public static final int INF_UMU = 18;
    public static final int AIRMECH = 19;
    public static final int AEROSPACE = 20; // this might be a synonym for
                                            // AERODYNE.
}
