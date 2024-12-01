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
package megamek.utilities;

import static java.util.stream.Collectors.toSet;
import static megamek.common.Terrains.*;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import megamek.common.*;
import megamek.logging.MMLogger;

/**
 * Scans all boards and applies automated tags to them.
 * Examples: Woods (Auto), DenseUrban (Auto). Deletes its own tags (and only
 * those) before applying them (again), so if the rules for applying tags are
 * changed, they may be removed accordingly and will also not be applied twice.
 *
 * @author Simon (Juliez)
 */
public class BoardsTagger {
    private static final MMLogger logger = MMLogger.create(BoardsTagger.class);

    /** When true, will print some information to Logger. */
    private static final boolean DEBUG = false;

    /**
     * A suffix added to tags to distinguish them from manually added tags. The
     * suffix is also used to identify them for deleting tags that no longer apply
     * to a board or that got renamed. If the suffix is changed, this tool will not
     * remove tags with the old suffix before applying new ones, so those must be
     * handled manually.
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
        TAG_SNOWTERRAIN("SnowTerrain"),
        TAG_HANGAR("Hangar"),
        TAG_FORTRESS("Fortress"),
        TAG_GUNEMPLACEMENT("GunEmplacement"),
        TAG_HEAVYBUILDING("HeavyBuilding"),
        TAG_HARDENEDBUILDING("HardenedBuilding"),
        TAG_ARMOREDBUILDING("ArmoredBuilding"),
        TAG_IMPASSABLE("Impassable"),
        TAG_ELEVATOR("Elevator"),
        TAG_MULTIPLETHEME("MultipleTheme");

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

    public static void main(String... args) {
        try {
            Map<String, List<String>> boardCheckSum = new HashMap<>();

            File boardDir = Configuration.boardsDir();
            scanForBoards(boardDir, boardCheckSum);

            boardCheckSum.forEach((key, value) -> {
                if (value.size() > 1) {
                    String message = key + " : " + value.stream().sorted().collect(Collectors.joining(", "));
                    logger.info(message);
                }
            });
        } catch (Exception ex) {
            logger.error(ex, "Board tagger cannot scan boards");
            System.exit(64);
        }

        logger.info("Finished.");
    }

    /**
     * Recursively scans the supplied file/directory for any boards and auto-tags
     * them.
     */
    private static void scanForBoards(File file, Map<String, List<String>> boardCheckSum) throws IOException {
        if (file.isDirectory()) {
            String[] fileList = file.list();
            for (String filename : fileList) {
                File filepath = new File(file, filename);
                if (filepath.isDirectory()) {
                    scanForBoards(new File(file, filename), boardCheckSum);
                } else {
                    tagBoard(filepath);
                    checkSum(boardCheckSum, filepath);
                }
            }
        } else {
            tagBoard(file);
            checkSum(boardCheckSum, file);
        }
    }

