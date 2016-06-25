/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import megamek.common.Building.BasementType;
import megamek.common.event.BoardEvent;
import megamek.common.event.BoardListener;

public class Board implements Serializable, IBoard {
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

    protected int width;
    protected int height;

    // MapType
    public static final int T_GROUND = 0;
    public static final int T_ATMOSPHERE = 1;
    public static final int T_SPACE = 2;

    private static final String[] typeNames = { "Ground", "Low Atmosphere",
            "Space" };

    // Min and Max elevation values for when they are undefined (since you cant
    // set an int to null).
    private static final int UNDEFINED_MIN_ELEV = 10000;
    private static final int UNDEFINED_MAX_ELEV = -10000;

    // The min and max elevation values for this board.
    // set when getMinElevation/getMax is called for the first time.
    private int minElevation = UNDEFINED_MIN_ELEV;
    private int maxElevation = UNDEFINED_MAX_ELEV;

    private int mapType = T_GROUND;

    private IHex[] data;

    /**
     * The path to the file to load as background image for this board. To avoid
     * the Server sending a serialized image, the image isn't loaded until
     * requested.
     */
    private List<String> backgroundPaths = new ArrayList<>();

    /**
     * Keeps track of how many boards were combined to create this board.  These
     * are necessary to properly index into the background image, and only need
     * to be set if backgroundPaths are present.
     */
    private int numBoardsWidth, numBoardsHeight;

    /**
     * Keeps track of the size of the boards used to create this board.  These
     * are necessary to properly index into the background image, and only need
     * to be set if backgroundPaths are present.
     */
    private int subBoardWidth, subBoardHeight;

    /**
     * Flags that determine if the background image should be flipped.  These
     * are necessary to properly index into the background image, and only need
     * to be set if backgroundPaths are present.
     */
    private List<Boolean> flipBGHoriz = new ArrayList<>(),
            flipBGVert = new ArrayList<>();

    /**
     * Building data structures.
     */
    private Vector<Building> buildings = new Vector<Building>();
    private transient Hashtable<Coords, Building> bldgByCoords = new Hashtable<Coords, Building>();

    protected transient Vector<BoardListener> boardListeners = new Vector<BoardListener>();

    /**
     * Record the infernos placed on the board.
     */
    private Hashtable<Coords, InfernoTracker> infernos = new Hashtable<Coords, InfernoTracker>();

    private Hashtable<Coords, Collection<SpecialHexDisplay>> specialHexes = new Hashtable<Coords, Collection<SpecialHexDisplay>>();

