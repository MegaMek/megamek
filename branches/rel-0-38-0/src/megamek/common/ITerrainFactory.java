/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

public interface ITerrainFactory {

    /**
     * Create terrain of cpecified type and level
     * 
     * @param type terrain type
     * @param level level
     * @return new terrain
     */
    public abstract ITerrain createTerrain(int type, int level);

    /**
     * Create terrain of cpecified type, level , exitsSpecified flag and exits,
     * 
     * @param type
     * @param level
     * @param exitsSpecified
     * @param exits
     * @return new terrain
     */
    public abstract ITerrain createTerrain(int type, int level,
            boolean exitsSpecified, int exits);

    /**
     * Create Terrain using string containing terrain info TODO I think this
     * shoul be removed. It's too much implementation specfic
     * 
     * @param terrain
     * @return new terrain
     */
    public abstract ITerrain createTerrain(String terrain);

    /**
     * Create terrain as copy of other
     * 
     * @param other
     * @return new terrain
     */
    public abstract ITerrain createTerrain(ITerrain other);

}
