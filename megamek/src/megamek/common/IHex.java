package megamek.common;

import megamek.common.annotations.Nullable;

/**
 * Hex represents a single hex on the board.
 */
public interface IHex extends Cloneable {

    /**
     * The level of a hex, as defined in TW. This refers to the height of the
     * ground terrain.
     * 
     * @return Hex level
     */
    int getLevel();

    /**
     * Set the level of the hex.
     *
     * @param level
     */
    void setLevel(int level);

    /**
     * The theme is intended as a tag for the tileset file to indicate a special
     * graphic for the hex.
     *
     * @return theme name
     */
    String getTheme();

    /**
     * Set the hex theme.
     *
     * @param theme
     *            theme name
     * @see getTheme
     */
    void setTheme(String theme);

    /** Resets the theme to what was specified in the board file. */
    void resetTheme();

    /**
     * Clears the "exits" flag for all terrains in the hex where it is not
     * manually specified.
     */
    void clearExits();

    /**
     * Sets the "exits" flag appropriately, assuming the specified hex lies in
     * the specified direction on the board. Does not reset connects in other
     * directions. All <code>Terrain.ROAD</code>s will exit onto
     * <code>Terrain.PAVEMENT</code> hexes automatically.
     *
     * @param other
     *            neighbour hex
     * @param direction
     *            - the <code>int</code> direction of the exit. This value
     *            should be between 0 and 5 (inclusive).
     * @see Hex#setExits(Hex, int, boolean)
     */
    void setExits(Hex other, int direction);

    /**
     * Sets the "exits" flag appropriately, assuming the specified hex lies in
     * the specified direction on the board. Does not reset connects in other
     * directions. If the value of <code>roadsAutoExit</code> is
     * <code>true</code>, any <code>Terrain.ROAD</code> will exit onto
     * <code>Terrain.PAVEMENT</code> hexes automatically.
     *
     * @param other
     *            neighbour hex
     * @param direction
     *            - the <code>int</code> direction of the exit. This value
     *            should be between 0 and 5 (inclusive).
     * @param roadsAutoExit
     * @see Hex#setExits(Hex, int)
     */
    void setExits(Hex other, int direction, boolean roadsAutoExit);

    /**
     * Determine if this <code>Hex</code> contains the indicated terrain that
     * exits in the specified direction.
     *
     * @param terrType
     *            - the <code>int</code> type of the terrain.
     * @param direction
     *            - the <code>int</code> direction of the exit. This value
     *            should be between 0 and 5 (inclusive).
     * @return <code>true</code> if this <code>Hex</code> contains the indicated
     *         terrain that exits in the specified direction. <code>false</code>
     *         if bad input is supplied, if no such terrain exists, or if it
     *         doesn't exit in that direction.
     * @see Hex#setExits(Hex, int, boolean)
     */
    boolean containsTerrainExit(int terrType, int direction);

    /**
     * Determines if this <code>Hex</code> contains any exists in the specified
     * direction.
     * 
     * @param direction
     *            the <code>int</code> direction of the exit. This value should
     *            be between 0 and 5 (inclusive).
     * @return <code>true</code> if this <code>Hex</code> contains any terrain
     *         that exits in the specified direction. <code>false</code> if bad
     *         input is supplied, if no terrain exits in that direction.
     * @see Hex#setExits(Hex, int, boolean)
     */
    boolean containsExit(int direction);

    /**
     * Returns true if this hex contains a terrain type that can have exits,
     * else false.
     * 
     */
    boolean hasExitableTerrain();

    /**
     * @return the highest level that features in this hex extend to. Above this
     *         level is assumed to be air. This assumes a ground map.
     */
    int ceiling();

    /**
     * 
     * @param inAtmosphere
     *            Determines if the ceiling should be determined for an
     *            atmospheric map (eg, altitudes) or ground map (eg, levels)
     * @return the highest level or altitude (depending on flag) that features
     *         in this hex extend to. Above this level is assumed to be air.
     * 
     */
    int ceiling(boolean inAtmosphere);

    /**
     * Returns the elevation or altitude of the terrain feature that rises the
     * highest above the surface of the hex. For example, if the hex is on the
     * ground map and contains woods, this would return 2.
     * 
     * @param inAtmo
     *            Determines if altitudes or elevations are returned
     * @return
     */
    int maxTerrainFeatureElevation(boolean inAtmo);

    /**
     * @return the surface level of the hex. Equal to getLevel().
     */
    int surface();

    /**
     * Returns the lowest reachable point of this hex, used for terrain types
     * that can extend below the surface of the hex, such as water and
     * basements. Unrevealed basements will not effect this value.
     * 
     * @return the lowest level that revealed features in this hex extend to.
     *         Below this level is assumed to be bedrock and/or basement.
     *         Unrevealed basements will not effect this value.
     */
    int floor();

    /**
     * @return a level indicating how far features in this hex extend below the
     *         surface level.
     */
    int depth();

