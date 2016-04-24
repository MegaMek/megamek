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
 * IHex represents a single hex on the board.
 */
public interface IHex extends Cloneable {

    /**
     * The level of a hex, as defined in TW.  This refers to the height of the
     * ground terrain.
     *  
     * @return Hex level 
     */
    public abstract int getLevel();

    /**
     * Set the level of the hex.
     *
     * @param level
     */
    public abstract void setLevel(int level);

    /**
     * The theme is intended as a tag for the tileset file to indicate a special
     * graphic for the hex.
     *
     * @return theme name
     */
    public abstract String getTheme();

    /**
     * Set the hex theme.
     *
     * @param theme theme name
     * @see getTheme
     */
    public abstract void setTheme(String theme);
    
    /** Resets the theme to what was specified in the board file. */
    public abstract void resetTheme(); 

    /**
     * Clears the "exits" flag for all terrains in the hex where it is not
     * manually specified.
     */
    public abstract void clearExits();

    /**
     * Sets the "exits" flag appropriately, assuming the specified hex lies in
     * the specified direction on the board. Does not reset connects in other
     * directions. All <code>Terrain.ROAD</code>s will exit onto
     * <code>Terrain.PAVEMENT</code> hexes automatically.
     *
     * @param other neighbour hex
     * @param direction - the <code>int</code> direction of the exit. This
     *            value should be between 0 and 5 (inclusive).
     * @see IHex#setExits(IHex, int, boolean)
     */
    public abstract void setExits(IHex other, int direction);

    /**
     * Sets the "exits" flag appropriately, assuming the specified hex lies in
     * the specified direction on the board. Does not reset connects in other
     * directions. If the value of <code>roadsAutoExit</code> is
     * <code>true</code>, any <code>Terrain.ROAD</code> will exit onto
     * <code>Terrain.PAVEMENT</code> hexes automatically.
     *
     * @param other neighbour hex
     * @param direction - the <code>int</code> direction of the exit. This
     *            value should be between 0 and 5 (inclusive).
     * @param roadsAutoExit
     * @see IHex#setExits(IHex, int)
     */
    public abstract void setExits(IHex other, int direction,
            boolean roadsAutoExit);

    /**
     * Determine if this <code>Hex</code> contains the indicated terrain that
     * exits in the specified direction.
     *
     * @param terrType - the <code>int</code> type of the terrain.
     * @param direction - the <code>int</code> direction of the exit. This
     *            value should be between 0 and 5 (inclusive).
     * @return <code>true</code> if this <code>Hex</code> contains the
     *         indicated terrain that exits in the specified direction.
     *         <code>false</code> if bad input is supplied, if no such terrain
     *         exists, or if it doesn't exit in that direction.
     * @see IHex#setExits(IHex, int, boolean)
     */
    public abstract boolean containsTerrainExit(int terrType, int direction);
    
    /**
     * Determines if this <code>Hex</code> contains any exists in the specified
     * direction.
     * 
     * @param direction the <code>int</code> direction of the exit. This
     *            value should be between 0 and 5 (inclusive).
     * @return <code>true</code> if this <code>Hex</code> contains any terrain 
     *         that exits in the specified direction.
     *         <code>false</code> if bad input is supplied, if no terrain
     *         exits in that direction.
     * @see IHex#setExits(IHex, int, boolean)
     */
    public abstract boolean containsExit(int direction);
    
    /**
     * Returns true if this hex contains a terrain type that can have exits,
     * else false.
     * 
     */
    public abstract boolean hasExitableTerrain();

    /**
     * @return the highest level that features in this hex extend to. Above this
     *         level is assumed to be air.  This assumes a ground map.
     */
    public abstract int ceiling();
    
    /**
     * 
     * @param inAtmosphere
     *            Determines if the ceiling should be determined for an
     *            atmospheric map (eg, altitudes) or ground map (eg, levels)
     * @return the highest level or altitude (depending on flag) that features
     *         in this hex extend to. Above this level is assumed to be air.
     * 
     */
    public abstract int ceiling(boolean inAtmosphere);
    
