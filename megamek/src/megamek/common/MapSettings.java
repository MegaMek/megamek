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
	public static final String BOARD_GENERATED = "[GENERATED]";    
    
    private int boardWidth = 16;
    private int boardHeight = 17;
    private int mapWidth = 1;
    private int mapHeight = 1;
    
    private Vector boardsSelected = new Vector();
    private Vector boardsAvailable = new Vector();

     /** Parameters for the Map Generator 
         Parameters refer to a default map siz 16 x 17, with other size
         some of the parameters get linear transformed to give good
         result for new size */
    
	/** how much hills there should be, Range 0..99 */
	private int hilliness = 40;
	/** Maximum difference between highest elevation and lowest sink */
	private int range = 5;
	/** Probabiltity for invertion of the map, Range 0..100 */
	private int probInvert = 5;
	
	/** how much Lakes at least */
	private int minWaterSpots = 1;
	/** how much Lakes at most */
	private int maxWaterSpots = 3;
	/** minimum size of a lake */
	private int minWaterSize = 5;
	/** maximum Size of a lake */
	private int maxWaterSize = 10;
	/** probability for water deeper than lvl1, Range 0..100 */
	private int probDeep = 33;
	
	/** how much forests at least */
	private int minForestSpots = 3;
	/** how much forests at most */
	private int maxForestSpots = 8;
	/** minimum size of a forest */
	private int minForestSize = 4;
	/** maximum Size of a forest */
	private int maxForestSize = 12;
	/** probability for heavy woods, Range 0..100 */
	private int probHeavy = 30;
	
	/** how much rough spots at least */
	private int minRoughSpots = 2;
	/** how much rough spots  at most */
	private int maxRoughSpots = 10;
	/** minimum size of a rough spot */
	private int minRoughSize = 1;
	/** maximum Size of a rough spot */
	private int maxRoughSize = 2;
	
	/** probability for a road, range 0..100 */
	private int probRoad = 0;
	
	/** probability for a river, range 0..100 */
	private int probRiver = 0;
	
	/** probabilitay for Crater 0..100 */
	private int probCrater = 0;
	
	/** minimum Radius of the Craters */
	private int minRadius = 2;
	
	/** maximum Radius of the Craters */
	private int maxRadius = 7;
	
	/** maximum Number of Craters on one map */
	private int maxCraters = 2;
	
	/** minimum Number of Craters on one map */
	private int minCraters = 1;
	
	/** which landscape generation Algortihm to use */
	/* atm there are 2 different: 0= first, 1=second */
	private int algorithmToUse = 0;

	/** end Map Generator Parameters */

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

     	this.hilliness = other.getHilliness();
	 	this.range = other.getRange();
		this.probInvert = other.getProbInvert();
	 	this.minWaterSpots = other.getMinWaterSpots();
	 	this.maxWaterSpots = other.getMaxWaterSpots();
	 	this.minWaterSize = other.getMinWaterSize();
	 	this.maxWaterSize = other.getMaxWaterSize();
	 	this.probDeep = other.getProbDeep();
	 	this.minForestSpots = other.getMinForestSpots();
	 	this.maxForestSpots = other.getMaxForestSpots();
	 	this.minForestSize = other.getMinForestSize();
	 	this.maxForestSize = other.getMaxForestSize();
	 	this.probHeavy = other.getProbHeavy();
	 	this.minRoughSpots = other.getMinRoughSpots();
	 	this.maxRoughSpots = other.getMaxRoughSpots();
	 	this.minRoughSize = other.getMinRoughSize();
	 	this.maxRoughSize = other.getMaxRoughSize();
	 	this.probRoad = other.getProbRoad();
	 	this.probRiver = other.getProbRiver();
	 	this.probCrater = other.getProbCrater();
	 	this.minRadius = other.getMinRadius();
	 	this.maxRadius = other.getMaxRadius();
		this.minCraters = other.getMinCraters();
	 	this.maxCraters = other.getMaxCraters();
		this.algorithmToUse = other.getAlgorithmToUse();
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
                int rindex = Compute.randomInt(boardsAvailable.size() - 2) + 2;
                // Do a one pi rotation half of the time.
                if ( 0 == Compute.randomInt(2) ) {
                    boardsSelected.setElementAt
                        (Board.BOARD_REQUEST_ROTATION +
                         boardsAvailable.elementAt(rindex), i);
                } else {
                    boardsSelected.setElementAt
                        (boardsAvailable.elementAt(rindex), i);
                }
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
    
    
	/**
		Returns true if the this Mapsetting has the same mapgenerator
		settings and size  as the parameter.
		@param other The Mapsetting to which compare.
		@return True if settings are the same.
	*/
	public boolean equalMapGenParameters(MapSettings other) {
		if ((this.boardWidth != other.getBoardWidth()) ||
			(this.boardHeight != other.getBoardHeight()) ||
			(this.mapWidth != other.getMapWidth()) ||
			(this.mapHeight != other.getMapHeight()) ||
			(this.hilliness != other.getHilliness()) ||
			(this.range != other.getRange()) ||
			(this.minWaterSpots != other.getMinWaterSpots()) ||
			(this.maxWaterSpots != other.getMaxWaterSpots()) ||
			(this.minWaterSize != other.getMinWaterSize()) ||
			(this.maxWaterSize != other.getMaxWaterSize()) ||
			(this.probDeep != other.getProbDeep()) ||
			(this.minForestSpots != other.getMinForestSpots()) ||
			(this.maxForestSpots != other.getMaxForestSpots()) ||
			(this.minForestSize != other.getMinForestSize()) ||
			(this.maxForestSize != other.getMaxForestSize()) ||
			(this.probHeavy != other.getProbHeavy()) ||
			(this.minRoughSpots != other.getMinRoughSpots()) ||
			(this.maxRoughSpots != other.getMaxRoughSpots()) ||
			(this.minRoughSize != other.getMinRoughSize()) ||
			(this.maxRoughSize != other.getMaxRoughSize()) ||
			(this.probRoad != other.getProbRoad()) ||
			(this.probInvert != other.getProbInvert()) ||
			(this.probRiver != other.getProbRiver()) ||
			(this.probCrater != other.getProbCrater()) ||
			(this.minRadius != other.getMinRadius()) ||
			(this.maxRadius != other.getMaxRadius()) ||
			(this.minCraters != other.getMinCraters()) ||
			(this.maxCraters != other.getMaxCraters()) ||
			(this.algorithmToUse != other.getAlgorithmToUse())) {
			return false;
		} else { 
			return true;
		}
	} /* equalMapGenParameters */

    /** clone! */
    public Object clone() {
        return new MapSettings(this);
    }

	public int getHilliness() { return hilliness; }
	public int getRange() { return range; }
	public int getProbInvert() { return probInvert; }
	
	public int getMinWaterSpots() { return minWaterSpots; }
	public int getMaxWaterSpots() { return maxWaterSpots; }
	public int getMinWaterSize() { return minWaterSize; }
	public int getMaxWaterSize() { return maxWaterSize; }
	public int getProbDeep() { return probDeep; }
	
	public int getMinForestSpots() { return minForestSpots; }
	public int getMaxForestSpots() { return maxForestSpots; }
	public int getMinForestSize() { return minForestSize; }
	public int getMaxForestSize() { return maxForestSize; }
	public int getProbHeavy() { return probHeavy; }
	
	public int getMinRoughSpots() { return minRoughSpots; }
	public int getMaxRoughSpots() { return maxRoughSpots; }
	public int getMinRoughSize() { return minRoughSize; }
	public int getMaxRoughSize() { return maxRoughSize; }
	
	public int getProbRoad() { return probRoad; }
	
	public int getProbRiver() { return probRiver; }
	
	public int getProbCrater() { return probCrater; }
	public int getMinRadius() { return minRadius; }
	public int getMaxRadius() { return maxRadius; }
	public int getMinCraters() { return minCraters; }
	public int getMaxCraters() { return maxCraters; }
	public int getAlgorithmToUse() { return algorithmToUse; }
	
 
	/** set the Parameters for the Map Generator 
	*/
	public void setElevationParams(int hill, int newRange, int prob) {
		hilliness = hill;
		range = newRange;
		probInvert = prob;
	}
	
	/** set the Parameters for the Map Generator 
	*/
	public void setWaterParams(int minSpots, int maxSpots,
								int minSize, int maxSize, int prob) {
		minWaterSpots = minSpots;
		maxWaterSpots = maxSpots; 
		minWaterSize = minSize;
		maxWaterSize = maxSize;
		probDeep = prob;
	}
	
	/** set the Parameters for the Map Generator 
	*/    
	public void setForestParams(int minSpots, int maxSpots,
								int minSize, int maxSize, int prob) {
		minForestSpots = minSpots;
		maxForestSpots = maxSpots;
		minForestSize = minSize;
		maxForestSize = maxSize;
		probHeavy = prob;
	}
	
	/** set the Parameters for the Map Generator 
	*/
	public void setRoughParams(int minSpots, int maxSpots,
								int minSize, int maxSize) {
		minRoughSpots = minSpots;
		maxRoughSpots = maxSpots;
		minRoughSize = minSize;
		maxRoughSize = maxSize;
	}
	
	/** set the Parameters for the Map Generator 
	*/
	public void setRiverParam(int prob) { probRiver = prob; }
	
	/** set the Parameters for the Map Generator 
	*/
	public void setRoadParam(int prob) { probRoad = prob; }
	
	/** set the Parameters for the Map Generator 
	*/
	public void setCraterParam(int prob, int minCrat,
								int maxCrat, int minRad, int maxRad) {

		probCrater = prob; 
		maxCraters = maxCrat;
		minCraters=minCrat;
		minRadius = minRad;
		maxRadius = maxRad;
	}
	
	public void setAlgorithmToUse(int alg) {
		algorithmToUse = alg;
	}    
}
