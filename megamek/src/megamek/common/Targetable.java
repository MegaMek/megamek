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
import java.util.Map;

public interface Targetable extends Serializable {
    public static final int TYPE_ENTITY = 0;
    public static final int TYPE_HEX_CLEAR = 1;
    public static final int TYPE_HEX_IGNITE = 2;
    public static final int TYPE_HEX_TAG = 19;
    public static final int TYPE_BUILDING = 3;
    public static final int TYPE_BLDG_IGNITE = 4;
    public static final int TYPE_BLDG_TAG = 20;    
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
    public static final int TYPE_HEX_AERO_BOMB = 18;

    public int getTargetType();

    public int getTargetId();

    /** @return the coordinates of the hex containing the target */
    public Coords getPosition();
    
    public Map<Integer, Coords> getSecondaryPositions();

    /**
     * @return elevation of the top (e.g. torso) of the target relative to
     *         surface
     */
    public int relHeight();

    /**
     * Returns the height of the target, that is, how many levels above its
     * elevation it is for LOS purposes.
     * 
     * @return height of the target in elevation levels */
    public int getHeight();

    /**
     * Returns the elevation of this target, relative to the position Hex's
     * surface
     * @return elevation of the bottom (e.g. legs) of the target relative to
     *         surface
     */
    public int getElevation();
    
    /**
     * @return altitude of target
     */
    public int getAltitude();

    /** @return true if the target is immobile (-4 to hit) */
    public boolean isImmobile();

    /** @return name of the target for ui purposes */
    public String getDisplayName();

    /** @return side hit from location */
    public int sideTable(Coords src);
    
    /** @return side hit from location */
    public int sideTable(Coords src, boolean usePrior);

    /** @return if this is off the board */
    public boolean isOffBoard();
    

    /**
     * @return if this is an <code>Entity</code> capable of aerospace movement
     */
    default boolean isAero() {
        return false;
    }
    
    /**
     * @return if this is an <code>Entity</code> capable of carrying and using bombs
     */
    default boolean isBomber() {
        return false;
    }

    /**
     * @return Is the entity airborne in the fashion of an aerospace unit?
     * Does not include VTOL movement (see {@link Targetable#isAirborneVTOLorWIGE()}
     */
    public boolean isAirborne();
    
    /**
     * @return is the entity airborne in the fashion of a VTOL
     * Not used for aerospace units, see {@link Targetable#isAirborne()}
     */
    public boolean isAirborneVTOLorWIGE();
    
    // Make sure Targetable implements both
    @Override
    public boolean equals(Object obj);

    /**
     * Determines if this target should be considered the enemy of the supplied player.  Targets that aren't owned by
     * any player, such as buildings or terrain, are always considered enemies, since this will most often be used to
     * determine if something is valid to be shot at.
     *
     * @param other
     * @return
     */
    public boolean isEnemyOf(Entity other);

    @Override
    int hashCode();
}
