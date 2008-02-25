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

public class TerrainFactory implements ITerrainFactory {

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.ITerrainFactory#createTerrain(int, int)
     */
    public ITerrain createTerrain(int type, int level) {
        return new Terrain(type, level);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.ITerrainFactory#createTerrain(int, int, boolean, int)
     */
    public ITerrain createTerrain(int type, int level, boolean exitsSpecified,
            int exits) {
        return new Terrain(type, level, exitsSpecified, exits);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.ITerrainFactory#createTerrain(java.lang.String)
     */
    public ITerrain createTerrain(String terrain) {
        return new Terrain(terrain);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.ITerrainFactory#createTerrain(megamek.common.ITerrain)
     */
    public ITerrain createTerrain(ITerrain other) {
        return new Terrain(other);
    }

}
