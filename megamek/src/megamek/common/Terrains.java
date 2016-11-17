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

import megamek.server.SmokeCloud;

public class Terrains implements ITerrainFactory {

    // base terrain types
    public static final int WOODS = 1; // 1: light 2: heavy 3: ultra
    public static final int WATER = 2; // level = depth
    public static final int ROUGH = 3; // 1: normal 2: ultra
    public static final int RUBBLE = 4; // 1: light bldg 2: medium bldg 3: heavy
                                        // bldg 4: hardened bldg 5: wall 6:
                                        // ultra
    public static final int JUNGLE = 5; // 1: light 2: heavy 3: ultra
    public static final int SAND = 6;
    public static final int TUNDRA = 7;
    public static final int MAGMA = 8; // 1: crust 2: liquid
    public static final int FIELDS = 9;
    public static final int INDUSTRIAL = 10; // level indicates height
    public static final int SPACE = 11;
    // unimplemented
    // Level 1 Foliage
    // Sheer Cliffs

    // Terrain modifications
    public static final int PAVEMENT = 12;
    public static final int ROAD = 13;
    public static final int SWAMP = 14; // 1: normal 2: just became quicksand 3:
                                        // quicksand
    public static final int MUD = 15;
    public static final int RAPIDS = 16; // 1: rapids 2: torrent
    public static final int ICE = 17;
    public static final int SNOW = 18; // 1: thin 2: deep
    public static final int FIRE = 19; // 1: normal, fire 2: inferno fire, 3:
                                       // inferno bombs, 4: inferno IV
    public static final int SMOKE = 20; // 1: light smoke 2: heavy smoke 3:light
                                        // LI smoke 4: Heavy LI smoke
    public static final int GEYSER = 21; // 1: dormant 2: active 3: magma vent
    // unimplemented
    // Black Ice
    // Bug Storm
    // Extreme Depths
    // Hazardous Liquid Pools
    // Rail
    // Dirt Roads, Gravel Roads
    // Water Flow

    public static final int FIRE_LVL_NORMAL = 1;
    public static final int FIRE_LVL_INFERNO = 2;
    public static final int FIRE_LVL_INFERNO_BOMB = 3;
    public static final int FIRE_LVL_INFERNO_IV = 4;

    // Building stuff
    public static final int BUILDING = 22; // 1: light 2: medium 3: heavy 4:
                                           // hardened 5: wall
    public static final int BLDG_CF = 23;
    public static final int BLDG_ELEV = 24;
    public static final int BLDG_BASEMENT_TYPE = 25; // level equals
                                                     // BasemenType, one of the
                                                     // values of the
                                                     // BasementType enum
    public static final int BLDG_CLASS = 26; // 1: hangars 2: fortresses 3: gun
                                             // emplacements
    public static final int BLDG_ARMOR = 27;
    // leaving this empty will be interpreted as standard
    public static final int BRIDGE = 28;
    public static final int BRIDGE_CF = 29;
    public static final int BRIDGE_ELEV = 30;
    public static final int FUEL_TANK = 31;
    public static final int FUEL_TANK_CF = 32;
    public static final int FUEL_TANK_ELEV = 33;
    public static final int FUEL_TANK_MAGN = 34;

    // special types
    public static final int IMPASSABLE = 35;
    public static final int ELEVATOR = 36; // level=elevation it moves
                                           // to,exits=d6 rolls it moves on
    public static final int FORTIFIED = 37;
    public static final int SCREEN = 38;

    // fluff
    public static final int FLUFF = 39;
    public static final int ARMS = 40; // blown off arms for use as clubs, level
                                       // = number of arms in that hex
    public static final int LEGS = 41; // blown off legs for use as clubs, level
                                       // = number of legs in that hex

    public static final int METAL_CONTENT = 42; // Is there metal content that
                                                // will block magscan sensors?
    public static final int BLDG_BASE_COLLAPSED = 43; // 1 means collapsed