    /**
     * Scans the board for the types and number of terrains present, deletes old
     * automatic tags and applies new automatic tags as appropriate.
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
            List<String> errors = new ArrayList<>();
            board.load(is, errors, true);
            if (!errors.isEmpty()) {
                String message = String.format("Board has errors: %s", boardFile);
                logger.debug(message);
                return;
            }
        } catch (Exception e) {
            String message = String.format("Could not load board: %s", boardFile);
            logger.error(e, message);
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
        int hangar = 0;
        int fortress = 0;
        int gunEnplacement = 0;
        int heavyBuilding = 0;
        int hardenedBuilding = 0;
        int armoredBuilding = 0;
        int impassable = 0;
        int elevator = 0;
        int multipleTheme = 0;

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
                        && ((hex.terrainLevel(BUILDING) == Building.LIGHT)
                                || (hex.terrainLevel(BUILDING) == Building.MEDIUM))) {
                    stdBuildings++;
                    int height = hex.terrainLevel(BLDG_ELEV);
                    lowBuildings += (height <= 2) ? 1 : 0;
                    highBuildings += (height > 2) ? 1 : 0;
                }
                if (hex.containsTerrain(BUILDING)) {
                    hangar += hex.terrainLevel(BLDG_CLASS) == Building.HANGAR ? 1 : 0;
                    fortress += hex.terrainLevel(BLDG_CLASS) == Building.FORTRESS ? 1 : 0;
                    gunEnplacement += hex.terrainLevel(BLDG_CLASS) == Building.GUN_EMPLACEMENT ? 1 : 0;
                    heavyBuilding += hex.terrainLevel(BUILDING) == Building.HEAVY ? 1 : 0;
                    hardenedBuilding += hex.terrainLevel(BUILDING) == Building.HARDENED ? 1 : 0;
                    armoredBuilding += hex.containsTerrain(BLDG_ARMOR) && hex.terrainLevel(Terrains.BLDG_ARMOR) > 0 ? 1 : 0;
                }
                impassable += hex.containsTerrain(IMPASSABLE) ? 1 : 0;
                elevator += hex.containsTerrain(ELEVATOR) ? 1 : 0;
            }
        }

        // The board area
        int area = board.getWidth() * board.getHeight();
        // The geometric mean of the board sides. Better for some comparisons than the
        // area.
        int normSide = (int) Math.sqrt(area);
        int levelExtent = board.getMaxElevation() - board.getMinElevation();

        // Calculate which tags apply
        EnumMap<Tags, Boolean> matchingTags = new EnumMap<>(Tags.class);

        matchingTags.put(Tags.TAG_MEDFOREST,
                (forest >= normSide * 5) && (forest < normSide * 10) && (forestHU < normSide * 2));
        matchingTags.put(Tags.TAG_LIGHTFOREST,
                (forest >= normSide * 2) && (forestHU < normSide) && (forest < normSide * 5));
        matchingTags.put(Tags.TAG_DENSEFOREST, (forest >= normSide * 10) && (forestHU > normSide * 2));
        matchingTags.put(Tags.TAG_WOODS, woods > forest / 2);
        matchingTags.put(Tags.TAG_JUNGLE, jungles > forest / 2);
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
        boolean lightUrban = (lowBuildings > normSide) && (highBuildings < normSide / 3)
                && (lowBuildings < normSide * 2) && (roads > normSide / 3);
        matchingTags.put(Tags.TAG_LIGHTURBAN, lightUrban);
        matchingTags.put(Tags.TAG_MEDURBAN, !lightUrban && (stdBuildings >= normSide)
                && (roads > normSide / 3) && (stdBuildings < normSide * 4));
        matchingTags.put(Tags.TAG_HEAVYURBAN, (stdBuildings >= normSide * 4) && (roads > normSide / 3));
        matchingTags.put(Tags.TAG_SNOWTERRAIN, (snowTerrain > normSide * 2));
        matchingTags.put(Tags.TAG_FLAT, (levelExtent <= 2) && (weighedLevels < normSide * 5));
        matchingTags.put(Tags.TAG_HANGAR, hangar > 10);
        matchingTags.put(Tags.TAG_FORTRESS, fortress > 10);
        matchingTags.put(Tags.TAG_GUNEMPLACEMENT, gunEnplacement > 10);
        matchingTags.put(Tags.TAG_HEAVYBUILDING, heavyBuilding > 10);
        matchingTags.put(Tags.TAG_HARDENEDBUILDING, hardenedBuilding > 10);
        matchingTags.put(Tags.TAG_ARMOREDBUILDING, armoredBuilding > 10);
        matchingTags.put(Tags.TAG_IMPASSABLE, impassable > 0);
        matchingTags.put(Tags.TAG_ELEVATOR, elevator > 0);

        multipleTheme = 0;
        multipleTheme += deserts > 0 ? 1 : 0;
        multipleTheme += lunar > 0 ? 1 : 0;
        multipleTheme += grass > 0 ? 1 : 0;
        multipleTheme += tropical > 0 ? 1 : 0;
        multipleTheme += mars > 0 ? 1 : 0;
        multipleTheme += snowTheme > 0 ? 1 : 0;
        multipleTheme += volcanic > 0 ? 1 : 0;
        matchingTags.put(Tags.TAG_MULTIPLETHEME, multipleTheme > 1);

        // Remove (see below) any auto tags that might be present so that auto tags that
        // no longer apply
        // are not left in the board file.
        Set<String> toRemove = board.getTags().stream().filter(t -> t.contains(AUTO_SUFFIX)).collect(toSet());

        // Find any applicable tags to give the board
        Set<String> toAdd = matchingTags.keySet().stream().filter(matchingTags::get).map(Tags::getName)
                .collect(toSet());

        if (DEBUG) {
            String message = String.format("----- Board: %s", boardFile);
            logger.debug(message);
            if (toRemove.equals(toAdd)) {
                logger.debug("No changes.");
            } else {
                toAdd.forEach(logger::debug);
            }
        }

        // Now, if there are actual changes in the auto tags, remove the former ones,
        // add the new ones and save
        if (!toRemove.equals(toAdd)) {
            toRemove.forEach(board::removeTag);
            toAdd.forEach(board::addTag);

            // Re-save the board
            try (OutputStream os = new FileOutputStream(boardFile)) {
                board.save(os);
            } catch (Exception ex) {
                String message = String.format("Could not save board: %s", boardFile);
                logger.error(ex, message);
            }
        }
    }

    private static void checkSum(Map<String, List<String>> boardCheckSum,  File boardFile) {
        MessageDigest md;

        try {
            md = MessageDigest.getInstance("SHA-256");

            String line;
            List<String> lines = new ArrayList<>();

            // remove tag lines
            try (BufferedReader br = new BufferedReader(new FileReader(boardFile));) {
                while ((line = br.readLine()) != null) {
                    if (!line.startsWith("tag ")) {
                        lines.add(line);
                    }
                }
            } catch (Exception e) {
                logger.error(e, "Error Calculating Hash");
            }

            String sortedLines = lines.stream().sorted().collect(Collectors.joining());

            md.update(sortedLines.getBytes(), 0, sortedLines.length());
            HexFormat hexFormat = HexFormat.of();
            String cs = hexFormat.formatHex(md.digest()).toUpperCase();
            boardCheckSum.putIfAbsent(cs, new ArrayList<>());

            boardCheckSum.get(cs).add(boardFile.getPath());
        } catch (NoSuchAlgorithmException e) {
            logger.error(e, "SHA-256 Algorithm Can't be Found");
        }
    }
}
