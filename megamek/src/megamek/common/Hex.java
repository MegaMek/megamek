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

    private int elevation;
    private ITerrain[] terrains;
    private String theme;

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
        for (StringTokenizer st = new StringTokenizer(terrain, ";", false); st.hasMoreTokens();) {
            addTerrain(Terrains.getTerrainFactory().createTerrain(st.nextToken()));
        }
    }

    /* (non-Javadoc)
     * @see megamek.common.IHex#getElevation()
     */
    public int getElevation() {
        return elevation;
    }

    /* (non-Javadoc)
     * @see megamek.common.IHex#setElevation(int)
     */
    public void setElevation(int elevation) {
        this.elevation = elevation;
    }

    /* (non-Javadoc)
     * @see megamek.common.IHex#getTheme()
     */
    public String getTheme() {
        return theme;
    }

    /* (non-Javadoc)
     * @see megamek.common.IHex#setTheme(java.lang.String)
     */
    public void setTheme(String theme) {
        this.theme = theme;
    }

    /* (non-Javadoc)
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

    /* (non-Javadoc)
     * @see megamek.common.IHex#setExits(megamek.common.IHex, int)
     */
    public void setExits(IHex other, int direction) {
        this.setExits( other, direction, true );
    }

    /* (non-Javadoc)
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
            if ( other != null &&
                 roadsAutoExit &&
                 cTerr.getType() == Terrains.ROAD &&
                 other.containsTerrain(Terrains.PAVEMENT) ) {
                cTerr.setExit( direction, true );
            }
        }
    }

    /* (non-Javadoc)
     * @see megamek.common.IHex#containsTerrainExit(int, int)
     */
    public boolean containsTerrainExit(int terrType, int direction) {
        boolean result = false;
        final ITerrain terr = getTerrain( terrType );

        // Do we have the given terrain that has exits?
        if ( direction >= 0 && direction <= 5 && terr != null ) {

            // See if we have an exit in the given direction.
            final int exits = terr.getExits();
            final int exitInDir = (int) Math.pow(2, direction);
            if ( (exits & exitInDir) > 0 ) {
                result = true;
            }
        }
        return result;
    }

    /* (non-Javadoc)
     * @see megamek.common.IHex#ceiling()
     */
    public int ceiling() {
        int maxFeature = 0;

        // Account for woods.
        // N.B. VTOLs are allowed to enter smoke.
        if ( this.containsTerrain( Terrains.WOODS ) ) {
            maxFeature = 2;
        }

        // Account for buildings.
        if ( maxFeature < this.terrainLevel(Terrains.BLDG_ELEV) ) {
            maxFeature = this.terrainLevel(Terrains.BLDG_ELEV);
        }

        // Account for bridges.
        if ( maxFeature < this.terrainLevel(Terrains.BRIDGE_ELEV) ) {
            maxFeature = this.terrainLevel(Terrains.BRIDGE_ELEV);
        }

        return elevation + maxFeature;
    }

    /* (non-Javadoc)
     * @see megamek.common.IHex#surface()
     */
    public int surface() {
        return elevation;
    }

    /* (non-Javadoc)
     * @see megamek.common.IHex#floor()
     */
    public int floor() {
        return elevation - depth();
    }

    /* (non-Javadoc)
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

    /* (non-Javadoc)
     * @see megamek.common.IHex#contains(int)
     */
    public boolean containsTerrain(int type) {
        return getTerrain(type) != null;
    }

    /* (non-Javadoc)
     * @see megamek.common.IHex#contains(int, int)
     */
    public boolean containsTerrain(int type, int level) {
        ITerrain terrain = getTerrain(type);
        if (terrain != null) {
            return terrain.getLevel() == level;
        } else {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see megamek.common.IHex#hasPavement()
     */
    public boolean hasPavement() {
        return containsTerrain(Terrains.PAVEMENT)
        || containsTerrain(Terrains.ROAD)
        || containsTerrain(Terrains.BRIDGE);
    }

    /* (non-Javadoc)
     * @see megamek.common.IHex#levelOf(int)
     */
    public int terrainLevel(int type) {
        ITerrain terrain = getTerrain(type);
        if (terrain != null) {
            return terrain.getLevel();
        } else {
            return ITerrain.LEVEL_NONE;
        }
    }

    /* (non-Javadoc)
     * @see megamek.common.IHex#getTerrain(int)
     */
    public ITerrain getTerrain(int type) {
        return terrains[type];
    }

    /* (non-Javadoc)
     * @see megamek.common.IHex#addTerrain(megamek.common.Terrain)
     */
    public void addTerrain(ITerrain terrain) {
        terrains[terrain.getType()] = terrain;
    }

    /* (non-Javadoc)
     * @see megamek.common.IHex#removeTerrain(int)
     */
    public void removeTerrain(int type) {
        terrains[type] = null;
    }

    /* (non-Javadoc)
     * @see megamek.common.IHex#removeAllTerrains()
     */
    public void removeAllTerrains() {
        for (int i = 0; i < terrains.length; i++) {
            terrains[i] = null;
        }
    }

    /* (non-Javadoc)
     * @see megamek.common.IHex#terrainsPresent()
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

    /* (non-Javadoc)
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
}
