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

import java.io.*;
import java.awt.*;

/**
 * Represents a single type of terrain or condition in a hex.  The type of a
 * terrain is immutable, once created, but the level and exits are changable.
 *
 * Each type of terrain should only be represented once in a hex.
 *
 * @author Ben
 */
public class Terrain
implements Serializable {
    
    public static final int     LEVEL_NONE      = Integer.MIN_VALUE;
    public static final int     WILDCARD        = Integer.MAX_VALUE;
    
    public static final int     WOODS           = 1;
    public static final int     ROUGH           = 2;
    public static final int     RUBBLE          = 3;
    public static final int     WATER           = 4;
    public static final int     PAVEMENT        = 5;
    public static final int     ROAD            = 6;
    public static final int     RIVER           = 7;
    public static final int     FIRE            = 8;
    public static final int     SMOKE           = 9; 
    public static final int     SWAMP           = 10;
    public static final int     BUILDING        = 11;
    public static final int     BLDG_CF         = 12;
    public static final int     BLDG_ELEV       = 13;
    public static final int     BLDG_BASEMENT   = 14;
    public static final int     BRIDGE          = 15;
    public static final int     BRIDGE_CF       = 16;
    public static final int     BRIDGE_ELEV     = 17;
    
    private static final String[] names = {"none", "woods", "rough", "rubble",
    "water", "pavement", "road", "river", "fire", "smoke", "swamp",
    "building", "bldg_cf", "bldg_elev", "bldg_basement", "bridge", "bridge_cf",
    "bridge_elev"};
    
    public static final int     SIZE            = names.length;
    
    private final int           type;
    private int                 level;
    private boolean             exitsSpecified = false;
    private int                 exits;
    
    /**
     * Terrain constructor
     */
    public Terrain(int type, int level) {
        this(type, level, false, 0);
    }
    
    public Terrain(int type, int level, boolean exitsSpecified, int exits) {
        this.type = type;
        this.level = level;
        this.exitsSpecified = exitsSpecified;
        this.exits = exits;
    }
    
    public Terrain(Terrain other) {
        this.type = other.type;
        this.level = other.level;
        this.exitsSpecified = other.exitsSpecified;
        this.exits = other.exits;
    }
    
    /**
     * Parses a string containing terrain info into the actual terrain
     */
    public Terrain(String terrain) {
        // should have at least one colon, maybe two
        int firstColon = terrain.indexOf(':');
        int lastColon = terrain.lastIndexOf(':');
        String name = terrain.substring(0, firstColon);
        
        this.type = parse(name);
        if (firstColon == lastColon) {
            this.level = Integer.parseInt(terrain.substring(firstColon + 1));
            this.exitsSpecified = false;
        } else {
            this.level = Integer.parseInt(terrain.substring(firstColon + 1, lastColon));
            this.exitsSpecified = true;
            this.exits = Integer.parseInt(terrain.substring(lastColon + 1));
        }
    }
    
    public int getType() {
        return type;
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getExits() {
        return exits;
    }
    
    public boolean hasExitsSpecified() {
        return exitsSpecified;
    }
    
    public void setExits(int exits) {
        this.exits = exits;
    }
    
    public void setExit(int direction, boolean connection) {
        int mask = (int)Math.pow(2, direction);
        if (connection) {
            exits |= mask;
        } else {
            exits &= (63 ^ mask);
        }
    }
    
    public static int parse(String name) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) {
                return i;
            }
        }
        return 0;
    }
    
    public static String getName(int type) {
        return names[type];
    }
    
    /**
     * Returns true if the terrain in this hex exits to the terrain in 
     * the other hex.
     */
    public boolean exitsTo(Terrain other) {
        if (other == null) {
            return false;
        }
        return this.type == other.type && this.level == other.level;
    }
    
    /**
     * Terrains are equal if their types and levels are equal.  Does not pay
     * attention to exits.
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Terrain other = (Terrain)object;
        return this.type == other.type && this.level == other.level;
    }
    
    public String toString() {
        return names[type] + ":" + level + (exitsSpecified ? ":" + exits : "");
    }
}
