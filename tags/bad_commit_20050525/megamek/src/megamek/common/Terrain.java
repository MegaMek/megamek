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
public class Terrain implements ITerrain, Serializable {

    private final int type;
    private int level;
    private boolean exitsSpecified = false;
    private int exits;
    
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
    
    public Terrain(ITerrain other) {
        this.type = other.getType();
        this.level = other.getLevel();
        this.exitsSpecified = other.hasExitsSpecified();
        this.exits = other.getExits();
    }
    
    /**
     * Parses a string containing terrain info into the actual terrain
     */
    public Terrain(String terrain) {
        // should have at least one colon, maybe two
        int firstColon = terrain.indexOf(':');
        int lastColon = terrain.lastIndexOf(':');
        String name = terrain.substring(0, firstColon);
        
        this.type = Terrains.getType(name);
        if (firstColon == lastColon) {
            this.level = levelFor(terrain.substring(firstColon + 1));
            this.exitsSpecified = false;

            // Buildings *never* use implicit exits.
            if ( this.type == Terrains.BUILDING ) {
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
    public void flipExits(boolean horiz, boolean vert) {
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

    }
    
    /**
     * Returns true if the terrain in this hex exits to the terrain in 
     * the other hex.
     */
    public boolean exitsTo(ITerrain other) {
        if (other == null) {
            return false;
        }
        return this.type == other.getType() && this.level == other.getLevel();
    }
    
    /**
     * Terrains are equal if their types and levels are equal.  Does not pay
     * attention to exits.
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || !(object instanceof ITerrain)) {
            return false;
        }
        ITerrain other = (ITerrain)object;
        return this.type == other.getType() && this.level == other.getLevel();
    }
    
    public String toString() {
        return Terrains.getName(type) + ":" + level + (exitsSpecified ? ":" + exits : "");
    }
}
