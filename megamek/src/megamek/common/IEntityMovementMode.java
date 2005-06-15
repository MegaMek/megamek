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
    
    public static final int NONE = 0; //Future expansion. Turrets?
    public static final int BIPED = 1;
    public static final int QUAD = 2;
    public static final int TRACKED = 3;
    public static final int WHEELED = 4;
    public static final int HOVER = 5;
    public static final int VTOL = 6;
    public static final int NAVAL = 7;
    public static final int HYDROFOIL = 8;
    public static final int SUBMARINE = 9;
}