    /**
     * Keeps track of the different type of terrains that can have exits.
     */
    public static final int[] exitableTerrains = { PAVEMENT, ROAD, BUILDING, FUEL_TANK, BRIDGE };

    private static final String[] names = { "none", "woods", "water", "rough", "rubble", "jungle", "sand", "tundra",
            "magma", "planted_fields", "heavy_industrial", "space", "pavement", "road", "swamp", "mud", "rapids", "ice",
            "snow", "fire", "smoke", "geyser", "building", "bldg_cf", "bldg_elev", "bldg_basement_type", "bldg_class",
            "bldg_armor", "bridge", "bridge_cf", "bridge_elev", "fuel_tank", "fuel_tank_cf", "fuel_tank_elev",
            "fuel_tank_magn", "impassable", "elevator", "fortified", "screen", "fluff", "arms", "legs", "metal_deposit",
            "bldg_base_collapsed" };

    public static final int SIZE = names.length;

    private static Hashtable<String, Integer> hash;

    private static ITerrainFactory factory;

    /**
     * Checks to see if the given terrain type can have exits.
     * 
     * @param terrType
     *            The terrain type to test
     * @return True if the input terrain type can have exits, else false.
     */
    public static boolean exitableTerrain(int terrType) {
        boolean exitableTerrainType = false;
        for (int i = 0; i < Terrains.exitableTerrains.length; i++) {
            exitableTerrainType |= terrType == Terrains.exitableTerrains[i];
        }
        return exitableTerrainType;
    }

    /**
     * @param type
     * @return
     */
    public static String getName(int type) {
        return names[type];
    }

    /**
     * @param type
     * @param level
     * @return a displayable name for this terrain (for tooltips)
     */

    public static String getDisplayName(int type, int level) {
        switch (type) {
        case (WOODS):
            if (level == 1) {
                return "Light woods";
            }
            if (level == 2) {
                return "Heavy woods";
            }
            if (level == 3) {
                return "Ultra-heavy woods";
            }
            return "Woods (unknown)";
        case (ROUGH):
            if (level == 1) {
                return "Rough";
            }
            if (level == 2) {
                return "Ultra rough";
            }
            return "Rough (unknown)";
        case (RUBBLE):
            if (level < 6) {
                return "Rubble";
            }
            if (level > 5) {
                return "Ultra rubble";
            }
            return "Rubble (unknown)";
        case (WATER):
            return "Water (depth " + level + ")";
        case (PAVEMENT):
            return "Pavement";
        case (ROAD):
            return "Road";
        case (FIRE):
            if (level == 1) {
                return "Fire";
            }
            if (level == 2 || level == 3 || level == 4) {
                return "Inferno fire";
            }
            return "Fire (unknown)";
        case (SMOKE):
            if (level == SmokeCloud.SMOKE_LIGHT) {
                return "Light smoke";
            }
            if (level == SmokeCloud.SMOKE_HEAVY) {
                return "Heavy smoke";
            }
            if (level == SmokeCloud.SMOKE_LI_LIGHT) {
                return "LASER inhibiting smoke";
            }
            if (level == SmokeCloud.SMOKE_LI_HEAVY) {
                return "LASER inhibiting smoke";
            }
            if (level == SmokeCloud.SMOKE_CHAFF_LIGHT) {
                return "Chaff (ECM)";
            }
            return "Smoke (unknown)";
        case (SWAMP):
            if (level == 1) {
                return "Swamp";
            }
            if ((level == 2) || (level == 3)) {
                return "Quicksand";
            }
            return "Swamp";
        case (ICE):
            return "Ice";
        case (FORTIFIED):
            return "Improved position";
        case (GEYSER):
            if (level == 1) {
                return "Dormant";
            }
            if (level == 2) {
                return "Active";
            }
            if (level == 3) {
                return "Magma vent";
            }
            return "Geyser (unknown)";
        case (JUNGLE):
            if (level == 1) {
                return "Light jungle";
            }
            if (level == 2) {
                return "Heavy jungle";
            }
            if (level == 3) {
                return "Ultra-heavy jungle";
            }
            return "Jungle (unknown)";
        case (MAGMA):
            if (level == 1) {
                return "Magma crust";
            }
            if (level == 2) {
                return "Magma liquid";
            }
            return "Magma (unknown)";
        case (MUD):
            return "Mud";
        case (RAPIDS):
            if (level == 1) {
                return "Rapids";
            }
            if (level == 2) {
                return "Torrent";
            }
            return "Rapids (unknown)";
        case (SAND):
            return "Sand";
        case (SNOW):
            if (level == 1) {
                return "Thin snow";
            }
            if (level == 2) {
                return "Heavy snow";
            }
            return "Snow (unknown)";
        case (TUNDRA):
            return "Tundra";
        case (SPACE):
            return "Space";
        case (SCREEN):
            return "Screen";
        case (FIELDS):
            return "Planted fields";
        case (INDUSTRIAL):
            return "Heavy industrial zone (height " + level + ")";
        case (IMPASSABLE):
            return "Impassable terrain";
        case (ELEVATOR):
            return "Elevator";
        case (METAL_CONTENT):
            if (level < 1) {
                return "No metal content";
            }
            if (level == 1) {
                return "Very low metal content";
            }
            if (level == 2) {
                return "Low metal content";
            }
            if ((level == 3) || (level == 4)) {
                return "Medium metal content";
            }
            if ((level == 5) || (level == 6)) {
                return "High metal content";
            }
            if ((level == 7) || (level == 8)) {
                return "Very high metal content";
            }
            return "Extremely high metal content";
        default:
            return null;
        }

    }

