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

import java.io.*;
import java.awt.*;

/**
 * The type of terrain a hex represents.
 */
public class Terrain
  implements Serializable
{
    public static final int        TERRAIN_FIRST    = 0;
    public static final int        TERRAIN_LAST    = 12;
  
    public static final int        PLAINS            = 0;
    public static final int        FOREST_LITE        = 1;
    public static final int        FOREST_HVY        = 2;
    public static final int        WATER            = 3;
    public static final int        ROUGH            = 4;
    public static final int        FERROCRETE        = 5;
    public static final int        BUILDING_LITE    = 6;
    public static final int        BUILDING_MED    = 7;
    public static final int        BUILDING_HVY    = 8;
    public static final int        BUILDING_HARD    = 9;
    public static final int        ICE                = 10;
    public static final int        ROAD            = 11;
    public static final int        RUBBLE            = 12;
    
    public static final String[] TERRAIN_NAMES    = 
    {"PLAINS", "FOREST_LITE", "FOREST_HVY", "WATER",
     "ROUGH", "FERROCRETE", "BUILDING_LITE", "BUILDING_MED",
     "BUILDING_HVY", "BUILDING_HARD", "ICE", "ROAD", "RUBBLE"};

  public String name;
  int type;
  String picfile;
  
  transient Image pic;
  
  /**
   * Terrain constructor
   */
  public Terrain(String name, int type, String picfile) {
    this.name = name;
    this.type = type;
    this.picfile = picfile;
  }
  
  /**
   * Returns the name of this terrain type
   */
  public String getName() {
    return name;
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
        if(pic == null) {
      String fileWithPath = picfile;
      if(fileWithPath.indexOf("hexes/") == -1) {
        fileWithPath = "hexes/" + fileWithPath;
      }
      if(fileWithPath.indexOf("data/") == -1) {
        fileWithPath = "data/" + fileWithPath;
      }
            pic = comp.getToolkit().getImage(fileWithPath);
        }
        return pic;
    }
    
    /**
     * Check through the TERRAIN_NAMES for a match to the
     * specified string.
     * 
     * @return the terrain type if found; -1 if not;
     * 
     * @param s                the string.
     */
    public static int parse(String s) {
        for(int i = TERRAIN_FIRST; i <= TERRAIN_LAST; i++) {
            if(TERRAIN_NAMES[i].equalsIgnoreCase(s)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Terrains are equal if their names are equal.
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Terrain other = (Terrain)object;
        return other.getName().equals(name);
    }
}