    /**
     * Option to turn have roads auto-exiting to pavement.
     */
    private boolean roadsAutoExit = true;

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
        data = new IHex[width * height];
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
    public Board(int width, int height, IHex[] data) {
        this.width = width;
        this.height = height;
        this.data = new IHex[width * height];
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
    public Board(int width, int height, IHex[] hexes, Vector<Building> bldgs,
            Hashtable<Coords, InfernoTracker> infMap) {
        this.width = width;
        this.height = height;
        data = hexes;
        buildings = bldgs;
        infernos = infMap;
        createBldgByCoords();
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IBoard#getHeight()
     */
    public int getHeight() {
        return height;
    }

    @Override
    public Coords getCenter() {
        return new Coords(getWidth() / 2, getHeight() / 2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IBoard#getWidth()
     */
    public int getWidth() {
        return width;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IBoard#newData(int, int, megamek.common.IHex[])
     */
    public void newData(int width, int height, IHex[] data) {
        this.width = width;
        this.height = height;
        this.data = data;

        initializeAll();
        processBoardEvent(new BoardEvent(this, null, BoardEvent.BOARD_NEW_BOARD));
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IBoard#newData(int, int)
     */
    public void newData(int width, int height) {
        newData(width, height, new IHex[width * height]);
    }

    public Enumeration<Coords> getHexesAtDistance(Coords coords, int distance) {
        // Initialize the one necessary variable.
        Vector<Coords> retVal = new Vector<Coords>();

        // Handle boundary conditions.
        if (distance < 0) {
            return retVal.elements();
        }
        if (distance == 0) {
            retVal.add(coords);
            return retVal.elements();
        }

        // Okay, handle the "real" case.
        // This is a bit of a cludge. Is there a better way to do this?
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (coords.distance(x, y) == distance) {
                    retVal.add(new Coords(x, y));
                    // retVal.add(getHex(x, y));
                }
            }
        }
        return retVal.elements();
    }

    /**
     * Determines if this Board contains the (x, y) Coords, and if so, returns
     * the Hex at that position.
     *
     * @param x
     *            the x Coords.
     * @param y
     *            the y Coords.
     * @return the Hex, if this Board contains the (x, y) location; null
     *         otherwise.
     */
    public IHex getHex(int x, int y) {
        if (contains(x, y)) {
            return data[(y * width) + x];
        }
        return null;
    }

    /**
     * Gets the hex in the specified direction from the specified starting
     * coordinates.
     */
    public IHex getHexInDir(Coords c, int dir) {
        return getHexInDir(c.getX(), c.getY(), dir);
    }

    /**
     * Gets the hex in the specified direction from the specified starting
     * coordinates. Avoids calls to Coords.translated, and thus, object
     * construction.
     */
    public IHex getHexInDir(int x, int y, int dir) {
        return getHex(Coords.xInDir(x, y, dir), Coords.yInDir(x, y, dir));
    }

    /**
     * Initialize all hexes
     */
    protected void initializeAll() {
        // Initialize all buildings.
        buildings.removeAllElements();
        if (bldgByCoords == null) {
            bldgByCoords = new Hashtable<Coords, Building>();
        } else {
            bldgByCoords.clear();
        }
        // Walk through the hexes, creating buildings.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Does this hex contain a building?
                IHex curHex = getHex(x, y);
                if ((curHex != null)
                        && (curHex.containsTerrain(Terrains.BUILDING))) {
                    // Yup, but is it a repeat?
                    Coords coords = new Coords(x, y);
                    if (!bldgByCoords.containsKey(coords)) {

                        // Nope. Try to create an object for the new building.
                        try {
                            Building bldg = new Building(
                                    coords,
                                    this,
                                    Terrains.BUILDING,
                                    BasementType.getType(curHex
                                            .terrainLevel(Terrains.BLDG_BASEMENT_TYPE)));
                            buildings.addElement(bldg);

                            // Each building will identify the hexes it covers.
                            Enumeration<Coords> iter = bldg.getCoords();
                            while (iter.hasMoreElements()) {
                                bldgByCoords.put(iter.nextElement(), bldg);
                            }
                        } catch (IllegalArgumentException excep) {
                            // Log the error and remove the
                            // building from the board.
                            System.err.println("Unable to create building.");
                            excep.printStackTrace();
                            curHex.removeTerrain(Terrains.BUILDING);
                        }
                    } // End building-is-new
                } // End hex-has-building
                if ((curHex != null)
                        && (curHex.containsTerrain(Terrains.FUEL_TANK))) {
                    // Yup, but is it a repeat?
                    Coords coords = new Coords(x, y);
                    if (!bldgByCoords.containsKey(coords)) {

                        // Nope. Try to create an object for the new building.
                        try {
                            int magnitude = curHex.getTerrain(
                                    Terrains.FUEL_TANK_MAGN).getLevel();
                            FuelTank bldg = new FuelTank(coords, this,
                                    Terrains.FUEL_TANK, magnitude);
                            buildings.addElement(bldg);

                            // Each building will identify the hexes it covers.
                            Enumeration<Coords> iter = bldg.getCoords();
                            while (iter.hasMoreElements()) {
                                bldgByCoords.put(iter.nextElement(), bldg);
                            }
                        } catch (IllegalArgumentException excep) {
                            // Log the error and remove the
                            // building from the board.
                            System.err.println("Unable to create building.");
                            excep.printStackTrace();
                            curHex.removeTerrain(Terrains.BUILDING);
                        }
                    } // End building-is-new
                } // End hex-has-building
                if ((curHex != null) && curHex.containsTerrain(Terrains.BRIDGE)) {

                    // Yup, but is it a repeat?
                    Coords coords = new Coords(x, y);
                    if (!bldgByCoords.containsKey(coords)) {

                        // Nope. Try to create an object for the new building.
                        try {
                            Building bldg = new Building(coords, this,
                                    Terrains.BRIDGE, BasementType.NONE);
                            buildings.addElement(bldg);

                            // Each building will identify the hexes it covers.
                            Enumeration<Coords> iter = bldg.getCoords();
                            while (iter.hasMoreElements()) {
                                bldgByCoords.put(iter.nextElement(), bldg);
                            }
                        } catch (IllegalArgumentException excep) {
                            // Log the error and remove the
                            // building from the board.
                            System.err.println("Unable to create bridge.");
                            excep.printStackTrace();
                            curHex.removeTerrain(Terrains.BRIDGE);
                        }

                    } // End bridge-is-new

                } // End hex-has-bridge
            }
        }
        // Initialize all exits.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                initializeHex(x, y, false);
            }
        }
        processBoardEvent(new BoardEvent(this, null,
                BoardEvent.BOARD_CHANGED_ALL_HEXES));
        // good time to ensure hex cache
        IdealHex.ensureCacheSize(width + 1, height + 1);

    } // End private void initializeAll()

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
    private void initializeHex(int x, int y) {
        initializeHex(x, y, true);
    }

