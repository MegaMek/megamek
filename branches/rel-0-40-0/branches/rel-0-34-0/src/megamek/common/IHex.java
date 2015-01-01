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
     * @return hex elevation
     */
    public abstract int getElevation();

    /**
     * set the elevation
     * 
     * @param elevation
     */
    public abstract void setElevation(int elevation);

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
     * @return the highest level that features in this hex extend to. Above this
     *         level is assumed to be air.
     */
    public abstract int ceiling();

    /**
     * @return the surface level of the hex. Equal to getElevation().
     */
    public abstract int surface();

    /**
     * @return the lowest level that features in this hex extend to. Below this
     *         level is assumed to be bedrock.
     */
    public abstract int floor();

    /**
     * @return a level indicating how far features in this hex extend below the
     *         surface elevation.
     */
    public abstract int depth();

    /**
     * @return true if there is pavement, a road or a bridge in the hex.
     */
    public abstract boolean hasPavement();

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
     * Removes all Terreains from the hex.
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
     * @return new hex wich is equals to this
     */
    public abstract IHex duplicate();

    /**
     * @return modifier to PSRs made in the hex
     */
    public abstract int terrainPilotingModifier(int moveType);

    /**
     * (Only if statically determinable)
     * 
     * @return extra movement cost for entering the hex
     */
    public abstract int movementCost(int moveType);
    
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
    
    public abstract int getBogDownModifier(int moveType, boolean largeVee);
    
    public abstract int getUnstuckModifier(int elev);
}
