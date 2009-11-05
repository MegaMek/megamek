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
public enum EntityMovementType {
    MOVE_LEGAL,
    MOVE_SKID,
    MOVE_ILLEGAL,
    MOVE_NONE,
    MOVE_WALK,
    MOVE_RUN,
    MOVE_JUMP,
    MOVE_VTOL_WALK,
    MOVE_VTOL_RUN,
    MOVE_SUBMARINE_WALK,
    MOVE_SUBMARINE_RUN,
    MOVE_FLYING,
    MOVE_SAFE_THRUST,
    MOVE_OVER_THRUST,
    MOVE_CAREFUL_STAND,
    MOVE_SPRINT
}
