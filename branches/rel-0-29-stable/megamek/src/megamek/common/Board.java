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

import java.util.*;
import java.io.*;

public class Board
implements Serializable {
    public static final int     BOARD_HEX_CLICK         = 1;
    public static final int     BOARD_HEX_DOUBLECLICK   = 2;
    public static final int     BOARD_HEX_DRAG          = 3;
    public static final String  BOARD_REQUEST_ROTATION  = "rotate:";
    
    public int                  width;
    public int                  height;
    
    public transient Coords     lastCursor;
    public transient Coords     highlighted;
    public transient Coords     selected;
    
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
        
        boardListeners = new Vector();
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
     * Initializes a hex in its surroundings.
     */
    private void initializeHex(Coords pos) {
        initializeHex(pos.x, pos.y);
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
                            Enumeration enum = bldg.getCoords();
                            while ( enum.hasMoreElements() ) {
                                bldgByCoords.put( enum.nextElement(), bldg );
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
                    processBoardEvent(new BoardEvent(this, c, null, BoardEvent.BOARD_HEX_CLICKED, modifiers));
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
     * Determine if the given coordinatess has a burning inferno.
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
        // of burning and see if  the fire is still burning.
        tracker = (InfernoTracker) this.infernos.get( coords );
        if ( null != tracker ) {
            tracker.newRound();
            if ( tracker.isStillBurning() ) {
                result = true;
            }
        }

        return result;
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
        bldgByCoords = new Hashtable();

        // Walk through the vector of buildings.
        Enumeration loop = buildings.elements();
        while ( loop.hasMoreElements() ) {
            final Building bldg = (Building) loop.nextElement();

            // Each building identifies the hexes it covers.
            Enumeration enum = bldg.getCoords();
            while ( enum.hasMoreElements() ) {
                bldgByCoords.put( enum.nextElement(), bldg );
            }

        }

    }

}
