/**
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
    public static final int    BOARD_HEX_CLICK            = 1;
    public static final int    BOARD_HEX_DOUBLECLICK    = 2;
    public static final int    BOARD_HEX_DRAG            = 3;
    
    public int                width;
    public int                height;
    
    public transient Coords        lastCursor;
    public transient Coords        highlighted;
    public transient Coords        selected;
    
    public Hex[] data;
    
    public Terrain[] terrains;
    
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
        
        terrains = new Terrain[0];
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
                mergeTerrain(boards[i * sheetWidth + j]);
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
     * Merges the terrain sets this and another board.  This may result in
     * each board having terrain in its set that is not used on the board.
     *
     * If I was really ambitious, I'd make it so that the other board would update
     * all of its hexes to use the new, combines terrain set.
     */
    private void mergeTerrain(Board other) {
        Vector combined = new Vector();
        // add this board's terrain
        for (int i = 0; i < this.terrains.length; i++) {
            combined.addElement(this.terrains[i]);
        }
        // add the other board's terrain that is not already in the combined set
        for (int i = 0; i < other.terrains.length; i++) {
            if (combined.indexOf(other.terrains[i]) == -1) {
                combined.addElement(other.terrains[i]);
            }
        }
        // give both boards the resulting terrain set
        this.terrains = new Terrain[combined.size()];
        other.terrains = new Terrain[combined.size()];
        combined.copyInto(this.terrains);
        combined.copyInto(other.terrains);
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
     * and if so, sets the specified Hex into that position.
     *
     * @param x the x Coords.
     * @param y the y Coords.
     * @param hex the hex to be set into position.
     */
    public void setHex(int x, int y, Hex hex) {
        data[y * width + x] = hex;
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
        Hashtable terrainHash = new Hashtable();
        //Vector terrainLoaded = new Vector();
        for(int i = 0; i < terrains.length; i++) {
            terrainHash.put(terrains[i].name, terrains[i]);
        }
        
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
                }
                if(st.ttype == StreamTokenizer.TT_WORD && st.sval.equalsIgnoreCase("hex")) {
                    // read rest of line
                    String[] args = {"", "", "", "0"};
                    int i = 0;
                    while(st.nextToken() == StreamTokenizer.TT_WORD || st.ttype == '"' || st.ttype == StreamTokenizer.TT_NUMBER) {
                        args[i++] = st.ttype == StreamTokenizer.TT_NUMBER ? (int)st.nval + "" : st.sval;
                    }
                    if(!terrainHash.containsKey(args[0])) {
                        Terrain terrain = new Terrain(args[0], Terrain.parse(args[1]), args[2]);
                        terrainHash.put(terrain.name, terrain);
                    }
                    Hex hex = new Hex((Terrain)terrainHash.get(args[0]), Integer.parseInt(args[3]));
                    nd[di++] = hex;
//                    // check terrainB type
//                    int hex_terrain = Hex.parse(args[1]);
//                    // if valid, add to list
//                    if(hex_terrain != -1) {
//                        Hex hex = new Hex(args[0], hex_terrain, args[2], Integer.parseInt(args[3]));
//           
//                        nd[di++] = hex;
//                    }
                }
                if(st.ttype == StreamTokenizer.TT_WORD && st.sval.equalsIgnoreCase("end")) {
                    break;
                }
            }
        } catch(IOException ex) {
            System.err.println("i/o error reading board");
            System.err.println(ex);
        }
        // check data integrity
        if(nw > 1 || nh > 1 || di == nw * nh) {
            newData(nw, nh, nd);
            
            terrains = new Terrain[terrainHash.size()];
            int tcount = 0;
            for (Enumeration i = terrainHash.elements(); i.hasMoreElements();) {
                terrains[tcount++] = (Terrain)i.nextElement();
            }
            
            //System.out.println("board.load: terrain array length = " + terrains.length);
        } else {
            System.err.println("board data invalid");
        }
    }
    
    
    /**
     * Writes data for the board, as text to the OutputStream
     */
    public void save(OutputStream os) {
        try {
            Writer w = new OutputStreamWriter(os);
            // write
            w.write("size " + width + " " + height + "\r\n");
            for(int i = 0; i < data.length; i++) {
                Hex hex = data[i];
                w.write("hex \"" + hex.getTerrain().name + "\" " + Terrain.TERRAIN_NAMES[hex.getTerrainType()] + " \"" + hex.getTerrain().picfile + "\" " + hex.getElevation() + "\r\n");
            }
            w.write("end" + "\r\n");
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
