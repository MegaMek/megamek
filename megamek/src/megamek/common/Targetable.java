/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import java.io.Serializable;
import java.util.Map;

import megamek.common.annotations.Nullable;

public interface Targetable extends Serializable {
    int TYPE_ENTITY = 0;
    int TYPE_HEX_CLEAR = 1;
    int TYPE_HEX_IGNITE = 2;
    int TYPE_HEX_TAG = 19;
    int TYPE_BUILDING = 3;
    int TYPE_BLDG_IGNITE = 4;
    int TYPE_BLDG_TAG = 20;    
    int TYPE_MINEFIELD_CLEAR = 5;
    int TYPE_MINEFIELD_DELIVER = 6;
    int TYPE_HEX_ARTILLERY = 7;
    int TYPE_HEX_EXTINGUISH = 8;
    int TYPE_INARC_POD = 11;
    int TYPE_SEARCHLIGHT = 12;
    int TYPE_FLARE_DELIVER = 13;
    int TYPE_HEX_BOMB = 14;
    int TYPE_FUEL_TANK = 15;
    int TYPE_FUEL_TANK_IGNITE = 16;
    int TYPE_HEX_SCREEN = 17;
    int TYPE_HEX_AERO_BOMB = 18;

    int getTargetType();

    int getTargetId();

    /** @return the coordinates of the hex containing the target */
    Coords getPosition();
    
    Map<Integer, Coords> getSecondaryPositions();

    /**
     * @return elevation of the top (e.g. torso) of the target relative to
     *         surface
     */
    int relHeight();

    /**
     * Returns the height of the target, that is, how many levels above its
     * elevation it is for LOS purposes.
     * 
     * @return height of the target in elevation levels */
    int getHeight();

    /**
     * Returns the elevation of this target, relative to the position Hex's
     * surface
     * @return elevation of the bottom (e.g. legs) of the target relative to
     *         surface
     */
    int getElevation();
    
    /**
     * @return altitude of target
     */
    int getAltitude();

    /** @return true if the target is immobile (-4 to hit) */
    boolean isImmobile();

    /** @return name of the target for ui purposes */
    String getDisplayName();

    /** @return side hit from location */
    int sideTable(Coords src);
    
    /** @return side hit from location */
    int sideTable(Coords src, boolean usePrior);

    /** @return if this is off the board */
    boolean isOffBoard();

    /**
     * @return true if this is an entity subject to rules for conventional infantry
     */
    default boolean isConventionalInfantry() {
        return false;
    }

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
    boolean isAirborne();
    
    /**
     * @return is the entity airborne in the fashion of a VTOL
     * Not used for aerospace units, see {@link Targetable#isAirborne()}
     */
    boolean isAirborneVTOLorWIGE();
    
    // Make sure Targetable implements both
    @Override
    boolean equals(Object obj);

    /**
     * Determines if this target should be considered the enemy of the supplied player.  Targets that aren't owned by
     * any player, such as buildings or terrain, are always considered enemies, since this will most often be used to
     * determine if something is valid to be shot at.
     *
     * @param other
     * @return
     */
    boolean isEnemyOf(Entity other);
    
    default boolean isHexBeingBombed() {
        return getTargetType() == TYPE_HEX_AERO_BOMB || 
                getTargetType() == TYPE_HEX_BOMB;
    }

    /**
     * Used to identify an target that tracks heat buildup (Mechs, ASFs, and small craft).
     * 
     * @return Whether the target tracks heat buildup.
     */
    default boolean tracksHeat() {
        return false;
    }
    
    @Override
    int hashCode();
    
    /**
     * Utility function used to safely tell whether two Targetables are in the same hex.
     * Does not throw exceptions in case of nulls.
     */
    static boolean areAtSamePosition(@Nullable Targetable first, @Nullable Targetable second) {
        if ((first == null) || (second == null) ||
                (first.getPosition() == null) || (second.getPosition() == null)) {
            return false;
        }
        
        return first.getPosition().equals(second.getPosition());
    }
}