    /**
     * Returns the elevation or altitude of the terrain feature that rises the
     * highest above the surface of the hex. For example, if the hex is on the
     * ground map and contains woods, this would return 2.
     * 
     * @param inAtmo Determines if altitudes or elevations are returned
     * @return
     */
    public abstract int maxTerrainFeatureElevation(boolean inAtmo);

    /**
     * @return the surface level of the hex. Equal to getLevel().
     */
    public abstract int surface();

    /**
     * Returns the lowest reachable point of this hex, used for 
     * terrain types that can extend below the surface of the hex, such as water
     * and basements.  Unrevealed basements will not effect this value.
     * 
     * @return the lowest level that revealed features in this hex extend to. 
     *         Below this level is assumed to be bedrock and/or basement.
     *         Unrevealed basements will not effect this value.
     */
    public abstract int floor();

    /**
     * @return a level indicating how far features in this hex extend below the
     *         surface level.
     */
    public abstract int depth();
    public abstract int depth(boolean hidden);

    /**
     * @return true if there is pavement, a road or a bridge in the hex.
     */
    public abstract boolean hasPavement();

    /**
     * Returns true if this hex has a terrain with a non-zero terrain factor
     * @return
     */
    public boolean hasTerrainfactor();
    
    /**
     * @return <code>true</code> if the specified terrain is represented in
     *         the hex at any level.
     * @param type terrain to check
     * @see IHex#containsTerrain(int, int)
     */
    public abstract boolean containsTerrain(int type);

    /**
     * @param type terrain type to check
     * @param level level to check the presence of the given terrain at
     * @return <code>true</code> if the specified terrain is represented in
     *         the hex at given level.
     * @see IHex#containsTerrain(int)
     */
    public abstract boolean containsTerrain(int type, int level);

    /**
     * @return the level of the terrain specified, or ITerrain.LEVEL_NONE if the
     *         terrain is not present in the hex
     */
    public abstract int terrainLevel(int type);

    /**
     * @param type
     * @return the terrain of the specified type, or <code>null</code> if the
     *         terrain is not present in the hex
     */
    public abstract ITerrain getTerrain(int type);
    
    /**
     * Returns a collection of terrain ids for all terrains present in this hex.
     * 
     * @return A set that contains an id for each terrain present in this hex.
     */
    public abstract int[] getTerrainTypes();

    /**
     * Adds the specified terrain
     *
     * @param terrain terrain to add
     */
    public abstract void addTerrain(ITerrain terrain);

    /**
     * Removes the specified terrain
     *
     * @param type
     */
    public abstract void removeTerrain(int type);

    /**
     * Removes all Terrains from the hex.
     */
    public abstract void removeAllTerrains();

    /**
     * @return the number of terrain attributes present that are displayable in tooltips
     */
    public abstract int displayableTerrainsPresent();

    /**
     * @return the number of terrain attributes present
     */
    public abstract int terrainsPresent();

    /**
     * @return new hex which is equal to this
     */
    public abstract IHex duplicate();

    /**
     * @return modifier to PSRs made in the hex
     */
    public abstract void terrainPilotingModifier(EntityMovementMode moveType, PilotingRollData roll, boolean enteringRubble);

    /**
     * (Only if statically determinable)
     *
     * @return extra movement cost for entering the hex
     */
    public abstract int movementCost(Entity entity);

    /**
     * @return the modifier to the roll to ignite this hex
     */
    public abstract int getIgnitionModifier();

    /**
     * @return <code>true</code> if this hex is ignitable
     */
    public abstract boolean isIgnitable();

    public abstract int getFireTurn();

    public abstract void incrementFireTurn();

    public abstract void resetFireTurn();

    public abstract int getBogDownModifier(EntityMovementMode moveMode, boolean largeVee);

    public abstract void getUnstuckModifier(int elev, PilotingRollData rollTarget);

    public abstract boolean isClearForTakeoff();

    public abstract boolean isClearForLanding();

    public abstract Coords getCoords();

    public abstract void setCoords(Coords c);
}
