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

import java.util.Hashtable;

public class Terrains implements ITerrainFactory {

    public static final int WOODS = 1;
    public static final int ROUGH = 2;
    public static final int RUBBLE = 3;
    public static final int WATER = 4;
    public static final int PAVEMENT = 5;
    public static final int ROAD = 6;
    public static final int FIRE = 7;
    public static final int SMOKE = 8; 
    public static final int SWAMP = 9; 
    public static final int BUILDING = 10;
    public static final int BLDG_CF = 11;
    public static final int BLDG_ELEV = 12;
    public static final int BLDG_BASEMENT = 13;
    public static final int BRIDGE = 14;
    public static final int BRIDGE_CF = 15;
    public static final int BRIDGE_ELEV = 16;
    public static final int FLUFF = 17;
    public static final int ARMS = 18; //blown off arms for use as clubs, level = number of arms in that hex
    public static final int LEGS = 19; //blown off legs for use as clubs, level = number of legs in that hex
    public static final int ICE = 20;
    //level 3 terrain types (MaxTech)
    public static final int FORTIFIED = 21;
    public static final int GEYSER = 22; //1: dormant 2: active 3: magma vent
    public static final int JUNGLE = 23; //levels 1,2,3 
    public static final int MAGMA = 24; //1: crust 2: liquid
    public static final int MUD = 25; //1: can't bog down 2: can bog down
    public static final int RAPIDS = 26;
    public static final int SAND = 27;
    public static final int SNOW = 28;
    public static final int TUNDRA = 29;
    //special types
    public static final int IMPASSABLE = 30;
    public static final int ELEVATOR = 31; //level=elevation it moves to, exits=d6 rolls it moves on
    public static final int FUEL_TANK = 32;
    public static final int FUEL_TANK_CF = 33;
    public static final int FUEL_TANK_ELEV = 34;
    public static final int FUEL_TANK_MAGN = 35;

    private static final String[] names = {"none", "woods", "rough", "rubble",
            "water", "pavement", "road", "fire", "smoke", "swamp",
            "building", "bldg_cf", "bldg_elev", "bldg_basement", "bridge", "bridge_cf",
            "bridge_elev", "fluff", "arms", "legs", "ice", "fortified", "geyser",
            "jungle", "magma", "mud", "rapids", "sand", "snow", "tundra", "impassable", "elevator",
            "fuel_tank", "fuel_tank_cf", "fuel_tank_elev", "fuel_tank_magn"};
    
    public static final int SIZE = names.length;

    private static Hashtable hash;
    
    private static ITerrainFactory factory;

    public static String getName(int type) {
        return names[type];
    }

    public static int getType(String name) {
        Object o = getHash().get(name);
        if (o instanceof Integer) {
            return ((Integer)o).intValue();
        }
        return 0;
    }
    
    
    public static ITerrainFactory getTerrainFactory() {
        if (factory==null) {
            factory = new TerrainFactory();
        }
        return factory;
    }
    
    protected static Hashtable getHash() {
        if (hash == null) {
            hash=new Hashtable(SIZE);
            for(int i=0; i<names.length; i++) {
                hash.put(names[i], new Integer(i));
            }            
        }
        return hash;
    }

    /* (non-Javadoc)
     * @see megamek.common.ITerrainFactory#createTerrain(int, int)
     */
    public ITerrain createTerrain(int type, int level) {
        return getTerrainFactory().createTerrain(type, level);
    }

    /* (non-Javadoc)
     * @see megamek.common.ITerrainFactory#createTerrain(int, int, boolean, int)
     */
    public ITerrain createTerrain(int type, int level, boolean exitsSpecified, int exits) {
        return getTerrainFactory().createTerrain(type, level, exitsSpecified, exits);
    }

    /* (non-Javadoc)
     * @see megamek.common.ITerrainFactory#createTerrain(java.lang.String)
     */
    public ITerrain createTerrain(String terrain) {
        return getTerrainFactory().createTerrain(terrain);
    }

    /* (non-Javadoc)
     * @see megamek.common.ITerrainFactory#createTerrain(megamek.common.ITerrain)
     */
    public ITerrain createTerrain(ITerrain other) {
        return getTerrainFactory().createTerrain(other);
    }
}