    int depth(boolean hidden);

    /**
     * @return true if there is pavement, a road or a bridge in the hex.
     */
    boolean hasPavement();

    /**
     * Returns true if this hex has a terrain with a non-zero terrain factor
     * 
     * @return
     */
    boolean hasTerrainfactor();

    /**
     * @return <code>true</code> if the specified terrain is represented in the
     *         hex at any level.
     * @param type
     *            terrain to check
     * @see Hex#containsTerrain(int, int)
     * @see Hex#containsAllTerrainsOf(int...)
     * @see Hex#containsAnyTerrainOf(int...)
     */
    boolean containsTerrain(int type);

    /**
     * @param type
     *            terrain type to check
     * @param level
     *            level to check the presence of the given terrain at
     * @return <code>true</code> if the specified terrain is represented in the
     *         hex at given level.
     * @see Hex#containsTerrain(int)
     * @see Hex#containsAllTerrainsOf(int...)
     * @see Hex#containsAnyTerrainOf(int...)
     */
    boolean containsTerrain(int type, int level);
    
    /**
     * @return <code>true</code> if at least one of the specified terrains are represented in the
     *         hex at any level.
     * @param types
     *            terrains to check
     * @see Hex#containsTerrain(int, int)
     * @see Hex#containsTerrain(int)
     * @see Hex#containsAllTerrainsOf(int...)
     */
    boolean containsAnyTerrainOf(int... types);
    
    /**
     * @return <code>true</code> if all of the specified terrains are represented in the
     *         hex at any level.
     * @param types
     *            terrains to check
     * @see Hex#containsTerrain(int, int)
     * @see Hex#containsAllTerrainsOf(int...)
     * @see Hex#containsAnyTerrainOf(int...)
     */
    boolean containsAllTerrainsOf(int... types);

    /**
     * @return the level of the terrain specified, or Terrain.LEVEL_NONE if the
     *         terrain is not present in the hex
     */
    int terrainLevel(int type);

    /**
     * @param type
     * @return the terrain of the specified type, or <code>null</code> if the
     *         terrain is not present in the hex
     */
    Terrain getTerrain(int type);

    /**
     * Returns the Terrain for one of the given types of terrain if at least one of 
     * them is present in the hex. If multiple are present, the returned terrain
     * can be any of the given types. 
     * 
     * @return One of the Terrains of the types that is present in the hex or null if none are 
     * present in the hex.
     * @param types the terrain types to check
     * @see Hex#containsAnyTerrainsOf(int...)
     */
    Terrain getAnyTerrainOf(int type, int... types);

    /**
     * Returns a collection of terrain ids for all terrains present in this hex.
     * 
     * @return A set that contains an id for each terrain present in this hex.
     */
    int[] getTerrainTypes();

    /**
     * Adds the specified terrain
     *
     * @param terrain
     *            terrain to add
     */
    void addTerrain(Terrain terrain);

    /**
     * Removes the specified terrain
     *
     * @param type
     */
    void removeTerrain(int type);

    /**
     * Removes all Terrains from the hex.
     */
    void removeAllTerrains();

    /**
     * @return the number of terrain attributes present that are displayable in
     *         tooltips
     */
    int displayableTerrainsPresent();

    /**
     * @return the number of terrain attributes present
     */
    int terrainsPresent();

    /**
     * @return new hex which is equal to this
     */
    Hex duplicate();

    /**
     * @return modifier to PSRs made in the hex
     */
    void terrainPilotingModifier(EntityMovementMode moveType, PilotingRollData roll,
            boolean enteringRubble);

    /**
     * (Only if statically determinable)
     *
     * @return extra movement cost for entering the hex
     */
    int movementCost(Entity entity);

    /**
     * @return the modifier to the roll to ignite this hex
     */
    int getIgnitionModifier();

    /**
     * @return <code>true</code> if this hex is ignitable
     */
    boolean isIgnitable();

    int getFireTurn();

    void incrementFireTurn();

    void resetFireTurn();

    int getBogDownModifier(EntityMovementMode moveMode, boolean largeVee);

    void getUnstuckModifier(int elev, PilotingRollData rollTarget);

    boolean isClearForTakeoff();

    boolean isClearForLanding();

    /**
     * Used to determine if this hex is "clear", based on the absense of most
     * other terrain types.
     * 
     * @return
     */
    boolean isClearHex();

    Coords getCoords();

    void setCoords(Coords c);
    
    boolean hasCliffTopTowards(Hex otherHex); 

    /**
     * Determines if the Hex is valid or not. <code>errBuff</code> can be used to return a report of why the hex is
     * valid.  If errBuff is null, isValid shortcircuits on the first failure, otherwise it checks for all failures
     * and logs them.
     * 
     * @param errBuff  Buffer to contain error messages.  If null, method returns on first failure.
     * @return
     */
    boolean isValid(@Nullable StringBuffer errBuff);
}
