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

import java.awt.*;
import java.io.*;

/**
 * Hex represents a single hex on the board.  Right now, it just
 * has two parameters: terrain and elevation.  Eventually, it will have
 */
public class Hex 
  implements Serializable
{
    private Terrain      terrain;
    private int                    elevation;
    
    /**
     * Constructs a hex with all parameters
     */
    public Hex(Terrain terrain, int elevation) {
        this.terrain = terrain;
        this.elevation = elevation;
    }
    
    /**
     * Constructs a new hex, using the parameters of another
     * hex.
     * 
     * @param hex            the hex to use.
     */
    public Hex(Hex hex) {
        this(hex.terrain, hex.elevation);
    }
  
    public int getTerrainType() {
        return terrain.type;
    }
  
    public Terrain getTerrain() {
        return terrain;
    }
  
    public void setTerrain(Terrain terrain) {
        this.terrain = terrain;
    }
  
    public int getElevation() {
        return elevation;
    }
  
    public void setElevation(int elevation) {
        this.elevation = elevation;
    }

    /**
     * Checks whether the images has been initialized; if
     * so, returns it; if not, uses the component's toolkit
     * to load the image.
     * 
     * @return the image for the hex.
     * 
     * @param comp            the component where the picture
     *                        will be displayed.
     */
    public Image getImage(Component comp) {
        return terrain.getImage(comp);
    }
    
    /**
     * Check through the TERRAIN_NAMES for a match to the
     * specified string.
     * 
     * @return the terrain variable if found; -1 if not;
     * 
     * @param s                the string.
     */
    public static int parse(String s) {
        return Terrain.parse(s);
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
        return other.getTerrain().equals(this.terrain) 
               && other.getElevation() == elevation;
    }
}

