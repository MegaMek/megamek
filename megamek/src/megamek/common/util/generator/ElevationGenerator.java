/*
 * MegaMek - Copyright (C) 2000-2016 Ben Mazur (bmazur@sev.org)
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
package megamek.common.util.generator;

public interface ElevationGenerator {
    /** @return translatable string for the generator name */
    String getName();
    
    /** @return translatable string for the tooltip / description */
    String getTooltip();
    
    /**
     * Generate a map of given width and height and put it into the supplied elevation map
     * 
     * @param hilliness 1-100
     * @param width width of the map, in hexes
     * @param height height of the map, in hexes
     * @param elevationMap the target elevation map, indexed as <tt>elevationMap[width][height]</tt>
     */
    void generate(int hilliness, int width, int height, int elevationMap[][]);
}
