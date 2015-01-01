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

import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * Hex represents a single hex on the board.
 * 
 * @author Ben
 */
public class Hex implements IHex, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 82483704768044696L;
    private int elevation;
    private ITerrain[] terrains;
    private String theme;
    private int fireTurn;

    /** Constructs clear, plain hex at level 0. */
    public Hex() {
        this(0);
    }

    /** Constructs clean, plain hex at specified elevation. */
    public Hex(int elevation) {
        this(elevation, new ITerrain[Terrains.SIZE], null);
    }

    /** Constructs hex with all parameters. */
    public Hex(int elevation, ITerrain[] terrains, String theme) {
        this.elevation = elevation;
        this.terrains = terrains;
        if (theme == null || theme.length() > 0) {
            this.theme = theme;
        } else {
            this.theme = null;
        }
    }

    /** Contructs hex with string terrain info */
    public Hex(int elevation, String terrain, String theme) {
        this(elevation, new ITerrain[Terrains.SIZE], theme);
        for (StringTokenizer st = new StringTokenizer(terrain, ";", false); st
                .hasMoreTokens();) {
            addTerrain(Terrains.getTerrainFactory().createTerrain(
                    st.nextToken()));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#getElevation()
     */
    public int getElevation() {
        return elevation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#setElevation(int)
     */
    public void setElevation(int elevation) {
        this.elevation = elevation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#getTheme()
     */
    public String getTheme() {
        return theme;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#setTheme(java.lang.String)
     */
    public void setTheme(String theme) {
        this.theme = theme;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#clearExits()
     */
    public void clearExits() {
        for (int i = 0; i < Terrains.SIZE; i++) {
            ITerrain terr = getTerrain(i);
            if (terr != null && !terr.hasExitsSpecified()) {
                terr.setExits(0);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#setExits(megamek.common.IHex, int)
     */
    public void setExits(IHex other, int direction) {
        this.setExits(other, direction, true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#setExits(megamek.common.IHex, int, boolean)
     */
    public void setExits(IHex other, int direction, boolean roadsAutoExit) {
        for (int i = 0; i < Terrains.SIZE; i++) {
            ITerrain cTerr = getTerrain(i);
            ITerrain oTerr;

            if (cTerr == null || cTerr.hasExitsSpecified()) {
                continue;
            }

            if (other != null) {
                oTerr = other.getTerrain(i);
            } else {
                oTerr = null;
            }

            cTerr.setExit(direction, cTerr.exitsTo(oTerr));

            // Roads exit into pavement, too.
            if (other != null && roadsAutoExit
                    && cTerr.getType() == Terrains.ROAD
                    && other.containsTerrain(Terrains.PAVEMENT)) {
                cTerr.setExit(direction, true);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#containsTerrainExit(int, int)
     */
    public boolean containsTerrainExit(int terrType, int direction) {
        boolean result = false;
        final ITerrain terr = getTerrain(terrType);

        // Do we have the given terrain that has exits?
        if (direction >= 0 && direction <= 5 && terr != null) {

            // See if we have an exit in the given direction.
            final int exits = terr.getExits();
            final int exitInDir = (int) Math.pow(2, direction);
            if ((exits & exitInDir) > 0) {
                result = true;
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#ceiling()
     */
    public int ceiling() {
        int maxFeature = 0;

        //TODO: maxfeature should really be a method in Terrain.java
        
        //planted fields rise one level above the terrain
        if (containsTerrain(Terrains.FIELDS)) {
            maxFeature = 1;
        }
        
        // Account for woods. They are 2 levels high
        // N.B. VTOLs are allowed to enter smoke.
        if (containsTerrain(Terrains.WOODS) || containsTerrain(Terrains.JUNGLE)) {
            maxFeature = 2;
        }
        //not so fast ultra jungles and woods are three levels high
        if(terrainLevel(Terrains.WOODS) > 2 || terrainLevel(Terrains.JUNGLE) > 2) {
            maxFeature = 3;
        }     

        //account for heavy industrial zones, which can vary in height
        if (maxFeature < this.terrainLevel(Terrains.INDUSTRIAL)) {
            maxFeature = this.terrainLevel(Terrains.INDUSTRIAL);
        }
        
        // Account for buildings.
        if (maxFeature < this.terrainLevel(Terrains.BLDG_ELEV)) {
            maxFeature = this.terrainLevel(Terrains.BLDG_ELEV);
        }

        // Account for bridges.
        if (maxFeature < this.terrainLevel(Terrains.BRIDGE_ELEV)) {
            maxFeature = this.terrainLevel(Terrains.BRIDGE_ELEV);
        }

        return elevation + maxFeature;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#surface()
     */
    public int surface() {
        return elevation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#floor()
     */
    public int floor() {
        return elevation - depth();
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#depth()
     */
    public int depth() {
        int depth = 0;
        ITerrain water = getTerrain(Terrains.WATER);
        ITerrain basement = getTerrain(Terrains.BLDG_BASEMENT);
        if (water != null) {
            depth += water.getLevel();
        }
        if (basement != null) {
            depth += basement.getLevel();
        }
        return depth;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#contains(int)
     */
    public boolean containsTerrain(int type) {
        return getTerrain(type) != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#contains(int, int)
     */
    public boolean containsTerrain(int type, int level) {
        ITerrain terrain = getTerrain(type);
        if (terrain != null) {
            return terrain.getLevel() == level;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#hasPavement()
     */
    public boolean hasPavement() {
        return containsTerrain(Terrains.PAVEMENT)
                || containsTerrain(Terrains.ROAD)
                || containsTerrain(Terrains.BRIDGE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#levelOf(int)
     */
    public int terrainLevel(int type) {
        ITerrain terrain = getTerrain(type);
        if (terrain != null) {
            return terrain.getLevel();
        }
        return ITerrain.LEVEL_NONE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#getTerrain(int)
     */
    public ITerrain getTerrain(int type) {
        return terrains[type];
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#addTerrain(megamek.common.Terrain)
     */
    public void addTerrain(ITerrain terrain) {
        terrains[terrain.getType()] = terrain;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#removeTerrain(int)
     */
    public void removeTerrain(int type) {
        terrains[type] = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#removeAllTerrains()
     */
    public void removeAllTerrains() {
        for (int i = 0; i < terrains.length; i++) {
            terrains[i] = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#terrainsPresent()
     */
    public int displayableTerrainsPresent() {
        int present = 0;
        for (int i = 0; i < terrains.length; i++) {
            if (null != terrains[i] && null != Terrains.getDisplayName(i, terrains[i].getLevel())) {
                present++;
            }
        }
        return present;
    }
    
    /*
     * report the number of displayable terrains present for the tooltips. 
     * This should not include any terrains which don't report back a display name
     */
    public int terrainsPresent() {
        int present = 0;
        for (int i = 0; i < terrains.length; i++) {
            if (terrains[i] != null) {
                present++;
            }
        }
        return present;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.IHex#duplicate
     */
    public IHex duplicate() {
        ITerrain[] tcopy = new ITerrain[terrains.length];
        ITerrainFactory f = Terrains.getTerrainFactory();
        for (int i = 0; i < terrains.length; i++) {
            if (terrains[i] != null) {
                tcopy[i] = f.createTerrain(terrains[i]);
            }
        }
        return new Hex(elevation, tcopy, theme);
    }

    public int terrainPilotingModifier(int moveType) {
        int rv = 0;
        for (int i = 0; i < terrains.length; i++) {
            if (terrains[i] != null)
                rv += terrains[i].pilotingModifier(moveType);
        }
        return rv;
    }

    public int movementCost(int moveType) {
        int rv = 0;
        for (int i = 0; i < terrains.length; i++) {
            if (terrains[i] != null)
                rv += terrains[i].movementCost(moveType);
        }
        return rv;
    }

    public String toString() {
        String temp;
        temp = "Elevation: " + getElevation();
        temp = temp + "  Features: ";
        for (ITerrain terrain : terrains) {
            if (terrain != null) {
                switch (terrain.getType()) {
                    case Terrains.WOODS:
                        if (terrain.getLevel() == 2) {
                            temp = temp + "Heavy Woods";
                        } else if (terrain.getLevel() == 1) {
                            temp = temp + "Light Woods";
                        } else {
                            temp = temp + "??? Woods";
                        }
                        break;
                    case Terrains.WATER:
                        temp = temp + "Water, depth: " + terrain.getLevel();
                        break;
                    case Terrains.ROAD:
                        temp = temp + "Road";
                        break;
                    case Terrains.ROUGH:
                        temp = temp + "Rough";
                        break;
                    case Terrains.RUBBLE:
                        temp = temp + "Rubble";
                        break;
                    case Terrains.SWAMP:
                        temp = temp + "Swamp";
                        break;
                    case Terrains.ARMS:
                        temp = temp + "Arm";
                        break;
                    case Terrains.LEGS:
                        temp = temp + "Leg";
                        break;
                    default:
                        temp = temp + Terrains.getName(terrain.getType()) + "("
                                + terrain.getLevel() + ", "
                                + terrain.getTerrainFactor() + ")";
                }
                temp = temp + "; ";
            }
        }
        return temp;
    }
    
    /*
     * Get the fire ignition modifier for this hex, based on its terrain
     */
    public int getIgnitionModifier() {
        int mod = 0;
        for (int i = 0; i < terrains.length; i++) {
            if (terrains[i] != null)
                mod += terrains[i].ignitionModifier();
        }
        return mod;
    }
    
    /**
     * Is this hex ignitable?
     */
    public boolean isIgnitable() {
        return (containsTerrain(Terrains.WOODS) 
                || containsTerrain(Terrains.JUNGLE) 
                || containsTerrain(Terrains.BUILDING)
                || containsTerrain(Terrains.FUEL_TANK)
                || containsTerrain(Terrains.FIELDS)
                || containsTerrain(Terrains.INDUSTRIAL));
        
    }
    
    public int getFireTurn() {
        return fireTurn;
    }
    
    public void incrementFireTurn() {
        fireTurn = fireTurn + 1;
    }
    
    public void resetFireTurn() {
        fireTurn = 0;
    }
    
    /**
     * get any modifiers to a bog-down roll in this hex. Takes the worst modifier
     * If there is no bog-down chance in this hex, then it returns TargetRoll.AUTOMATIC_SUCCESS
     */
    public int getBogDownModifier(int moveType, boolean largeVee) {
        int mod = TargetRoll.AUTOMATIC_SUCCESS;
        for (int i = 0; i < terrains.length; i++) {
            if (terrains[i] != null && mod < terrains[i].getBogDownModifier(moveType, largeVee))
                mod = terrains[i].getBogDownModifier(moveType, largeVee);
        }
        return mod;
    }
    
    /**
     * get any modifiers to a an unstuck roll in this hex.
     */
    public int getUnstuckModifier(int elev) {
        int mod = 0;
        for (int i = 0; i < terrains.length; i++) {
            if (terrains[i] != null)
                mod += terrains[i].getUnstuckModifier(elev);
        }
        return mod;
    }
}
