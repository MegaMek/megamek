/*
 * MegaMek - Copyright (C) 2002,2003 Ben Mazur (bmazur@sev.org)
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

/**
 *
 * MapSettings.java
 *
 * Created on March 27, 2002, 1:07 PM
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
    
    /** how much swamp spots at least */
    private int minSwampSpots = 2;
    /** how much swamp spots  at most */
    private int maxSwampSpots = 10;
    /** minimum size of a swamp spot */
    private int minSwampSize = 1;
    /** maximum Size of a swamp spot */
    private int maxSwampSize = 2;
    
    /** how much pavement spots at least */
    private int minPavementSpots = 0;
    /** how much pavement spots  at most */
    private int maxPavementSpots = 0;
    /** minimum size of a pavement spot */
    private int minPavementSize = 1;
    /** maximum Size of a pavement spot */
    private int maxPavementSize = 6;
    
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
    
    /** a tileset theme to apply */
    private String theme = "";
    
    /** probability of flooded map */
    private int probFlood = 0;
    /** probability of forest fire */
    private int probForestFire = 0;
    /** probability of frozen map */
    private int probFreeze = 0;
    /** probability of drought */
    private int probDrought = 0;
    /** special FX modifier */
    private int fxMod = 0;

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
        this.minSwampSpots = other.getMinSwampSpots();
        this.maxSwampSpots = other.getMaxSwampSpots();
        this.minSwampSize = other.getMinSwampSize();
        this.maxSwampSize = other.getMaxSwampSize();
        this.minPavementSpots = other.getMinPavementSpots();
        this.maxPavementSpots = other.getMaxPavementSpots();
        this.minPavementSize = other.getMinPavementSize();
        this.maxPavementSize = other.getMaxPavementSize();
        this.probRoad = other.getProbRoad();
        this.probRiver = other.getProbRiver();
        this.probCrater = other.getProbCrater();
        this.minRadius = other.getMinRadius();
        this.maxRadius = other.getMaxRadius();
        this.minCraters = other.getMinCraters();
        this.maxCraters = other.getMaxCraters();
        this.algorithmToUse = other.getAlgorithmToUse();
        this.theme = other.getTheme();
        this.probFlood = other.getProbFlood();
        this.probForestFire = other.getProbForestFire();
        this.probFreeze = other.getProbFreeze();
        this.probDrought = other.getProbDrought();
        this.fxMod = other.getFxMod();
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

    public String getTheme() {
        return theme;
    }
    
    public void setTheme(String th) {
        theme = th;
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
                int rindex = Compute.randomInt(boardsAvailable.size() - 3) + 3;
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
       Checks, if the Mapgenerator parameters are all valid. If not 
       they are changed to valid values.
    */
    public void validateMapGenParameters() {
    if (hilliness < 0) {
        hilliness = 0;
    }
    if (hilliness > 99) {
        hilliness = 99;
    }
    if (range < 0 ) {
        range = 0;
    }   
    if (minWaterSpots < 0) {
        minWaterSpots = 0;
    }
    if (maxWaterSpots < minWaterSpots) {
        maxWaterSpots = minWaterSpots;
    }
    if (minWaterSize < 0) {
        minWaterSize = 0;
    }
    if (maxWaterSize < minWaterSize) {
        maxWaterSize = minWaterSize;
    }
    if (probDeep < 0) {
        probDeep = 0;
    }
    if (probDeep > 100) {
        probDeep = 100;
    }
    if (minForestSpots < 0) {
        minForestSpots = 0;
    }
    if (maxForestSpots < minForestSpots) {
        maxForestSpots = minForestSpots;
    }
    if (minForestSize < 0) {
        minForestSize = 0;
    }
    if (maxForestSize < minForestSize) {
        maxForestSize = minForestSize;
    }
    if (probHeavy < 0) {
        probHeavy = 0;
    }
    if (probHeavy > 100) {
        probHeavy = 100;
    }
    if (minRoughSpots < 0) {
        minRoughSpots = 0;
    }
    if (maxRoughSpots < minRoughSpots) {
        maxRoughSpots = minRoughSpots;
    }
    if (minRoughSize < 0) {
        minRoughSize = 0;
    }
    if (maxRoughSize < minRoughSize) {
        maxRoughSize = minRoughSize;
    }
    if (minSwampSpots < 0) {
        minSwampSpots = 0;
    }
    if (maxSwampSpots < minSwampSpots) {
        maxSwampSpots = minSwampSpots;
    }
    if (minSwampSize < 0) {
        minSwampSize = 0;
    }
    if (maxSwampSize < minSwampSize) {
        maxSwampSize = minSwampSize;
    }
    if (minPavementSpots < 0) {
        minPavementSpots = 0;
    }
    if (maxPavementSpots < minPavementSpots) {
        maxPavementSpots = minPavementSpots;
    }
    if (minPavementSize < 0) {
        minPavementSize = 0;
    }
    if (maxPavementSize < minPavementSize) {
        maxPavementSize = minPavementSize;
    }
    if (probRoad < 0) {
        probRoad = 0;
    }
    if (probRoad > 100) {
        probRoad = 100;
    }
    if (probInvert < 0) {
        probInvert = 0;
    }
    if (probInvert > 100) {
        probInvert = 100;
    }
    if (probRiver < 0) {
        probRiver = 0;
    }
    if (probRiver > 100) {
        probRiver = 100;
    }
    if (probCrater < 0) {
        probCrater = 0;
    }
    if (probCrater > 100) {
        probCrater = 100;
    }
    if (minRadius < 0) {
        minRadius = 0;
    }
    if (maxRadius < minRadius) {
        maxRadius = minRadius;
    }
    if (minCraters < 0 ) {
        minCraters = 0;
    }
    if (maxCraters < minCraters) {
        maxCraters = minCraters;
    }
    if (algorithmToUse < 0) {
        algorithmToUse = 0;
    }
    if (algorithmToUse > 2) {
        algorithmToUse = 2;
    }
    } /* validateMapGenParameters */
    
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
            (this.minSwampSpots != other.getMinSwampSpots()) ||
            (this.maxSwampSpots != other.getMaxSwampSpots()) ||
            (this.minSwampSize != other.getMinSwampSize()) ||
            (this.maxSwampSize != other.getMaxSwampSize()) ||
            (this.minPavementSpots != other.getMinPavementSpots()) ||
            (this.maxPavementSpots != other.getMaxPavementSpots()) ||
            (this.minPavementSize != other.getMinPavementSize()) ||
            (this.maxPavementSize != other.getMaxPavementSize()) ||
            (this.probRoad != other.getProbRoad()) ||
            (this.probInvert != other.getProbInvert()) ||
            (this.probRiver != other.getProbRiver()) ||
            (this.probCrater != other.getProbCrater()) ||
            (this.minRadius != other.getMinRadius()) ||
            (this.maxRadius != other.getMaxRadius()) ||
            (this.minCraters != other.getMinCraters()) ||
            (this.maxCraters != other.getMaxCraters()) ||
            (this.theme != other.getTheme()) ||
            (this.fxMod != other.getFxMod()) ||
            (this.probFlood != other.getProbFlood()) ||
            (this.probForestFire != other.getProbForestFire()) ||
            (this.probFreeze != other.getProbFreeze()) ||
            (this.probDrought != other.getProbDrought()) ||
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
    
    public int getMinSwampSpots() { return minSwampSpots; }
    public int getMaxSwampSpots() { return maxSwampSpots; }
    public int getMinSwampSize() { return minSwampSize; }
    public int getMaxSwampSize() { return maxSwampSize; }
    
    public int getMinPavementSpots() { return minPavementSpots; }
    public int getMaxPavementSpots() { return maxPavementSpots; }
    public int getMinPavementSize() { return minPavementSize; }
    public int getMaxPavementSize() { return maxPavementSize; }
    
    public int getProbRoad() { return probRoad; }
    
    public int getProbRiver() { return probRiver; }
    
    public int getProbCrater() { return probCrater; }
    public int getMinRadius() { return minRadius; }
    public int getMaxRadius() { return maxRadius; }
    public int getMinCraters() { return minCraters; }
    public int getMaxCraters() { return maxCraters; }
    public int getAlgorithmToUse() { return algorithmToUse; }
 
    public int getProbFlood() {return probFlood;}
    public int getProbForestFire() {return probForestFire;}
    public int getProbFreeze() {return probFreeze;}
    public int getProbDrought() {return probDrought;}
    public int getFxMod() {return fxMod;}
 
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
     public void setSwampParams(int minSpots, int maxSpots,
                                 int minSize, int maxSize) {
         minSwampSpots = minSpots;
         maxSwampSpots = maxSpots;
         minSwampSize = minSize;
         maxSwampSize = maxSize;
     }
    
    /** set the Parameters for the Map Generator 
     */
     public void setPavementParams(int minSpots, int maxSpots,
                                 int minSize, int maxSize) {
         minPavementSpots = minSpots;
         maxPavementSpots = maxSpots;
         minPavementSize = minSize;
         maxPavementSize = maxSize;
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
    
    /** 
     * set Map generator parameters
     */
    public void setSpecialFX(int modifier, int fire, int freeze, int flood, int drought) {
        fxMod = modifier;
        probForestFire = fire;
        probFreeze = freeze;
        probFlood = flood;
        probDrought = drought;
    }
    public void setAlgorithmToUse(int alg) {
        algorithmToUse = alg;
    }    
}
