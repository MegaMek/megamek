/*
  Copyright (Cc) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.board;

import static java.util.stream.Collectors.toList;
import static megamek.common.SpecialHexDisplay.Type.BOMB_DRIFT;
import static megamek.common.SpecialHexDisplay.Type.BOMB_HIT;
import static megamek.common.SpecialHexDisplay.Type.BOMB_MISS;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.Configuration;
import megamek.common.Hex;
import megamek.common.IdealHex;
import megamek.common.Inferno;
import megamek.common.InfernoTracker;
import megamek.common.Player;
import megamek.common.SpecialHexDisplay;
import megamek.common.annotations.Nullable;
import megamek.common.enums.BasementType;
import megamek.common.equipment.FuelTank;
import megamek.common.event.board.BoardEvent;
import megamek.common.event.board.BoardListener;
import megamek.common.hexArea.HexArea;
import megamek.common.loaders.MapSettings;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingTerrain;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

public class Board implements Serializable {
    @Serial
    private static final long serialVersionUID = -5744058872091016636L;
    private static final MMLogger logger = MMLogger.create(Board.class);

    // region Variable Declarations

    public static final String BOARD_REQUEST_ROTATION = "rotate:";
    public static final int BOARD_NONE = -1;
    public static final String BOARD_NAME_UNNAMED = "Unnamed";

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
    public static final int NUM_ZONES = 11;
    public static final int NUM_ZONES_X2 = 22;

    // Board Dimensions
    // Used for things like artillery rules that reference the standard map sheet dimensions
    public static final int DEFAULT_BOARD_HEIGHT = 17;
    public static final int DEFAULT_BOARD_WIDTH = 16;

    // Variable board width and height. Used for most everything else since we're not restricted to paper map sizes
    private int width;
    private int height;

    // Min and Max elevation values for when they are undefined (since you can't set an int to null).
    private static final int UNDEFINED_MIN_ELEV = 10000;
    private static final int UNDEFINED_MAX_ELEV = -10000;

    // The min and max elevation values for this board.
    // set when getMinElevation/getMax is called for the first time.
    private int minElevation = UNDEFINED_MIN_ELEV;
    private int maxElevation = UNDEFINED_MAX_ELEV;

    private BoardType boardType = BoardType.GROUND;

    private Hex[] data;

    /**
     * Building data structures.
     */
    private final Vector<IBuilding> buildings = new Vector<>();
    private transient Hashtable<Coords, IBuilding> bldgByCoords = new Hashtable<>();

    protected transient Vector<BoardListener> boardListeners = new Vector<>();

    /**
     * Record the infernos placed on the board.
     */
    private final Hashtable<Coords, InfernoTracker> infernos = new Hashtable<>();

    private Map<Coords, Collection<SpecialHexDisplay>> specialHexes = new Hashtable<>();

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
    private final Map<Coords, Collection<String>> annotations = new HashMap<>();

    /** Tags associated with this board to facilitate searching for it. */
    private final Set<String> tags = new HashSet<>();

    private int boardId = 0;

    public static final int MAX_DEPLOYMENT_ZONE_NUMBER = 31;

    /**
     * The board's deployment zones. These may come as terrains from the board file, or they may be set by code. The
     * field is transient as zones can be reconstructed from terrain and the areas field and may have many coords.
     */
    private transient Map<Integer, Set<Coords>> deploymentZones = null;

    /**
     * HexAreas that are set by code to be deployment zones.
     */
    private final Map<Integer, HexArea> areas = new HashMap<>();

    /**
     * At each Coords, one other, lower type board can be located, e.g. a ground board can be embedded in a low
     * atmosphere board hex or a low atmosphere board can be embedded in a ground row hex of a high altitude board. This
     * map gives the board ID for each affected Coords. This and {@link #enclosingBoard} should correspond to each other
     * across the boards of a game.
     */
    private final Map<Coords, Integer> embeddedBoards = new HashMap<>();

    /**
     * This board may be embedded in (= enclosed by) a higher type board, e.g. if this is a ground map, it may be
     * embedded in one or more hexes of a low atmosphere map. This and {@link #embeddedBoards} should correspond to each
     * other across the boards of a game.
     */
    private int enclosingBoard = -1;

    private String mapName = BOARD_NAME_UNNAMED;

    // endregion Variable Declarations

    // region Constructors

    /**
     * Creates a new board with zero as its width and height parameters.
     */
    public Board() {
        this(0, 0);
    }

    /**
     * Creates a new board of the specified dimensions. All hexes in the board will be null until otherwise set.
     *
     * @param width  the width dimension.
     * @param height the height dimension.
     */
    public Board(int width, int height) {
        this.width = width;
        this.height = height;
        data = new Hex[width * height];
    }

    /**
     * Creates a new board of the specified dimensions and specified hex data. Note that the number of Hexes given
     * should be equal to width * height to avoid null Hexes in the board.
     *
     * @param width  the width dimension
     * @param height the height dimension
     * @param data   the Hexes of the new board
     */
    public Board(int width, int height, Hex... data) {
        this.width = width;
        this.height = height;
        this.data = Arrays.copyOf(data, data.length);
    }

    /**
     * Returns a new atmospheric (low altitude) board with no terrain (sky map) of the given size.
     *
     * @param width  the width of the board
     * @param height the height of the board
     *
     * @return the new board, ready to be used
     */
    public static Board getSkyBoard(int width, int height) {
        Hex[] data = new Hex[width * height];
        int index = 0;
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                data[index++] = new Hex(0, "sky:1", "", new Coords(w, h));
            }
        }
        Board result = new Board(width, height, data);
        result.setBoardType(BoardType.SKY);
        return result;
    }

    /**
     * Returns a new space board of the given size.
     *
     * @param width  the width of the board
     * @param height the height of the board
     *
     * @return the new board, ready to be used
     */
    public static Board getSpaceBoard(int width, int height) {
        Hex[] data = new Hex[width * height];
        int index = 0;
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                data[index++] = new Hex(0, "space:1", "", new Coords(w, h));
            }
        }
        Board result = new Board(width, height, data);
        result.setBoardType(BoardType.FAR_SPACE);
        return result;
    }
    // endregion Constructors

    /**
     * @return Map height in hexes
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return Map width in hexes
     */
    public int getWidth() {
        return width;
    }

    public Coords getCenter() {
        return new Coords(getWidth() / 2, getHeight() / 2);
    }

    /**
     * Creates a new data set for the board, with the specified dimensions and data; notifies listeners that a new data
     * set has been created.
     *
     * @param width  the width dimension.
     * @param height the height dimension.
     * @param data   new hex data appropriate for the board.
     * @param errors A buffer for storing error messages, if any. This is allowed to be null.
     */
    public void newData(final int width, final int height, final Hex[] data,
          final @Nullable List<String> errors) {
        this.width = width;
        this.height = height;
        this.data = data;

        initializeAll(errors);
        processBoardEvent(new BoardEvent(this, null, BoardEvent.BOARD_NEW_BOARD));
    }

    /**
     * Determines if this Board contains the (x, y) Coords, and if so, returns the Hex at that position.
     *
     * @param x the x Coords.
     * @param y the y Coords.
     *
     * @return the Hex, if this Board contains the (x, y) location; null otherwise.
     */
    public @Nullable Hex getHex(final int x, final int y) {
        return contains(x, y) ? data[(y * width) + x] : null;
    }

    /**
     * @param c   starting coordinates
     * @param dir direction
     *
     * @return the hex in the specified direction from the specified starting coordinates.
     */
    public Hex getHexInDir(Coords c, int dir) {
        return getHex(c.xInDir(dir), c.yInDir(dir));
    }

    /**
     * Gets the hex in the specified direction from the specified starting coordinates. This avoids calls to
     * Coords.translated, and thus, object construction.
     *
     * @param x   starting x coordinate
     * @param y   starting y coordinate
     * @param dir direction
     *
     * @return the hex in the specified direction from the specified starting coordinates.
     */
    public Hex getHexInDir(int x, int y, int dir) {
        return getHex(Coords.xInDir(x, y, dir), Coords.yInDir(x, y, dir));
    }

    /**
     * Initialize all hexes
     */
    protected void initializeAll(final @Nullable List<String> errors) {
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
                            IBuilding bldg = new BuildingTerrain(coords, this, Terrains.BUILDING,
                                  BasementType.getType(curHex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE)));
                            addBuildingToBoard(bldg);
                        } catch (IllegalArgumentException exception) {
                            // Log the error and remove the building from the board.
                            if (errors == null) {
                                logger.error(exception, "Unable to create building.");
                            } else {
                                errors.add("Unable to create building at " + coords + ". " + exception.getMessage());
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
                            addBuildingToBoard(bldg);
                        } catch (IllegalArgumentException exception) {
                            // Log the error and remove the fuel tank from the board.
                            if (errors == null) {
                                logger.error(exception, "Unable to create fuel tank.");
                            } else {
                                errors.add("Unable to create fuel tank at " + coords + ". " + exception.getMessage());
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
                            IBuilding bldg = new BuildingTerrain(coords, this, Terrains.BRIDGE, BasementType.NONE);
                            addBuildingToBoard(bldg);
                        } catch (IllegalArgumentException exception) {
                            // Log the error and remove the bridge from the board.
                            if (errors == null) {
                                logger.error(exception, "Unable to create bridge.");
                            } else {
                                errors.add("Unable to create bridge at " + coords + ". " + exception.getMessage());
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
     * Initializes a hex in its surroundings. Currently, sets the connects parameter appropriately to the surrounding
     * hexes. If a surrounding hex is off the board, it checks the hex opposite the missing hex.
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
        initializeAutomaticTerrain(x, y);

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
     * Checks all hex edges of the hex at (x, y) if automatically handled terrains such as inclines must be placed or
     * removed.
     *
     * @param x The hex X-Coordinate.
     * @param y The hex Y-Coordinate.
     */
    private void initializeAutomaticTerrain(int x, int y) {
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

            // int levelDiff = hex.getLevel() - other.getLevel();
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
        if (GUIPreferences.getInstance().getHexInclines()) {
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
     * Adds automatically handled terrain such as inclines when the given exits value is not 0, otherwise removes it.
     */
    private void addOrRemoveAutoTerrain(Hex hex, int terrainType, int exits) {
        if (exits > 0) {
            hex.addTerrain(new Terrain(terrainType, 1, true, exits));
        } else {
            hex.removeTerrain(terrainType);
        }
    }

    /**
     * Rebuilds automatic terrains for the whole board, such as incline highlighting. Also fires a
     * BOARD_CHANGED_ALL_HEXES event.
     */
    public void initializeAllAutomaticTerrain() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                initializeAutomaticTerrain(x, y);
            }
        }
        processBoardEvent(new BoardEvent(this, null, BoardEvent.BOARD_CHANGED_ALL_HEXES));
    }

    /**
     * Determines whether this Board "contains" the specified Coords.
     *
     * @param x the x Coords.
     * @param y the y Coords.
     *
     * @return <code>true</code> if the board contains the specified coords
     */
    public boolean contains(int x, int y) {
        return (x >= 0) && (y >= 0) && (x < width) && (y < height);
    }

    /**
     * Determines whether this Board "contains" the specified Coords.
     *
     * @param coords the Coords.
     *
     * @return <code>true</code> if the board contains the specified coords
     */
    public boolean contains(@Nullable Coords coords) {
        return coords != null && contains(coords.getX(), coords.getY());
    }

    /**
     * Determines whether this Board "contains" the specified location. True only when the board IDs match and the
     * coords of the location are within the borders of the board.
     *
     * @param location the location to test
     *
     * @return true if the board contains the specified location
     */
    public boolean contains(@Nullable BoardLocation location) {
        return (location != null) && location.isOn(boardId) && contains(location.coords());
    }

    /**
     * Returns the Hex at the given Coords, both of which may be null.
     *
     * @param coords the Coords to look for the Hex
     *
     * @return the Hex at the specified Coords, or null if there is not a hex there
     */
    public @Nullable Hex getHex(final @Nullable Coords coords) {
        return (coords == null) ? null : getHex(coords.getX(), coords.getY());
    }

    /**
     * Returns a list of Hexes at the given coords. The list will never be null but may be empty depending on the given
     * Coords collection. If the given Coords collection is null, the returned list will be empty.
     *
     * @param coords the Coords to query
     *
     * @return the Hexes at the specified Coords
     */
    public List<Hex> getHexes(final @Nullable Collection<Coords> coords) {
        if (coords == null) {
            logger.warn("Method called with null Coords list!");
            return new ArrayList<>();
        } else {
            return coords.stream().map(this::getHex).filter(Objects::nonNull).collect(toList());
        }
    }

    /**
     * Determines if this Board contains the (x, y) Coords, and if so, sets the specified Hex into that position and
     * initializes it.
     *
     * @param x   the x Coords.
     * @param y   the y Coords.
     * @param hex the hex to be set into position.
     */
    public void setHex(int x, int y, Hex hex) {
        Map<BoardLocation, Hex> changedHex = new HashMap<>();
        changedHex.put(BoardLocation.of(new Coords(x, y), boardId), hex);
        setHexes(changedHex);
    }

    /**
     * Copies in the given hexes, overwriting any affected hexes that are on this board. For simplicity, this method
     * ignores {@link BoardLocation}'s that are not on this board, i.e., it can be called without first filtering the
     * locations for board ID.
     *
     * @param changedHexes A map of locations and hexes; the locations need not all (or any) match this board
     */
    public void setHexes(Map<BoardLocation, Hex> changedHexes) {
        Set<Coords> needsUpdate = new HashSet<>();
        for (Map.Entry<BoardLocation, Hex> entry : changedHexes.entrySet()) {
            if (boardId != entry.getKey().boardId()) {
                continue;
            }

            Coords currCoords = entry.getKey().coords();
            Hex currHex = entry.getValue();
            int x = currCoords.getX();
            int y = currCoords.getY();

            // Client may have sent off-board coordinates or null info; ignore.
            if (!contains(x, y) || null == currHex) {
                continue;
            }

            data[(y * width) + x] = currHex;
            initializeHex(x, y);

            // Add any adjacent hexes that may need to have exits updated
            if (currHex.hasExitableTerrain()) {
                for (int dir = 0; dir < 6; dir++) {
                    if (currHex.containsExit(dir)) {
                        needsUpdate.add(currCoords.translated(dir));
                    }
                }
            }
        }

        for (Coords coords : needsUpdate) {
            initializeHex(coords.getX(), coords.getY());
        }
    }

    /**
     * Sets the hex into the location specified by the Coords.
     *
     * @param c   the Coords.
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
     * @param filepath The path to the board file.
     * @param size     The dimensions of the board to test.
     *
     * @return {@code true} if the dimensions match.
     */
    public static boolean boardIsSize(final File filepath, final BoardDimensions size) {
        int boardX = 0;
        int boardY = 0;
        try (FileReader fr = new FileReader(filepath); BufferedReader br = new BufferedReader(fr)) {
            // read board, looking for "size"
            StreamTokenizer streamTokenizer = new StreamTokenizer(br);
            streamTokenizer.eolIsSignificant(true);
            streamTokenizer.commentChar('#');
            streamTokenizer.quoteChar('"');
            streamTokenizer.wordChars('_', '_');
            while (streamTokenizer.nextToken() != StreamTokenizer.TT_EOF) {
                if ((streamTokenizer.ttype == StreamTokenizer.TT_WORD)
                      && streamTokenizer.sval.equalsIgnoreCase("size")) {
                    streamTokenizer.nextToken();
                    boardX = (int) streamTokenizer.nval;
                    streamTokenizer.nextToken();
                    boardY = (int) streamTokenizer.nval;
                    break;
                }
            }
        } catch (IOException ex) {
            return false;
        }

        // check and return
        return (boardX == size.width()) && (boardY == size.height());
    }

    /**
     * Inspect specified board file and return its dimensions.
     *
     * @param filepath The path to the board file.
     *
     * @return A {@link BoardDimensions} object containing the dimension.
     */
    public static BoardDimensions getSize(final File filepath) {
        int boardX = 0;
        int boardY = 0;
        try (FileReader fileReader = new FileReader(filepath);
              BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            // read board, looking for "size"
            StreamTokenizer streamTokenizer = new StreamTokenizer(bufferedReader);
            streamTokenizer.eolIsSignificant(true);
            streamTokenizer.commentChar('#');
            streamTokenizer.quoteChar('"');
            streamTokenizer.wordChars('_', '_');
            while (streamTokenizer.nextToken() != StreamTokenizer.TT_EOF) {
                if ((streamTokenizer.ttype == StreamTokenizer.TT_WORD)
                      && streamTokenizer.sval.equalsIgnoreCase("size")) {
                    streamTokenizer.nextToken();
                    boardX = (int) streamTokenizer.nval;
                    streamTokenizer.nextToken();
                    boardY = (int) streamTokenizer.nval;
                    break;
                }
            }
        } catch (IOException ex) {
            return null;
        }
        return new BoardDimensions(boardX, boardY);
    }

    /** Inspects the given board file and returns a set of its tags. */
    public static Set<String> getTags(final File filepath) {
        var result = new HashSet<String>();
        try (FileReader fr = new FileReader(filepath); BufferedReader br = new BufferedReader(fr)) {
            // read board, looking for "size"
            StreamTokenizer st = new StreamTokenizer(br);
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
                } else if ((st.ttype == StreamTokenizer.TT_WORD) && st.sval.equalsIgnoreCase("hex")) {
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

        try (InputStream is = new FileInputStream(new MegaMekFile(Configuration.boardsDir(), board).getFile())) {
            tempBoard.load(is, null, false);
        } catch (IOException ex) {
            return false;
        }

        return tempBoard.isValid();
    }

    /**
     * Can the given player deploy at these coordinates?
     */
    public boolean isLegalDeployment(Coords c, Player p) {
        return isLegalDeployment(c, p.getStartingPos(), p.getStartWidth(), p.getStartOffset(), p.getStartingAnyNWx(),
              p.getStartingAnyNWy(), p.getStartingAnySEx(), p.getStartingAnySEy());
    }

    /**
     * Can the given entity be deployed at these coordinates
     */
    public boolean isLegalDeployment(Coords c, Entity e) {
        if (e == null) {
            return false;
        }

        return isLegalDeployment(c, e.getStartingPos(), e.getStartingWidth(), e.getStartingOffset(),
              e.getStartingAnyNWx(), e.getStartingAnyNWy(), e.getStartingAnySEx(), e.getStartingAnySEy());
    }

    /**
     * Can an object be deployed at these coordinates, given a starting zone, width of starting zone and offset from
     * edge of board?
     */
    public boolean isLegalDeployment(Coords c, int zoneType, int startingWidth, int startingOffset, int startingAnyNWx,
          int startingAnyNWy, int startingAnySEx, int startingAnySEy) {
        if ((c == null) || !contains(c)) {
            return false;
        }

        int maxX = width - startingOffset;
        int maxy = height - startingOffset;

        return switch (zoneType) {
            case START_ANY -> (((startingAnyNWx == Entity.STARTING_ANY_NONE) || (c.getX() >= startingAnyNWx))
                  && ((startingAnySEx == Entity.STARTING_ANY_NONE) || (c.getX() <= startingAnySEx))
                  && ((startingAnyNWy == Entity.STARTING_ANY_NONE) || (c.getY() >= startingAnyNWy))
                  && ((startingAnySEy == Entity.STARTING_ANY_NONE) || (c.getY() <= startingAnySEy)));
            case START_NW -> ((c.getX() < (startingOffset + startingWidth)) && (c.getX() >= startingOffset) && (c.getY()
                  >= startingOffset)
                  && (c.getY() < (height / 2)))
                  || ((c.getY() < (startingOffset + startingWidth)) && (c.getY() >= startingOffset) && (c.getX()
                  >= startingOffset)
                  && (c.getX() < (width / 2)));
            case START_N -> (c.getY() < (startingOffset + startingWidth)) && (c.getY() >= startingOffset);
            case START_NE -> ((c.getX() >= (maxX - startingWidth)) && (c.getX() < maxX) && (c.getY() >= startingOffset)
                  && (c.getY() < (height / 2)))
                  || ((c.getY() < (startingOffset + startingWidth)) && (c.getY() >= startingOffset) && (c.getX()
                  < maxX)
                  && (c.getX() > (width / 2)));
            case START_E -> (c.getX() >= (maxX - startingWidth)) && (c.getX() < maxX);
            case START_SE -> ((c.getX() >= (maxX - startingWidth)) && (c.getX() < maxX) && (c.getY() < maxy)
                  && (c.getY() > (height / 2)))
                  || ((c.getY() >= (maxy - startingWidth)) && (c.getY() < maxy) && (c.getX() < maxX)
                  && (c.getX() > (width / 2)));
            case START_S -> (c.getY() >= (maxy - startingWidth)) && (c.getY() < maxy);
            case START_SW -> ((c.getX() < (startingOffset + startingWidth)) && (c.getX() >= startingOffset) && (c.getY()
                  < maxy)
                  && (c.getY() > (height / 2)))
                  || ((c.getY() >= (maxy - startingWidth)) && (c.getY() < maxy) && (c.getX() >= startingOffset)
                  && (c.getX() < (width / 2)));
            case START_W -> (c.getX() < (startingOffset + startingWidth)) && (c.getX() >= startingOffset);
            case START_EDGE ->
                  ((c.getX() < (startingOffset + startingWidth)) && (c.getX() >= startingOffset) && (c.getY()
                        >= startingOffset) && (c.getY() < maxy))
                        || ((c.getY() < (startingOffset + startingWidth)) && (c.getY() >= startingOffset) && (c.getX()
                        >= startingOffset)
                        && (c.getX() < maxX))
                        || ((c.getX() >= (maxX - startingWidth)) && (c.getX() < maxX) && (c.getY() >= startingOffset)
                        && (c.getY() < maxy))
                        || ((c.getY() >= (maxy - startingWidth)) && (c.getY() < maxy) && (c.getX() >= startingOffset)
                        && (c.getX() < maxX));
            case START_CENTER ->
                  (c.getX() >= (width / 3)) && (c.getX() <= ((2 * width) / 3)) && (c.getY() >= (height / 3))
                        && (c.getY() <= ((2 * height) / 3));
            default -> {
                Set<Coords> customDeploymentZone = getCustomDeploymentZone(decodeCustomDeploymentZoneID(zoneType));
                yield customDeploymentZone.contains(c);
            }
        };
    }

    /**
     * Determine the opposite edge from the given edge Returns START_NONE for non-cardinal edges (North, South, West,
     * East)
     *
     * @param cardinalEdge The edge to return the opposite of
     *
     * @return Constant representing the opposite edge
     */
    public int getOppositeEdge(int cardinalEdge) {
        return switch (cardinalEdge) {
            case Board.START_E -> Board.START_W;
            case Board.START_N -> Board.START_S;
            case Board.START_W -> Board.START_E;
            case Board.START_S -> Board.START_N;
            default -> Board.START_NONE;
        };
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
            logger.error("IO Error opening file to load board! {}", String.valueOf(ex));
        }
    }

    /**
     * Loads this board from an InputStream
     */
    public void load(InputStream is) {
        load(is, null, false);
    }

    public void load(String boardString, @Nullable List<String> errors) {
        try (InputStream is = new ByteArrayInputStream(boardString.getBytes(StandardCharsets.UTF_8))) {
            load(is, errors, false);
        } catch (IOException ex) {
            logger.error(ex, "Error loading string to build board - {}", ex.getMessage());
            throw new IllegalArgumentException("Error loading string to build board - " + ex.getMessage());
        }
    }

    public void load(InputStream is, @Nullable List<String> errors, boolean continueLoadOnError) {
        int nw = 0, nh = 0, di = 0;
        Hex[] nd = new Hex[0];
        int index = 0;
        resetStoredElevation();
        try (InputStreamReader isr = new InputStreamReader(is);
              BufferedReader br = new BufferedReader(isr)) {
            StreamTokenizer st = new StreamTokenizer(br);
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
                        int x, y, coordsWidth = 100;
                        int coords = (int) st.nval;
                        if (coords > 9999) {
                            coordsWidth = 1000;
                        }
                        y = coords % coordsWidth;
                        coords /= coordsWidth;
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
            logger.error("Load - I/O Error: {}", String.valueOf(ex));
        }

        // fill nulls with blank hexes
        for (int i = 0; i < nd.length; i++) {
            if (nd[i] == null) {
                nd[i] = new Hex();
            }
        }

        // check data integrity
        if (isValid(nd, nw, nh, errors) && ((nw > 0) || (nh > 0) || (di == (nw * nh)))) {
            newData(nw, nh, nd, errors);
        } else if (continueLoadOnError && ((nw > 0) || (nh > 0) || (di == (nw * nh)))) {
            logger.error("continueLoadOnError - Invalid board data!");
            newData(nw, nh, nd, errors);
        } else if (errors == null) {
            logger.error("errors - Invalid board data!");
        }
    }

    public boolean isValid() {
        // Search for black-listed hexes
        return isValid(data, width, height, null);
    }

    public boolean isValid(@Nullable List<String> errors) {
        // Search for black-listed hexes
        return isValid(data, width, height, errors);
    }

    private boolean isValid(Hex[] data, int width, int height, @Nullable List<String> errors) {
        List<String> newErrors = new ArrayList<>();
        // Search for black-listed hexes
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Hex hex = data[(y * width) + x];
                if (hex == null) {
                    if (errors != null) {
                        errors.add("Null hex for coordinates " + x + "; " + y + ". Breaking off validity check.");
                    }
                    // A null hex must never happen. No need to process the rest of the board.
                    return false;
                }
                List<String> hexErrors = new ArrayList<>();
                hex.isValid(hexErrors);

                // Multi-hex problems
                // A building hex must only have exits to other building hexes of the same
                // Building Type and Class
                if (hex.containsTerrain(Terrains.BUILDING) && hex.getTerrain(Terrains.BUILDING).hasExitsSpecified()) {
                    for (int dir = 0; dir < 6; dir++) {
                        Hex adjHex = getHexInDir(x, y, dir);
                        if ((adjHex != null)
                              && adjHex.containsTerrain(Terrains.BUILDING)
                              && hex.containsTerrainExit(Terrains.BUILDING, dir)) {
                            if (adjHex.getTerrain(Terrains.BUILDING).getLevel() != hex.getTerrain(Terrains.BUILDING)
                                  .getLevel()) {
                                hexErrors.add("Building has an exit to a building of another Building Type " +
                                      "(Light, Medium...).");
                            }
                            int thisClass = hex.containsTerrain(Terrains.BLDG_CLASS)
                                  ? hex.getTerrain(Terrains.BLDG_CLASS).getLevel()
                                  : 0;
                            int adjClass = adjHex.containsTerrain(Terrains.BLDG_CLASS)
                                  ? adjHex.getTerrain(Terrains.BLDG_CLASS).getLevel()
                                  : 0;
                            if (thisClass != adjClass) {
                                hexErrors.add("Building has an exit in direction " + dir + " to a building of " +
                                      "another Building Class.");
                            }
                        }
                    }
                }

                if (!hexErrors.isEmpty() && (errors == null)) {
                    // Return early if we aren't logging errors
                    return false;
                } else if (!hexErrors.isEmpty()) {
                    // Prepend a line that gives the hex coordinate for all found errors
                    newErrors.add("Errors in hex " + new Coords(x, y).getBoardNum() + ":");
                    newErrors.addAll(hexErrors);
                    hexErrors.clear();
                }
            }
        }

        if (errors != null) {
            errors.addAll(newErrors);
        }

        return newErrors.isEmpty();
    }

    /**
     * Writes data for the board, as text to the OutputStream
     */
    public void save(OutputStream os) {
        try (Writer w = new OutputStreamWriter(os)) {
            w.write("size " + width + ' ' + height + "\r\n");
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
                for (int terrType : terrainTypes) {
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
            }
            w.write("end\r\n");
            // make sure it's written
            w.flush();
        } catch (IOException ex) {
            logger.error("I/O Error: {}", String.valueOf(ex));
        }
    }

    /**
     * Record that the given coordinates have received a hit from an inferno.
     *
     * @param coords the <code>Coords</code> of the hit.
     * @param round  the kind of round that hit the hex.
     * @param hits   the <code>int</code> number of rounds that hit
     *
     * @throws IllegalArgumentException if the hits number is negative
     */
    public void addInfernoTo(Coords coords, Inferno round, int hits) {
        // Make sure the # of hits is valid.
        if (hits < 0) {
            throw new IllegalArgumentException("Board can't track negative hits. ");
        }

        // Do nothing if the coords aren't on this board.
        if (!this.contains(coords)) {
            return;
        }

        // Do we already have a tracker for those coords?
        InfernoTracker tracker = infernos.computeIfAbsent(coords, k -> new InfernoTracker());
        // Nope. Make one.

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

    public void removeBombIconsFrom(Coords coords) {
        // Do nothing if the coords aren't on this board.
        if (!this.contains(coords) || null == specialHexes.get(coords)) {
            return;
        }

        // Use iterator so we can remove while traversing
        specialHexes.get(coords).removeIf(shd -> Set.of(BOMB_HIT, BOMB_MISS, BOMB_DRIFT).contains(shd.getType()));
    }

    public void clearBombIcons() {
        for (Coords coords : specialHexes.keySet()) {
            removeBombIconsFrom(coords);
        }
    }

    /**
     * Determine if the given coordinates has a burning inferno.
     *
     * @param coords - the <code>Coords</code> being checked.
     *
     * @return <code>true</code> if those coordinates have a burning inferno
     *       round. <code>false</code> if no inferno has hit those coordinates or if it has burned out.
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
    public Enumeration<IBuilding> getBuildings() {
        return buildings.elements();
    }

    /**
     * @return the Vector of all the board's buildings
     */
    public Vector<IBuilding> getBuildingsVector() {
        return buildings;
    }

    /**
     * Get the building at the given coordinates.
     *
     * @param coords the <code>Coords</code> being examined.
     *
     * @return a <code>Building</code> object, if there is one at the given coordinates, otherwise a
     *       <code>null</code> will be returned.
     */
    public @Nullable IBuilding getBuildingAt(Coords coords) {
        return bldgByCoords.get(coords);
    }

    /**
     * Get the local object for the given building. Call this routine any time the input <code>Building</code> is
     * suspect.
     *
     * @param other - a <code>Building</code> object which may or may not be represented on this board. This value may
     *              be <code>null</code> .
     *
     * @return The local <code>Building</code> object if we can find a match. If the other building is not on this
     *       board, a <code>null</code> is returned instead.
     */
    private IBuilding getLocalBuilding(IBuilding other) {
        return buildings.stream().filter(building -> building.equals(other)).findFirst().orElse(null);
    }

    /**
     * Collapse a vector of building hexes.
     *
     * @param coords the <code>Vector</code> of {@link Coords} objects to be collapsed.
     */
    public void collapseBuilding(Vector<Coords> coords) {
        // Walk through the vector of coords.
        Enumeration<Coords> loop = coords.elements();
        while (loop.hasMoreElements()) {
            final Coords other = loop.nextElement();
            collapseBuilding(other);
        }
    }

    /**
     * The given building hex has collapsed. Remove it from the board and replace it with rubble.
     *
     * @param coords the <code>Building</code> that has collapsed.
     */
    public void collapseBuilding(Coords coords) {
        final Hex curHex = getHex(coords);

        // Remove the building from the building map.
        IBuilding bldg = bldgByCoords.get(coords);
        if (bldg == null) {
            logger.error("No building found at {}", coords);
            return;
        }
        bldg.removeHex(coords);
        bldgByCoords.remove(coords);

        // determine type of rubble
        // Terrain type can be a max of 4 for hardened building
        // 5 for walls, but the only place where we actually check
        // for rubble type is resolveFindClub in Server, and we
        // make it impossible to find clubs in wall rubble there
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
            int rubbleLevel = bldg.getBuildingType().getTypeValue();
            curHex.addTerrain(new Terrain(Terrains.RUBBLE, rubbleLevel));
        }

        if (curHex.containsTerrain(Terrains.BLDG_BASEMENT_TYPE)) {
            // per TW 176 the basement doesn't change the elevation of the
            // building hex
            // the basement fills in with the rubble of the building
            // any units in the basement are destroyed
            curHex.removeTerrain(Terrains.BLDG_BASEMENT_TYPE);
        }

        // Update the hex.
        // TODO : Do I need to initialize it???
        // ASSUMPTION: It's faster to update one at a time.
        setHex(coords, curHex);
    }

    /**
     * The given building has collapsed. Remove it from the board and replace it with rubble.
     *
     * @param bldg the <code>Building</code> that has collapsed.
     */
    public void collapseBuilding(IBuilding bldg) {

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
     * Update a locally stored building with CF and other values from a building received from the server.
     *
     * @param receivedBuilding The Building received from the server
     */
    public void updateBuilding(IBuilding receivedBuilding) {
        IBuilding localBuilding = getLocalBuilding(receivedBuilding);

        if ((receivedBuilding.getBoardId() != boardId) || (localBuilding == null)) {
            logger.error("Could not find a match for {} to update.", receivedBuilding);
            return;
        }
        for (Coords coords : localBuilding.getCoordsList()) {
            localBuilding.setCurrentCF(receivedBuilding.getCurrentCF(coords), coords);
            localBuilding.setPhaseCF(receivedBuilding.getPhaseCF(coords), coords);
            localBuilding.setArmor(receivedBuilding.getArmor(coords), coords);
            localBuilding.setBasement(coords,
                  BasementType.getType(getHex(coords).terrainLevel(Terrains.BLDG_BASEMENT_TYPE)));
            localBuilding.setBasementCollapsed(coords, receivedBuilding.getBasementCollapsed(coords));
            localBuilding.setDemolitionCharges(receivedBuilding.getDemolitionCharges());
        }
    }

    /**
     * Get the current value of the "road auto-exit" option.
     *
     * @return <code>true</code> if roads should automatically exit onto all
     *       adjacent pavement hexes.
     *       <code>false</code> otherwise.
     */
    public boolean getRoadsAutoExit() {
        return roadsAutoExit;
    }

    /**
     * Set the value of the "road auto-exit" option.
     *
     * @param value The value to set for the option; <code>true</code> if roads should automatically exit onto all
     *              adjacent pavement hexes. <code>false</code> otherwise.
     */
    public void setRoadsAutoExit(boolean value) {
        roadsAutoExit = value;
    }

    /**
     * Populate the <code>bldgByCoords</code> member from the current
     * <code>Vector</code> of buildings. Use this method after deserializing a
     * <code>Board</code> object.
     */
    private void createBldgByCoords() {
        // Make a new hashtable.
        bldgByCoords = new Hashtable<>();

        // Walk through the vector of buildings.
        Enumeration<IBuilding> loop = buildings.elements();
        while (loop.hasMoreElements()) {
            final IBuilding bldg = loop.nextElement();

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
     */
    @Serial
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

    /**
     * Fires a board event which typically leads to the {@link megamek.client.ui.clientGUI.boardview.BoardView} and
     * minimap being redrawn. This is public as the boards and minimaps show some data that is not part of the Board
     * class and Board has no way of knowing when a change happens. An example of this is arty auto hexes which are
     * stored in the player class
     */
    public void processBoardEvent(BoardEvent event) {
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
        for (IBuilding bldg : buildings) {
            for (Enumeration<Coords> coords = bldg.getCoords(); coords.hasMoreElements(); ) {
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
        for (IBuilding b : buildings) {
            for (Enumeration<Coords> coords = b.getCoords(); coords.hasMoreElements(); ) {
                Coords c = coords.nextElement();
                if (b.getBasement(c).isUnknown()) {
                    b.setBasement(c, BasementType.NONE);
                }
            }
        }
    }

    /**
     * @return Special events that should be marked on hexes, such as artillery fire as well as notes players can leave
     *       manually on hexes. Always returns at least an empty list, never null.
     */
    public Collection<SpecialHexDisplay> getSpecialHexDisplay(Coords coords) {
        return specialHexes.getOrDefault(coords, Collections.emptyList());
    }

    /**
     * Adds the given SHD at the given coords to this board. An event can be fired for this board change (this should
     * not be done on the Server).
     *
     * @param coords    The position of the SHD on this board
     * @param shd       The SpecialHexDisplay to add
     * @param fireEvent When true, a BoardEvent is fired for the affected coords
     */
    public void addSpecialHexDisplay(Coords coords, SpecialHexDisplay shd, boolean fireEvent) {
        Collection<SpecialHexDisplay> col;
        if (!specialHexes.containsKey(coords)) {
            col = new LinkedList<>();
            specialHexes.put(coords, col);
        } else {
            col = specialHexes.get(coords);
            // It's possible we are updating a SHD that is already entered.
            // If that is the case, we want to remove the original entry.
            // FIXME: An updated shd will likely not get removed because of SpecialHexDisplay.equals. Why not use a Set?
            col.remove(shd);
        }

        col.add(shd);

        if (fireEvent) {
            processBoardEvent(new BoardEvent(this, coords, BoardEvent.BOARD_CHANGED_HEX));
        }
    }

    /**
     * Adds the given SHD at the given coords to this board. This method does not fire an event for the change.
     *
     * @param coords The position of the SHD on this board
     * @param shd    The SpecialHexDisplay to add
     */
    public void addSpecialHexDisplay(Coords coords, SpecialHexDisplay shd) {
        addSpecialHexDisplay(coords, shd, false);
    }

    /**
     * Removes the given SHD from the given coords.
     *
     * @param coords The position of the SHD on this board
     * @param shd    The SpecialHexDisplay to remove
     */
    public void removeSpecialHexDisplay(Coords coords, SpecialHexDisplay shd) {
        removeSpecialHexDisplay(coords, shd, false);
    }

    /**
     * Removes the given SHD from the given coords.
     *
     * @param coords The position of the SHD on this board
     * @param shd    The SpecialHexDisplay to remove
     */
    public void removeSpecialHexDisplay(Coords coords, SpecialHexDisplay shd, boolean fireEvent) {
        Collection<SpecialHexDisplay> col = specialHexes.get(coords);
        if (col != null) {
            col.remove(shd);
        }
        if (fireEvent) {
            processBoardEvent(new BoardEvent(this, coords, BoardEvent.BOARD_CHANGED_HEX));
        }
    }

    public Map<Coords, Collection<SpecialHexDisplay>> getSpecialHexDisplayTable() {
        return specialHexes;
    }

    /**
     * Sets this board's specialHexes to a new set. This method should be used by the client when receiving an update
     * from the server.
     *
     * @param shd The new map of SpecialHexDisplays
     */
    public void setSpecialHexDisplayTable(Map<Coords, Collection<SpecialHexDisplay>> shd) {
        // save the former SHDs to redraw their hexes if they've vanished
        Set<Coords> toRedraw = new HashSet<>(specialHexes.keySet());
        specialHexes = shd;
        toRedraw.addAll(shd.keySet());
        toRedraw.forEach(coords ->
              processBoardEvent(new BoardEvent(this, coords, BoardEvent.BOARD_CHANGED_HEX)));
        //TODO: Add a BoardEvent for a set of coords to avoid many events
    }

    public void setType(int t) {
        setBoardType(switch (t) {
            case MapSettings.MEDIUM_ATMOSPHERE -> BoardType.SKY_WITH_TERRAIN;
            case MapSettings.MEDIUM_SPACE -> BoardType.FAR_SPACE;
            default -> BoardType.GROUND;
        });
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
        if (isSpace() || isSky()) {
            return false;
        }
        for (Coords c : bldgByCoords.keySet()) {
            Hex hex = getHex(c);
            if (hex.containsTerrain(Terrains.BRIDGE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the description of the map.
     *
     * @return The description of the map, if one exists, otherwise null.
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the map.
     *
     * @param s The description of the map; may be null.
     */
    public void setDescription(@Nullable String s) {
        description = s;
    }

    /**
     * Gets every annotation on the map.
     *
     * @return A read-only map of per-hex annotations.
     */
    public Map<Coords, Collection<String>> getAnnotations() {
        return Collections.unmodifiableMap(annotations);
    }

    /**
     * Gets the annotations associated with a hex.
     *
     * @param c Coordinates of the hex.
     *
     * @return A collection of annotations for the hex.
     */
    public Collection<String> getAnnotations(Coords c) {
        return annotations.getOrDefault(c, Collections.emptyList());
    }

    /**
     * Sets annotations on a given hex.
     *
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
     * Sets a tileset theme for all hexes of the board. Passing null as newTheme resets the theme to the theme specified
     * in the board file.
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
     * @return true when the given {@link Coords} coords is on the edge of the board.
     */
    public boolean isOnBoardEdge(Coords coords) {
        return (coords.getX() == 0) || (coords.getY() == 0) || (coords.getX() == (width - 1)) || (coords.getY() == (
              height - 1));
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

    /** @return The name of this map; this is meant to be displayed in the GUI. */
    public String getBoardName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    /**
     * @return Given an "exits" value, returns it in a list form. (i.e. exits value of 4 returns {3}, exit value of 5
     *       returns {1, 3}
     */
    public static List<Integer> exitsAsIntList(int exits) {
        List<Integer> results = new ArrayList<>();
        int exitIndex = 1;

        for (long bitToCheck = 1; bitToCheck <= Integer.MAX_VALUE; bitToCheck *= 2, exitIndex++) {
            if ((exits & bitToCheck) != 0) {
                results.add(exitIndex);
            }
        }

        return results;
    }

    /**
     * Given a list of integers, returns them in a bit-packed form.
     */
    public static int IntListAsExits(List<Integer> list) {
        int result = 0;

        for (int listItem : list) {
            result |= 1 << (listItem - 1);
        }

        return result;
    }

    /**
     * Worker function that initializes any custom deployment zones present on the board
     */
    private void initializeDeploymentZones() {
        deploymentZones = new HashMap<>();

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                Hex currentHex = getHex(x, y);
                Terrain deploymentZone = currentHex.getTerrain(Terrains.DEPLOYMENT_ZONE);

                if (deploymentZone != null) {
                    for (int zoneID : Board.exitsAsIntList(deploymentZone.getExits())) {
                        deploymentZones.computeIfAbsent(zoneID, k -> new HashSet<>()).add(new Coords(x, y));
                    }
                }
            }
        }
        areas.forEach(this::convertDeploymentZone);
    }

    /**
     * Converts a custom deployment zone from the hex area definition to board hexes; also translates the ID. Note that
     * the deploymentZones field must not be null.
     */
    private void convertDeploymentZone(int zoneId, HexArea hexArea) {
        deploymentZones.put(zoneId - NUM_ZONES_X2, hexArea.getCoords(this));
    }

    /**
     * Adds a deployment zone with the given ID and the hexes described by the given HexArea to this board, replacing
     * the previously present zone of that ID, if there had been one. Note that the zone ID can be outside those
     * reachable by board files; e.g. the zone ID can be 1000. Note however that zone IDs in the range of 0 to 50 should
     * be avoided as they'll overwrite terrain deployment zones.
     *
     * @param zoneId  The zone Id
     * @param hexArea The hexes comprising this deployment zone
     */
    public void addDeploymentZone(int zoneId, HexArea hexArea) {
        areas.put(zoneId, hexArea);
    }

    /**
     * Resets the "intermediate" deployment zones associated with this board, in case the deployment zones change
     */
    public void resetDeploymentZones() {
        deploymentZones = null;
    }

    /**
     * Gets the IDs of all custom deployment zones defined for this board.
     */
    public Set<Integer> getCustomDeploymentZones() {
        if (deploymentZones == null) {
            initializeDeploymentZones();
        }

        return deploymentZones.keySet();
    }

    /**
     * Gets all the coordinates in the given custom deployment zone
     */
    public Set<Coords> getCustomDeploymentZone(int zoneID) {
        if (deploymentZones == null) {
            initializeDeploymentZones();
        }

        return deploymentZones.getOrDefault(zoneID, Set.of());
    }

    /**
     * Use this method to convert a deployment zone ID as represented in the UI zone selectors (e.g. in the
     * PlayerSettingsDialog) to a deployment zone ID as stored in the board.
     */
    public static int decodeCustomDeploymentZoneID(int zoneID) {
        return zoneID - NUM_ZONES_X2;
    }

    /**
     * Use this method to convert a deployment zone ID as stored in the board to a number suitable for representation in
     * the UI zone selectors (e.g. PlayerSettingsDialog)
     */
    public static int encodeCustomDeploymentZoneID(int zoneID) {
        return zoneID + NUM_ZONES_X2;
    }

    /**
     * Sets the board's ID. Within an MM game, the ID must be unique. To preserve "normal" games, 0 is the default.
     */
    public void setBoardId(int boardId) {
        this.boardId = boardId;
        // must update buildings that have already been created.
        for (IBuilding building : buildings) {
            building.setBoardId(boardId);
        }
    }

    public int getBoardId() {
        return boardId;
    }

    public void setEnclosingBoard(int enclosingBoardId) {
        enclosingBoard = enclosingBoardId;
    }

    /** @return The ID of the enclosing board of this board, or -1 if it has no enclosing board. */
    public int getEnclosingBoardId() {
        return enclosingBoard;
    }

    /**
     * Sets the given board ID as an embedded board at the given coords of this board. The board ID is not checked nor
     * the board type. The coords are checked against the size of this board.
     *
     * @param boardId The board ID to embed
     * @param coords  The location to place the given board
     *
     * @throws IllegalArgumentException When this board does not contain the given coords
     */
    public void setEmbeddedBoard(int boardId, Coords coords) {
        if (contains(coords)) {
            embeddedBoards.put(coords, boardId);
        } else {
            throw new IllegalArgumentException("Board does not contain the given coords.");
        }
    }

    public Set<Coords> embeddedBoardCoords() {
        return embeddedBoards.keySet();
    }

    public @Nullable Coords embeddedBoardPosition(int boardId) {
        for (Map.Entry<Coords, Integer> entry : embeddedBoards.entrySet()) {
            if (entry.getValue() == boardId) {
                return entry.getKey();
            }
        }
        return null;
    }

    public int getEmbeddedBoardAt(Coords coords) {
        return embeddedBoards.getOrDefault(coords, -1);
    }

    public Set<Coords> getEmbeddedBoardHexes() {
        return embeddedBoards.keySet();
    }

    public boolean isGround() {
        return boardType.isGround();
    }

    /**
     * @return True if this board is a low altitude (a.k.a. atmospheric) board, either with terrain or without terrain
     *       ("sky").
     */
    public boolean isLowAltitude() {
        return boardType.isLowAltitude();
    }

    /**
     * @return True if this board is a low altitude (a.k.a. atmospheric) board without terrain ("sky").
     */
    public boolean isSky() {
        return boardType.isSky();
    }

    /**
     * @return True if this board is a space board, either close to a planet with some atmospheric hexes ("high
     *       altitude") or in deeper space.
     */
    public boolean isSpace() {
        return boardType.isSpace();
    }

    /**
     * @return True if this board is a high altitude board, i.e. a space board close to a planet with some atmospheric
     *       hexes.
     */
    public boolean isHighAltitude() {
        return boardType.isHighAltitude();
    }

    public void setBoardType(BoardType boardType) {
        this.boardType = boardType;
    }

    public BoardType getBoardType() {
        return boardType;
    }

    @Override
    public String toString() {
        return "[Board-%s] (%s) %dx%d".formatted(boardType, mapName, width, height);
    }

    /**
     * Add a building and all of its coordinates to the board. {@link BuildingTerrain} should be added when
     * initializing, this method is public so {@link AbstractBuildingEntity} can register buildings when deploying buildings.
     * @param bldg {@link IBuilding} to add to the board
     */
    public void addBuildingToBoard(IBuilding bldg) {
        buildings.addElement(bldg);

        // Each building will identify the hexes it covers.
        Enumeration<Coords> iter = bldg.getCoords();
        while (iter.hasMoreElements()) {
            bldgByCoords.put(iter.nextElement(), bldg);
        }
    }
}
