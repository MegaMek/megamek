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
    public static final int     FIRE            = 7;
    public static final int     SMOKE           = 8; 
    public static final int     SWAMP           = 9; 
    public static final int     BUILDING        = 10;
    public static final int     BLDG_CF         = 11;
    public static final int     BLDG_ELEV       = 12;
    public static final int     BLDG_BASEMENT   = 13;
    public static final int     BRIDGE          = 14;
    public static final int     BRIDGE_CF       = 15;
    public static final int     BRIDGE_ELEV     = 16;
    public static final int     FLUFF           = 17;
    public static final int     ARMS            = 18; //blown off arms for use as clubs, level = number of arms in that hex
    public static final int     LEGS            = 19; //blown off legs for use as clubs, level = number of legs in that hex
    
    private static final String[] names = {"none", "woods", "rough", "rubble",
    "water", "pavement", "road", "fire", "smoke", "swamp",
    "building", "bldg_cf", "bldg_elev", "bldg_basement", "bridge", "bridge_cf",
    "bridge_elev", "fluff", "arms", "legs"};
    
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
            this.level = levelFor(terrain.substring(firstColon + 1));
            this.exitsSpecified = false;

            // Buildings *never* use implicit exits.
            if ( this.type == Terrain.BUILDING ) {
                this.exitsSpecified = true;
            }
        } else {
            this.level = levelFor(terrain.substring(firstColon + 1, lastColon));
            this.exitsSpecified = true;
            this.exits = levelFor(terrain.substring(lastColon + 1));
        }
    }
    
    public static int levelFor(String string) {
        if (string.equals("*")) {
            return WILDCARD;
        }
        else {
            return Integer.parseInt(string);
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
    
    /**
     * Flips the exits around the vertical axis (North-for-South) and/or
     * the horizontal axis (East-for-West).
     *
     * @param   horiz - a <code>boolean</code> value that, if <code>true</code>,
     *          indicates that the exits are being flipped North-for-South.
     * @param   vert - a <code>boolean</code> value that, if <code>true</code>,
     *          indicates that the exits are being flipped East-for-West.
     */
    public void flipExits( boolean horiz, boolean vert ) {
        // Do nothing if no flips are defined.
        if ( !horiz && !vert ) {
            return;
        }

        // Determine the new exits.
        int newExits = 0;

        // Is there a North exit?
        if ( 0 != (exits & 0x0001) ) {
            if ( vert ) {
                // Becomes South.
                newExits |= 0x08;
            }
        }
        // Is there a North-East exit?
        if ( 0 != (exits & 0x0002) ) {
            if ( vert && horiz ) {
                // Becomes South-West
                newExits |= 0x10;
            } else if ( horiz ) {
                // Becomes North-West.
                newExits |= 0x20;
            } else if ( vert ) {
                // Becomes South-East.
                newExits |= 0x04;
            }
        }
        // Is there a South-East exit?
        if ( 0 != (exits & 0x0004) ) {
            if ( vert && horiz ) {
                // Becomes North-West
                newExits |= 0x20;
            } else if ( horiz ) {
                // Becomes South-West.
                newExits |= 0x10;
            } else if ( vert ) {
                // Becomes North-East.
                newExits |= 0x02;
            }
        }
        // Is there a South exit?
        if ( 0 != (exits & 0x0008) ) {
            if ( vert ) {
                // Becomes North.
                newExits |= 0x01;
            }
        }
        // Is there a South-West exit?
        if ( 0 != (exits & 0x0010) ) {
            if ( vert && horiz ) {
                // Becomes North-East
                newExits |= 0x02;
            } else if ( horiz ) {
                // Becomes South-East.
                newExits |= 0x04;
            } else if ( vert ) {
                // Becomes North-West.
                newExits |= 0x20;
            }
        }
        // Is there a North-West exit?
        if ( 0 != (exits & 0x0020) ) {
            if ( vert && horiz ) {
                // Becomes South-East
                newExits |= 0x04;
            } else if ( horiz ) {
                // Becomes North-East.
                newExits |= 0x02;
            } else if ( vert ) {
                // Becomes South-West.
                newExits |= 0x10;
            }
        }

        // Update the exits.
        this.setExits( newExits );

    } // End public void flipExits( boolean, boolean )

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