    /**
     * This function converts the name of a terrain into the constant.
     *
     * @param name
     *            the name of the terain (from the names list.
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
    public ITerrain createTerrain(int type, int level, boolean exitsSpecified, int exits) {
        return getTerrainFactory().createTerrain(type, level, exitsSpecified, exits);
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
     * @see
     * megamek.common.ITerrainFactory#createTerrain(megamek.common.ITerrain)
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
        switch (type) {
        case (WOODS):
            if (level == 1) {
                return 50;
            }
            if (level == 2) {
                return 90;
            }
            if (level == 3) {
                return 130;
            }
            return 50;
        case (ROUGH):
            return 200;
        case (PAVEMENT):
            return 200;
        case (ROAD):
            return 150;
        case (ICE):
            return 40;
        case (JUNGLE):
            if (level == 1) {
                return 50;
            }
            if (level == 2) {
                return 90;
            }
            if (level == 3) {
                return 130;
            }
            return 50;
        case (MAGMA):
            if (level == 1) {
                return 30;
            }
            return 0;
        case (SAND):
            return 100;
        case (SNOW):
            if (level == 1) {
                return 15;
            }
            if (level == 2) {
                return 30;
            }
            return 15;
        case (TUNDRA):
            return 70;
        case (FIELDS):
            return 30;
        /*
         * case(METAL_CONTENT): if(level < 1) { return 0; } return level;
         */
        default:
            return 0;
        }
    }

    /**
     * Returns the number of elevations or altitudes above the hex level a given
     * terrainType rises.
     * 
     * @param terrainType
     * @param inAtmosphere
     *            Flag that determines whether elevations or altitudes should be
     *            returned.
     * @return The number of altitudes or elevations the given terrain type
     *         rises above the hex level.
     */
    public static int getTerrainElevation(int terrainType, int terrainLevel, boolean inAtmosphere) {
        // Handle altitudes
        if (inAtmosphere) {
            switch (terrainType) {
            case WOODS:
            case JUNGLE:
                return 1;
            default:
                return 0;
            }
        }
        // Handle elevations
        switch (terrainType) {
        case INDUSTRIAL:
        case BLDG_ELEV:
        case BRIDGE_ELEV:
            return terrainLevel;
        case WOODS:
        case JUNGLE:
            if (terrainLevel > 2) {
                return 3;
            } else {
                return 2;
            }
        case FIELDS:
            return 1;
        default:
            return 0;
        }
    }

}
