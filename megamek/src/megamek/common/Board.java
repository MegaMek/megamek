/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

import megamek.common.annotations.Nullable;
import megamek.common.enums.BasementType;
import megamek.common.event.BoardEvent;
import megamek.common.event.BoardListener;
import megamek.common.util.fileUtils.MegaMekFile;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.util.*;

public class Board implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = -5744058872091016636L;

    public static final String BOARD_REQUEST_ROTATION = "rotate:";

    // starting positions
    public static final int START_NONE = -1;
    public static final int START_ANY = 0;
    public static final int START_NW = 1;
    public static final int START_N = 2;
    public static final int START_NE = 3;
    public static final int START_E = 4;
    public static final int START_SE = 5;
    public static final int START_S = 6;
    public static final int START_SW = 7;
    public static final int START_W = 8;
    public static final int START_EDGE = 9;
    public static final int START_CENTER = 10;
    
    //Board Dimensions
    //Used for things like artillery rules that reference the standard mapsheet dimensions
    public static final int DEFAULT_BOARD_HEIGHT = 17;
    public static final int DEFAULT_BOARD_WIDTH = 16;
    //Variable board width and height. Used for most everything else since we're not restricted to paper map sizes
    protected int width;
    protected int height;

    // MapType
    public static final int T_GROUND = 0;
    public static final int T_ATMOSPHERE = 1;
    public static final int T_SPACE = 2;

    private static final String[] typeNames = { "Ground", "Low Atmosphere", "Space" };

    // Min and Max elevation values for when they are undefined (since you cant
    // set an int to null).
    private static final int UNDEFINED_MIN_ELEV = 10000;
    private static final int UNDEFINED_MAX_ELEV = -10000;

    // The min and max elevation values for this board.
    // set when getMinElevation/getMax is called for the first time.
    private int minElevation = UNDEFINED_MIN_ELEV;
    private int maxElevation = UNDEFINED_MAX_ELEV;

    private int mapType = T_GROUND;

    private Hex[] data;

    /**
     * The path to the file to load as background image for this board. To avoid
     * the Server sending a serialized image, the image isn't loaded until
     * requested.
     */
    private List<String> backgroundPaths = new ArrayList<>();

    /**
     * Keeps track of how many boards were combined to create this board. These
     * are necessary to properly index into the background image, and only need
     * to be set if backgroundPaths are present.
     */
    private int numBoardsWidth, numBoardsHeight;

    /**
     * Keeps track of the size of the boards used to create this board. These
     * are necessary to properly index into the background image, and only need
     * to be set if backgroundPaths are present.
     */
    private int subBoardWidth, subBoardHeight;

    /**
     * Flags that determine if the background image should be flipped. These are
     * necessary to properly index into the background image, and only need to
     * be set if backgroundPaths are present.
     */
    private List<Boolean> flipBGHoriz = new ArrayList<>(), flipBGVert = new ArrayList<>();

    /**
     * Building data structures.
     */
    private Vector<Building> buildings = new Vector<>();
    private transient Hashtable<Coords, Building> bldgByCoords = new Hashtable<>();

    protected transient Vector<BoardListener> boardListeners = new Vector<>();

    /**
     * Record the infernos placed on the board.
     */
    private Hashtable<Coords, InfernoTracker> infernos = new Hashtable<>();

    private Hashtable<Coords, Collection<SpecialHexDisplay>> specialHexes = new Hashtable<>();

    /**
     * Option to turn have roads auto-exiting to pavement.
     */
    private boolean roadsAutoExit = true;

    /**
     * A description of the map.
     */
    private String description;

    /**
     * Per-hex annotations on the map.
     */
    private Map<Coords, Collection<String>> annotations = new HashMap<>();

    /** Tags associated with this board to facilitate searching for it. */
    private Set<String> tags = new HashSet<>();
    //endregion Variable Declarations

    //region Constructors
    /**
     * Creates a new board with zero as its width and height parameters.
     */
    public Board() {
        this(0, 0);
    }

    /**
     * Creates a new board of the specified dimensions. All hexes in the board
     * will be null until otherwise set.
     *
     * @param width
     *            the width dimension.
     * @param height
     *            the height dimension.
     */
    public Board(int width, int height) {
        this.width = width;
        this.height = height;
        data = new Hex[width * height];
    }

    /**
     * Creates a new board of the specified dimensions and specified hex data.
     *
     * @param width
     *            the width dimension.
     * @param height
     *            the height dimension.
     * @param data
     */
    public Board(int width, int height, Hex... data) {
        this.width = width;
        this.height = height;
        this.data = new Hex[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                this.data[(y * width) + x] = data[(y * width) + x];
            }
        }
    }

    /**
     * Creates a new board of the specified dimensions, hexes, buildings, and
     * inferno trackers. Do *not* use this method unless you have carefully
     * examined this class.
     *
     * @param width
     *            The <code>int</code> width dimension in hexes.
     * @param height
     *            The <code>int</code> height dimension in hexes.
     * @param hexes
     *            The array of <code>Hex</code>es for this board. This object is
     *            used directly without being copied. This value should only be
     *            <code>null</code> if either <code>width</code> or
     *            <code>height</code> is zero.
     * @param bldgs
     *            The <code>Vector</code> of <code>Building</code>s for this
     *            board. This object is used directly without being copied.
     * @param infMap
     *            The <code>Hashtable</code> that map <code>Coords</code> to
     *            <code>InfernoTracker</code>s for this board. This object is
     *            used directly without being copied.
     */
    public Board(int width, int height, Hex[] hexes, Vector<Building> bldgs,
            Hashtable<Coords, InfernoTracker> infMap) {
        this.width = width;
        this.height = height;
        data = hexes;
        buildings = bldgs;
        infernos = infMap;
        createBldgByCoords();
    }
    //endregion Constructors

    /**
     * @return Map width in hexes
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return Map height in hexes
     */
    public int getWidth() {
        return width;
    }

    public Coords getCenter() {
        return new Coords(getWidth() / 2, getHeight() / 2);
    }

    /**
     * Creates a new data set for the board, with the specified dimensions and
     * data; notifies listeners that a new data set has been created.
     *
     * @param width the width dimension.
     * @param height the height dimension.
     * @param data new hex data appropriate for the board.
     * @param errBuff A buffer for storing error messages, if any.  This is allowed to be null.
     */
    public void newData(final int width, final int height, final Hex[] data,
                        final @Nullable StringBuffer errBuff) {
        this.width = width;
        this.height = height;
        this.data = data;

        initializeAll(errBuff);
        processBoardEvent(new BoardEvent(this, null, BoardEvent.BOARD_NEW_BOARD));
    }

    /**
     * Determines if this Board contains the (x, y) Coords, and if so, returns the Hex at that position.
     *
     * @param x the x Coords.
     * @param y the y Coords.
     * @return the Hex, if this Board contains the (x, y) location; null otherwise.
     */
    public @Nullable Hex getHex(final int x, final int y) {
        return contains(x, y) ? data[(y * width) + x] : null;
    }

    /**
     * @param c starting coordinates
     * @param dir direction
     * @return the hex in the specified direction from the specified starting coordinates.
     */
    public Hex getHexInDir(Coords c, int dir) {
        return getHex(c.xInDir(dir), c.yInDir(dir));
    }

    /**
     * Gets the hex in the specified direction from the specified starting coordinates. This avoids
     * calls to Coords.translated, and thus, object construction.
     *
     * @param x starting x coordinate
     * @param y starting y coordinate
     * @param dir direction
     * @return the hex in the specified direction from the specified starting  coordinates.
     */
    public Hex getHexInDir(int x, int y, int dir) {
        return getHex(Coords.xInDir(x, y, dir), Coords.yInDir(x, y, dir));
    }

    /**
     * Initialize all hexes
     */
    protected void initializeAll(final @Nullable StringBuffer errBuff) {
        // Initialize all buildings.
        buildings.removeAllElements();
        if (bldgByCoords == null) {
            bldgByCoords = new Hashtable<>();
        } else {
            bldgByCoords.clear();
        }
        // Walk through the hexes, creating buildings.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Does this hex contain a building?
                Hex curHex = getHex(x, y);
                if ((curHex != null) && (curHex.containsTerrain(Terrains.BUILDING))) {
                    // Yup, but is it a repeat?
                    Coords coords = new Coords(x, y);
                    if (!bldgByCoords.containsKey(coords)) {

                        // Nope. Try to create an object for the new building.
                        try {
                            Building bldg = new Building(coords, this, Terrains.BUILDING,
                                    BasementType.getType(curHex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE)));
                            buildings.addElement(bldg);

                            // Each building will identify the hexes it covers.
                            Enumeration<Coords> iter = bldg.getCoords();
                            while (iter.hasMoreElements()) {
                                bldgByCoords.put(iter.nextElement(), bldg);
                            }
                        } catch (IllegalArgumentException excep) {
                            // Log the error and remove the
                            // building from the board.
                            if (errBuff == null) {
                                LogManager.getLogger().error("Unable to create building.", excep);
                            } else {
                                errBuff.append("Unable to create building at ").append(coords)
                                        .append("!\n").append(excep.getMessage()).append("\n");
                            }
                            curHex.removeTerrain(Terrains.BUILDING);
                        }
                    }
                }

                if ((curHex != null) && (curHex.containsTerrain(Terrains.FUEL_TANK))) {
                    // Yup, but is it a repeat?
                    Coords coords = new Coords(x, y);
                    if (!bldgByCoords.containsKey(coords)) {
                        // Nope. Try to create an object for the new building.
                        try {
                            int magnitude = curHex.getTerrain(Terrains.FUEL_TANK_MAGN).getLevel();
                            FuelTank bldg = new FuelTank(coords, this, Terrains.FUEL_TANK, magnitude);
                            buildings.addElement(bldg);

                            // Each building will identify the hexes it covers.
                            Enumeration<Coords> iter = bldg.getCoords();
                            while (iter.hasMoreElements()) {
                                bldgByCoords.put(iter.nextElement(), bldg);
                            }
                        } catch (IllegalArgumentException excep) {
                            // Log the error and remove the fuel tank from the board.
                            if (errBuff == null) {
                                LogManager.getLogger().error("Unable to create fuel tank.", excep);
                            } else {
                                errBuff.append("Unable to create fuel tank at ").append(coords)
                                        .append("!\n").append(excep.getMessage()).append("\n");
                            }
                            curHex.removeTerrain(Terrains.FUEL_TANK);
                        }
                    }
                }

                if ((curHex != null) && curHex.containsTerrain(Terrains.BRIDGE)) {
                    // Yup, but is it a repeat?
                    Coords coords = new Coords(x, y);
                    if (!bldgByCoords.containsKey(coords)) {
                        // Nope. Try to create an object for the new building.
                        try {
                            Building bldg = new Building(coords, this, Terrains.BRIDGE, BasementType.NONE);
                            buildings.addElement(bldg);

                            // Each building will identify the hexes it covers.
                            Enumeration<Coords> iter = bldg.getCoords();
                            while (iter.hasMoreElements()) {
                                bldgByCoords.put(iter.nextElement(), bldg);
                            }
                        } catch (IllegalArgumentException excep) {
                            // Log the error and remove the bridge from the board.
                            if (errBuff == null) {
                                LogManager.getLogger().error("Unable to create bridge.", excep);
                            } else {
                                errBuff.append("Unable to create bridge at ").append(coords)
                                        .append("!\n").append(excep.getMessage()).append("\n");
                            }
                            curHex.removeTerrain(Terrains.BRIDGE);
                        }
                    }
                }
            }
        }

        // Initialize all exits.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                initializeHex(x, y, false);
            }
        }
        processBoardEvent(new BoardEvent(this, null, BoardEvent.BOARD_CHANGED_ALL_HEXES));
        // good time to ensure hex cache
        IdealHex.ensureCacheSize(width + 1, height + 1);

    }

    /**
     * Initialize a hex and the hexes around it
     */
    public void initializeAround(int x, int y) {
        initializeHex(x, y);
        for (int i = 0; i < 6; i++) {
            initializeInDir(x, y, i);
        }
    }

    /**
     * Initializes a hex in a specific direction from an origin hex
     */
    private void initializeInDir(int x, int y, int dir) {
        initializeHex(Coords.xInDir(x, y, dir), Coords.yInDir(x, y, dir));
    }

    /**
     * Initializes a hex in its surroundings. Currently sets the connects
     * parameter appropriately to the surrounding hexes. If a surrounding hex is
     * off the board, it checks the hex opposite the missing hex.
     */
    public void initializeHex(int x, int y) {
        initializeHex(x, y, true);
    }

    private void initializeHex(int x, int y, boolean event) {
        Hex hex = getHex(x, y);

        if (hex == null) {
            return;
        }

        // Always make the coords of the hex match the actual position on the board
        hex.setCoords(new Coords(x, y));
        
        hex.clearExits();
        for (int i = 0; i < 6; i++) {
            Hex other = getHexInDir(x, y, i);
            hex.setExits(other, i, roadsAutoExit);
        }
        
        // Internally handled terrain (inclines, cliff-bottoms)
        initializeAutomaticTerrain(x, y, /* useInclines: */ true);
        
        // Add woods/jungle elevation where none was saved
        initializeFoliageElev(x, y);
        
        if (event) {
            processBoardEvent(new BoardEvent(this, new Coords(x, y), BoardEvent.BOARD_CHANGED_HEX));
        }
    }
    
    /** Adds the FOLIAGE_ELEV terrain when none is present. */
    private void initializeFoliageElev(int x, int y) {
        Hex hex = getHex(x, y);

        // If the foliage elevation is present or the hex doesn't even have foliage,
        // nothing needs to be done
        if (hex.containsTerrain(Terrains.FOLIAGE_ELEV) || 
                (!hex.containsTerrain(Terrains.WOODS) && !hex.containsTerrain(Terrains.JUNGLE))) {
            return;
        }
        
        // Foliage is missing, therefore add it with the standard TW values
        // elevation 3 for Ultra Woods/Jungle and 2 for Light/Heavy
        if (hex.terrainLevel(Terrains.WOODS) == 3 || hex.terrainLevel(Terrains.JUNGLE) == 3) {
            hex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 3));
        } else {
            hex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 2));
        }
    }
    
    /** 
     * Checks all hex edges of the hex at (x, y) if automatically handled 
     * terrains such as inclines must be placed or removed.
     * @param x The hex X-coord.
     * @param y The hex Y-coord.
     * @param useInclines Indicates whether or not to include inclines at hex exits.
     */
    private void initializeAutomaticTerrain(int x, int y, boolean useInclines) {
        Hex hex = getHex(x, y);
        int origCliffTopExits = 0;
        int correctedCliffTopExits = 0;
        int cliffBotExits = 0;
        int inclineTopExits = 0;
        int inclineBotExits = 0;
        int highInclineTopExits = 0;
        int highInclineBotExits = 0;

        // Get the currently set cliff-tops for correction. When exits
        // are not specified, the cliff-tops are removed.
        if (hex.containsTerrain(Terrains.CLIFF_TOP) 
                && hex.getTerrain(Terrains.CLIFF_TOP).hasExitsSpecified()) {
            origCliffTopExits = hex.getTerrain(Terrains.CLIFF_TOP).getExits();
        }

        for (int i = 0; i < 6; i++) {
            Hex other = getHexInDir(x, y, i);
            if (other == null) {
                continue;
            }

//            int levelDiff = hex.getLevel() - other.getLevel();
            int levelDiff = hex.floor() - other.floor();
            int levelDiffToWaterSurface = hex.floor() - other.getLevel();
            boolean inWater = hex.containsTerrain(Terrains.WATER);
            boolean towardsWater = other.containsTerrain(Terrains.WATER);
            boolean manualCliffTopExitInThisDir = ((origCliffTopExits & (1 << i)) != 0);
            boolean cliffTopExitInThisDir = false;

            if (((levelDiff == 1) || (levelDiff == 2)) && manualCliffTopExitInThisDir) {
                correctedCliffTopExits += (1 << i);
                cliffTopExitInThisDir = true;
            }

            // Should there be an incline top?
            if (((levelDiff == 1) || (levelDiff == 2))
                    && !cliffTopExitInThisDir 
                    && !inWater
                    && !towardsWater) {
                inclineTopExits += (1 << i);
            }
            
            if (towardsWater
                    && !inWater
                    && !cliffTopExitInThisDir 
                    && ((levelDiffToWaterSurface == 1) || levelDiffToWaterSurface == 2)) {
                inclineTopExits += (1 << i);
            }

            // Should there be a high level cliff top?
            if (levelDiff > 2 
                    && !inWater
                    && (!towardsWater || levelDiffToWaterSurface > 2)) {
                highInclineTopExits += (1 << i);
            }
            
            // Should there be an incline bottom or a cliff bottom?
            // This needs to check for a cliff-top in the other hex and
            // in the opposite direction
            if ((levelDiff == -1) || (levelDiff == -2)) {
                if (other.hasCliffTopTowards(hex)) {
                    cliffBotExits += (1 << i);
                } else if (!inWater) {
                    inclineBotExits += (1 << i);
                }
            }

            // Should there be a high level cliff bottom?
            if (levelDiff < -2 && !inWater) {
                highInclineBotExits += (1 << i);
            }
        }
        addOrRemoveAutoTerrain(hex, Terrains.CLIFF_TOP, correctedCliffTopExits);
        addOrRemoveAutoTerrain(hex, Terrains.CLIFF_BOTTOM, cliffBotExits);
        if (useInclines) {
            addOrRemoveAutoTerrain(hex, Terrains.INCLINE_TOP, inclineTopExits);
            addOrRemoveAutoTerrain(hex, Terrains.INCLINE_BOTTOM, inclineBotExits);
            addOrRemoveAutoTerrain(hex, Terrains.INCLINE_HIGH_TOP, highInclineTopExits);
            addOrRemoveAutoTerrain(hex, Terrains.INCLINE_HIGH_BOTTOM, highInclineBotExits);
        } else {
            hex.removeTerrain(Terrains.INCLINE_TOP);
            hex.removeTerrain(Terrains.INCLINE_BOTTOM);
            hex.removeTerrain(Terrains.INCLINE_HIGH_TOP);
            hex.removeTerrain(Terrains.INCLINE_HIGH_BOTTOM);
        }
    }

    /** 
     * Adds automatically handled terrain such as inclines when the given
     * exits value is not 0, otherwise removes it.
     */
    private void addOrRemoveAutoTerrain(Hex hex, int terrainType, int exits) {
        if (exits > 0) {
            hex.addTerrain(new Terrain(terrainType, 1, true, exits));
        } else {
            hex.removeTerrain(terrainType);
        }
    }
    
    /**
     * Rebuilds automatic terrains for the whole board.
     * @param useInclines Indicates whether to use inclines on hex exits.
     */
    public void initializeAllAutomaticTerrain(boolean useInclines) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                initializeAutomaticTerrain(x, y, useInclines);
            }
        }
        processBoardEvent(new BoardEvent(this, null, BoardEvent.BOARD_CHANGED_ALL_HEXES));
    }

    /**
     * Determines whether this Board "contains" the specified Coords.
     *
     * @param x the x Coords.
     * @param y the y Coords.
     * @return <code>true</code> if the board contains the specified coords
     */
    public boolean contains(int x, int y) {
        return (x >= 0) && (y >= 0) && (x < width) && (y < height);
    }

    /**
     * Determines whether this Board "contains" the specified Coords.
     *
     * @param c the Coords.
     * @return <code>true</code> if the board contains the specified coords
     */
    public boolean contains(Coords c) {
        if (c == null) {
            return false;
        }
        return contains(c.getX(), c.getY());
    }

    /**
     * @param c the Coords, which may be null
     * @return the Hex at the specified Coords, or null if there is not a hex there
     */
    public @Nullable Hex getHex(final @Nullable Coords c) {
        return (c == null) ? null : getHex(c.getX(), c.getY());
    }

    /**
     * Determines if this Board contains the (x, y) Coords, and if so, sets the specified Hex into
     * that position and initializes it.
     *
     * @param x the x Coords.
     * @param y the y Coords.
     * @param hex the hex to be set into position.
     */
    public void setHex(int x, int y, Hex hex) {
        data[(y * width) + x] = hex;
        initializeHex(x, y);
        // If this hex has exitable terrain, we may need to update the exits in
        // adjacent hexes
        if (hex.hasExitableTerrain()) {
            for (int dir = 0; dir < 6; dir++) {
                if (hex.containsExit(dir)) {
                    initializeInDir(x, y, dir);
                }
            }
        }
    }

    /**
     * Similar to the setHex function for a collection of coordinates and hexes.
     * For each coord/hex pair in the supplied collections, this method determines if the Board
     * contains the coords and if so updates the specified hex into that position and initializes it.
     *
     * The method ensures that each hex that needs to be updated is only updated once.
     *
     * @param coords A list of coordinates to be updated
     * @param hexes The hex to be updated for each coordinate
     */
    public void setHexes(List<Coords> coords, List<Hex> hexes) {
        // Keeps track of hexes that will need to be reinitialized
        LinkedHashSet<Coords> needsUpdate = new LinkedHashSet<>((int) (coords.size() * 1.25 + 0.5));

        // Sanity check
        if (coords.size() != hexes.size()) {
            throw new IllegalStateException("setHexes received two collections differeing size!");
        }

        // Update all input hexes, plus create a set of coords that need
        // updating
        Iterator<Coords> coordIter = coords.iterator();
        Iterator<Hex> hexIter = hexes.iterator();
        while (coordIter.hasNext() && hexIter.hasNext()) {
            Coords currCoord = coordIter.next();
            Hex currHex = hexIter.next();
            int x = currCoord.getX();
            int y = currCoord.getY();
            data[(y * width) + x] = currHex;
            initializeHex(x, y);

            // Add any adjacent hexes that may need to have exits updated
            if (currHex.hasExitableTerrain()) {
                for (int dir = 0; dir < 6; dir++) {
                    if (currHex.containsExit(dir)) {
                        needsUpdate.add(currCoord.translated(dir));
                    }
                }
            }

        }

        for (Coords coord : needsUpdate) {
            initializeHex(coord.getX(), coord.getY());
        }

    }

    /**
     * Sets the hex into the location specified by the Coords.
     *
     * @param c the Coords.
     * @param hex the hex to be set into position.
     */
    public void setHex(Coords c, Hex hex) {
        setHex(c.getX(), c.getY(), hex);
        if (hex.getLevel() < minElevation && minElevation != UNDEFINED_MIN_ELEV) {
            minElevation = hex.getLevel();
        }
        if (hex.getLevel() > maxElevation && maxElevation != UNDEFINED_MAX_ELEV) {
            maxElevation = hex.getLevel();
        }
    }

    /**
     * Checks if a board file is the specified size.
     *
     * @param filepath
     *            The path to the board file.
     * @param size
     *            The dimensions of the board to test.
     * @return {@code true} if the dimensions match.
     */
    public static boolean boardIsSize(final File filepath, final BoardDimensions size) {
        int boardx = 0;
        int boardy = 0;
        try (Reader r = new BufferedReader(new FileReader(filepath))) {
            // read board, looking for "size"
            StreamTokenizer st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('#');
            st.quoteChar('"');
            st.wordChars('_', '_');
            while (st.nextToken() != StreamTokenizer.TT_EOF) {
                if ((st.ttype == StreamTokenizer.TT_WORD) && st.sval.equalsIgnoreCase("size")) {
                    st.nextToken();
                    boardx = (int) st.nval;
                    st.nextToken();
                    boardy = (int) st.nval;
                    break;
                }
            }
        } catch (IOException ex) {
            return false;
        }

        // check and return
        return (boardx == size.width()) && (boardy == size.height());
    }

    /**
     * Inspect specified board file and return its dimensions.
     *
     * @param filepath
     *            The path to the board file.
     * @return A {@link BoardDimensions} object containing the dimension.
     */
    public static BoardDimensions getSize(final File filepath) {
        int boardx = 0;
        int boardy = 0;
        try (Reader r = new BufferedReader(new FileReader(filepath))) {
            // read board, looking for "size"
            StreamTokenizer st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('#');
            st.quoteChar('"');
            st.wordChars('_', '_');
            while (st.nextToken() != StreamTokenizer.TT_EOF) {
                if ((st.ttype == StreamTokenizer.TT_WORD) && st.sval.equalsIgnoreCase("size")) {
                    st.nextToken();
                    boardx = (int) st.nval;
                    st.nextToken();
                    boardy = (int) st.nval;
                    break;
                }
            }
        } catch (IOException ex) {
            return null;
        }
        return new BoardDimensions(boardx, boardy);
    }
    
    /** Inspects the given board file and returns a set of its tags. */
    public static Set<String> getTags(final File filepath) {
        var result = new HashSet<String>();
        try (Reader r = new BufferedReader(new FileReader(filepath))) {
            // read board, looking for "size"
            StreamTokenizer st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('#');
            st.quoteChar('"');
            st.wordChars('_', '_');
            while (st.nextToken() != StreamTokenizer.TT_EOF) {
                if ((st.ttype == StreamTokenizer.TT_WORD) && st.sval.equalsIgnoreCase("tag")) {
                    st.nextToken();
                    if (st.ttype == '"') {
                        result.add(st.sval);
                    }
                } else if ((st.ttype == StreamTokenizer.TT_WORD) && st.sval.equalsIgnoreCase("end")) {
                    break;
                }
            }
        } catch (IOException ex) {
            // return the empty Set
        }
        return result;
    }
    
    public static boolean isValid(String board) {
        Board tempBoard = new Board(16, 17);
        if (!board.endsWith(".board")) {
            board += ".board";
        }

        StringBuffer errBuff = new StringBuffer();
        try (InputStream is = new FileInputStream(new MegaMekFile(Configuration.boardsDir(), board).getFile())) {
            tempBoard.load(is, errBuff, false);
        } catch (IOException ex) {
            return false;
        }
        
        return tempBoard.isValid();
    }

    /**
     * Can the player deploy an entity here? There are no canon rules for the
     * deployment phase (?!). I'm using 3 hexes from map edge.
     */
    public boolean isLegalDeployment(Coords c, int nDir) {
        if ((c == null) || !contains(c)) {
            return false;
        }

        int nLimit = 3;
        // int nDir = en.getStartingPos();
        int minx = 0;
        int maxx = width;
        int miny = 0;
        int maxy = height;
        if (nDir > 10) {
            // Deep deployment, the board is effectively smaller
            nDir -= 10;
            minx = width / 5;
            maxx -= width / 5;
            miny = height / 5;
            maxy -= height / 5;
            if ((c.getX() < minx) || (c.getY() < miny) || (c.getX() >= maxx) || (c.getY() >= maxy)) {
                return false;
            }
        }
        switch (nDir) {
            case START_ANY:
                return true;
            case START_NW:
                return ((c.getX() < (minx + nLimit)) && (c.getX() >= minx) && (c.getY() < (height / 2)))
                        || ((c.getY() < (miny + nLimit)) && (c.getY() >= miny) && (c.getX() < (width / 2)));
            case START_N:
                return (c.getY() < (miny + nLimit)) && (c.getY() >= miny);
            case START_NE:
                return ((c.getX() > (maxx - nLimit)) && (c.getX() < maxx) && (c.getY() < (height / 2)))
                        || ((c.getY() < (miny + nLimit)) && (c.getY() >= miny) && (c.getX() > (width / 2)));
            case START_E:
                return (c.getX() >= (maxx - nLimit)) && (c.getX() < maxx);
            case START_SE:
                return ((c.getX() >= (maxx - nLimit)) && (c.getX() < maxx) && (c.getY() > (height / 2)))
                        || ((c.getY() >= (maxy - nLimit)) && (c.getY() < maxy) && (c.getX() > (width / 2)));
            case START_S:
                return (c.getY() >= (maxy - nLimit)) && (c.getY() < maxy);
            case START_SW:
                return ((c.getX() < (minx + nLimit)) && (c.getX() >= minx) && (c.getY() > (height / 2)))
                        || ((c.getY() >= (maxy - nLimit)) && (c.getY() < maxy) && (c.getX() < (width / 2)));
            case START_W:
                return (c.getX() < (minx + nLimit)) && (c.getX() >= minx);
            case START_EDGE:
                return ((c.getX() < (minx + nLimit)) && (c.getX() >= minx))
                        || ((c.getY() < (miny + nLimit)) && (c.getY() >= miny))
                        || ((c.getX() >= (maxx - nLimit)) && (c.getX() < maxx))
                        || ((c.getY() >= (maxy - nLimit)) && (c.getY() < maxy));
            case START_CENTER:
                return (c.getX() >= (width / 3)) && (c.getX() <= ((2 * width) / 3)) && (c.getY() >= (height / 3))
                        && (c.getY() <= ((2 * height) / 3));
            default: // ummm. .
                return false;
        }

    }

    /**
     * Determine the opposite edge from the given edge
     * Returns START_NONE for non-cardinal edges (North, South, West, East)
     * @param cardinalEdge The edge to return the opposite of
     * @return Constant representing the opposite edge
     */
    public int getOppositeEdge(int cardinalEdge) {
        switch (cardinalEdge) {
            case Board.START_E:
                return Board.START_W;
            case Board.START_N:
                return Board.START_S;
            case Board.START_W:
                return Board.START_E;
            case Board.START_S:
                return Board.START_N;
            default:
                return Board.START_NONE;
        }
    }

    /**
     * Load board data from a file.
     *
     * @param filepath The path to the file.
     */
    public void load(final File filepath) {
        try (InputStream is = new FileInputStream(filepath)) {
            load(is);
        } catch (IOException ex) {
            LogManager.getLogger().error("IO Error opening file to load board! " + ex);
        }
    }

    /**
     * Loads this board from an InputStream
     */
    public void load(InputStream is) {
        load(is, null, false);
    }

    public void load(InputStream is, StringBuffer errBuff, boolean continueLoadOnError) {
        int nw = 0, nh = 0, di = 0;
        Hex[] nd = new Hex[0];
        int index = 0;
        resetStoredElevation();
        try (Reader r = new BufferedReader(new InputStreamReader(is))) {
            StreamTokenizer st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('#');
            st.quoteChar('"');
            st.wordChars('_', '_');
            while (st.nextToken() != StreamTokenizer.TT_EOF) {
                if ((st.ttype == StreamTokenizer.TT_WORD) && st.sval.equalsIgnoreCase("size")) {
                    // read rest of line
                    String[] args = { "0", "0" };
                    int i = 0;
                    while ((st.nextToken() == StreamTokenizer.TT_WORD) || (st.ttype == '"')
                            || (st.ttype == StreamTokenizer.TT_NUMBER)) {
                        args[i++] = st.ttype == StreamTokenizer.TT_NUMBER ? (int) st.nval + "" : st.sval;
                    }
                    nw = Integer.parseInt(args[0]);
                    nh = Integer.parseInt(args[1]);
                    nd = new Hex[nw * nh];
                    di = 0;
                } else if ((st.ttype == StreamTokenizer.TT_WORD) && st.sval.equalsIgnoreCase("option")) {
                    // read rest of line
                    String[] args = { "", "" };
                    int i = 0;
                    while ((st.nextToken() == StreamTokenizer.TT_WORD) || (st.ttype == '"')
                            || (st.ttype == StreamTokenizer.TT_NUMBER)) {
                        args[i++] = st.ttype == StreamTokenizer.TT_NUMBER ? (int) st.nval + "" : st.sval;
                    }
                    // Only expect certain options.
                    if (args[0].equalsIgnoreCase("exit_roads_to_pavement")) {
                        roadsAutoExit = !args[1].equalsIgnoreCase("false");
                    }
                } else if ((st.ttype == StreamTokenizer.TT_WORD) && st.sval.equalsIgnoreCase("hex")) {
                    // read rest of line
                    String[] args = { "", "0", "", "" };
                    int i = 0;
                    while ((st.nextToken() == StreamTokenizer.TT_WORD) || (st.ttype == '"')
                            || (st.ttype == StreamTokenizer.TT_NUMBER)) {
                        args[i++] = st.ttype == StreamTokenizer.TT_NUMBER ? (int) st.nval + "" : st.sval;
                    }
                    int elevation = Integer.parseInt(args[1]);
                    // The coordinates in the .board file are ignored!
                    nd[index] = new Hex(elevation, args[2], args[3], new Coords(index % nw, index / nw));
                    index++;
                } else if ((st.ttype == StreamTokenizer.TT_WORD) && st.sval.equalsIgnoreCase("background")) {
                    st.nextToken();
                    File bgFile = new MegaMekFile(Configuration.boardBackgroundsDir(),
                            st.sval).getFile();
                    if (bgFile.exists()) {
                        backgroundPaths.add(bgFile.getPath());
                    } else {
                        LogManager.getLogger().error("Board specified background image, but path couldn't be found! Path: " + bgFile.getPath());
                    }
                } else if ((st.ttype == StreamTokenizer.TT_WORD) && st.sval.equalsIgnoreCase("description")) {
                    st.nextToken();
                    if (st.ttype == '"') {
                        String d = getDescription();
                        if (null == d) {
                            setDescription(st.sval);
                        } else {
                            setDescription(d + "\n\n" + st.sval);
                        }
                    }
                } else if ((st.ttype == StreamTokenizer.TT_WORD) && st.sval.equalsIgnoreCase("note")) {
                    st.nextToken();
                    if (st.ttype == StreamTokenizer.TT_NUMBER) {
                        int x, y, coordWidth = 100;
                        int coords = (int) st.nval;
                        if (coords > 9999) {
                            coordWidth = 1000;
                        }
                        y = coords % coordWidth;
                        coords /= coordWidth;
                        x = coords;
                        st.nextToken();
                        Coords c = new Coords(x, y);
                        if (st.ttype == '"') {
                            Collection<String> a = new ArrayList<>(getAnnotations(c));
                            a.add(st.sval);
                            setAnnotations(c, a);
                        }
                    }
                } else if ((st.ttype == StreamTokenizer.TT_WORD) && st.sval.equalsIgnoreCase("tag")) {
                    st.nextToken();
                    if (st.ttype == '"') {
                        addTag(st.sval);
                    }
                } else if ((st.ttype == StreamTokenizer.TT_WORD) && st.sval.equalsIgnoreCase("end")) {
                    break;
                }
            }
        } catch (IOException ex) {
            LogManager.getLogger().error("I/O Error: " + ex);
        }

        // fill nulls with blank hexes
        for (int i = 0; i < nd.length; i++) {
            if (nd[i] == null) {
                nd[i] = new Hex();
            }
        }

        // check data integrity
        if (isValid(nd, nw, nh, errBuff) && ((nw > 1) || (nh > 1) || (di == (nw * nh)))) {
            newData(nw, nh, nd, errBuff);
        } else if (continueLoadOnError && ((nw > 1) || (nh > 1) || (di == (nw * nh)))) {
            LogManager.getLogger().error("Invalid board data!");
            newData(nw, nh, nd, errBuff);
        } else if (errBuff == null) {
            LogManager.getLogger().error("Invalid board data!");
        }

    }

    public boolean isValid() {
        // Search for black-listed hexes
        return isValid(data, width, height, null);
    }

    public boolean isValid(StringBuffer errBuff) {
        // Search for black-listed hexes
        return isValid(data, width, height, errBuff);
    }

    private boolean isValid(Hex[] data, int width, int height, StringBuffer errBuff) {
        // Search for black-listed hexes
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Hex hex = data[(y * width) + x];
                if (hex == null) {
                    return false;
                }
                StringBuffer currBuff = new StringBuffer();
                boolean valid = hex.isValid(currBuff);
                
                // Multi-hex problems 
                // A building hex must only have exits to other building hexes of the same Building Type and Class
                if (hex.containsTerrain(Terrains.BUILDING) && hex.getTerrain(Terrains.BUILDING).hasExitsSpecified()) {
                    for (int dir = 0; dir < 6; dir++) {
                        Hex adjHex = getHexInDir(x, y, dir);
                        if ((adjHex != null) 
                                && adjHex.containsTerrain(Terrains.BUILDING) 
                                && hex.containsTerrainExit(Terrains.BUILDING, dir)) {
                            if (adjHex.getTerrain(Terrains.BUILDING).getLevel() != hex.getTerrain(Terrains.BUILDING).getLevel()) {
                                valid = false;
                                currBuff.append("Building has an exit to a building of another Building Type (Light, Medium...).\n");
                            }
                            if (hex.containsTerrain(Terrains.BLDG_CLASS) 
                                    && ((adjHex.containsTerrain(Terrains.BLDG_CLASS) 
                                            && (adjHex.getTerrain(Terrains.BLDG_CLASS).getLevel() != hex.getTerrain(Terrains.BLDG_CLASS).getLevel()))
                                            || (!adjHex.containsTerrain(Terrains.BLDG_CLASS)))) {
                                valid = false;
                                currBuff.append("Building has an exit in direction ").append(dir).append(" to a building of another Building Class.\n");
                            }
                        }
                    }
                }
                
                // Return early if we aren't logging errors
                if (!valid && (errBuff == null)) {
                    return false;
                } else if (!valid) { // Otherwise, log the error for later output
                    if (errBuff.length() > 0) {
                        errBuff.append("----\n");
                    }
                    Coords c = new Coords(x, y);
                    errBuff.append("Hex ").append(c.getBoardNum()).append(" is invalid:\n").append(currBuff);
                }
            }
        }
        return true;
    }

    /**
     * Writes data for the board, as text to the OutputStream
     */
    public void save(OutputStream os) {
        try (Writer w = new OutputStreamWriter(os)) {
            w.write("size " + width + " " + height + "\r\n");
            if (!roadsAutoExit) {
                w.write("option exit_roads_to_pavement false\r\n");
            }
            for (String tag : tags) {
                w.write("tag \"" + tag + "\"\r\n");
            }
            for (int i = 0; i < data.length; i++) {
                Hex hex = data[i];
                boolean firstTerrain = true;

                StringBuilder hexBuff = new StringBuilder("hex ");
                // The coordinates in the .board file are ignored when loading the board!
                hexBuff.append(new Coords(i % width, i / width).getBoardNum());
                hexBuff.append(" ");
                hexBuff.append(hex.getLevel());
                hexBuff.append(" \"");
                int[] terrainTypes = hex.getTerrainTypes();
                for (int j = 0; j < terrainTypes.length; j++) {
                    int terrType = terrainTypes[j];
                    // do not save internally handled terrains
                    if (Terrains.AUTOMATIC.contains(terrType)) {
                        continue;
                    }
                    Terrain terrain = hex.getTerrain(terrType);
                    if (terrain != null) {
                        if (!firstTerrain) {
                            hexBuff.append(";");
                        }
                        hexBuff.append(terrain);
                        // Do something funky to save building exits.
                        if (((Terrains.BUILDING == terrType) || (terrType == Terrains.FUEL_TANK))
                                && !terrain.hasExitsSpecified() && (terrain.getExits() != 0)) {
                            hexBuff.append(":").append(terrain.getExits());
                        }
                        firstTerrain = false;
                    }
                }
                hexBuff.append("\" \"");
                if (hex.getTheme() != null) {
                    hexBuff.append(hex.getTheme());
                }
                hexBuff.append("\"\r\n");

                w.write(hexBuff.toString());
                // w.write("hex \"" + hex.getTerrain().name + "\" " +
                // Terrain.TERRAIN_NAMES[hex.getTerrainType()] + " \"" +
                // hex.getTerrain().picfile + "\" " + hex.getElevation() +
                // "\r\n");
            }
            w.write("end\r\n");
            // make sure it's written
            w.flush();
        } catch (IOException ex) {
            LogManager.getLogger().error("I/O Error: " + ex);
        }
    }

    /**
     * Record that the given coordinates have received a hit from an inferno.
     *
     * @param coords the <code>Coords</code> of the hit.
     * @param round the kind of round that hit the hex.
     * @param hits the <code>int</code> number of rounds that hit
     * @throws IllegalArgumentException if the hits number is negative
     */
    public void addInfernoTo(Coords coords, InfernoTracker.Inferno round, int hits) {
        // Make sure the # of hits is valid.
        if (hits < 0) {
            throw new IllegalArgumentException("Board can't track negative hits. ");
        }

        // Do nothing if the coords aren't on this board.
        if (!this.contains(coords)) {
            return;
        }

        // Do we already have a tracker for those coords?
        InfernoTracker tracker = infernos.get(coords);
        if (null == tracker) {
            // Nope. Make one.
            tracker = new InfernoTracker();
            infernos.put(coords, tracker);
        }

        // Update the tracker.
        tracker.add(round, hits);

    }

    /**
     * Extinguish inferno at the target hex.
     *
     * @param coords the <code>Coords</code> of the hit.
     */
    public void removeInfernoFrom(Coords coords) {
        // Do nothing if the coords aren't on this board.
        if (!this.contains(coords)) {
            return;
        }
        infernos.remove(coords);
    }

    /**
     * Determine if the given coordinates has a burning inferno.
     *
     * @param coords
     *            - the <code>Coords</code> being checked.
     * @return <code>true</code> if those coordinates have a burning inferno
     *         round. <code>false</code> if no inferno has hit those coordinates
     *         or if it has burned out.
     */
    public boolean isInfernoBurning(Coords coords) {
        boolean result = false;
        InfernoTracker tracker;

        // Get the tracker for those coordinates
        // and see if the fire is still burning.
        tracker = infernos.get(coords);
        if (null != tracker) {
            if (tracker.isStillBurning()) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Get an enumeration of all coordinates with infernos still burning.
     *
     * @return an <code>Enumeration</code> of <code>Coords</code> that have infernos still burning.
     */
    public Enumeration<Coords> getInfernoBurningCoords() {
        // Only include *burning* inferno trackers.
        Vector<Coords> burning = new Vector<>();
        Enumeration<Coords> iter = infernos.keys();
        while (iter.hasMoreElements()) {
            final Coords coords = iter.nextElement();
            if (isInfernoBurning(coords)) {
                burning.addElement(coords);
            }
        }
        return burning.elements();
    }

    /**
     * @return an <code>Enumeration</code> of <code>Building</code>s on the Board
     */
    public Enumeration<Building> getBuildings() {
        return buildings.elements();
    }

    /**
     * @return the Vector of all the board's buildings
     */
    public Vector<Building> getBuildingsVector() {
        return buildings;
    }

    /**
     * Get the building at the given coordinates.
     *
     * @param coords the <code>Coords</code> being examined.
     * @return a <code>Building</code> object, if there is one at the given coordinates, otherwise a
     * <code>null</code> will be returned.
     */
    public @Nullable Building getBuildingAt(Coords coords) {
        return bldgByCoords.get(coords);
    }

    /**
     * Get the local object for the given building. Call this routine any time
     * the input <code>Building</code> is suspect.
     *
     * @param other
     *            - a <code>Building</code> object which may or may not be
     *            represented on this board. This value may be <code>null</code>
     *            .
     * @return The local <code>Building</code> object if we can find a match. If
     *         the other building is not on this board, a <code>null</code> is
     *         returned instead.
     */
    private Building getLocalBuilding(Building other) {
        // Handle garbage input.
        if (other == null) {
            return null;
        }

        // ASSUMPTION: it is better to use the Hashtable than the Vector.
        Building local = null;
        Enumeration<Coords> coords = other.getCoords();
        if (coords.hasMoreElements()) {
            local = bldgByCoords.get(coords.nextElement());
            if (!other.equals(local)) {
                local = null;
            }
        }

        // TODO: if local is still null, try the Vector.
        return local;
    }

    /**
     * Collapse a vector of building hexes.
     *
     * @param coords the <code>Vector</code> of <code>Coord</code> objects to be collapsed.
     */
    public void collapseBuilding(Vector<Coords> coords) {
        // Walk through the vector of coords.
        Enumeration<Coords> loop = coords.elements();
        while (loop.hasMoreElements()) {
            final Coords other = loop.nextElement();

            // Update the building.
            this.collapseBuilding(other);
        }
    }

    /**
     * The given building hex has collapsed. Remove it from the board and replace it with rubble.
     *
     * @param coords the <code>Building</code> that has collapsed.
     */
    public void collapseBuilding(Coords coords) {
        final Hex curHex = this.getHex(coords);

        // Remove the building from the building map.
        Building bldg = bldgByCoords.get(coords);
        if (bldg == null) {
            return;
        }
        bldg.removeHex(coords);
        bldgByCoords.remove(coords);

        // determine type of rubble
        // Terrain type can be a max of 4 for harded building
        // 5 for walls, but the only place where we actually check
        // for rubble type is resolveFindClub in Server, and we
        // make it impossible to find clubs in wallrubble there
        int type = curHex.terrainLevel(Terrains.BUILDING);
        type = Math.max(type, curHex.terrainLevel(Terrains.BRIDGE));
        type = Math.max(type, curHex.terrainLevel(Terrains.FUEL_TANK));

        // Remove the building terrain.
        curHex.removeTerrain(Terrains.BUILDING);
        curHex.removeTerrain(Terrains.BLDG_CF);
        curHex.removeTerrain(Terrains.BLDG_ELEV);
        curHex.removeTerrain(Terrains.FUEL_TANK);
        curHex.removeTerrain(Terrains.FUEL_TANK_CF);
        curHex.removeTerrain(Terrains.FUEL_TANK_ELEV);
        curHex.removeTerrain(Terrains.FUEL_TANK_MAGN);
        curHex.removeTerrain(Terrains.BRIDGE);
        curHex.removeTerrain(Terrains.BRIDGE_CF);
        curHex.removeTerrain(Terrains.BRIDGE_ELEV);

        // Add rubble terrain that matches the building type.
        if (type > 0) {
            int rubbleLevel = bldg.getBldgClass() == Building.FORTRESS ? 2 : 1;
            curHex.addTerrain(new Terrain(Terrains.RUBBLE, rubbleLevel));
        }

        if (curHex.containsTerrain(Terrains.BLDG_BASEMENT_TYPE)) {
            // per TW 176 the basement doesn't change the elevation of the
            // bulding hex
            // the basement fills in with the rubble of the building
            // any units in the basement are destroyed
            curHex.removeTerrain(Terrains.BLDG_BASEMENT_TYPE);
        }

        // Update the hex.
        // TODO : Do I need to initialize it???
        // ASSUMPTION: It's faster to update one at a time.
        this.setHex(coords, curHex);

    }

    /**
     * The given building has collapsed. Remove it from the board and replace it with rubble.
     *
     * @param bldg the <code>Building</code> that has collapsed.
     */
    public void collapseBuilding(Building bldg) {

        // Remove the building from our building vector.
        buildings.removeElement(bldg);

        // Walk through the building's hexes.
        Enumeration<Coords> bldgCoords = bldg.getCoords();
        while (bldgCoords.hasMoreElements()) {
            final Coords coords = bldgCoords.nextElement();
            collapseBuilding(coords);
        }
    }

    /**
     * Update the construction factors on an array of buildings.
     *
     * @param bldgs the <code>Vector</code> of <code>Building</code> objects to be updated.
     */
    public void updateBuildings(Vector<Building> bldgs) {

        // Walk through the vector of buildings.
        Enumeration<Building> loop = bldgs.elements();
        while (loop.hasMoreElements()) {
            final Building other = loop.nextElement();

            // Find the local object for the given building.
            Building bldg = getLocalBuilding(other);

            // Handle garbage input.
            if (bldg == null) {
                LogManager.getLogger().error("Could not find a match for " + other + " to update.");
                continue;
            }
            Enumeration<Coords> coordsEnum = bldg.getCoords();
            while (coordsEnum.hasMoreElements()) {
                // Set the current and phase CFs of the building hexes.
                final Coords coords = coordsEnum.nextElement();
                bldg.setCurrentCF(other.getCurrentCF(coords), coords);
                bldg.setPhaseCF(other.getPhaseCF(coords), coords);
                bldg.setArmor(other.getArmor(coords), coords);
                bldg.setBasement(coords,
                        BasementType.getType(getHex(coords).terrainLevel(Terrains.BLDG_BASEMENT_TYPE)));
                bldg.setBasementCollapsed(coords, other.getBasementCollapsed(coords));
                bldg.setDemolitionCharges(other.getDemolitionCharges());
            }
        }
    }

    /**
     * Get the current value of the "road auto-exit" option.
     *
     * @return <code>true</code> if roads should automatically exit onto all adjacent pavement hexes.
     * <code>false</code> otherwise.
     */
    public boolean getRoadsAutoExit() {
        return roadsAutoExit;
    }

    /**
     * Set the value of the "road auto-exit" option.
     *
     * @param value The value to set for the option; <code>true</code> if roads should automatically
     *              exit onto all adjacent pavement hexes. <code>false</code> otherwise.
     */
    public void setRoadsAutoExit(boolean value) {
        roadsAutoExit = value;
    }

    /**
     * Populate the <code>bldgByCoords</code> member from the current
     * <code>Vector</code> of buildings. Use this method after de- serializing a
     * <code>Board</code> object.
     */
    private void createBldgByCoords() {
        // Make a new hashtable.
        bldgByCoords = new Hashtable<>();

        // Walk through the vector of buildings.
        Enumeration<Building> loop = buildings.elements();
        while (loop.hasMoreElements()) {
            final Building bldg = loop.nextElement();

            // Each building identifies the hexes it covers.
            Enumeration<Coords> iter = bldg.getCoords();
            while (iter.hasMoreElements()) {
                bldgByCoords.put(iter.nextElement(), bldg);
            }
        }
    }

    /**
     * Override the default deserialization to populate the transient
     * <code>bldgByCoords</code> member.
     *
     * @param in the <code>ObjectInputStream</code> to read.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // Restore bldgByCoords from buildings.
        createBldgByCoords();
    }

    /**
     * Adds the specified board listener to receive board events from this board.
     *
     * @param listener the board listener.
     */
    public void addBoardListener(BoardListener listener) {
        if (boardListeners == null) {
            boardListeners = new Vector<>();
        }
        boardListeners.addElement(listener);
    }

    /**
     * Removes the specified board listener.
     *
     * @param listener the board listener.
     */
    public void removeBoardListener(BoardListener listener) {
        if (boardListeners != null) {
            boardListeners.removeElement(listener);
        }
    }

    protected void processBoardEvent(BoardEvent event) {
        if (boardListeners == null) {
            return;
        }
        for (BoardListener l : boardListeners) {
            switch (event.getType()) {
                case BoardEvent.BOARD_CHANGED_HEX:
                    l.boardChangedHex(event);
                    break;
                case BoardEvent.BOARD_NEW_BOARD:
                    l.boardNewBoard(event);
                    break;
                case BoardEvent.BOARD_CHANGED_ALL_HEXES:
                    l.boardChangedAllHexes(event);
                    break;
            }
        }
    }

    protected Vector<BoardListener> getListeners() {
        if (boardListeners == null) {
            boardListeners = new Vector<>();
        }
        return boardListeners;
    }

    /**
     * @return an <code>Hashtable</code> of <code>InfernoTrackers</code> on the board.
     */
    public Hashtable<Coords, InfernoTracker> getInfernos() {
        return infernos;
    }

    /**
     * Set the CF of bridges
     *
     * @param value The value to set the bridge CF to.
     */
    public void setBridgeCF(int value) {
        for (Building bldg : buildings) {
            for (Enumeration<Coords> coords = bldg.getCoords(); coords.hasMoreElements();) {
                Coords c = coords.nextElement();
                Hex h = getHex(c);
                if (h.containsTerrain(Terrains.BRIDGE)) {
                    bldg.setCurrentCF(value, c);
                }
            }
        }
    }

    // Kill all the unknown basements
    public void setRandomBasementsOff() {
        for (Building b : buildings) {
            for (Enumeration<Coords> coords = b.getCoords(); coords.hasMoreElements();) {
                Coords c = coords.nextElement();
                if (b.getBasement(c).isUnknown()) {
                    b.setBasement(c, BasementType.NONE);
                }
            }
        }
    }

    /**
     * This returns special events that should be marked on hexes, such as artillery fire.
     */
    public Collection<SpecialHexDisplay> getSpecialHexDisplay(Coords coords) {
        return specialHexes.get(coords);
    }

    public void addSpecialHexDisplay(Coords coords, SpecialHexDisplay shd) {
        Collection<SpecialHexDisplay> col;
        if (!specialHexes.containsKey(coords)) {
            col = new LinkedList<>();
            specialHexes.put(coords, col);
        } else {
            col = specialHexes.get(coords);
            // It's possible we are updating a SHD that is already entered.
            // If that is the case, we want to remove the original entry.
            col.remove(shd);
        }

        col.add(shd);
    }

    public void removeSpecialHexDisplay(Coords coords, SpecialHexDisplay shd) {
        Collection<SpecialHexDisplay> col = specialHexes.get(coords);
        if (col != null) {
            col.remove(shd);
        }
    }

    public Hashtable<Coords, Collection<SpecialHexDisplay>> getSpecialHexDisplayTable() {
        return specialHexes;
    }

    public void setSpecialHexDisplayTable(Hashtable<Coords, Collection<SpecialHexDisplay>> shd) {
        specialHexes = shd;
    }

    public void setType(int t) {
        mapType = t;
    }

    public int getType() {
        return mapType;
    }

    public static String getTypeName(int t) {
        return typeNames[t];
    }

    // some convenience functions
    public boolean onGround() {
        return (mapType == T_GROUND);
    }

    public boolean inAtmosphere() {
        return (mapType == T_ATMOSPHERE);
    }

    public boolean inSpace() {
        return (mapType == T_SPACE);
    }

    /**
     * @return the highest elevation hex on the board.
     */
    public int getMaxElevation() {
        if (maxElevation != UNDEFINED_MAX_ELEV) {
            return maxElevation;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int elevation = data[(y * width) + x].getLevel();
                if (maxElevation < elevation) {
                    maxElevation = elevation;
                }
            }
        }
        return maxElevation;
    }

    /**
     * @return the lowest elevation hex on the board.
     */
    public int getMinElevation() {
        if (minElevation != UNDEFINED_MIN_ELEV) {
            return minElevation;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int elevation = data[(y * width) + x].getLevel();
                if (minElevation > elevation) {
                    minElevation = elevation;
                }
            }
        }
        return minElevation;
    }

    /**
     * Resets the Min and Max elevations to their default values.
     */
    public void resetStoredElevation() {
        minElevation = UNDEFINED_MIN_ELEV;
        maxElevation = UNDEFINED_MAX_ELEV;
    }

    public boolean containsBridges() {
        for (Coords c : bldgByCoords.keySet()) {
            Hex hex = getHex(c);
            if (hex.containsTerrain(Terrains.BRIDGE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the list of background images associated with this board. If created from a single
     * board file, then the list should only have one element. Multiple elements exist when the
     * board is created by combining multiple board files.
     */
    public List<String> getBackgroundPaths() {
        return backgroundPaths;
    }

    /**
     * @return the first element of the background path list, or null if it is empty.
     */
    public @Nullable String getBackgroundPath() {
        return getBackgroundPaths().isEmpty() ? null : backgroundPaths.get(0);
    }

    /**
     * @return the number of boards in width that were used to create this board. Only used when
     * background paths are set.
     */
    public int getNumBoardsWidth() {
        return numBoardsWidth;
    }

    /**
     * @return the number of boards in height that were used to create this board. Only used when
     * background paths are set.
     */
    public int getNumBoardsHeight() {
        return numBoardsHeight;
    }

    /**
     * Flag that determines if the board background image should be flipped horizontally. Only used
     * when background paths are set.
     */
    public List<Boolean> getFlipBGHoriz() {
        return flipBGHoriz;
    }


    /**
     * Flag that determines if the board background image should be flipped vertically. Only used
     * when background paths are set.
     */
    public List<Boolean> getFlipBGVert() {
        return flipBGVert;
    }

    public int getSubBoardWidth() {
        return subBoardWidth;
    }

    public int getSubBoardHeight() {
        return subBoardHeight;
    }

    public void setSubBoardWidth(int width) {
        subBoardWidth = width;
    }

    public void setSubBoardHeight(int height) {
        subBoardHeight = height;
    }

    public void setNumBoardsWidth(int width) {
        numBoardsWidth = width;
    }

    public void setNumBoardsHeight(int height) {
        numBoardsHeight = height;
    }

    public void addBackgroundPath(String path, boolean flipVert, boolean flipHorz) {
        backgroundPaths.add(path);

        flipBGVert.add(flipVert);
        flipBGHoriz.add(flipHorz);
    }

    public boolean hasBoardBackground() {
        return (backgroundPaths != null) && !backgroundPaths.isEmpty();
    }

    /**
     * Gets the description of the map.
     * @return The description of the map, if one exists, otherwise null.
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the map.
     * @param s The description of the map; may be null.
     */
    public void setDescription(@Nullable String s) {
        description = s;
    }

    /**
     * Gets every annotation on the map.
     * @return A read-only map of per-hex annotations.
     */
    public Map<Coords, Collection<String>> getAnnotations() {
        return Collections.unmodifiableMap(annotations);
    }

    /**
     * Gets the annotations associated with a hex.
     * @param x The X-Coordinate of the hex.
     * @param y The Y-Coordinate of the hex.
     * @return A collection of annotations for the hex.
     */
    public Collection<String> getAnnotations(int x, int y) {
        return getAnnotations(new Coords(x, y));
    }

    /**
     * Gets the annotations associated with a hex.
     * @param c Coordinates of the hex.
     * @return A collection of annotations for the hex.
     */
    public Collection<String> getAnnotations(Coords c) {
        return annotations.getOrDefault(c, Collections.emptyList());
    }

    /**
     * Sets annotations on a given hex.
     * @param c Coordinates of the hex to apply the annotations to.
     * @param a A collection of annotations to assign to the hex. This may be null.
     */
    public void setAnnotations(Coords c, @Nullable Collection<String> a) {
        if (null == a || a.isEmpty()) {
            annotations.remove(c);
        } else {
            annotations.put(c, a);
        }
    }

    /** 
     * Sets a tileset theme for all hexes of the board. 
     * Passing null as newTheme resets the theme to the theme specified in the board file.
     */ 
    public void setTheme(final @Nullable String newTheme) {
        boolean reset = newTheme == null;
        
        for (int c = 0; c < width * height; c++) {
            if (reset) {
                data[c].resetTheme();
            } else {
                data[c].setTheme(newTheme);
            }
        }
        processBoardEvent(new BoardEvent(this, null, BoardEvent.BOARD_CHANGED_ALL_HEXES));
    }

    /**
     * @return true when the given Coord c is on the edge of the board.
     */
    public boolean isOnBoardEdge(Coords c) {
        return (c.getX() == 0) 
                || (c.getY() == 0)
                || (c.getX() == (width - 1)) 
                || (c.getY() == (height - 1));
    }

    public static Board createEmptyBoard(int width, int height) {
        Hex[] hexes = new Hex[width * height];
        for (int i = 0; i < width * height; i++) {
            hexes[i] = new Hex();
        }
        return new Board(width, height, hexes);
    }

    /**
     * Add the given tag string to the board's tags list.
     */
    public void addTag(String newTag) {
        tags.add(newTag);
    }
    /** Removes the given tag string from the board's tags list. */

    public void removeTag(String tag) {
        tags.remove(tag);
    }

    /**
     * @return the board's tags list. The list is unmodifiable. Use addTag and removeTag to change it.
     */
    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }
}
