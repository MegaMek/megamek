/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package megamek.common;

/**
 * Enumeration of the different qualities -- types -- of movement.
 * Used in relation to MovePath
 * 
 * @author kwirkyj
 */
public enum MoveStepType {
    NONE, FORWARDS, BACKWARDS, TURN_LEFT, TURN_RIGHT, GET_UP, GO_PRONE, 
    START_JUMP, CHARGE, DFA, FLEE, LATERAL_LEFT, LATERAL_RIGHT, 
    LATERAL_LEFT_BACKWARDS, LATERAL_RIGHT_BACKWARDS, UNJAM_RAC, LOAD, UNLOAD, 
    EJECT, CLEAR_MINEFIELD, UP, DOWN, SEARCHLIGHT, LAY_MINE, HULL_DOWN, 
    CLIMB_MODE_ON, CLIMB_MODE_OFF, SWIM, DIG_IN, FORTIFY, SHAKE_OFF_SWARMERS, 
    TAKEOFF, VTAKEOFF, LAND, ACC, DEC, EVADE, SHUTDOWN, STARTUP, SELF_DESTRUCT, 
    ACCN, DECN, ROLL, OFF, RETURN, LAUNCH, THRUST, YAW, CRASH, RECOVER, RAM, 
    HOVER, MANEUVER, LOOP, CAREFUL_STAND, JOIN, DROP, VLAND, MOUNT, UNDOCK;    
}
