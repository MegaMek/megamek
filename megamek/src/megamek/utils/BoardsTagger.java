/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.utils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.*;
import static megamek.common.Terrains.*;
import megamek.common.*;

/** 
 * Scans all boards and applies automated tags to them.
 * Examples: Woods (Auto), DenseUrban (Auto). Deletes its own tags (and only those) 
 * before applying them (again), so if the rules for applying tags are changed, 
 * they may be removed accordingly and will also not be applied twice.  
 * 
 * @author Simon (Juliez)
 *
 */
public class BoardsTagger {

    /** When true, will print some information to System.out. */
    private final static boolean DEBUG = false;

    /**
     * A suffix added to tags to distinguish them from manually added tags. The suffix
     * is also used to identify them for deleting tags that no longer apply to a board or
     * that got renamed. 
     * If the suffix is changed, this tool will not remove tags with the old suffix
     * before applying new ones, so those must be handled manually.
     */
    private static final String AUTO_SUFFIX = " (Auto)";

    public enum Tags {

        TAG_LIGHTFOREST("LightForest"),
        TAG_MEDFOREST("MediumForest"),
        TAG_DENSEFOREST("DenseForest"),
        TAG_WOODS("Woods"),
        TAG_JUNGLE("Jungle"),
        TAG_ROUGH("Rough"),
        TAG_MEDURBAN("MediumUrban"),
        TAG_LIGHTURBAN("LightUrban"),
        TAG_HEAVYURBAN("HeavyUrban"),
        TAG_SWAMP("Swamp"),
        TAG_HILLS("LowHills"),
        TAG_HIGHHILLS("HighHills"),
        TAG_ROADS("Roads"),
        TAG_FOLIAGE("Foliage"),
        TAG_LAVA("Lava"),
        TAG_CLIFFS("Cliffs"),
        TAG_RURAL("Rural"),
        TAG_FIELDS("Fields"),
        TAG_GRASS("GrassTheme"),
        TAG_DESERT("DesertTheme"),
        TAG_TROPICAL("TropicalTheme"),
        TAG_LUNAR("LunarTheme"),
        TAG_VOLCANIC("VolcanicTheme"),
        TAG_SNOWTHEME("SnowTheme"),
        TAG_MARS("MarsTheme"),
        TAG_OCEAN("Ocean"),
        TAG_WATER("Water"),
        TAG_ICE("IceTerrain"),
        TAG_FLAT("Flat"),
        TAG_SNOWTERRAIN("SnowTerrain");

        private String tagName;
        private static final Map<String, Tags> internalTagMap;

        static {
            internalTagMap = new HashMap<>();
            
            for (Tags tag : values()) {
                internalTagMap.put(tag.getName().replace(AUTO_SUFFIX, ""), tag);
            }
        }
        
        Tags(String name) {
            tagName = name + AUTO_SUFFIX;
        }
        
        public String getName() {
            return tagName;
        }
        
        public static Tags parse(String tag) {
            String noAutoTag = tag.replace(AUTO_SUFFIX, "");
            if (internalTagMap.containsKey(noAutoTag)) {
                return internalTagMap.get(noAutoTag);
            }
            
            return null;
        }
    }


    public static void main(String[] args) {
        try {
            File boardDir = Configuration.boardsDir();
            scanForBoards(boardDir);
        } catch (IOException e) {
            System.out.println("Something is not quite right.");
            e.printStackTrace();
            System.exit(64);
        }
        System.out.println("Finished.");
    }

    /** Recursively scans the supplied file/directory for any boards and auto-tags them. */
    private static void scanForBoards(File file) throws IOException {
        if (file.isDirectory()) {
            String[] fileList = file.list();
            for (String filename : fileList) {
                File filepath = new File(file, filename);
                if (filepath.isDirectory()) {
                    scanForBoards(new File(file, filename));
                } else {
                    tagBoard(filepath);
                }
            }
        } else {
            tagBoard(file);
        }
    }