    private void initializeHex(int x, int y, boolean event) {
        IHex hex = getHex(x, y);

        if (hex == null) {
            return;
        }

        hex.clearExits();
        for (int i = 0; i < 6; i++) {
            IHex other = getHexInDir(x, y, i);
            hex.setExits(other, i, roadsAutoExit);
        }
        if (event) {
            processBoardEvent(new BoardEvent(this, new Coords(x, y),
                    BoardEvent.BOARD_CHANGED_HEX));
        }
    }

    /**
     * Determines whether this Board "contains" the specified Coords.
     *
     * @param x
     *            the x Coords.
     * @param y
     *            the y Coords.
     */
    public boolean contains(int x, int y) {
        return (x >= 0) && (y >= 0) && (x < width) && (y < height);
    }

    /**
     * Determines whether this Board "contains" the specified Coords.
     *
     * @param c
     *            the Coords.
     */
    public boolean contains(Coords c) {
        if (c == null) {
            return false;
        }
        return contains(c.getX(), c.getY());
    }

    /**
     * Returns the Hex at the specified Coords.
     *
     * @param c
     *            the Coords.
     */
    public IHex getHex(Coords c) {
        if (c == null) {
            return null;
        }
        return getHex(c.getX(), c.getY());
    }

    /**
     * Determines if this Board contains the (x, y) Coords, and if so, sets the
     * specified Hex into that position and initializes it.
     *
     * @param x
     *            the x Coords.
     * @param y
     *            the y Coords.
     * @param hex
     *            the hex to be set into position.
     */
    public void setHex(int x, int y, IHex hex) {
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
     * For each coord/hex pair in the supplied collections, this method
     * determines if the Board contains the coords and if so updates the
     * specified hex into that position and intializes it.
     * <p/>
     * The method ensures that each hex that needs to be updated is only updated
     * once.
     *
     * @param coords
     *            A list of coordinates to be updated
     * @param hexes
     *            The hex to be updated for each coordinate
     */
    public void setHexes(List<Coords> coords, List<IHex> hexes) {
        // Keeps track of hexes that will need to be reinitialized
        LinkedHashSet<Coords> needsUpdate = new LinkedHashSet<Coords>(
                (int) (coords.size() * 1.25 + 0.5));

        // Sanity check
        if (coords.size() != hexes.size()) {
            throw new IllegalStateException(
                    "setHexes received two collections differeing size!");
        }

        // Update all input hexes, plus create a set of coords that need
        // updating
        Iterator<Coords> coordIter = coords.iterator();
        Iterator<IHex> hexIter = hexes.iterator();
        while (coordIter.hasNext() && hexIter.hasNext()) {
            Coords currCoord = coordIter.next();
            IHex currHex = hexIter.next();
            int x = currCoord.getX();
            int y = currCoord.getY();
            data[(y * width) + x] = currHex;
            initializeHex(x, y);

            // Add any adjacent hexes that may need to have exits updated
            if (currHex.hasExitableTerrain()) {
                for (int dir = 0; dir < 6; dir++) {
                    if (currHex.containsExit(dir)) {
                        needsUpdate.add(new Coords(Coords.xInDir(x, y, dir),
                                Coords.yInDir(x, y, dir)));
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
     * @param c
     *            the Coords.
     * @param hex
     *            the hex to be set into position.
     */
    public void setHex(Coords c, IHex hex) {
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
    public static boolean boardIsSize(final File filepath,
            final BoardDimensions size) {
        int boardx = 0;
        int boardy = 0;
        try {
            // make inpustream for board
            Reader r = new BufferedReader(new FileReader(filepath));
            // read board, looking for "size"
            StreamTokenizer st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('#');
            st.quoteChar('"');
            st.wordChars('_', '_');
            while (st.nextToken() != StreamTokenizer.TT_EOF) {
                if ((st.ttype == StreamTokenizer.TT_WORD)
                        && st.sval.equalsIgnoreCase("size")) {
                    st.nextToken();
                    boardx = (int) st.nval;
                    st.nextToken();
                    boardy = (int) st.nval;
                    break;
                }
            }
            r.close();
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
        try {
            // make inpustream for board
            Reader r = new BufferedReader(new FileReader(filepath));
            // read board, looking for "size"
            StreamTokenizer st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('#');
            st.quoteChar('"');
            st.wordChars('_', '_');
            while (st.nextToken() != StreamTokenizer.TT_EOF) {
                if ((st.ttype == StreamTokenizer.TT_WORD)
                        && st.sval.equalsIgnoreCase("size")) {
                    st.nextToken();
                    boardx = (int) st.nval;
                    st.nextToken();
                    boardy = (int) st.nval;
                    break;
                }
            }
            r.close();
        } catch (IOException ex) {
            return null;
        }
        return new BoardDimensions(boardx, boardy);
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
            if ((c.getX() < minx) || (c.getY() < miny) || (c.getX() >= maxx)
                    || (c.getY() >= maxy)) {
                return false;
            }
        }
        switch (nDir) {
            case START_ANY:
                return true;
            case START_NW:
                return ((c.getX() < (minx + nLimit)) && (c.getX() >= minx) && (c
                        .getY() < (height / 2)))
                        || ((c.getY() < (miny + nLimit)) && (c.getY() >= miny) && (c
                                .getX() < (width / 2)));
            case START_N:
                return (c.getY() < (miny + nLimit)) && (c.getY() >= miny);
            case START_NE:
                return ((c.getX() > (maxx - nLimit)) && (c.getX() < maxx) && (c
                        .getY() < (height / 2)))
                        || ((c.getY() < (miny + nLimit)) && (c.getY() >= miny) && (c
                                .getX() > (width / 2)));
            case START_E:
                return (c.getX() >= (maxx - nLimit)) && (c.getX() < maxx);
            case START_SE:
                return ((c.getX() >= (maxx - nLimit)) && (c.getX() < maxx) && (c
                        .getY() > (height / 2)))
                        || ((c.getY() >= (maxy - nLimit)) && (c.getY() < maxy) && (c
                                .getX() > (width / 2)));
            case START_S:
                return (c.getY() >= (maxy - nLimit)) && (c.getY() < maxy);
            case START_SW:
                return ((c.getX() < (minx + nLimit)) && (c.getX() >= minx) && (c
                        .getY() > (height / 2)))
                        || ((c.getY() >= (maxy - nLimit)) && (c.getY() < maxy) && (c
                                .getX() < (width / 2)));
            case START_W:
                return (c.getX() < (minx + nLimit)) && (c.getX() >= minx);
            case START_EDGE:
                return ((c.getX() < (minx + nLimit)) && (c.getX() >= minx))
                        || ((c.getY() < (miny + nLimit)) && (c.getY() >= miny))
                        || ((c.getX() >= (maxx - nLimit)) && (c.getX() < maxx))
                        || ((c.getY() >= (maxy - nLimit)) && (c.getY() < maxy));
            case START_CENTER:
                return (c.getX() >= (width / 3))
                        && (c.getX() <= ((2 * width) / 3))
                        && (c.getY() >= (height / 3))
                        && (c.getY() <= ((2 * height) / 3));
            default: // ummm. .
                return false;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public void load(final String filename) {
        load(new File(Configuration.boardsDir(), filename));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load(final File filepath) {
        // load a board
        try {
            java.io.InputStream is = new FileInputStream(filepath);
            // tell the board to load!
            load(is);
            // okay, done!
            is.close();
        } catch (java.io.IOException ex) {
            System.err.println("error opening file to load board!");
            System.err.println(ex);
        }
    }

    /**
     * Loads this board from an InputStream
     */
    public void load(InputStream is) {
        int nw = 0, nh = 0, di = 0;
        IHex[] nd = new IHex[0];
        resetStoredElevation();
        try {
            Reader r = new BufferedReader(new InputStreamReader(is));
            StreamTokenizer st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('#');
            st.quoteChar('"');
            st.wordChars('_', '_');
            int x_pos = 1;
            int y_pos = 1;
            while (st.nextToken() != StreamTokenizer.TT_EOF) {
                if ((st.ttype == StreamTokenizer.TT_WORD)
                        && st.sval.equalsIgnoreCase("size")) {
                    // read rest of line
                    String[] args = { "0", "0" };
                    int i = 0;
                    while ((st.nextToken() == StreamTokenizer.TT_WORD)
                            || (st.ttype == '"')
                            || (st.ttype == StreamTokenizer.TT_NUMBER)) {
                        args[i++] = st.ttype == StreamTokenizer.TT_NUMBER ? (int) st.nval
                                + ""
                                : st.sval;
                    }
                    nw = Integer.parseInt(args[0]);
                    nh = Integer.parseInt(args[1]);
                    nd = new IHex[nw * nh];
                    di = 0;
                } else if ((st.ttype == StreamTokenizer.TT_WORD)
                        && st.sval.equalsIgnoreCase("option")) {
                    // read rest of line
                    String[] args = { "", "" };
                    int i = 0;
                    while ((st.nextToken() == StreamTokenizer.TT_WORD)
                            || (st.ttype == '"')
                            || (st.ttype == StreamTokenizer.TT_NUMBER)) {
                        args[i++] = st.ttype == StreamTokenizer.TT_NUMBER ? (int) st.nval
                                + ""
                                : st.sval;
                    }
                    // Only expect certain options.
                    if (args[0].equalsIgnoreCase("exit_roads_to_pavement")) {
                        if (args[1].equalsIgnoreCase("false")) {
                            roadsAutoExit = false;
                        } else {
                            roadsAutoExit = true;
                        }
                    } // End exit_roads_to_pavement-option
                } else if ((st.ttype == StreamTokenizer.TT_WORD)
                        && st.sval.equalsIgnoreCase("hex")) {
                    // read rest of line
                    String[] args = { "", "0", "", "" };
                    int i = 0;
                    while ((st.nextToken() == StreamTokenizer.TT_WORD)
                            || (st.ttype == '"')
                            || (st.ttype == StreamTokenizer.TT_NUMBER)) {
                        args[i++] = st.ttype == StreamTokenizer.TT_NUMBER ? (int) st.nval
                                + ""
                                : st.sval;
                    }
                    int elevation = Integer.parseInt(args[1]);
                    int newIndex = indexFor(args[0], nw, y_pos);
                    nd[newIndex] = new Hex(elevation, args[2], args[3],
                            new Coords(x_pos - 1, y_pos - 1));
                    x_pos++;
                    if (x_pos > nw) {
                        y_pos++;
                        x_pos = 1;
                    }
                } else if ((st.ttype == StreamTokenizer.TT_WORD)
                        && st.sval.equalsIgnoreCase("background")) {
                    st.nextToken();
                    File bgFile = new File(Configuration.boardBackgroundsDir(),
                            st.sval);
                    if (bgFile.exists()) {
                        backgroundPaths.add(bgFile.getPath());
                    } else {
                        System.err.println("Board specified background image, "
                                + "but path couldn't be found! Path: "
                                + bgFile.getPath());
                    }
                } else if ((st.ttype == StreamTokenizer.TT_WORD)
                        && st.sval.equalsIgnoreCase("end")) {
                    break;
                }
            }
        } catch (IOException ex) {
            System.err.println("i/o error reading board");
            System.err.println(ex);
        }

        // fill nulls with blank hexes
        for (int i = 0; i < nd.length; i++) {
            if (nd[i] == null) {
                nd[i] = new Hex();
            }
        }

        // check data integrity
        if ((nw > 1) || (nh > 1) || (di == (nw * nh))) {
            newData(nw, nh, nd);
        } else {
            System.err.println("board data invalid");
        }

    }

    private int indexFor(String hexNum, int width, int row) {
        int substringDiff = 2;
        if (row > 99) {
            substringDiff = Integer.toString(width).length();
        }
        int x = Integer.parseInt(hexNum.substring(0, hexNum.length()
                - substringDiff)) - 1;
        int y = Integer.parseInt(hexNum.substring(hexNum.length()
                - substringDiff)) - 1;
        return (y * width) + x;
    }

    /**
     * Writes data for the board, as text to the OutputStream
     */
    public void save(OutputStream os) {
        try {
            Writer w = new OutputStreamWriter(os);
            // write
            w.write("size " + width + " " + height + "\r\n");
            if (!roadsAutoExit) {
                w.write("option exit_roads_to_pavement false\r\n");
            }
            for (int i = 0; i < data.length; i++) {
                IHex hex = data[i];
                boolean firstTerrain = true;

                StringBuffer hexBuff = new StringBuffer("hex ");
                hexBuff.append(new Coords(i % width, i / width).getBoardNum());
                hexBuff.append(" ");
                hexBuff.append(hex.getLevel());
                hexBuff.append(" \"");
                int terrainTypes[] = hex.getTerrainTypes();
                for (int j = 0; j < terrainTypes.length; j++) {
                    int terrType = terrainTypes[j];
                    ITerrain terrain = hex.getTerrain(terrType);
                    if (terrain != null) {
                        if (!firstTerrain) {
                            hexBuff.append(";");
                        }
                        hexBuff.append(terrain.toString());
                        // Do something funky to save building exits.
                        if (((Terrains.BUILDING == terrType) || (terrType == Terrains.FUEL_TANK))
                                && !terrain.hasExitsSpecified()
                                && (terrain.getExits() != 0)) {
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
            System.err.println("i/o error writing board");
            System.err.println(ex);
        }
    }

    /**
     * Writes data for the board, as serialization, to the OutputStream
     */
    public void save2(OutputStream os) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(this);
            oos.flush();
        } catch (IOException ex) {
            System.err.println("i/o error writing board");
            System.err.println(ex);
        }
    }

    /**
     * Record that the given coordinates have recieved a hit from an inferno.
     *
     * @param coords
     *            - the <code>Coords</code> of the hit.
     * @param round
     *            - the kind of round that hit the hex.
     * @param hits
     *            - the <code>int</code> number of rounds that hit. If a
     *            negative number is passed, then an
     *            <code>IllegalArgumentException</code> will be thrown.
     */
    public void addInfernoTo(Coords coords, InfernoTracker.Inferno round,
            int hits) {
        // Declare local variables.
        InfernoTracker tracker = null;

        // Make sure the # of hits is valid.
        if (hits < 0) {
            throw new IllegalArgumentException(
                    "Board can't track negative hits. ");
        }

        // Do nothing if the coords aren't on this board.
        if (!this.contains(coords)) {
            return;
        }

        // Do we already have a tracker for those coords?
        tracker = infernos.get(coords);
        if (null == tracker) {
            // Nope. Make one.
            tracker = new InfernoTracker();
            infernos.put(coords, tracker);
        }

        // Update the tracker.
        tracker.add(round, hits);

    }

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
        InfernoTracker tracker = null;

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
     * Record that a new round of burning has passed for the given coordinates.
     * This routine also determines if the fire is still burning.
     *
     * @param coords
     *            - the <code>Coords</code> being checked.
     * @return <code>true</code> if those coordinates have a burning inferno
     *         round. <code>false</code> if no inferno has hit those coordinates
     *         or if it has burned out.
     */
    public boolean burnInferno(Coords coords) {
        boolean result = false;
        InfernoTracker tracker = null;

        // Get the tracker for those coordinates, record the round
        // of burning and see if the fire is still burning.
        tracker = infernos.get(coords);
        if (null != tracker) {
            tracker.newRound(-1);
            if (tracker.isStillBurning()) {
                result = true;
            } else {
                infernos.remove(coords);
            }
        }

        return result;
    }

    /**
     * Get an enumeration of all coordinates with infernos still burning.
     *
     * @return an <code>Enumeration</code> of <code>Coords</code> that have
     *         infernos still burning.
     */
    public Enumeration<Coords> getInfernoBurningCoords() {
        // Only include *burning* inferno trackers.
        Vector<Coords> burning = new Vector<Coords>();
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
     * Determine the remaining number of turns the given coordinates will have a
     * burning inferno.
     *
     * @param coords
     *            - the <code>Coords</code> being checked. This value must not
     *            be <code>null</code>. Unchecked.
     * @return the <code>int</code> number of burn turns left for all infernos
     *         This value will be non-negative.
     */
    public int getInfernoBurnTurns(Coords coords) {
        int turns = 0;
        InfernoTracker tracker = null;

        // Get the tracker for those coordinates
        // and see if the fire is still burning.
        tracker = infernos.get(coords);
        if (null != tracker) {
            turns = tracker.getTurnsLeftToBurn();
        }
        return turns;
    }

    /**
     * Determine the remaining number of turns the given coordinates will have a
     * burning Inferno IV round.
     *
     * @param coords
     *            - the <code>Coords</code> being checked. This value must not
     *            be <code>null</code>. Unchecked.
     * @return the <code>int</code> number of burn turns left for Arrow IV
     *         infernos. This value will be non-negative.
     */
    public int getInfernoIVBurnTurns(Coords coords) {
        int turns = 0;
        InfernoTracker tracker = null;

        // Get the tracker for those coordinates
        // and see if the fire is still burning.
        tracker = infernos.get(coords);
        if (null != tracker) {
            turns = tracker.getArrowIVTurnsLeftToBurn();
        }
        return turns;
    }

    /**
     * Get an enumeration of all buildings on the board.
     *
     * @return an <code>Enumeration</code> of <code>Building</code>s.
     */
    public Enumeration<Building> getBuildings() {
        return buildings.elements();
    }

    /**
     * Get the building at the given coordinates.
     *
     * @param coords
     *            - the <code>Coords</code> being examined.
     * @return a <code>Building</code> object, if there is one at the given
     *         coordinates, otherwise a <code>null</code> will be returned.
     */
    public Building getBuildingAt(Coords coords) {
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
     * @param coords
     *            - the <code>Vector</code> of <code>Coord</code> objects to be
     *            collapsed.
     */
    public void collapseBuilding(Vector<Coords> coords) {

        // Walk through the vector of coords.
        Enumeration<Coords> loop = coords.elements();
        while (loop.hasMoreElements()) {
            final Coords other = loop.nextElement();

            // Update the building.
            this.collapseBuilding(other);

        } // Handle the next building.

    }

    /**
     * The given building hex has collapsed. Remove it from the board and
     * replace it with rubble.
     *
     * @param coords
     *            - the <code>Building</code> that has collapsed.
     */
    public void collapseBuilding(Coords coords) {
        final IHex curHex = this.getHex(coords);

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
        curHex.removeTerrain(Terrains.BRIDGE);
        curHex.removeTerrain(Terrains.BRIDGE_CF);
        curHex.removeTerrain(Terrains.BRIDGE_ELEV);

        // Add rubble terrain that matches the building type.
        if (type > 0) {
            int rubbleLevel = bldg.getBldgClass() == Building.FORTRESS ? 2 : 1;
            curHex.addTerrain(Terrains.getTerrainFactory().createTerrain(
                    Terrains.RUBBLE, rubbleLevel));
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
     * The given building has collapsed. Remove it from the board and replace it
     * with rubble.
     *
     * @param bldg
     *            - the <code>Building</code> that has collapsed.
     */
    public void collapseBuilding(Building bldg) {

        // Remove the building from our building vector.
        buildings.removeElement(bldg);

        // Walk through the building's hexes.
        Enumeration<Coords> bldgCoords = bldg.getCoords();
        while (bldgCoords.hasMoreElements()) {
            final Coords coords = bldgCoords.nextElement();
            collapseBuilding(coords);
        } // Handle the next building hex.

    } // End public void collapseBuilding( Building )

    /**
     * Update the construction factors on an array of buildings.
     *
     * @param bldgs
     *            - the <code>Vector</code> of <code>Building</code> objects to
     *            be updated.
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
                System.err.print("Could not find a match for ");
                System.err.print(other);
                System.err.println(" to update.");
                continue;
            }
            Enumeration<Coords> coordsEnum = bldg.getCoords();
            while (coordsEnum.hasMoreElements()) {
                // Set the current and phase CFs of the building hexes.
                final Coords coords = coordsEnum.nextElement();
                bldg.setCurrentCF(other.getCurrentCF(coords), coords);
                bldg.setPhaseCF(other.getPhaseCF(coords), coords);
                bldg.setArmor(other.getArmor(coords), coords);
                bldg.setBasement(
                        coords,
                        BasementType.getType(getHex(coords).terrainLevel(
                                Terrains.BLDG_BASEMENT_TYPE)));
                bldg.setBasementCollapsed(coords,
                        other.getBasementCollapsed(coords));
            }
        } // Handle the next building.

    }

    /**
     * Get the current value of the "road auto-exit" option.
     *
     * @return <code>true</code> if roads should automatically exit onto all
     *         adjacent pavement hexes. <code>false</code> otherwise.
     */
    public boolean getRoadsAutoExit() {
        return roadsAutoExit;
    }

    /**
     * Set the value of the "road auto-exit" option.
     *
     * @param value
     *            - The value to set for the option; <code>true</code> if roads
     *            should automatically exit onto all adjacent pavement hexes.
     *            <code>false</code> otherwise.
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
        bldgByCoords = new Hashtable<Coords, Building>();

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
     * @param in
     *            - the <code>ObjectInputStream</code> to read.
     * @throws <code>IOException</code>
     * @throws <code>ClassNotFoundException</code>
     */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();

        // Restore bldgByCoords from buildings.
        createBldgByCoords();
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IBoard#addBoardListener(megamek.common.BoardListener)
     */
    public void addBoardListener(BoardListener listener) {
        if (boardListeners == null) {
            boardListeners = new Vector<BoardListener>();
        }
        boardListeners.addElement(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.IBoard#removeBoardListener(megamek.common.BoardListener)
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
            boardListeners = new Vector<BoardListener>();
        }
        return boardListeners;
    }

    // Is there any way I can get around using this?
    public Hashtable<Coords, InfernoTracker> getInfernos() {
        return infernos;
    }

    public void setBridgeCF(int value) {
        for (Building bldg : buildings) {
            for (Enumeration<Coords> coords = bldg.getCoords(); coords
                    .hasMoreElements();) {
                Coords c = coords.nextElement();
                IHex h = getHex(c);
                if (h.containsTerrain(Terrains.BRIDGE)) {
                    bldg.setCurrentCF(value, c);
                }
            }
        }
    }

    // Kill all the unknown basements
    public void setRandomBasementsOff() {
        Coords c = null;
        for (Building b : buildings) {
            for (Enumeration<Coords> coords = b.getCoords(); coords
                    .hasMoreElements();) {
                c = coords.nextElement();
                if (b.getBasement(c) == BasementType.UNKNOWN) {
                    b.setBasement(c, BasementType.NONE);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IBoard#getSpecialHexDisplay(megamek.common.Coords)
     */
    public Collection<SpecialHexDisplay> getSpecialHexDisplay(Coords coords) {
        return specialHexes.get(coords);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IBoard#addSpecialHexDisplay(megamek.common.Coords,
     * megamek.common.SpecialHexDisplay)
     */
    public void addSpecialHexDisplay(Coords coords, SpecialHexDisplay shd) {
        Collection<SpecialHexDisplay> col;
        if (!specialHexes.containsKey(coords)) {
            col = new LinkedList<SpecialHexDisplay>();
            specialHexes.put(coords, col);
        } else {
            col = specialHexes.get(coords);
            // It's possible we are updating a SHD that is already entered.
            // If that is the case, we want to remove the original entry.
            if (col.contains(shd)) {
                col.remove(shd);
            }
        }

        col.add(shd);
    }

    public void removeSpecialHexDisplay(Coords coords, SpecialHexDisplay shd) {
        Collection<SpecialHexDisplay> col = specialHexes.get(coords);
        if (col != null) {
            col.remove(shd);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IBoard#getSpecialHexDisplayTable()
     */
    public Hashtable<Coords, Collection<SpecialHexDisplay>> getSpecialHexDisplayTable() {
        return specialHexes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IBoard#setSpecialHexDisplayTable(java.util.Hashtable)
     */
    public void setSpecialHexDisplayTable(
            Hashtable<Coords, Collection<SpecialHexDisplay>> shd) {
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

    public void resetStoredElevation() {
        minElevation = UNDEFINED_MIN_ELEV;
        maxElevation = UNDEFINED_MAX_ELEV;
    }

    @Override
    public boolean containsBridges() {
        for (Coords c : bldgByCoords.keySet()) {
            IHex hex = getHex(c);
            if (hex.containsTerrain(Terrains.BRIDGE)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getBackgroundPaths() {
        return backgroundPaths;
    }

    public String getBackgroundPath() {
        if (backgroundPaths.size() > 0) {
            return backgroundPaths.get(0);
        } else {
            return null;
        }
    }

    public int getNumBoardsWidth() {
        return numBoardsWidth;
    }

    public int getNumBoardsHeight() {
        return numBoardsHeight;
    }

    public List<Boolean> getFlipBGHoriz() {
        return flipBGHoriz;
    }

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

    public void addBackgroundPath(String path, boolean flipVert,
            boolean flipHorz) {
        backgroundPaths.add(path);

        flipBGVert.add(flipVert);
        flipBGHoriz.add(flipHorz);
    }

    public boolean hasBoardBackground() {
        return (backgroundPaths != null) && backgroundPaths.size() > 0;
    }
}
