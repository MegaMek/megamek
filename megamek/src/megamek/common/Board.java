/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
    
    public int                  width;
    public int                  height;
    
    public transient Coords     lastCursor;
    public transient Coords     highlighted;
    public transient Coords     selected;
    
    public Hex[]                data;
    
    protected transient Vector boardListeners;
    
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
     * Combines one or more boards into one huge megaboard!
     *
     * @param width the width of each individual board, before the combine
     * @param height the height of each individual board, before the combine
     * @param sheetWidth how many sheets wide the combined map is
     * @param sheetHeight how many sheets tall the combined map is
     * @param boards an array of the boards to be combined
     */
    public void combine(int width, int height, int sheetWidth, int sheetHeight, Board[] boards) {
        int totalWidth = width * sheetWidth;
        int totalHeight = height * sheetHeight;
        
        newData(totalWidth, totalHeight);
        
        for (int i = 0; i < sheetHeight; i++) {
            for (int j = 0; j < sheetWidth; j++) {
                copyBoardInto(j * width, i * height, boards[i * sheetWidth + j]);
            }
        }
        
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
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                initializeHex(x, y);
            }
        }
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
     * Initializes a hex in its surroundings.  Currently sets the connects
     * parameter appropriately to the surrounding hexes.
     *
     * If a surrounding hex is off the board, it checks the hex opposite the
     * missing hex.
     */
    public void initializeHex(int x, int y) {
        Hex hex = getHex(x, y);
        
        if (hex == null) {
            return;
        }
        
        hex.clearExits();
        for (int i = 0; i < 6; i++) {
            Hex other = getHexInDir(x, y, i);
            hex.setExits(other, i);
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
        if (c == null || p == null) return false;
        
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
                return c.y > (height - nLimit);
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
}