    /**
     * Scans the board for the types and number of terrains present, deletes old automatic tags
     * and applies new automatic tags as approptiate.
     */
    private static void tagBoard(File boardFile) {
        // If this isn't a board, ignore it
        if (!boardFile.toString().endsWith(".board")) {
            return;
        }

        // Load the board
        Board board = new Board();
        try (InputStream is = new FileInputStream(boardFile)) {
            // Apply tags only to boards that are valid and fully functional
            StringBuffer errors = new StringBuffer();
            board.load(is, errors, true);
            if (errors.length() > 0) {
                System.out.println("Board has errors: " + boardFile);
                return;
            }
        } catch (Exception e) {
            System.out.println("Could not load board: " + boardFile);
            return;
        }

        // Count stuff in the hexes of the board
        int forest = 0;
        int woods = 0;
        int jungles = 0;
        int foliage = 0;
        int roads = 0;
        int roughs = 0;
        int forestHU = 0;
        int lavas = 0;
        int stdBuildings = 0;
        int lowBuildings = 0;
        int highBuildings = 0;
        int swamps = 0;
        int cliffsTO = 0;
        int highCliffs = 0;
        int fields = 0;
        int deserts = 0;
        int grass = 0;
        int tropical = 0;
        int lunar = 0;
        int mars = 0;
        int snowTheme = 0;
        int volcanic = 0;
        int nEdgeWater = 0;
        int sEdgeWater = 0;
        int eEdgeWater = 0;
        int wEdgeWater = 0;
        int weighedLevels = 0;
        int water = 0;
        int ice = 0;
        int snowTerrain = 0;
        
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Hex hex = board.getHex(x, y);
                forest += hex.containsAnyTerrainOf(WOODS, JUNGLE) ? 1 : 0;
                woods += hex.containsTerrain(WOODS) ? 1 : 0;
                jungles += hex.containsTerrain(JUNGLE) ? 1 : 0;
                forestHU += hex.containsTerrain(WOODS, 2) ? 1 : 0;
                forestHU += hex.containsTerrain(WOODS, 3) ? 1 : 0;
                forestHU += hex.containsTerrain(JUNGLE, 2) ? 1 : 0;
                forestHU += hex.containsTerrain(JUNGLE, 3) ? 1 : 0;
                foliage += hex.containsTerrain(FOLIAGE_ELEV, 1) ? 1 : 0;
                roads += hex.containsAnyTerrainOf(ROAD, BRIDGE) ? 1 : 0;
                roughs += hex.containsTerrain(ROUGH) ? 1 : 0;
                swamps += hex.containsTerrain(SWAMP) ? 1 : 0;
                lavas += hex.containsTerrain(MAGMA) ? 1 : 0;
                snowTerrain += hex.containsTerrain(SNOW) ? 1 : 0;
                ice += hex.containsTerrain(ICE) ? 1 : 0;
                cliffsTO += hex.containsTerrain(CLIFF_TOP) ? 1 : 0;
                highCliffs += hex.containsTerrain(INCLINE_HIGH_TOP) ? 1 : 0;
                fields += hex.containsTerrain(FIELDS) ? 1 : 0;
                if (hex.getTheme() != null && !hex.containsTerrain(WATER)) {
                    deserts += hex.getTheme().equalsIgnoreCase("desert") ? 1 : 0;
                    lunar += hex.getTheme().equalsIgnoreCase("lunar") ? 1 : 0;
                    grass += hex.getTheme().equalsIgnoreCase("grass") ? 1 : 0;
                    grass += hex.getTheme().equalsIgnoreCase("") ? 1 : 0;
                    tropical += hex.getTheme().equalsIgnoreCase("tropical") ? 1 : 0;
                    mars += hex.getTheme().equalsIgnoreCase("mars") ? 1 : 0;
                    snowTheme += hex.getTheme().equalsIgnoreCase("snow") ? 1 : 0;
                    volcanic += hex.getTheme().equalsIgnoreCase("volcanic") ? 1 : 0;
                } else {
                    grass++;
                }
                wEdgeWater += ((x == 0) && hex.containsTerrain(WATER)) ? 1 : 0;
                eEdgeWater += ((x == board.getWidth() - 1) && hex.containsTerrain(WATER)) ? 1 : 0;
                nEdgeWater += ((y == 0) && hex.containsTerrain(WATER)) ? 1 : 0;
                sEdgeWater += ((y == board.getHeight() - 1) && hex.containsTerrain(WATER)) ? 1 : 0;
                weighedLevels += Math.abs(hex.getLevel());
                water += hex.containsTerrain(WATER) ? 1 : 0;
                if (hex.containsTerrain(BUILDING) 
                        && (!hex.containsTerrain(BLDG_CLASS) || hex.terrainLevel(BLDG_CLASS) == Building.STANDARD)
                        && ((hex.terrainLevel(BUILDING) == Building.LIGHT) || (hex.terrainLevel(BUILDING) == Building.MEDIUM))) {
                    stdBuildings++;
                    int height = hex.terrainLevel(BLDG_ELEV); 
                    lowBuildings += (height <= 2) ? 1 : 0;
                    highBuildings += (height > 2) ? 1 : 0;
                }
            }
        }

        // The board area
        int area = board.getWidth() * board.getHeight();
        // The geometric mean of the board sides. Better for some comparisons than the area.
        int normSide = (int) Math.sqrt(area);
        int levelExtent = board.getMaxElevation() - board.getMinElevation();

        // Calculate which tags apply
        var matchingTags = new HashMap<Tags, Boolean>();
        
        matchingTags.put(Tags.TAG_MEDFOREST, (forest >= normSide * 5) && (forest < normSide * 10) && (forestHU < normSide * 2));
        matchingTags.put(Tags.TAG_LIGHTFOREST, (forest >= normSide * 2) && (forestHU < normSide) && (forest < normSide * 5));
        matchingTags.put(Tags.TAG_DENSEFOREST, (forest >= normSide * 10) && (forestHU > normSide * 2));
        matchingTags.put(Tags.TAG_WOODS, woods > forest / 2);
        matchingTags.put(Tags.TAG_JUNGLE,  jungles > forest / 2);
        matchingTags.put(Tags.TAG_ROADS, roads > 10);
        matchingTags.put(Tags.TAG_ROUGH, roughs > normSide / 2);
        matchingTags.put(Tags.TAG_FOLIAGE, foliage > 5);
        matchingTags.put(Tags.TAG_LAVA, lavas > 5);
        matchingTags.put(Tags.TAG_CLIFFS, (cliffsTO > 5) || (highCliffs > 20));
        matchingTags.put(Tags.TAG_FIELDS, fields > normSide * 5);
        matchingTags.put(Tags.TAG_SWAMP, swamps > normSide);
        matchingTags.put(Tags.TAG_DESERT, deserts > area / 2);
        matchingTags.put(Tags.TAG_GRASS, grass > area / 2);
        matchingTags.put(Tags.TAG_TROPICAL, tropical > area / 2);
        matchingTags.put(Tags.TAG_LUNAR, lunar > area / 2);
        matchingTags.put(Tags.TAG_MARS, mars > area / 2);
        matchingTags.put(Tags.TAG_VOLCANIC, volcanic > area / 2);
        matchingTags.put(Tags.TAG_SNOWTHEME, snowTheme > area / 2);
        matchingTags.put(Tags.TAG_OCEAN, nEdgeWater > (board.getWidth() * 9 / 10) 
                || sEdgeWater > (board.getWidth() * 9 / 10)
                || eEdgeWater > (board.getHeight() * 9 / 10)
                || wEdgeWater > (board.getHeight() * 9 / 10));
        matchingTags.put(Tags.TAG_HILLS, (levelExtent >= 2) && (levelExtent < 5) && (weighedLevels > normSide * 15));
        matchingTags.put(Tags.TAG_HIGHHILLS, (levelExtent >= 5) && (weighedLevels > normSide * 15));
        matchingTags.put(Tags.TAG_WATER, water > normSide / 3);
        matchingTags.put(Tags.TAG_ICE, ice > normSide / 3);
        boolean lightUrban = (lowBuildings > normSide) && (highBuildings < normSide / 3) && (lowBuildings < normSide * 2) && (roads > normSide / 3);
        matchingTags.put(Tags.TAG_LIGHTURBAN, lightUrban);
        matchingTags.put(Tags.TAG_MEDURBAN, !lightUrban && (stdBuildings >= normSide) 
                && (roads > normSide / 3) && (stdBuildings < normSide * 4));
        matchingTags.put(Tags.TAG_HEAVYURBAN, (stdBuildings >= normSide * 4) && (roads > normSide / 3));
        matchingTags.put(Tags.TAG_SNOWTERRAIN, (snowTerrain > normSide * 2));
        matchingTags.put(Tags.TAG_FLAT, (levelExtent <= 2) && (weighedLevels < normSide * 5));

        // Remove (see below) any auto tags that might be present so that auto tags that no longer apply
        // are not left in the board file.
        Set<String> toRemove = board.getTags().stream().filter(t -> t.contains(AUTO_SUFFIX)).collect(toSet());

        // Find any applicable tags to give the board
        Set<String> toAdd = matchingTags.keySet().stream().filter(t -> matchingTags.get(t)).map(Tags::getName).collect(toSet());

        if (DEBUG) {
            System.out.println("----- Board: " + boardFile);
            if (toRemove.equals(toAdd)) {
                System.out.println("No changes");
            } else {
                toAdd.stream().forEach(System.out::println);
            }
        }
        
        // Now, if there are actual changes in the autotags, remove the former ones, add the new ones and save
        if (!toRemove.equals(toAdd)) {
            toRemove.forEach(board::removeTag);
            toAdd.forEach(board::addTag);

            // Re-save the board
            try (OutputStream os = new FileOutputStream(boardFile)) {
                board.save(os);
            } catch (IOException e) {
                System.out.println("Error: Could not save board: " + boardFile);
                e.printStackTrace();
            }

        }
    }

}
