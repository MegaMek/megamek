/*
 * MapSettings.java
 *
 * Created on March 27, 2002, 1:07 PM
 */

package megamek.common;

import java.util.*;
import java.io.*;

/**
 *
 * @author  Ben
 * @version 
 */
public class MapSettings implements Serializable {
    
    public static final String BOARD_RANDOM = "[RANDOM]";
    public static final String BOARD_SURPRISE = "[SURPRISE]";
    
    
    private int boardWidth = 16;
    private int boardHeight = 17;
    private int mapWidth = 1;
    private int mapHeight = 1;
    
    private Vector boardsSelected = new Vector();
    private Vector boardsAvailable = new Vector();


    /** Creates new MapSettings */
    public MapSettings() {
        this(16, 17, 1, 1);
    }
    
    /** Create new MapSettings with all size settings specified */
    public MapSettings(int boardWidth, int boardHeight, int mapWidth, int mapHeight) {
        setBoardSize(boardWidth, boardHeight);
        setMapSize(mapWidth, mapHeight);
    }
    
    /** Creates new MapSettings that is a duplicate of another */
    public MapSettings(MapSettings other) {
        this.boardWidth = other.getBoardWidth();
        this.boardHeight = other.getBoardHeight();
        this.mapWidth = other.getMapWidth();
        this.mapHeight = other.getMapHeight();
        
        this.boardsSelected = (Vector)other.getBoardsSelectedVector().clone();
        this.boardsAvailable = (Vector)other.getBoardsAvailableVector().clone();
    }
    
    public int getBoardWidth() {
        return boardWidth;
    }

    public int getBoardHeight() {
        return boardHeight;
    }
    
    public void setBoardSize(int boardWidth, int boardHeight) {
        if (boardWidth <= 0 || boardHeight <= 0) {
            throw new IllegalArgumentException("Total board area must be positive");
        }
        
        // change only if actually different
        if (this.boardWidth != boardWidth || this.boardHeight != boardHeight) {
            this.boardWidth = boardWidth;
            this.boardHeight = boardHeight;

            boardsAvailable.removeAllElements();
        }
    }

    public int getMapWidth() {
        return mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    public void setMapSize(int mapWidth, int mapHeight) {
        if (mapWidth <= 0 || mapHeight <= 0) {
            throw new IllegalArgumentException("Total map area must be positive");
        }
        
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        
        boardsSelected.setSize(mapWidth * mapHeight);
    }
    
    public Enumeration getBoardsSelected() {
        return boardsSelected.elements();
    }

    public Vector getBoardsSelectedVector() {
        return boardsSelected;
    }
    
    public void setBoardsSelectedVector(Vector boardsSelected) {
        this.boardsSelected = boardsSelected;
    }
    
    /**
     * Fills in all nulls in the boards selected list with the specified board
     */
    public void setNullBoards(String board) {
        for (int i = 0; i < boardsSelected.size(); i++) {
            if (boardsSelected.elementAt(i) == null) {
                boardsSelected.setElementAt(board, i);
            }
        }
    }
    
    /**
     * Replaces the specified type of board with random boards
     */
    public void replaceBoardWithRandom(String board) {
        for (int i = 0; i < boardsSelected.size(); i++) {
            if (boardsSelected.elementAt(i).equals(board)) {
                int rindex = Compute.random.nextInt(boardsAvailable.size() - 2) + 2;
                boardsSelected.setElementAt(boardsAvailable.elementAt(rindex), i);
            }
        }
    }
    
    /**
     * Removes selected boards that aren't listed in the available boards
     */
    public void removeUnavailable() {
        for (int i = 0; i < boardsSelected.size(); i++) {
            if (boardsSelected.elementAt(i) == null || boardsAvailable.size() == 0 
            || boardsAvailable.indexOf(boardsSelected.elementAt(i)) == -1) {
                boardsSelected.setElementAt(null, i);
            }
        }
    }


    
    public Enumeration getBoardsAvailable() {
        return boardsAvailable.elements();
    }

    public Vector getBoardsAvailableVector() {
        return boardsAvailable;
    }
    
    public void setBoardsAvailableVector(Vector boardsAvailable) {
        this.boardsAvailable = boardsAvailable;
    }
    
    
    /** clone! */
    public Object clone() {
        return new MapSettings(this);
    }
    
}
