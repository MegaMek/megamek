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
 * Represents a single type of terrain or condition in a hex. The type of a
 * terrain is immutable, once created, but the level and exits are changable.
 * Each type of terrain should only be represented once in a hex.
 *
 * @author Ben
 */
public interface ITerrain {

    public static final int LEVEL_NONE = Integer.MIN_VALUE;
    public static final int WILDCARD = Integer.MAX_VALUE;

    /**
     * @return terrain type
     */
    public abstract int getType();

    /**
     * @return terrain level
     */
    public abstract int getLevel();

    /**
     * @return terrain factor
     */
    public abstract int getTerrainFactor();

    /**
     * set terrain factor
     */
    public abstract void setTerrainFactor(int tf);
    
    /**
     * Returns the number of altitudes/elevations this Terrain is above the
     * surface of a hex.
     * 
     * @param inAtmosphere
     *            Determines whether altitudes or elevations are returned.
     * @return
     */
    public abstract int getTerrainElevation(boolean inAtmosphere);

    /**
     * @return exits
     */
    public abstract int getExits();

    /**
     * @return <code>true</code> if exits are specified
     */
    public abstract boolean hasExitsSpecified();

    /**
     * Sets the exits
     *
     * @param exits
     */
    public abstract void setExits(int exits);

    /**
     * Sets the exit in specified direction
     *
     * @param direction - the direction to add/remove the exit
     * @param connection - true to add, false to remove
     */
    public abstract void setExit(int direction, boolean connection);

    /**
     * Flips the exits around the vertical axis (North-for-South) and/or the
     * horizontal axis (East-for-West).
     *
     * @param horiz - a <code>boolean</code> value that, if <code>true</code>,
     *            indicates that the exits are being flipped North-for-South.
     * @param vert - a <code>boolean</code> value that, if <code>true</code>,
     *            indicates that the exits are being flipped East-for-West.
     */
    public abstract void flipExits(boolean horiz, boolean vert);

    /**
     * @return true if the terrain in this hex exits to the terrain in the other
     *         hex.
     */
    public abstract boolean exitsTo(ITerrain other);

    /**
     * @param roll 
     * @return the modifier to PSRs made in this terrain
     */
    public abstract void pilotingModifier(EntityMovementMode moveMode, PilotingRollData roll, boolean enteringRubble);

    /**
     * @return the additional movement cost for this terrain
     */
    public abstract int movementCost(Entity e);

    /**the fire ignition modifier for this terrain
     */
    public abstract int ignitionModifier();

    public abstract int getBogDownModifier(EntityMovementMode moveMode, boolean largeVee);

    public abstract void getUnstuckModifier(int elev, PilotingRollData rollTarget);
    
    public boolean isValid(StringBuffer errBuff);

}
