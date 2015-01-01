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
 * This interface represents Entity Movement Types
 */
public interface IEntityMovementType {
    public static final int MOVE_LEGAL = -3;
    public static final int MOVE_SKID = -2;
    public static final int MOVE_ILLEGAL = -1;
    public static final int MOVE_NONE = 0;
    public static final int MOVE_WALK = 1;
    public static final int MOVE_RUN = 2;
    public static final int MOVE_JUMP = 3;
    public static final int MOVE_VTOL_WALK = 4;
    public static final int MOVE_VTOL_RUN = 5;
    public static final int MOVE_SUBMARINE_WALK = 6;
    public static final int MOVE_SUBMARINE_RUN = 7;
    public static final int MOVE_FLYING = 8;
    public static final int MOVE_SAFE_THRUST = 9;
    public static final int MOVE_OVER_THRUST = 10;
    public static final int MOVE_CAREFUL_STAND = 11;
}
