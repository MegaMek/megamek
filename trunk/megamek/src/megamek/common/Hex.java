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

import com.sun.java.util.collections.*;
import java.io.*;
import java.awt.Image;
import java.util.StringTokenizer;

/**
 * Hex represents a single hex on the board. 
 *
 * @author Ben
 */
public class Hex 
  implements Serializable, Cloneable
{
    private int elevation;
    private Terrain[] terrains;
    private String theme;
    
    private transient Image base = null;
    private transient List supers = null;
    
    /** Constructs clear, plain hex at level 0. */
    public Hex() {
        this(0);
    }
    
    /** Constructs clean, plain hex at specified elevation. */
    public Hex(int elevation) {
        this(elevation, new Terrain[Terrain.SIZE], null);
    }
    
    /** Constructs hex with all parameters. */
    public Hex(int elevation, Terrain[] terrains, String theme) {
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
        this(elevation, new Terrain[Terrain.SIZE], theme);
        for (StringTokenizer st = new StringTokenizer(terrain, ";", false); st.hasMoreTokens();) {
            addTerrain(new Terrain(st.nextToken()));
        }
    }
    
    public int getElevation() {
        return elevation;
    }
    
    public void setElevation(int elevation) {
        this.elevation = elevation;
        invalidateCache();
    }
    
    private void invalidateCache() {
        this.base = null;
        this.supers = null;
        //depth = Terrain.LEVEL_NONE;
    }
    
    public String getTheme() {
        return theme;
    }
    
    public Image getBase() {
        return base;
    }
    
    public void setBase(Image base) {
        this.base = base;
    }
    
    public void setSupers(List supers) {
        this.supers = supers;
    }
    
    public List getSupers() {
        return supers;
    }
    
    /**
     * Clears the "exits" flag for all terrains in the hex where it is not
     * manually specified.
     */
    public void clearExits() {
        for (int i = 0; i < Terrain.SIZE; i++) {
            Terrain terr = getTerrain(i);
            if (terr != null && !terr.hasExitsSpecified()) {
                terr.setExits(0);
            }
        }
        invalidateCache();
    }
    
    /**
     * Sets the "exits" flag appropriately, assuming the specified hex
     * lies in the specified direction on the board.  Does not reset connects
     * in other directions.
     */
    public void setExits(Hex other, int direction) {
        for (int i = 0; i < Terrain.SIZE; i++) {
            Terrain cTerr = getTerrain(i);
            Terrain oTerr;
            
            if (cTerr == null || cTerr.hasExitsSpecified()) {
                continue;
            }
            
            if (other != null) {
                oTerr = other.getTerrain(i);
            } else {
                oTerr = null;
            }
            
            cTerr.setExit(direction, cTerr.exitsTo(oTerr));
        }
        invalidateCache();
    }
    
    /**
     * Returns the highest level that features in this hex extend to.  Above
     * this level is assumed to be air.
     */
    public int ceiling() {
        // TODO: implement
        
        return elevation;
    }
    
    /**
     * Returns the surface level of the hex.  Equal to getElevation().
     */
    public int surface() {
        return elevation;
    }
    
    /**
     * Returns the lowest level that features in this hex extend to.  Below
     * this level is assumed to be bedrock.
     */
    public int floor() {
        return elevation - depth();
    }
    
    /**
     * Returns a level indicating how far features in this hex extend below the
     * surface elevation.
     */
    public int depth() {
        int depth = 0;
        Terrain water = getTerrain(Terrain.WATER);
        Terrain basement = getTerrain(Terrain.BLDG_BASEMENT);
        if (water != null) {
            depth += water.getLevel();
        }
        if (basement != null) {
            depth += basement.getLevel();
        }
        return depth;
    }
    
    /**
     * @return true if the specified terrain is represented in the hex at any
     *  level.
     */
    public boolean contains(int type) {
        return getTerrain(type) != null;
    }
    
    public boolean contains(int type, int level) {
        Terrain terrain = getTerrain(type);
        if (terrain != null) {
            return terrain.getLevel() == level;
        } else {
            return false;
        }
    }
    
    /**
     * @return the level of the terrain specified, or Terrain.LEVEL_NONE if the
     *  terrain is not present in the hex
     */
    public int levelOf(int type) {
        Terrain terrain = getTerrain(type);
        if (terrain != null) {
            return terrain.getLevel();
        } else {
            return Terrain.LEVEL_NONE;
        }
    }
    
    public Terrain getTerrain(int type) {
        return terrains[type];
    }
    
    public void addTerrain(Terrain terrain) {
        terrains[terrain.getType()] = terrain;
        invalidateCache();
    }
    
    public void removeTerrain(int type) {
        terrains[type] = null;
        invalidateCache();
    }
    
    /**
     * Returns the number of terrain attributes present
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
    
    /**
     * Returns a pretty deep clone
     */
    public Object clone() {
        Terrain[] tcopy = new Terrain[terrains.length];
        for (int i = 0; i < terrains.length; i++) {
            if (terrains[i] != null) {
                tcopy[i] = new Terrain(terrains[i]);
            }
        }
        return new Hex(elevation, tcopy, theme);
    }
    
    /**
     * Hexes are equal if their terrains equal each other and if the 
     * elevations are equal
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Hex other = (Hex)object;
        return false;
    }
}

