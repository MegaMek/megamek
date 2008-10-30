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

    //base terrain types
    public static final int WOODS      = 1; //1: light 2: heavy 3: ultra
    public static final int WATER      = 2; //level = depth  
    public static final int ROUGH      = 3; //1: normal 2: ultra
    public static final int RUBBLE     = 4; //1: light bldg 2: medium bldg 3: heavy bldg 4: hardened bldg 5: wall 6: ultra  
    public static final int JUNGLE     = 5; //1: light 2: heavy 3: ultra
    public static final int SAND       = 6;
    public static final int TUNDRA     = 7;
    public static final int MAGMA      = 8; // 1: crust 2: liquid
    public static final int FIELDS     = 9;
    public static final int INDUSTRIAL = 10; //level indicates height
    public static final int SPACE      = 11;
    //unimplemented
    //Level 1 Foliage
    //Sheer Cliffs
    
    //Terrain modifications
    public static final int PAVEMENT = 12;
    public static final int ROAD     = 13;
    public static final int SWAMP    = 14; //1: normal 2: just became quicksand 3: quicksand
    public static final int MUD      = 15;
    public static final int RAPIDS   = 16; //1: rapids 2: torrent
    public static final int ICE      = 17;
    public static final int SNOW     = 18; // 1: thin 2: deep  
    public static final int FIRE     = 19; // 1: normal fire 2: inferno fire
    public static final int SMOKE    = 20;
    public static final int GEYSER   = 21; // 1: dormant 2: active 3: magma vent
    //unimplemented
    //Black Ice
    //Bug Storm
    //Extreme Depths
    //Hazardous Liquid Pools
    //Rail
    //Dirt Roads, Gravel Roads
    
    //Building stuff
    public static final int BUILDING       = 22; // 1: light 2: medium 3: heavy 4: hardened 5: wall
    public static final int BLDG_CF        = 23;
    public static final int BLDG_ELEV      = 24;
    public static final int BLDG_BASEMENT  = 25;
    public static final int BRIDGE         = 26;
    public static final int BRIDGE_CF      = 27;
    public static final int BRIDGE_ELEV    = 28;
    public static final int FUEL_TANK      = 29;
    public static final int FUEL_TANK_CF   = 30;
    public static final int FUEL_TANK_ELEV = 31;
    public static final int FUEL_TANK_MAGN = 32;
    //unimplemented
    //building types

    // special types
    public static final int IMPASSABLE = 33;
    public static final int ELEVATOR   = 34; // level=elevation it moves to,exits=d6 rolls it moves on
    public static final int FORTIFIED  = 35;
    public static final int SCREEN     = 36;
    
    //fluff
    public static final int FLUFF = 37;
    public static final int ARMS  = 38; // blown off arms for use as clubs, level = number of arms in that hex
    public static final int LEGS  = 39; // blown off legs for use as clubs, level = number of legs in that hex

    private static final String[] names = { "none", "woods", "water", "rough", 
        "rubble", "jungle", "sand", "tundra", "magma", "planted_fields",
        "heavy_industrial", "space",
        "pavement", "road", "swamp", "mud", "rapids", "ice", "snow", 
        "fire", "smoke", "geyser", 
        "building", "bldg_cf", "bldg_elev", "bldg_basement", "bridge", "bridge_cf",
        "bridge_elev", "fuel_tank", "fuel_tank_cf", "fuel_tank_elev", "fuel_tank_magn", 
        "impassable", "elevator", "fortified", "screen",
        "fluff", "arms", "legs" };

    public static final int SIZE = names.length;

    private static Hashtable<String, Integer> hash;

    private static ITerrainFactory factory;

    /**
     * @param type
     * @return
     */
    public static String getName(int type) {
        return names[type];
    }

    /**
     * 
     * @param type
     * @param level
     * @return a displayable name for this terrain (for tooltips)
     */
    public static String getDisplayName(int type, int level) {   
        switch(type) {
            case(WOODS):
                if(level == 1) {
                    return "light woods";
                }
                if(level == 2) {
                    return "heavy woods";
                }
                if(level == 3) {
                    return "ultra-heavy woods";
                }
                return "woods (unknown)";
            case(ROUGH):
                if(level == 1) {
                    return "rough";
                }
                if(level == 2) {
                    return "ultra rough";
                }
                return "rough (unknown)";
            case(RUBBLE):
                if(level < 6) {
                    return "rubble";
                }
                if(level > 5) {
                    return "ultra rubble";
                }
                return "rubble (unknown)";
            case(WATER):
                return "water (depth " + level + ")";
            case(PAVEMENT):
                return "pavement";
            case(ROAD):
                return "road";
            case(FIRE):
                if(level == 1) {
                    return "fire";
                }
                if(level == 2) {
                    return "inferno fire";
                }
                return "fire (unknown)";
            case(SMOKE):
                if(level == 1) {
                    return "light smoke";
                }
                if(level == 2) {
                    return "heavy smoke";
                }
                return "smoke (unknown)";
            case(SWAMP):
                if(level == 1) {
                    return "swamp";
                }
                if(level == 2 || level == 3) {
                    return "quicksand";
                }
                return "swamp";
            case(ICE):
                return "ice";
            case(FORTIFIED):
                return "improved position";
            case(GEYSER):
                if(level == 1) {
                    return "dormant";
                }
                if(level == 2) {
                    return "active";
                }
                if(level == 3) {
                    return "magma vent";
                }
                return "geyser (unknown)";
            case(JUNGLE):
                if(level == 1) {
                    return "light jungle";
                }
                if(level == 2) {
                    return "heavy jungle";
                }
                if(level == 3) {
                    return "ultra-heavy jungle";
                }
                return "jungle (unknown)";
            case(MAGMA):
                if(level == 1) {
                    return "magma crust";
                }
                if(level == 2) {
                    return "magma liquid";
                }
                return "magma (unknown)";
            case(MUD):
                return "mud";
            case(RAPIDS):
                if(level == 1) {
                    return "rapids";
                }
                if(level == 2) {
                    return "torrent";
                }
                return "rapids (unknown)";
            case(SAND):
                return "sand";
            case(SNOW):
                if(level == 1) {
                    return "thin snow";
                }
                if(level == 2) {
                    return "heavy snow";
                }
                return "snow (unknown)";
            case(TUNDRA):
                return "tundra";
            case(SPACE):
                return "space";
            case(SCREEN):
                return "screen";
            case(FIELDS):
                return "planted fields";
            case(INDUSTRIAL):
                return "heavy industrial zone (height " + level + ")";
            case(IMPASSABLE):
                return "impassable terrain";
            case(ELEVATOR):
                return "elevator";
            default:
                return null;
        }
        
    }
    
    /**
     * This function converts the name of a terrain into the constant.
     * 
     * @param name the name of the terain (from the names list.
     * @return an integer coresponding to the terain, or 0 if no match (terrain
     *         none)
     */
    public static int getType(String name) {
        Object o = getHash().get(name);
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }
        return 0;
    }

    public static ITerrainFactory getTerrainFactory() {
        if (factory == null) {
            factory = new TerrainFactory();
        }
        return factory;
    }

    protected static Hashtable<String, Integer> getHash() {
        if (hash == null) {
            hash = new Hashtable<String, Integer>(SIZE);
            for (int i = 0; i < names.length; i++) {
                hash.put(names[i], new Integer(i));
            }
        }
        return hash;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.ITerrainFactory#createTerrain(int, int)
     */
    public ITerrain createTerrain(int type, int level) {
        return getTerrainFactory().createTerrain(type, level);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.ITerrainFactory#createTerrain(int, int, boolean, int)
     */
    public ITerrain createTerrain(int type, int level, boolean exitsSpecified,
            int exits) {
        return getTerrainFactory().createTerrain(type, level, exitsSpecified,
                exits);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.ITerrainFactory#createTerrain(java.lang.String)
     */
    public ITerrain createTerrain(String terrain) {
        return getTerrainFactory().createTerrain(terrain);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.ITerrainFactory#createTerrain(megamek.common.ITerrain)
     */
    public ITerrain createTerrain(ITerrain other) {
        return getTerrainFactory().createTerrain(other);
    }
    
    /**
     * 
     * @param level
     * @return the terrain factor for the given type and level - pg. 64, TacOps
     */
    public static int getTerrainFactor(int type, int level) {
        switch(type) {
        case(WOODS):
            if(level == 1) {
                return 50;
            }
            if(level == 2) {
                return 90;
            }
            if(level == 3) {
                return 130;
            }
            return 50;
        case(ROUGH):
            return 200;
        case(PAVEMENT):
            return 200;
        case(ROAD):
            return 150;
        case(ICE):
            return 40;
        case(JUNGLE):
            if(level == 1) {
                return 50;
            }
            if(level == 2) {
                return 90;
            }
            if(level == 3) {
                return 130;
            }
            return 50;
        case(MAGMA):
            if(level == 1) {
                return 30;
            }
            return 0;
        case(SAND):
            return 100;
        case(SNOW):
            if(level == 1) {
                return 15;
            }
            if(level == 2) {
                return 30;
            }
            return 15;
        case(TUNDRA):
            return 70;
        case(FIELDS):
            return 30;
        default:
            return 0;
        }
    }
}
