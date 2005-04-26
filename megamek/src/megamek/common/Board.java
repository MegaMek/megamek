/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

import java.util.*;
import java.io.*;
import com.sun.java.util.collections.HashMap;
import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Iterator;

public class Board
    implements Serializable {
    public static final int     BOARD_HEX_CLICK         = 1;
    public static final int     BOARD_HEX_DOUBLECLICK   = 2;
    public static final int     BOARD_HEX_DRAG          = 3;
    public static final String  BOARD_REQUEST_ROTATION  = "rotate:";

    public static final int     BOARD_MAX_WIDTH         = Coords.MAX_BOARD_WIDTH;
    public static final int     BOARD_MAX_HEIGHT        = Coords.MAX_BOARD_HEIGHT;

    public int                  width;
    public int                  height;
    
    public transient Coords     lastCursor;
    public transient Coords     highlighted;
    public transient Coords     selected;
    public transient Coords     firstLOS;
    
    private Hex[]               data;
    
    protected transient Vector boardListeners;

    /** Building data structures. */
    private Vector              buildings = new Vector();
    private transient Hashtable bldgByCoords = new Hashtable();

    /**
     * Record the infernos placed on the board.
     */
    private Hashtable           infernos = new Hashtable();

    /** Option to turn have roads auto-exiting to pavement. */
    private boolean             roadsAutoExit = true;

    /**
     * Creates a new board with zero as its width and
     * height parameters.
     */
    public Board() {
        this(0, 0);
    }
    
    /**
     * Creates a new board of the specified dimensions.
     * All hexes in the board will be null until otherwise
     * set.
     *
     * @param width        the width dimension.
     * @param height    the height dimension.
     */
    public Board(int width, int height) {
        this.width = width;
        this.height = height;
        data = new Hex[width * height];
        lastCursor = null;
        highlighted = null;
        selected = null;
        firstLOS = null;
        
        boardListeners = new Vector();
    }

    /**
     * Creates a new board of the specified dimensions, hexes, buildings,
     * and inferno trackers.  Do *not* use this method unless you have
     * carefully examined this class.
     *
     * @param width     The <code>int</code> width dimension in hexes.
     * @param height    The <code>int</code> height dimension in hexes.
     * @param hexes     The array of <code>Hex</code>es for this board.
     *                  This object is used directly without being copied.
     *                  This value should only be <code>null</code> if
     *                  either <code>width</code> or <code>height</code> is
     *                  zero.
     * @param bldgs     The <code>Vector</code> of <code>Building</code>s
     *                  for this board.  This object is used directly without
     *                  being copied.
     * @param infMap    The <code>Hashtable</code> that map <code>Coords</code>
     *                  to <code>InfernoTracker</code>s for this board.  This
     *                  object is used directly without being copied.
     */
    public Board( int width, int height, Hex[] hexes,
                  Vector bldgs, Hashtable infMap ) {
        this.width = width;
        this.height = height;
        data = hexes;
        buildings = bldgs;
        infernos = infMap;
        lastCursor = null;
        highlighted = null;
        selected = null;
        firstLOS = null;
        
        boardListeners = new Vector();

        createBldgByCoords();
    }

    /**
     * Creates a new data set for the board, with the
     * specified dimensions and data; notifies listeners
     * that a new data set has been created.
     *
     * @param width        the width dimension.
     * @param height    the height dimension.
     * @param data        new hex data appropriate for the board.
     */
    public void newData(int width, int height, Hex[] data) {
        this.width = width;
        this.height = height;
        this.data = data;
        lastCursor = null;
        highlighted = null;
        selected = null;
        
        initializeAll();
        // good time to ensure hex cache
        IdealHex.ensureCacheSize(width + 1, height + 1);
        processBoardEvent(new BoardEvent(this, new Coords(), null, BoardEvent.BOARD_NEW_BOARD, 0));
    }
    
    /**
     * Creates a new data set for the board, with the
     * specified dimensions; notifies listeners that a
     * new data set has been created.
     *
     * @param width        the width dimension.
     * @param height    the height dimension.
     */
    public void newData(int width, int height) {
        newData(width, height, new Hex[width * height]);
    }
    
    /**
     * Creates a new data set for the board that is a
     * duplicate of another board; notifies listeners
     * that a new data set has been created.
     * <p/>
     * Please note that changes to the other board's data can
     * affect this board.
     *
     * @param   board - the other <code>Board</code> to be duplicated.
     */
    public void newData( Board other ) {
        this.roadsAutoExit = other.roadsAutoExit;
        newData( other.width, other.height, other.data );
        this.buildings = other.buildings;
        this.bldgByCoords = other.bldgByCoords;
        this.infernos = other.infernos;
    }
    
    /**
     * Combines one or more boards into one huge megaboard!
     *
     * @param width the width of each individual board, before the combine
     * @param height the height of each individual board, before the combine
     * @param sheetWidth how many sheets wide the combined map is
     * @param sheetHeight how many sheets tall the combined map is
     * @param boards an array of the boards to be combined
     */
    public void combine(int width, int height, int sheetWidth, int sheetHeight, Board[] boards) {

        // Update the width and height of this megaboard.
        this.width = width * sheetWidth;
        this.height = height * sheetHeight;

        // Make space for the boards' data.
        this.data = new Hex[this.width * this.height];

        // Copy the data from the sub-boards.
        for (int i = 0; i < sheetHeight; i++) {
            for (int j = 0; j < sheetWidth; j++) {
                copyBoardInto(j * width, i * height, boards[i * sheetWidth + j]);
                // Copy in the other board's options.
                if ( boards[i * sheetWidth + j].roadsAutoExit == false ) {
                    this.roadsAutoExit = false;
                }
            }
        }

        // Initizlize the hexes and alert the board listeners.
        // Keep the data we just copied into this board.
        newData( this.width, this.height, this.data );
        
    }
    
    /**
     * Copies the data of another board into this one, offset by the specified
     * x and y.
     *
     * Currently just shallowly copies the boards.
     *
     */
    private void copyBoardInto(int x, int y, Board copied) {
        for (int i = 0; i < copied.height; i++) {
            for (int j = 0; j < copied.width; j++) {
                this.data[(i + y) * this.width + (j + x)] = copied.data[i * copied.width + j];
            }
        }
    }
    
    /**
     * Determines if this Board contains the (x, y) Coords,
     * and if so, returns the Hex at that position.
     *
     * @return the Hex, if this Board contains the (x, y)
     * location; null otherwise.
     *
     * @param x            the x Coords.
     * @param y            the y Coords.
     */
    public Hex getHex(int x, int y) {
        if(contains(x, y)) {
            return data[y * width + x];
        } else {
            return null;
        }
    }
    
    /**
     * Gets the hex in the specified direction from the specified starting
     * coordinates.
     */
    public Hex getHexInDir(Coords c, int dir) {
        return getHexInDir(c.x, c.y, dir);
    }
    
    /**
     * Gets the hex in the specified direction from the specified starting
     * coordinates.
     *
     * Avoids calls to Coords.translated, and thus, object construction.
     */
    public Hex getHexInDir(int x, int y, int dir) {
        return getHex(Coords.xInDir(x, y, dir), Coords.yInDir(x, y, dir));
    }
    
    /**
     * Initialize all hexes
     */
    private void initializeAll() {

        // Initialize all buildings.
        buildings.removeAllElements();
        if (bldgByCoords == null) {
            bldgByCoords = new Hashtable();
        }
        else {
            bldgByCoords.clear();
        }

        // Walk through the hexes, creating buildings.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Does this hex contain a building?
                Hex curHex = getHex( x, y );
                if ( curHex != null && curHex.contains( Terrain.BUILDING ) ) {

                    // Yup, but is it a repeat?
                    Coords coords = new Coords(x,y);
                    if ( !bldgByCoords.containsKey(coords) ) {

                        // Nope.  Try to create an object for the new building.
                        try {
                            Building bldg = new Building( coords, this );
                            buildings.addElement( bldg );

                            // Each building will identify the hexes it covers.
                            Enumeration iter = bldg.getCoords();
                            while ( iter.hasMoreElements() ) {
                                bldgByCoords.put( iter.nextElement(), bldg );
                            }
                        }
                        catch ( IllegalArgumentException excep ) {
                            // Log the error and remove the
                            // building from the board.
                            System.err.println( "Unable to create building." );
                            excep.printStackTrace();
                            curHex.removeTerrain( Terrain.BUILDING );
                        }

                    } // End building-is-new

                } // End hex-has-building                    
            }
        }

        // Initialize all exits.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                initializeHex(x, y);
            }
        }

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
     * Initializes a hex in its surroundings.  Currently sets the connects
     * parameter appropriately to the surrounding hexes.
     *
     * If a surrounding hex is off the board, it checks the hex opposite the
     * missing hex.
     */
    private void initializeHex(int x, int y) {
        Hex hex = getHex(x, y);
        
        if (hex == null) {
            return;
        }
        
        hex.clearExits();
        for (int i = 0; i < 6; i++) {
            Hex other = getHexInDir(x, y, i);
            hex.setExits(other, i, this.roadsAutoExit);
        }
    }
    
    /**
     * Determines whether this Board "contains" the specified
     * Coords.
     *
     * @param x the x Coords.
     * @param y the y Coords.
     */
    public boolean contains(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }
    
    /**
     * Determines whether this Board "contains" the specified
     * Coords.
     *
     * @param c the Coords.
     */
    public boolean contains(Coords c) {
        if ( c == null ) return false;
        return contains(c.x, c.y);
    }
    
    /**
     * Returns the Hex at the specified Coords.
     *
     * @param c the Coords.
     */
    public Hex getHex(Coords c) {
        return getHex(c.x, c.y);
    }
    
    /**
     * Determines if this Board contains the (x, y) Coords,
     * and if so, sets the specified Hex into that position
     * and initializes it.
     *
     * @param x the x Coords.
     * @param y the y Coords.
     * @param hex the hex to be set into position.
     */
    public void setHex(int x, int y, Hex hex) {
        data[y * width + x] = hex;
        initializeAround(x, y);
        processBoardEvent(new BoardEvent(this, new Coords(x, y), null, BoardEvent.BOARD_CHANGED_HEX, 0));
    }
    
    /**
     * Sets the hex into the location specified by the
     * Coords.
     *
     * @param c the Coords.
     * @param hex the hex to be set into position.
     */
    public void setHex(Coords c, Hex hex) {
        setHex(c.x, c.y, hex);
    }
    
    /**
     * Determines if this Board contains the Coords,
     * and if so, "selects" that Coords.
     *
     * @param c            the Coords.
     */
    public void select(Coords c) {
        if(c == null || contains(c)) {
            selected = c;
            processBoardEvent(new BoardEvent(this, c, null, BoardEvent.BOARD_HEX_SELECTED,0));
        }
    }
    
    /**
     * "Selects" the specified Coords.
     *
     * @param x            the x Coords.
     * @param y            the y Coords.
     */
    public void select(int x, int y) {
        select(new Coords(x, y));
    }
    
    /**
     * Determines if this Board contains the Coords,
     * and if so, highlights that Coords.
     *
     * @param c            the Coords.
     */
    public void highlight(Coords c) {
        if(c == null || contains(c)) {
            highlighted = c;
            processBoardEvent(new BoardEvent(this, c, null, BoardEvent.BOARD_HEX_HIGHLIGHTED, 0));
        }
    }
    
    /**
     * Highlights the specified Coords.
     *
     * @param x            the x Coords.
     * @param y            the y Coords.
     */
    public void highlight(int x, int y) {
        highlight(new Coords(x, y));
    }
    
    public void checkLOS(Coords c) {
        if(c == null || contains(c)) {
            if (firstLOS == null) {
                firstLOS = c;
                processBoardEvent(new BoardEvent(this, c, null, BoardEvent.BOARD_FIRST_LOS_HEX, 0));
            } else {
                processBoardEvent(new BoardEvent(this, c, null, BoardEvent.BOARD_SECOND_LOS_HEX, 0));
                firstLOS = null;
            }
        }
    }

    /**
     * Determines if this Board contains the Coords,
     * and if so, "cursors" that Coords.
     *
     * @param c            the Coords.
     */
    public void cursor(Coords c) {
        if(c == null || contains(c)) {
            if(lastCursor == null || c == null || !c.equals(lastCursor)) {
                lastCursor = c;
                processBoardEvent(new BoardEvent(this, c, null, BoardEvent.BOARD_HEX_CURSOR, 0));
            } else {
                lastCursor = c;
            }
        }
    }
    
    /**
     * "Cursors" the specified Coords.
     *
     * @param x            the x Coords.
     * @param y            the y Coords.
     */
    public void cursor(int x, int y) {
        cursor(new Coords(x, y));
    }
    
    /**
     * Determines if this Board contains the (x, y) Coords,
     * and if so, notifies listeners about the specified mouse
     * action.
     */
    public void mouseAction(int x, int y, int mtype, int modifiers) {
        if(contains(x, y)) {
            Coords c = new Coords(x, y);
            switch(mtype) {
            case BOARD_HEX_CLICK :
                if ((modifiers & java.awt.event.InputEvent.CTRL_MASK) != 0) {
                    checkLOS(c);
                } else {
                    processBoardEvent(new BoardEvent(this, c, null, BoardEvent.BOARD_HEX_CLICKED, modifiers));
                }
                break;
            case BOARD_HEX_DOUBLECLICK :
                processBoardEvent(new BoardEvent(this, c, null, BoardEvent.BOARD_HEX_DOUBLECLICKED, modifiers));
                break;
            case BOARD_HEX_DRAG :
                processBoardEvent(new BoardEvent(this, c, null, BoardEvent.BOARD_HEX_DRAGGED, modifiers));
                break;
            }
        }
    }
    
    /**
     * Notifies listeners about the specified mouse action.
     *
     * @param c            the Coords.
     */
    public void mouseAction(Coords c, int mtype, int modifiers) {
        mouseAction(c.x, c.y, mtype, modifiers);
    }
    
    /**
     * Adds the specified board listener to receive
     * board events from this board.
     *
     * @param l            the board listener.
     */
    public void addBoardListener(BoardListener l) {
        boardListeners.addElement(l);
    }
    
    /**
     * Removes the specified board listener.
     *
     * @param l            the board listener.
     */
    public void removeBoardListener(BoardListener l) {
        boardListeners.removeElement(l);
    }
    
    /**
     * Notifies attached board listeners of the event.
     *
     * @param be        the board event.
     */
    public void processBoardEvent(BoardEvent be) {
        if (boardListeners == null) {
            return;
        }
        for(Enumeration e = boardListeners.elements(); e.hasMoreElements();) {
            BoardListener l = (BoardListener)e.nextElement();
            switch(be.getType()) {
            case BoardEvent.BOARD_HEX_CLICKED :
            case BoardEvent.BOARD_HEX_DOUBLECLICKED :
            case BoardEvent.BOARD_HEX_DRAGGED :
                l.boardHexMoused(be);
                break;
            case BoardEvent.BOARD_HEX_CURSOR :
                l.boardHexCursor(be);
                break;
            case BoardEvent.BOARD_HEX_HIGHLIGHTED :
                l.boardHexHighlighted(be);
                break;
            case BoardEvent.BOARD_HEX_SELECTED :
                l.boardHexSelected(be);
                break;
            case BoardEvent.BOARD_CHANGED_HEX :
                l.boardChangedHex(be);
                break;
            case BoardEvent.BOARD_NEW_BOARD :
                l.boardNewBoard(be);
                break;
            case BoardEvent.BOARD_FIRST_LOS_HEX :
                l.boardFirstLOSHex(be);
                break;
            case BoardEvent.BOARD_SECOND_LOS_HEX :
                l.boardSecondLOSHex(be, firstLOS);
                break;
            case BoardEvent.BOARD_CHANGED_ENTITY :
                l.boardChangedEntity(be);
                break;
            case BoardEvent.BOARD_NEW_ATTACK :
                l.boardNewAttack(be);
            }
        }
    }
    
    /**
     * Checks if a file in data/boards is the specified size
     */
    public static boolean boardIsSize(String filename, int x, int y) {
        int boardx = 0;
        int boardy = 0;
        try {
            // make inpustream for board
            Reader r = new BufferedReader(new FileReader("data/boards" + File.separator + filename));
            // read board, looking for "size"
            StreamTokenizer st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('#');
            st.quoteChar('"');
            st.wordChars('_', '_');
            while(st.nextToken() != StreamTokenizer.TT_EOF) {
                if(st.ttype == StreamTokenizer.TT_WORD && st.sval.equalsIgnoreCase("size")) {
                    st.nextToken();
                    boardx = (int)st.nval;
                    st.nextToken();
                    boardy = (int)st.nval;
                    break;
                }
            }
            r.close();
        } catch (IOException ex) {
            return false;
        }
        
        // check and return
        return boardx == x && boardy == y;
    }
    
    /**
     * Can the player deploy an entity here?
     * There are no canon rules for the deployment phase (?!).  I'm using
     * 3 hexes from map edge.
     */
    public boolean isLegalDeployment(Coords c, Player p)
    {
        if (c == null || p == null || !contains(c)) return false;
        
        int nLimit = 3;
        int nDir = p.getStartingPos();
        switch (nDir) {
        case 0 : // Any
            return true;
        case 1 : // NW
            return (c.x < nLimit && c.y < height / 2) || 
                (c.y < nLimit && c.x < width / 2);
        case 2 : // N
            return c.y < nLimit;
        case 3 : // NE
            return (c.x > (width - nLimit) && c.y < height / 2) ||
                (c.y < nLimit && c.x > width / 2);
        case 4 : // E
            return c.x >= (width - nLimit);
        case 5 : // SE
            return (c.x >= (width - nLimit) && c.y > height / 2) ||
                (c.y >= (height - nLimit) && c.x > width / 2);
        case 6 : // S
            return c.y >= (height - nLimit);
        case 7 : // SW
            return (c.x < nLimit && c.y > height / 2) ||
                (c.y >= (height - nLimit) && c.x < width / 2);
        case 8 : // W
            return c.x < nLimit;
        default : // ummm. . 
            return false;
        }
        
    }
    
    /**
     * Flips the board around the vertical axis (North-for-South) and/or
     * the horizontal axis (East-for-West).  The dimensions of the board
     * will remain the same, but the terrain of the hexes will be swiched.
     *
     * @param   horiz - a <code>boolean</code> value that, if <code>true</code>,
     *          indicates that the board is being flipped North-for-South.
     * @param   vert - a <code>boolean</code> value that, if <code>true</code>,
     *          indicates that the board is being flipped East-for-West.
     */
    public void flip( boolean horiz, boolean vert ) {
        // If we're not flipping around *some* axis, do nothing.
        if ( !vert && !horiz ) {
            return;
        }

        // We only walk through half the board, but *which* half?
        int stopX;
        int stopY;
        if ( horiz ) {
            // West half of board.
            stopX = this.width / 2;
            stopY = this.height;
        } else {
            // North half of board.
            stopX = this.width;
            stopY = this.height / 2;
        }

        // Walk through the current data array and build a new one.
        int newX;
        int newY;
        int newIndex;
        int oldIndex;
        Hex tempHex;
        Terrain terr;
        for ( int oldX = 0; oldX < stopX; oldX++ ) {
            // Calculate the new X position of the flipped hex.
            if ( horiz ) {
                newX = this.width - oldX - 1;
            } else {
                newX = oldX;
            }
            for ( int oldY = 0; oldY < stopY; oldY++ ) {
                // Calculate the new Y position of the flipped hex.
                if ( vert ) {
                    newY = this.height - oldY - 1;
                } else {
                    newY = oldY;
                }

                // Swap the old hex for the new hex.
                newIndex = newX + newY * this.width;
                oldIndex = oldX + oldY * this.width;
                tempHex = this.data[ oldIndex ];
                this.data[ oldIndex ] = this.data[ newIndex ];
                this.data[ newIndex ] = tempHex;

                // Update the road exits in the swapped hexes.
                terr = this.data[ newIndex ].getTerrain( Terrain.ROAD );
                if ( null != terr ) {
                    terr.flipExits( horiz, vert );
                }
                terr = this.data[ oldIndex ].getTerrain( Terrain.ROAD );
                if ( null != terr ) {
                    terr.flipExits( horiz, vert );
                }

                // Update the building exits in the swapped hexes.
                terr = this.data[ newIndex ].getTerrain( Terrain.BUILDING );
                if ( null != terr ) {
                    terr.flipExits( horiz, vert );
                }
                terr = this.data[ oldIndex ].getTerrain( Terrain.BUILDING );
                if ( null != terr ) {
                    terr.flipExits( horiz, vert );
                }

                // Update the bridge exits in the swapped hexes.
                terr = this.data[ newIndex ].getTerrain( Terrain.BRIDGE );
                if ( null != terr ) {
                    terr.flipExits( horiz, vert );
                }
                terr = this.data[ oldIndex ].getTerrain( Terrain.BRIDGE );
                if ( null != terr ) {
                    terr.flipExits( horiz, vert );
                }

            } // Handle the next row

        } // Handle the next column

        // Make certain all board listeners know.
        processBoardEvent(new BoardEvent(this, new Coords(), null, BoardEvent.BOARD_NEW_BOARD, 0));

    } // End public void flip( boolean, boolean )

    /**
     * Loads this board from a filename in data/boards
     */
    public void load(String filename) {
        // load a board
        try {
            java.io.InputStream is = new java.io.FileInputStream(new java.io.File("data/boards", filename));
            // tell the board to load!
            load(is);
            // okay, done!
            is.close();
        } catch(java.io.IOException ex) {
            System.err.println("error opening file to load board!");
            System.err.println(ex);
        }
    }
    
    /**
     * Loads this board from an InputStream
     */
    public void load(InputStream is) {
        int nw = 0, nh = 0, di = 0;
        Hex[] nd = new Hex[0];
        
        try {
            Reader r = new BufferedReader(new InputStreamReader(is));
            StreamTokenizer st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('#');
            st.quoteChar('"');
            st.wordChars('_', '_');
            while(st.nextToken() != StreamTokenizer.TT_EOF) {
                if(st.ttype == StreamTokenizer.TT_WORD && st.sval.equalsIgnoreCase("size")) {
                    // read rest of line
                    String[] args = {"0", "0"};
                    int i = 0;
                    while(st.nextToken() == StreamTokenizer.TT_WORD || st.ttype == '"' || st.ttype == StreamTokenizer.TT_NUMBER) {
                        args[i++] = st.ttype == StreamTokenizer.TT_NUMBER ? (int)st.nval + "" : st.sval;
                    }
                    nw = Integer.parseInt(args[0]);
                    nh = Integer.parseInt(args[1]);
                    nd = new Hex[nw * nh];
                    di = 0;
                } else if(st.ttype == StreamTokenizer.TT_WORD && st.sval.equalsIgnoreCase("option")) {
                    // read rest of line
                    String[] args = {"", ""};
                    int i = 0;
                    while(st.nextToken() == StreamTokenizer.TT_WORD || st.ttype == '"' || st.ttype == StreamTokenizer.TT_NUMBER) {
                        args[i++] = st.ttype == StreamTokenizer.TT_NUMBER ? (int)st.nval + "" : st.sval;
                    }
                    // Only expect certain options.
                    if ( args[0].equalsIgnoreCase("exit_roads_to_pavement") ) {
                        if ( args[1].equalsIgnoreCase("false") ) {
                            this.roadsAutoExit = false;
                        } else {
                            this.roadsAutoExit = true;
                        }
                    } // End exit_roads_to_pavement-option
                } else if(st.ttype == StreamTokenizer.TT_WORD && st.sval.equalsIgnoreCase("hex")) {
                    // read rest of line
                    String[] args = {"", "0", "", ""};
                    int i = 0;
                    while(st.nextToken() == StreamTokenizer.TT_WORD || st.ttype == '"' || st.ttype == StreamTokenizer.TT_NUMBER) {
                        args[i++] = st.ttype == StreamTokenizer.TT_NUMBER ? (int)st.nval + "" : st.sval;
                    }
                    int elevation = Integer.parseInt(args[1]);
                    nd[indexFor(args[0], nw)] = new Hex(elevation, args[2], args[3]);
                    
                } else if(st.ttype == StreamTokenizer.TT_WORD && st.sval.equalsIgnoreCase("end")) {
                    break;
                }
            }
        } catch(IOException ex) {
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
        if(nw > 1 || nh > 1 || di == nw * nh) {
            newData(nw, nh, nd);
        } else {
            System.err.println("board data invalid");
        }
    }
    
    private int indexFor(String hexNum, int width) {
        int x = Integer.parseInt(hexNum.substring(0, hexNum.length() - 2)) - 1;
        int y = Integer.parseInt(hexNum.substring(hexNum.length() - 2)) - 1;
        return y * width + x;
    }
    
    
    /**
     * Writes data for the board, as text to the OutputStream
     */
    public void save(OutputStream os) {
        try {
            Writer w = new OutputStreamWriter(os);
            // write
            w.write("size " + width + " " + height + "\r\n");
            if ( !this.roadsAutoExit ) {
                w.write( "option exit_roads_to_pavement false\r\n" );
            }
            for (int i = 0; i < data.length; i++) {
                Hex hex = data[i];
                boolean firstTerrain = true;
                
                StringBuffer hexBuff = new StringBuffer("hex ");
                hexBuff.append(new Coords(i % width, i / width).getBoardNum());
                hexBuff.append(" ");
                hexBuff.append(hex.getElevation());
                hexBuff.append(" \"");
                for (int j = 0; j < Terrain.SIZE; j++) {
                    Terrain terrain = hex.getTerrain(j);
                    if (terrain != null) {
                        if (!firstTerrain) {
                            hexBuff.append(";");
                        }
                        hexBuff.append(terrain.toString());
                        // Do something funky to save building exits.
                        if ( Terrain.BUILDING == j &&
                             !terrain.hasExitsSpecified() &&
                             terrain.getExits() != 0 ) {
                            hexBuff.append( ":" )
                                .append( terrain.getExits() );
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
                //                w.write("hex \"" + hex.getTerrain().name + "\" " + Terrain.TERRAIN_NAMES[hex.getTerrainType()] + " \"" + hex.getTerrain().picfile + "\" " + hex.getElevation() + "\r\n");
            }
            w.write("end\r\n");
            // make sure it's written
            w.flush();
        } catch(IOException ex) {
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
        } catch(IOException ex) {
            System.err.println("i/o error writing board");
            System.err.println(ex);
        }
    }

    /**
     * Record that the given coordinates have recieved a hit from an inferno.
     *
     * @param   coords - the <code>Coords</code> of the hit.
     * @param   round  - the kind of round that hit the hex.
     * @param   hits   - the <code>int</code> number of rounds that hit.
     *          If a negative number is passed, then an
     *          <code>IllegalArgumentException</code> will be thrown.
     */
    public void addInfernoTo( Coords coords, 
                              InfernoTracker.Inferno round,
                              int hits ) {
        // Declare local variables.
        InfernoTracker tracker = null;

        // Make sure the # of hits is valid.
        if ( hits < 0 ) {
            throw new IllegalArgumentException
                ( "Board can't track negative hits. " );
        }

        // Do nothing if the coords aren't on this board.
        if ( !this.contains( coords ) ) {
            return;
        }

        // Do we already have a tracker for those coords?
        tracker = (InfernoTracker) this.infernos.get( coords );
        if ( null == tracker ) {
            // Nope.  Make one.
            tracker = new InfernoTracker();
            this.infernos.put( coords, tracker );
        }

        // Update the tracker.
        tracker.add( round, hits );

    }

    /**
     * Determine if the given coordinates has a burning inferno.
     *
     * @param   coords - the <code>Coords</code> being checked.
     * @return  <code>true</code> if those coordinates have a burning
     *          inferno round. <code>false</code> if no inferno has hit
     *          those coordinates or if it has burned out.
     */
    public boolean isInfernoBurning( Coords coords ) {
        boolean result = false;
        InfernoTracker tracker = null;

        // Get the tracker for those coordinates
        // and see if the fire is still burning.
        tracker = (InfernoTracker) this.infernos.get( coords );
        if ( null != tracker ) {
            if ( tracker.isStillBurning() ) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Record that a new round of burning has passed for the given coordinates.
     * This routine also determines if the fire is still burning.
     *
     * @param   coords - the <code>Coords</code> being checked.
     * @return  <code>true</code> if those coordinates have a burning
     *          inferno round. <code>false</code> if no inferno has hit
     *          those coordinates or if it has burned out.
     */
    public boolean burnInferno( Coords coords ) {
        boolean result = false;
        InfernoTracker tracker = null;

        // Get the tracker for those coordinates, record the round
        // of burning and see if the fire is still burning.
        tracker = (InfernoTracker) this.infernos.get( coords );
        if ( null != tracker ) {
            tracker.newRound(-1);
            if ( tracker.isStillBurning() ) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Get an enumeration of all coordinates with infernos still burning.
     *
     * @return  an <code>Enumeration</code> of <code>Coords</code> that
     *          have infernos still burning.
     */
    public Enumeration getInfernoBurningCoords() {
        // Only include *burning* inferno trackers.
        Vector burning = new Vector();
        Enumeration iter = this.infernos.keys();
        while ( iter.hasMoreElements() ) {
            final Coords coords = (Coords) iter.nextElement();
            if ( this.isInfernoBurning(coords) ) {
                burning.addElement( coords );
            }
        }
        return burning.elements();
    }

    /**
     * 
     * Determine the remaining number of turns the given coordinates will have
     * a burning inferno.
     *
     * @param   coords - the <code>Coords</code> being checked.
     *          This value must not be <code>null</code>.  Unchecked.
     * @return  the <code>int</code> number of burn turns left for all infernos
     *          This value will be non-negative.
     */
    public int getInfernoBurnTurns( Coords coords ) {
        int turns = 0;
        InfernoTracker tracker = null;

        // Get the tracker for those coordinates
        // and see if the fire is still burning.
        tracker = (InfernoTracker) this.infernos.get( coords );
        if ( null != tracker ) {
            turns = tracker.getTurnsLeftToBurn();
        }
        return turns;
    }

    /**
     * 
     * Determine the remaining number of turns the given coordinates will have
     * a burning Inferno IV round.
     *
     * @param   coords - the <code>Coords</code> being checked.
     *          This value must not be <code>null</code>.  Unchecked.
     * @return  the <code>int</code> number of burn turns left for Arrow IV
     *          infernos.  This value will be non-negative.
     */
    public int getInfernoIVBurnTurns( Coords coords ) {
        int turns = 0;
        InfernoTracker tracker = null;

        // Get the tracker for those coordinates
        // and see if the fire is still burning.
        tracker = (InfernoTracker) this.infernos.get( coords );
        if ( null != tracker ) {
            turns = tracker.getArrowIVTurnsLeftToBurn();
        }
        return turns;
    }

    /**
     * Get an enumeration of all buildings on the board.
     *
     * @return  an <code>Enumeration</code> of <code>Building</code>s.
     */
    public Enumeration getBuildings() {
        return this.buildings.elements();
    }

    /**
     * Get the building at the given coordinates.
     *
     * @param   coords - the <code>Coords</code> being examined.
     * @return  a <code>Building</code> object, if there is one at the
     *          given coordinates, otherwise a <code>null</code> will
     *          be returned.
     */
    public Building getBuildingAt( Coords coords ) {
        return (Building) this.bldgByCoords.get( coords );
    }

    /**
     * Get the local object for the given building.  Call this routine
     * any time the input <code>Building</code> is suspect.
     *
     * @param   other - a <code>Building</code> object which may or may
     *          not be represented on this board.
     *          This value may be <code>null</code>.
     * @return  The local <code>Building</code> object if we can find a
     *          match.  If the other building is not on this board, a
     *          <code>null</code> is returned instead.
     */
    private Building getLocalBuilding( Building other ) {

        // Handle garbage input.
        if ( other == null ) {
            return null;
        }

        // ASSUMPTION: it is better to use the Hashtable than the Vector.
        Building local = null;
        Enumeration coords = other.getCoords();
        if ( coords.hasMoreElements() ) {
            local = (Building) bldgByCoords.get( coords.nextElement() );
            if ( !other.equals( local ) ) {
                local = null;
            }
        }

        // TODO: if local is still null, try the Vector.
        return local;
    }

    /**
     * Collapse an array of buildings.
     *
     * @param   bldgs - the <code>Vector</code> of <code>Building</code>
     *          objects to be collapsed.
     */
    public void collapseBuilding( Vector bldgs ) {

        // Walk through the vector of buildings.
        Enumeration loop = bldgs.elements();
        while ( loop.hasMoreElements() ) {
            final Building other = (Building) loop.nextElement();

            // Find the local object for the given building.
            Building bldg = this.getLocalBuilding( other );

            // Handle garbage input.
            if ( bldg == null ) {
                System.err.print( "Could not find a match for " );
                System.err.print( other );
                System.err.println( " to collapse." );
                continue;
            }

            // Update the building.
            this.collapseBuilding( bldg );

        } // Handle the next building.

    }

    /**
     * The given building has collapsed.  Remove it from the board and
     * replace it with rubble.
     *
     * @param   other - the <code>Building</code> that has collapsed.
     */
    public void collapseBuilding( Building bldg ) {

        // Remove the building from our building vector.
        this.buildings.removeElement( bldg );

        // Walk through the building's hexes.
        Enumeration bldgCoords = bldg.getCoords();
        while ( bldgCoords.hasMoreElements() ) {
            final Coords coords = (Coords) bldgCoords.nextElement();
            final Hex curHex = this.getHex( coords );
            int elevation = curHex.getElevation();

            // Remove the building from the building map.
            this.bldgByCoords.remove( coords );

            // Remove the building terrain.
            curHex.removeTerrain( Terrain.BUILDING );
            curHex.removeTerrain( Terrain.BLDG_CF );
            curHex.removeTerrain( Terrain.BLDG_ELEV );

            // Add rubble terrain that matches the building type.
            curHex.addTerrain( new Terrain( Terrain.RUBBLE, bldg.getType() ) );

            // Any basement reduces the hex's elevation.
            if ( curHex.contains( Terrain.BLDG_BASEMENT ) ) {
                elevation -= curHex.levelOf( Terrain.BLDG_BASEMENT );
                curHex.removeTerrain( Terrain.BLDG_BASEMENT );
                curHex.setElevation( elevation );
            }

            // Update the hex.
            // TODO : Do I need to initialize it???
            // ASSUMPTION: It's faster to update one at a time.
            this.setHex( coords, curHex );

        } // Handle the next building hex.

    } // End public void collapseBuilding( Building )

    /**
     * Update the construction factors on an array of buildings.
     *
     * @param   bldgs - the <code>Vector</code> of <code>Building</code>
     *          objects to be updated.
     */
    public void updateBuildingCF( Vector bldgs ) {

        // Walk through the vector of buildings.
        Enumeration loop = bldgs.elements();
        while ( loop.hasMoreElements() ) {
            final Building other = (Building) loop.nextElement();

            // Find the local object for the given building.
            Building bldg = this.getLocalBuilding( other );

            // Handle garbage input.
            if ( bldg == null ) {
                System.err.print( "Could not find a match for " );
                System.err.print( other );
                System.err.println( " to update." );
                continue;
            }

            // Set the current and phase CFs of the building.
            bldg.setCurrentCF( other.getCurrentCF() );
            bldg.setPhaseCF( other.getPhaseCF() );

        } // Handle the next building.

    }

    /**
     * Get the current value of the "road auto-exit" option.
     *
     * @return  <code>true</code> if roads should automatically exit onto
     *          all adjacent pavement hexes.  <code>false</code> otherwise.
     */
    public boolean getRoadsAutoExit() {
        return this.roadsAutoExit;
    }

    /**
     * Set the value of the "road auto-exit" option.
     *
     * @param   value - The value to set for the option; <code>true</code>
     *          if roads should automatically exit onto all adjacent pavement
     *          hexes.  <code>false</code> otherwise.
     */
    public void setRoadsAutoExit( boolean value ) {
        this.roadsAutoExit = value;
    }

    /**
    Helpfunctions for the map generator 
    increased a heightmap my a given value 
    */
    private void markRect(int x1, int x2, int inc, int elevationMap [][], int height) {
        for (int x = x1; x < x2; x++) {
            for (int y = 0; y < height; y++) {
                elevationMap[x][y] += inc;
            } // for
        }
    } // 
 
    /**
    Helpfunktion for map generator
    inreases all of one side and decreased on other side
    */
    private void markSides(Point p1, Point p2, int upperInc, int lowerInc, int elevationMap [][], int height) {
        for (int x = p1.x; x < p2.x; x++) {
            for (int y = 0; y < height; y++) {
                int point =(p2.y - p1.y) / (p2.x - p1.x) * (x - p1.x) + p1.y;
                if (y > point) {
                    elevationMap[x][y] += upperInc;
                } else if (y < point) {
                    elevationMap[x][y] += lowerInc;
                }
            } // for
        }
    } // 
 
    /**
    Searching starting from one Hex, all Terrains not matching
    terrainType, next to one of terrainType.
    @param terrainType The terrainType which the searching hexes
    should not have.
    @param alreadyUsed The hexes which should not looked at
    (because they are already supposed to visited in some way) 
    @param unUsed In this set the resulting hexes are stored. They
    are stored in addition to all previously stored.
    @param searchFrom The Hex where to start
    */
    private void findAllUnused(int terrainType, HashSet alreadyUsed,
                                   HashSet unUsed, Hex searchFrom,
                                   HashMap reverseHex) {
        Hex field;
        HashSet notYetUsed = new HashSet();
    
        notYetUsed.add(searchFrom);
        do {
            Iterator iter = notYetUsed.iterator();
            field = (Hex)iter.next();
            if (field == null) {
                continue;
            }
            for (int dir = 0; dir < 6; dir++) {
                Point loc = (Point) reverseHex.get(field);
                Hex newHex = getHexInDir(loc.x, loc.y, dir);
                if ((newHex != null) && 
                    (!alreadyUsed.contains(newHex)) &&
                    (!notYetUsed.contains(newHex)) &&
                    (!unUsed.contains(newHex))) {
                    ((newHex.contains(terrainType)) ? notYetUsed : unUsed ).add(newHex);
                }
            }
            notYetUsed.remove(field);
            alreadyUsed.add(field);
        } while (!notYetUsed.isEmpty());
    } // findAllUnused
 
    /**
    Places randomly some connected Woods.
    @param probHeavy The probability that a wood is a heavy wood
    (in %).
    @param maxWoods Maximum Number of Woods placed.
    */
    private void placeSomeTerrain(int terrainType, int probMore,
                                  int minHexes, int maxHexes,
                                  HashMap reverseHex) {
        Point p = new Point(Compute.randomInt(width),
                            Compute.randomInt(height));
        int count = minHexes;
        if ((maxHexes - minHexes) > 0) {
            count += Compute.randomInt(maxHexes-minHexes);
        }
        Hex field;
    
        HashSet alreadyUsed = new HashSet();
        HashSet unUsed = new HashSet();
        field = getHex(p.x, p.y);
        if (!field.contains(terrainType)) {
            unUsed.add(field);
        } else {
            findAllUnused(terrainType, alreadyUsed,
                          unUsed, field, reverseHex);
        }
        for (int i = 0; i < count; i++) {
            if (unUsed.isEmpty()) {
                return;
            }
            int which = Compute.randomInt(unUsed.size());
            Iterator iter = unUsed.iterator();
            for (int n = 0; n < (which - 1); n++)
                iter.next();
            field = (Hex)iter.next();
            field.removeAllTerrains();
            int tempInt = (Compute.randomInt(100) < probMore)? 2 : 1;
            Terrain tempTerrain = new Terrain(terrainType, tempInt);
            field.addTerrain(tempTerrain);
            unUsed.remove(field);
            findAllUnused(terrainType, alreadyUsed, unUsed, field, reverseHex);
        } // for
    
        if (terrainType == Terrain.WATER) {
            /* if next to an Water Hex is an lower lvl lower the hex.
               First we search for lowest Hex next to the lake */
            int min = Integer.MAX_VALUE;
            Iterator iter = unUsed.iterator();
            while (iter.hasNext()) {
                field = (Hex)iter.next();
                if (field.getElevation() < min) {
                    min = field.getElevation();
                }
            }
            iter = alreadyUsed.iterator();
            while (iter.hasNext()) {
                field = (Hex)iter.next();
                field.setElevation(min);
            }
        
        }
    } /* placeSomeTerrain */

    /** Gives a normal distributed Randomvalue, with mediumvalue from
    0 and a Varianz of factor.
    @param factor varianz of of the distribution.
    @return Random number, most times in the range -factor .. +factor,
    at most in the range of -3*factor .. +3*factor.
    */
    private int normRNG(int factor) {
        factor++;
        return (2 * (Compute.randomInt(factor) + Compute.randomInt(factor) +
                     Compute.randomInt(factor)) - 3 * (factor - 1)) / 32;
    } /* normRNG */
    
    /** Helpfunction for landscape generation */
    private void midPointStep(double fracdim, int size, int delta,
                              int elevationMap[][], int step, 
                              boolean newBorder) {
        int d1, d2;
        int delta5;
        int x,y;
        
        d1 = size >> (step - 1);
        d2 = d1 / 2;
        fracdim = (1.0 - fracdim) / 2.0;
        delta = (int)(delta * Math.exp(-0.6931 * fracdim * (2.0 * (double)step - 1)));
        delta5 = delta << 5;
        x = d2;
        do {
            y = d2;
            do {
                elevationMap[x][y] = middleValue(elevationMap[x + d2][y + d2],
                                                 elevationMap[x + d2][y - d2],
                                                 elevationMap[x - d2][y + d2],
                                                 elevationMap[x - d2][y - d2],
                                                 delta5);
                y += d1;
            } while (y < size - d2);
            x += d1;
        } while (x < size - d2);

        delta = (int)(delta * Math.exp(-0.6931 * fracdim ));
        delta5 = delta << 5;
        if (newBorder) {
            x = d2;
            do {
                y = x;
                elevationMap[0][x] = middleValue(elevationMap[0][x + d2],
                                                 elevationMap[0][x - d2],
                                                 elevationMap[d2][x], delta5);
                elevationMap[size][x] = middleValue(elevationMap[size - 1][x + d2],
                                                    elevationMap[size - 1][x - d2],
                                                    elevationMap[size - d2 - 1][x], 
                                                    delta5);
                y = 0;
                elevationMap[x][0] = middleValue(elevationMap[x + d2][0],
                                                 elevationMap[x - d2][0],
                                                 elevationMap[x][d2], 
                                                 delta5);
                elevationMap[x][size] = middleValue(elevationMap[x + d2][size - 1],
                                                    elevationMap[x - d2][size - 1],
                                                    elevationMap[x][size - d2 - 1], 
                                                    delta5);
                x += d1;
            } while (x < size - d2);
        } // if (newBorder)
        diagMid(new Point(d2, d1), d1, d2, delta5, size, elevationMap);
        diagMid(new Point(d1, d2), d1, d2, delta5, size, elevationMap);
    } /* MidPointStep */
    
    /** calculates the diagonal middlepoints with new values
    @param p Starting point.
    */
    private void diagMid(Point p, int d1, int d2, 
                         int delta, int size,
                         int elevationMap[][]) {
        int x = p.x;
        int y;
        int hx = x + d2;
        int hy;
        
        while ((x < size - d2) && (hx < size)) {
            y = p.y;
            hy = y + d2;
            while ( (y < size-d2) && (hy < size)) {
                elevationMap[x][y] = middleValue(elevationMap[x][hy],
                                                 elevationMap[x][y - d2],
                                                 elevationMap[hx][y],
                                                 elevationMap[x - d2][y],
                                                 delta);
                y += d1;
                hy = y + d2;
            }
            x += d1;
            hx = x + d2;
        }
    } 
    
    /** calculates the arithmetic medium of 3 values and add random
    value in range of delta.
    */
    private int middleValue(int a,  int b, int c, int delta) {
        int result=(((a + b + c) / 3) + normRNG(delta));
        return result;
    } /* middleValue */
    
    /** calculates the arithmetic medium of 4 values and add random
    value in range of delta.
    */
    private int middleValue(int a,  int b, int c, int d, int delta) {
        int result = (((a + b + c + d) / 4) + normRNG(delta));
        return result;
    } /* middleValue */
    
    /** one of the landscape generation algorithms */
    private void cutSteps(int hilliness, int width, int height, 
                          int elevationMap[][]) {
        Point p1, p2;
        int sideA, sideB;
        int type;
    
        p1 = new Point(0,0);
        p2 = new Point(0,0);
        for (int step = 0; step < 20 * hilliness; step++) {
            /* select which side should be decremented, and which
               increemented */
            sideA = (Compute.randomInt(2) == 0)? -1 : 1;
            sideB =- sideA;
            type = Compute.randomInt(6);
            /* 6 different lines in rectangular area from border to
               border possible */
            switch (type) {
            case 0: /* left to upper border */
                p1.setLocation(0, Compute.randomInt(height));
                p2.setLocation(Compute.randomInt(width), height-1);
                markSides(p1, p2, sideB, sideA, elevationMap, height);
                markRect(p2.x, width-1, sideA, elevationMap, height);
                break;
            case 1: /* upper to lower border */
                p1.setLocation(Compute.randomInt(width), 0);
                p2.setLocation(Compute.randomInt(width), height-1);
                if (p1.x < p2.x) {
                    markSides(p1, p2, sideA, sideB, elevationMap, height);
                } else {
                    markSides(p2, p1, sideB, sideA, elevationMap, height);
                }
                markRect(0, p1.x, sideA, elevationMap, height);
                markRect(p2.x, width, sideB, elevationMap, height);
                break;
            case 2: /* upper to right border */
                p1.setLocation(Compute.randomInt(width), height-1);
                p2.setLocation(width, Compute.randomInt(height));
                markSides(p1, p2, sideB, sideA, elevationMap, height);
                markRect(0, p1.x, sideA, elevationMap, height);
                break;
            case 3: /* left to right border */
                p1.setLocation(0, Compute.randomInt(height));
                p2.setLocation(width-1, Compute.randomInt(height));
                markSides(p1, p2, sideA, sideB, elevationMap, height);
                break;
            case 4: /* left to lower border */
                p1.setLocation(0, Compute.randomInt(height));
                p2.setLocation(Compute.randomInt(width), 0);
                markSides(p1, p2, sideB, sideA, elevationMap, height);
                markRect(p2.x, width-1, sideB, elevationMap, height);
                break;
            case 5: /* lower to right border */
                p1.setLocation(Compute.randomInt(width), 0);
                p2.setLocation(width, Compute.randomInt(height));
                markSides(p1, p2, sideB, sideA, elevationMap, height);
                markRect(0, p1.x, sideB, elevationMap, height);
                break;
            } // switch

        } /* for */
    } /* cutSteps */
    
    /** 
    midpoint algorithm for landscape generartion 
    */
    private void midPoint(int hilliness, int width, int height, 
                          int elevationMap[][]) {
        int size;
        int steps = 1;
        int tmpElevation[][];
        
        size = (width > height) ? width : height;
        while (size > 0) {
            steps++;
            size /= 2;
        }
        size = (1 << steps) + 1;
        tmpElevation = new int[size + 1][size + 1];
        /* init elevation map with 0 */
        for (int w = 0; w < size; w++)
            for (int h = 0; h < size; h++)
        if ((w < width) && (h < height)) {
                    tmpElevation[w][h] = elevationMap[w][h];
        } else {
                    tmpElevation[w][h] = 0;
        }
        for (int i = steps; i > 0; i--) {
            midPointStep((double)hilliness/1000, size, 1000,
                         tmpElevation, i, true);
        }
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                elevationMap[w][h] = tmpElevation[w][h];
            }
        }
    } // midPoint
    
    /** Generates the elevations 
    @param hilliness The Hilliness
    @param width The Width of the map.
    @param height The Height of the map.
    @param range Max difference betweenn highest and lowest level.
    @param invertProb Probability for the invertion of the map (0..100)
    @param elevationMap here is the result stored
    */
    public void generateElevation(int hilliness, int width, int height,
                                  int range, int invertProb,
                                  int elevationMap[][], int algorithm) {
        int minLevel = 0;
        int maxLevel = range;
        boolean invert = (Compute.randomInt(100) < invertProb);
        
        
        /* init elevation map with 0 */
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                elevationMap[w][h] = 0;
            }
        }
        /* generate landscape */
        switch (algorithm) {
        case 0: 
            cutSteps(hilliness, width, height, elevationMap);
            break;
        case 1: 
            midPoint(hilliness, width, height, elevationMap);
            break;
        case 2:
            cutSteps(hilliness, width, height, elevationMap);
            midPoint(hilliness, width, height, elevationMap);
            break;
        } // switch

        /* and now normalize it */
        int min = elevationMap[0][0];
        int max = elevationMap[0][0];
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                if (elevationMap[w][h] > max) {
                    max = elevationMap[w][h];
                }
                if (elevationMap[w][h] < min) {
                    min = elevationMap[w][h];
                }
            }
        }
    
        double scale = (double)(maxLevel - minLevel) / (double)(max - min);
        int inc = (int)(-scale * min + minLevel);
        int[] elevationCount = new int[maxLevel + 1];
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                elevationMap[w][h] *= scale;
                elevationMap[w][h] += inc;
                elevationCount[elevationMap[w][h]]++;
            } // for
        }
        int mostElevation = 0;
        for (int lvl = 1; lvl <= range; lvl++) {
            if (elevationCount[lvl] > elevationCount[mostElevation]) {
                mostElevation = lvl;
            }
        }
        for (int w=0; w<width; w++)     {
            for (int h=0; h<height; h++) {
                elevationMap[w][h]-=mostElevation;
                if (invert) {
                    elevationMap[w][h] *= -1;
                }
            }
        }
    
    } // generateElevation

    /** Extends a river hex to left and right sides.
    @param hexloc The location of the river hex,
    from which it should get started.
    @param width The width to wich the river should extend in
    the direction. So the actual width of the river is
    2*width+1. 
    @param direction Direction too which the riverhexes should be
    extended. 
    @return Hashset with the hexes from the side.
    */
    private HashSet extendRiverToSide(Point hexloc, int width,
                                      int direction, HashMap reverseHex) {
        Point current = new Point(hexloc);
        HashSet result = new HashSet();
        Hex hex;
        
        hex = getHexInDir(current.x, current.y, direction);
        while ((hex != null) && (width-- > 0)) {
            hex.removeAllTerrains();
            hex.addTerrain(new Terrain(Terrain.WATER, 1));
            result.add(hex);        
            current = (Point)reverseHex.get(hex);
            hex = getHexInDir(current.x, current.y, direction);
        } // while 
        
        return result;
    } // extendRiverToSide
    
    /** Adds an River to the map (if the map is at least 5x5 hexes
    big). The river has an width of 1-3 hexes (everything else is
    no more a river). The river goes from one border to another.
    Nor Params, no results.
    */
    public void addRiver(HashMap reverseHex) {
        int minElevation = Integer.MAX_VALUE;
        HashSet riverHexes = new HashSet();
        Hex field, rightHex, leftHex;
        Point p = null;
        int direction = 0;
        int nextLeft = 0;
        int nextRight = 0;
        
        /* if map is smaller than 5x5 no real space for an river */
        if ((width < 5) || (height < 5)) {
            return;
        }
        /* First select start and the direction */
        switch (Compute.randomInt(4)) {
        case 0:
            p = new Point(0, Compute.randomInt(5) - 2 + height / 2);
            direction = Compute.randomInt(2) + 1;
            nextLeft = direction - 1;
            nextRight = direction + 1;
            break;
        case 1:
            p = new Point(width - 1, Compute.randomInt(5) - 2 + height / 2);
            direction = Compute.randomInt(2) + 4;
            nextLeft = direction - 1;
            nextRight = (direction + 1) % 6;
            break;
        case 2:
        case 3:
            p = new Point(Compute.randomInt(5) - 2 + width / 2, 0);
            direction = 2;
            nextRight = 3;
            nextLeft = 4;
            break;
        } // switch
        /* place the river */
        field = getHex(p.x, p.y);
        do {
            /* first the hex itself */
            field.removeAllTerrains();
            field.addTerrain(new Terrain(Terrain.WATER, 1));
            riverHexes.add(field);
            p = (Point)reverseHex.get(field);
            /* then maybe the left and right neighbours */
            riverHexes.addAll(extendRiverToSide(p, Compute.randomInt(3), 
                                                nextLeft, reverseHex));
            riverHexes.addAll(extendRiverToSide(p, Compute.randomInt(3),
                                                nextRight, reverseHex));
            switch (Compute.randomInt(4)) {
            case 0: 
                field = getHexInDir(p.x, p.y, (direction + 5) % 6);
                break;
            case 1: 
                field = getHexInDir(p.x, p.y, (direction + 1) % 6);
                break;
            default:
                field = getHexInDir(p.x, p.y, direction);
                break;
            }
            
        } while (field != null); 
        
        /* search the elevation for the river */
        HashSet tmpRiverHexes = (HashSet)riverHexes.clone();
        while (!tmpRiverHexes.isEmpty()) {
            Iterator iter = tmpRiverHexes.iterator();
            field = (Hex)iter.next();
            if (field.getElevation() < minElevation) {
                minElevation = field.getElevation();
            }
            tmpRiverHexes.remove(field);
            Point thisHex = (Point)reverseHex.get(field);
            /* and now the six neighbours */
            for (int i = 0; i < 6; i++) {
                field = getHexInDir(thisHex.x, thisHex.y, i);
                if ((field != null) && (field.getElevation() < minElevation)) {
                    minElevation = field.getElevation();
                }
                tmpRiverHexes.remove(field);
            }
        } /* while */
        
        /* now adjust the elevation to same height */
        Iterator iter = riverHexes.iterator();
        while (iter.hasNext()) {
            field = (Hex)iter.next();
            field.setElevation(minElevation);
        } /* while */
        
        return;
    } // addRiver
    
    
    /** Adds an Road to the map. Goes from one border to another, and
     * has one turn in it. Map must be at least 3x3.
     */
    public void addRoad(HashMap reverseHex) {
        if ((width < 3) || (height < 3)) {
            return;
        }
        /* first determine the turning hex, and then the direction
           of the doglegs */
        Point start = new Point(Compute.randomInt(width - 2) + 1, 
                                Compute.randomInt(height - 2) + 1);
        Point p = null;
        int[] side = new int[2];
        Hex field = null;
        int lastLandElevation = 0;
        
        side[0] = Compute.randomInt(6);
        side[1] = Compute.randomInt(5);
        if (side[1] >= side[0]) {
            side[1]++;
        }
        for (int i = 0; i < 2; i++) {
            field = getHex(start.x, start.y);
            do {
                if (field.contains(Terrain.WATER)) {
                    field.addTerrain(new Terrain(Terrain.WATER, 0));
                    field.setElevation(lastLandElevation);
                } else {
                    lastLandElevation = field.getElevation();
                }
                field.addTerrain(new Terrain(Terrain.ROAD, 1));
                p = (Point)reverseHex.get(field);
                field = getHexInDir(p.x, p.y, side[i]);
            } while (field != null); 
        } // for 
    } // addRoad
    
    

    /** The profile of a crater: interior is exp-function, exterior cos
        function.
        @param x The x value of the function. range 0..1. 
        0=center of crater. 1=border of outer wall.
        @param scale Apply this scale before returning the result
        (recommend instead of afterwards scale, cause this way the intern
        floating values are scaled, instead of int result).
        @return The height of the crater at the position x from
        center. Unscaled, the results are between -0.5 and 1 (that
        means, if no scale is applied -1, 0 or 1).
    */
    public int craterProfile(double x, int scale) {
        double result = 0;
    
        result = (x < 0.75) ? 
            ((Math.exp(x * 5.0 / 0.75 - 3) - 0.04979) * 1.5 / 7.33926) - 0.5 : 
            ((Math.cos((x-0.75)*4.0)+1.0) / 2.0);
    
        return (int)(result * (double)scale);
    } // craterProfile
     
    // calculate the distance between two points
    private static double distance(Point p1, Point p2) {
        double x = p1.x - p2.x;
        double y = p1.y - p2.y;
        return Math.sqrt(x*x + y*y);
    }
 
       /** add a crater to the board */
    public void addCraters(int minRadius, int maxRadius, 
                           int minCraters, int maxCraters) {
        int numberCraters = minCraters;
        if (maxCraters > minCraters) {
            numberCraters += Compute.randomInt(maxCraters - minCraters);
        }
        for (int i = 0; i < numberCraters; i++) {
            Point center = new Point(Compute.randomInt(width), Compute.randomInt(height));
            
            int radius = Compute.randomInt(maxRadius - minRadius + 1) + minRadius;
            int maxLevel = 3;
            if (radius < 3) { 
                maxLevel = 1;
            }
            if ((radius >= 3) && (radius <= 8)) {
                maxLevel = 2;
            }
            if (radius > 14) {
                maxLevel = 4;
            }
            int maxHeight = Compute.randomInt(maxLevel) + 1;
            /* generate CraterProfile */
            int cratHeight[] = new int[radius];
            for (int x = 0; x < radius; x++) {
                cratHeight[x] = craterProfile((double)x / (double)radius, maxHeight);
            }
            /* btw, I am interested if someone actually reads
               this comments, so send me and email to f.stock@tu-bs.de, if
               you do ;-) */
            /* now recalculate every hex */
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    int distance = (int)distance(center, new Point(w,h));
                    if (distance < radius) {
                        double fac = (double)distance / (double)radius;
                        Hex field = getHex(w, h);
                        field.setElevation(//field.getElevation() +
                                           cratHeight[distance]);
                    } // if 
                }   
            } // for (int i=...         
        }
    } /* addCraters
 
      /**
      Generates a Random Board
      @param width The width of the generated Board.
      @param height The height of the gernerated Board.
      @param steps how often the iterative method should be repeated
      */
    public void generateRandom(MapSettings mapSettings) {
        int elevationMap[][] = new int[mapSettings.getBoardWidth()][mapSettings.getBoardHeight()];
        double sizeScale = (double)(mapSettings.getBoardWidth() * mapSettings.getBoardHeight()) / ((double)(16 * 17));
        
        generateElevation(mapSettings.getHilliness(),
                          mapSettings.getBoardWidth(), 
                          mapSettings.getBoardHeight(),
                          mapSettings.getRange() + 1, 
                          mapSettings.getProbInvert(),
                          elevationMap,
                          mapSettings.getAlgorithmToUse());
        
        Hex[] nb = new Hex[mapSettings.getBoardWidth() * mapSettings.getBoardHeight()];
        int index = 0;
        for (int h = 0; h < mapSettings.getBoardHeight(); h++) {
            for (int w = 0; w < mapSettings.getBoardWidth(); w++) {
                nb[index++] = new Hex(elevationMap[w][h],"","");
            } // for
        }
        
        newData(mapSettings.getBoardWidth(), 
                mapSettings.getBoardHeight(), nb);
        /* initalize reverseHex */
        HashMap reverseHex = new HashMap(2 * mapSettings.getBoardWidth() * mapSettings.getBoardHeight());
        for (int y = 0; y < mapSettings.getBoardHeight(); y++) {
            for (int x = 0; x < mapSettings.getBoardWidth(); x++) {
                reverseHex.put(getHex(x, y),new Point(x, y));
            }
        }
        
        /* Add the woods */
        int count = mapSettings.getMinForestSpots();
        if (mapSettings.getMaxForestSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxForestSpots());
        }
        count *= sizeScale;
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(Terrain.WOODS, mapSettings.getProbHeavy() ,
                             mapSettings.getMinForestSize(), 
                             mapSettings.getMaxForestSize(),
                             reverseHex);
        }
        /* Add the water */
        count = mapSettings.getMinWaterSpots();
        if (mapSettings.getMaxWaterSpots() > 0) { 
            count += Compute.randomInt(mapSettings.getMaxWaterSpots());
        }
        count *= sizeScale;
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(Terrain.WATER, mapSettings.getProbDeep() ,
                             mapSettings.getMinWaterSize(), 
                             mapSettings.getMaxWaterSize(),
                             reverseHex);
        }
        
        /* Add the rough */
        count = mapSettings.getMinRoughSpots();
        if (mapSettings.getMaxRoughSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxRoughSpots());
        }
        count *= sizeScale;
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(Terrain.ROUGH, 0,
                             mapSettings.getMinRoughSize(), 
                             mapSettings.getMaxRoughSize(),
                             reverseHex);
        }
        /* Add the craters */
        if (Compute.randomInt(100) < mapSettings.getProbCrater()) {
            addCraters(mapSettings.getMinRadius(), mapSettings.getMaxRadius(),
                       (int)(mapSettings.getMinCraters()*sizeScale),
                       (int)(mapSettings.getMaxCraters()*sizeScale));
        }
        
        /* Add the river */
        if (Compute.randomInt(100)<mapSettings.getProbRiver()) {
            addRiver(reverseHex);
        }
        
        /* Add the road */
        if (Compute.randomInt(100)<mapSettings.getProbRoad()) {
            addRoad(reverseHex);
        }
    
        reverseHex = null;
    } /* generateRandom */

    /**
     * Populate the <code>bldgByCoords</code> member from the current
     * <code>Vector</code> of buildings.  Use this method after de-
     * serializing a <code>Board</code> object.
     */
    private void createBldgByCoords() {

        // Make a new hashtable.
        bldgByCoords = new Hashtable();

        // Walk through the vector of buildings.
        Enumeration loop = buildings.elements();
        while ( loop.hasMoreElements() ) {
            final Building bldg = (Building) loop.nextElement();

            // Each building identifies the hexes it covers.
            Enumeration iter = bldg.getCoords();
            while ( iter.hasMoreElements() ) {
                bldgByCoords.put( iter.nextElement(), bldg );
            }

        }

    }

    protected static class Point {
        
        public int x;
        public int y;
        
        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        public Point(Point other) {
            this.x = other.x;
            this.y = other.y;
        }
        
        /**
         * Set the location 
         * @param x x coordinate
         * @param y y coordinate
         */
        public void setLocation(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Override the default deserialization to populate the transient
     * <code>bldgByCoords</code> member.
     *
     * @param   in - the <code>ObjectInputStream</code> to read.
     * @throws  <code>IOException</code>
     * @throws  <code>ClassNotFoundException</code>
     */
    private void readObject( ObjectInputStream in )
        throws IOException, ClassNotFoundException 
    {
        in.defaultReadObject();

        // Restore bldgByCoords from buildings.
        createBldgByCoords();
    }

}
