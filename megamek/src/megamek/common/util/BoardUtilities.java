/*
* MegaMek -
* Copyright (c) 2005 Ben Mazur (bmazur@sev.org)
* Copyright (c) 2018 - The MegaMek Team. All Rights Reserved.
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.common.util;

import megamek.client.bot.princess.CardinalEdge;
import megamek.codeUtilities.MathUtility;
import megamek.common.*;
import megamek.common.planetaryconditions.Weather;
import megamek.common.planetaryconditions.Wind;
import megamek.common.util.generator.ElevationGenerator;
import megamek.common.util.generator.SimplexGenerator;

import java.util.*;

public class BoardUtilities {
    private static final List<ElevationGenerator> elevationGenerators = new ArrayList<>();
    static {
        // TODO: make this externally accessible via registerElevationGenerator()
        elevationGenerators.add(new SimplexGenerator());
    }

    /** @return how many elevation generator algorithms there are; three built-in */
    public static int getAmountElevationGenerators() {
        return 3 + elevationGenerators.size();
    }

    /**
     * Combines one or more boards into one huge megaboard!
     *
     * @param width the width of each individual board, before the combine
     * @param height the height of each individual board, before the combine
     * @param sheetWidth how many sheets wide the combined map is
     * @param sheetHeight how many sheets tall the combined map is
     * @param boards an array of the boards to be combined
     * @param isRotated Flag that determines if any of the maps are rotated
     * @param medium Sets the medium the map is in (ie., ground, atmo, space)
     */
    public static Board combine(int width, int height, int sheetWidth, int sheetHeight,
                                Board[] boards, List<Boolean> isRotated, int medium) {

        int resultWidth = width * sheetWidth;
        int resultHeight = height * sheetHeight;

        Hex[] resultData = new Hex[resultWidth * resultHeight];
        boolean roadsAutoExit = true;
        // Copy the data from the sub-boards.
        for (int i = 0; i < sheetHeight; i++) {
            for (int j = 0; j < sheetWidth; j++) {
                Board b = boards[i * sheetWidth + j];
                if ((b.getWidth() != width) || (b.getHeight() != height)) {
                    throw new IllegalArgumentException(
                            "board is the wrong size, expected " + width + "x"
                                    + height + ", got " + b.getWidth() + "x"
                                    + b.getHeight());
                }
                copyBoardInto(resultData, resultWidth, j * width, i * height,
                        boards[i * sheetWidth + j]);
                // Copy in the other board's options.
                if (!boards[i * sheetWidth + j].getRoadsAutoExit()) {
                    roadsAutoExit = false;
                }
            }
        }

        Board result = new Board();
        result.setRoadsAutoExit(roadsAutoExit);
        // Initialize all hexes - buildings, exits, etc
        result.newData(resultWidth, resultHeight, resultData, null);

        // assuming that the map setting and board types match
        result.setType(medium);

        return result;
    }

    /**
     * Copies the data of another board into given array of Hexes, offset by the
     * specified x and y.
     */
    protected static void copyBoardInto(Hex[] dest, int destWidth, int x, int y, Board copied) {
        for (int i = 0; i < copied.getHeight(); i++) {
            for (int j = 0; j < copied.getWidth(); j++) {
                dest[(i + y) * destWidth + j + x] = copied.getHex(j, i);
            }
        }
    }

    /**
     * Generates a Random Board
     *
     * @param mapSettings The parameters for random board creation.
     */
    public static Board generateRandom(MapSettings mapSettings) {
        int[][] elevationMap = new int[mapSettings.getBoardWidth()][mapSettings.getBoardHeight()];
        double sizeScale = (double) (mapSettings.getBoardWidth() * mapSettings.getBoardHeight())
                / (16d * 17d);

        generateElevation(mapSettings.getHilliness(), mapSettings
                .getBoardWidth(), mapSettings.getBoardHeight(), mapSettings
                .getRange() + 1, mapSettings.getProbInvert(), mapSettings
                .getInvertNegativeTerrain(), elevationMap, mapSettings
                .getAlgorithmToUse());

        Hex[] nb = new Hex[mapSettings.getBoardWidth() * mapSettings.getBoardHeight()];
        int index = 0;
        for (int h = 0; h < mapSettings.getBoardHeight(); h++) {
            for (int w = 0; w < mapSettings.getBoardWidth(); w++) {
                if (mapSettings.getMedium() == MapSettings.MEDIUM_SPACE) {
                    nb[index++] = new Hex(0, "space:1", mapSettings.getTheme(), new Coords(w, h));
                } else {
                    nb[index++] = new Hex(elevationMap[w][h], "", mapSettings.getTheme(), new Coords(w, h));
                }
            }
        }

        Board result = new Board(mapSettings.getBoardWidth(), mapSettings.getBoardHeight(), nb);

        if (mapSettings.getMedium() == MapSettings.MEDIUM_SPACE) {
            result.setType(Board.T_SPACE);
            return result;
        }

        // initialize reverseHex
        HashMap<Hex, Point> reverseHex = new HashMap<>(2
                * mapSettings.getBoardWidth() * mapSettings.getBoardHeight());
        for (int y = 0; y < mapSettings.getBoardHeight(); y++) {
            for (int x = 0; x < mapSettings.getBoardWidth(); x++) {
                reverseHex.put(result.getHex(x, y), new Point(x, y));
            }
        }

        int peaks = mapSettings.getMountainPeaks();
        while (peaks > 0) {
            peaks--;
            int mountainHeight = mapSettings.getMountainHeightMin()
                    + Compute.randomInt(1 + mapSettings.getMountainHeightMax()
                            - mapSettings.getMountainHeightMin());
            int mountainWidth = mapSettings.getMountainWidthMin()
                    + Compute.randomInt(1 + mapSettings.getMountainWidthMax()
                            - mapSettings.getMountainWidthMin());
            int mapWidth = result.getWidth();
            int mapHeight = result.getHeight();

            // put the peak somewhere in the middle of the map...
            Coords peak = new Coords(mapWidth / 4
                    + Compute.randomInt((mapWidth + 1) / 2), mapHeight / 4
                    + Compute.randomInt((mapHeight + 1) / 2));

            generateMountain(result, mountainWidth, peak, mountainHeight,
                    mapSettings.getMountainStyle());
        }

        if (mapSettings.getCliffs() > 0) {
            addCliffs(result, mapSettings.getCliffs());
        }

        // Add the woods
        int count = mapSettings.getMinForestSpots();
        if (mapSettings.getMaxForestSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxForestSpots() + 1);
        }
        count = (int) Math.round(count * sizeScale);
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.WOODS,
                    mapSettings.getProbHeavy(), mapSettings.getProbUltra(),
                    mapSettings.getMinForestSize(),
                    mapSettings.getMaxForestSize(), reverseHex, true);
        }
        
        // Add foliage (1 elevation high woods)
        count = mapSettings.getMinFoliageSpots();
        if (mapSettings.getMaxFoliageSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxFoliageSpots() + 1);
        }
        count = (int) Math.round(count * sizeScale);
        for (int i = 0; i < count; i++) {
            placeFoliage(result, Terrains.WOODS,
                    mapSettings.getProbFoliageHeavy(), mapSettings.getMinFoliageSize(),
                    mapSettings.getMaxFoliageSize(), reverseHex, true);
        }

        // Add the jungle
        count = mapSettings.getMinJungleSpots();
        if (mapSettings.getMaxJungleSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxJungleSpots() + 1);
        }
        count = (int) Math.round(count * sizeScale);
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.JUNGLE,
                    mapSettings.getProbHeavyJungle(), mapSettings.getProbUltraJungle(),
                    mapSettings.getMinJungleSize(),
                    mapSettings.getMaxJungleSize(), reverseHex, true);
        }
        
        // Add the rough
        count = mapSettings.getMinRoughSpots();
        if (mapSettings.getMaxRoughSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxRoughSpots() + 1);
        }
        count = (int) Math.round(count * sizeScale);
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.ROUGH, mapSettings.getProbUltraRough(), mapSettings
                    .getMinRoughSize(), mapSettings.getMaxRoughSize(),
                    reverseHex, true);
        }

        // Add the sand
        count = mapSettings.getMinSandSpots();
        if (mapSettings.getMaxSandSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxSandSpots() + 1);
        }
        count = (int) Math.round(count * sizeScale);
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.SAND, 0, mapSettings
                    .getMinSandSize(), mapSettings.getMaxSandSize(),
                    reverseHex, true);
        }

        // Add the snow
        count = mapSettings.getMinSnowSpots();
        if (mapSettings.getMaxSnowSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxSnowSpots() + 1);
        }
        count = (int) Math.round(count * sizeScale);
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.SNOW, 0, mapSettings
                            .getMinSnowSize(), mapSettings.getMaxSnowSize(),
                    reverseHex, true);
        }

        // Add the tundra
        count = mapSettings.getMinTundraSpots();
        if (mapSettings.getMaxTundraSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxTundraSpots() + 1);
        }
        count = (int) Math.round(count * sizeScale);
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.TUNDRA, 0, mapSettings
                            .getMinTundraSize(), mapSettings.getMaxTundraSize(),
                    reverseHex, true);
        }

        // Add the planted field
        count = mapSettings.getMinPlantedFieldSpots();
        if (mapSettings.getMaxPlantedFieldSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxPlantedFieldSpots() + 1);
        }
        count = (int) Math.round(count * sizeScale);
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.FIELDS, 0, mapSettings
                    .getMinPlantedFieldSize(), mapSettings.getMaxPlantedFieldSize(),
                    reverseHex, true);
        }

        // Add the swamp
        count = mapSettings.getMinSwampSpots();
        if (mapSettings.getMaxSwampSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxSwampSpots() + 1);
        }
        count = (int) Math.round(count * sizeScale);
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.SWAMP, 0, mapSettings
                    .getMinSwampSize(), mapSettings.getMaxSwampSize(),
                    reverseHex, false); // can stack with woods or roughs
        }

        // Add the Fortified hexes
        count = mapSettings.getMinFortifiedSpots();
        if (mapSettings.getMaxFortifiedSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxFortifiedSpots() + 1);
        }
        count = (int) Math.round(count * sizeScale);
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.FORTIFIED, 0, mapSettings
                    .getMinFortifiedSize(), mapSettings.getMaxFortifiedSize(),
                    reverseHex, false);
        }

        // Add the rubble
        count = mapSettings.getMinRubbleSpots();
        if (mapSettings.getMaxRubbleSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxRubbleSpots() + 1);
        }
        count = (int) Math.round(count * sizeScale);
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.RUBBLE, mapSettings.getProbUltraRubble(), mapSettings
                    .getMinRubbleSize(), mapSettings.getMaxRubbleSize(),
                    reverseHex, true);
        }

        // Add the water
        count = mapSettings.getMinWaterSpots();
        if (mapSettings.getMaxWaterSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxWaterSpots() + 1);
        }
        count = (int) Math.round(count * sizeScale);
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.WATER, mapSettings.getProbDeep(),
                    mapSettings.getMinWaterSize(), mapSettings.getMaxWaterSize(), reverseHex, true);
        }

        // Add the pavements
        count = mapSettings.getMinPavementSpots();
        if (mapSettings.getMaxPavementSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxPavementSpots() + 1);
        }
        count = (int) Math.round(count * sizeScale);
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.PAVEMENT, 0, mapSettings.getMinPavementSize(),
                    mapSettings.getMaxPavementSize(), reverseHex, true);
        }

        // Add the ice
        count = mapSettings.getMinIceSpots();
        if (mapSettings.getMaxIceSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxIceSpots() + 1);
        }
        count = (int) Math.round(count * sizeScale);
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.ICE, 0, mapSettings.getMinIceSize(),
                    mapSettings.getMaxIceSize(), reverseHex, true);
        }

        // Add the craters
        if (Compute.randomInt(100) < mapSettings.getProbCrater()) {
            addCraters(result, mapSettings.getMinRadius(), mapSettings.getMaxRadius(),
                    (int) (mapSettings.getMinCraters() * sizeScale),
                    (int) (mapSettings.getMaxCraters() * sizeScale));
        }

        // Add the river
        if (Compute.randomInt(100) < mapSettings.getProbRiver()) {
            addRiver(result, reverseHex);
        }

        // Add special effects
        if (Compute.randomInt(100) < mapSettings.getProbFlood()) {
            postProcessFlood(nb, mapSettings.getFxMod());
        }

        if (Compute.randomInt(100) < mapSettings.getProbDrought()) {
            postProcessDrought(nb, mapSettings.getFxMod());
        }

        if (Compute.randomInt(100) < mapSettings.getProbFreeze()) {
            postProcessDeepFreeze(nb, mapSettings.getFxMod());
        }

        if (Compute.randomInt(100) < mapSettings.getProbForestFire()) {
            postProcessForestFire(nb, mapSettings.getFxMod());
        }

        // Add the road
        boolean roadNeeded = Compute.randomInt(100) < mapSettings.getProbRoad();

        // add buildings
        ArrayList<BuildingTemplate> buildings = mapSettings.getBoardBuildings();
        CityBuilder cityBuilder = new CityBuilder(mapSettings, result);
        if (buildings.isEmpty()) {
            buildings = cityBuilder.generateCity(roadNeeded);
        }

        for (final BuildingTemplate building : buildings) {
            placeBuilding(result, building);
        }
        return result;
    }

    private static void placeBuilding(Board board, BuildingTemplate building) {
        int type = building.getType();
        int cf = building.getCF();
        int height = building.getHeight();
        ArrayList<Hex> hexes = new ArrayList<>();
        int level = 0;
        for (Iterator<Coords> i = building.getCoords(); i.hasNext();) {
            Coords c = i.next();
            Hex hex = board.getHex(c);
            // work out exits...
            int exits = 0;
            for (int dir = 0; dir < 6; dir++) {
                if (building.containsCoords(c.translated(dir))) {
                    exits |= (1 << dir);
                }
            }

            // remove everything
            hex.removeAllTerrains();
            hex.addTerrain(new Terrain(Terrains.PAVEMENT, 1));
            hex.addTerrain(new Terrain(Terrains.BUILDING, type, true, exits));
            hex.addTerrain(new Terrain(Terrains.BLDG_CF, cf));
            hex.addTerrain(new Terrain(Terrains.BLDG_ELEV, height));
            hexes.add(hex);
            level += hex.getLevel();
        }
        // set everything to the same level
        for (int j = 0; j < hexes.size(); j++) {
            hexes.get(j).setLevel(level / hexes.size());
        }
    }

    /**
     * Overload that places some connected terrain with no chance of "ultra" version
     */
    protected static void placeSomeTerrain(Board board, int terrainType, int probMore, int minHexes,
                                           int maxHexes, Map<Hex, Point> reverseHex, boolean exclusive) {
        placeSomeTerrain(board, terrainType, probMore, 0, minHexes, maxHexes, reverseHex, exclusive);
    }

    /**
     * Places randomly some connected terrain.
     *
     * @param board The board the terrain goes on.
     * @param terrainType The type of terrain to place {@link Terrains}.
     * @param probMore
     * @param maxHexes Maximum number of hexes this terrain can cover.
     * @param reverseHex
     * @param exclusive Set TRUE if this terrain cannot be combined with any other terrain types.
     */
    protected static void placeSomeTerrain(Board board, int terrainType, int probMore, int probUltra, int minHexes,
                                           int maxHexes, Map<Hex, Point> reverseHex, boolean exclusive) {
        Point p = new Point(Compute.randomInt(board.getWidth()), Compute.randomInt(board.getHeight()));
        int count = minHexes;
        if ((maxHexes - minHexes) > 0) {
            count += Compute.randomInt(maxHexes - minHexes + 1);
        }
        Hex field;

        HashSet<Hex> alreadyUsed = new HashSet<>();
        HashSet<Hex> unUsed = new HashSet<>();
        field = board.getHex(p.x, p.y);
        if (!field.containsTerrain(terrainType)) {
            unUsed.add(field);
        } else {
            findAllUnused(board, terrainType, alreadyUsed, unUsed, field, reverseHex);
        }

        for (int i = 0; i < count; i++) {
            if (unUsed.isEmpty()) {
                return;
            }
            int which = Compute.randomInt(unUsed.size());
            Iterator<Hex> iter = unUsed.iterator();
            for (int n = 0; n < (which - 1); n++) {
                iter.next();
            }
            field = iter.next();
            if (exclusive) {
                field.removeAllTerrains();
            }
            int terrainDensity = pickTerrainDensity(terrainType, probMore, probUltra);
            Terrain tempTerrain = new Terrain(terrainType, terrainDensity);
            field.addTerrain(tempTerrain);
            growTreesIfNecessary(field, terrainType, terrainDensity);
            unUsed.remove(field);
            findAllUnused(board, terrainType, alreadyUsed, unUsed, field, reverseHex);
        }

        if (terrainType == Terrains.WATER) {
            /*
             * if next to an Water Hex is an lower lvl lower the hex. First we
             * search for lowest Hex next to the lake
             */
            int min = Integer.MAX_VALUE;
            Iterator<Hex> iter = unUsed.iterator();
            while (iter.hasNext()) {
                field = iter.next();
                if (field.getLevel() < min) {
                    min = field.getLevel();
                }
            }
            iter = alreadyUsed.iterator();
            while (iter.hasNext()) {
                field = iter.next();
                field.setLevel(min);
            }

        }
    }

    /**
     * Worker function that picks a terrain density (light, heavy, ultra) based on the passed-in weights.
     * Likelyhood of light is 100 - probHeavy.
     */
    private static int pickTerrainDensity(int terrainType, int probHeavy, int probUltra) {
        int heavyThreshold = 100 - probHeavy;
        int ultraThreshold = 100;
        int sum = 100 + probUltra;

        int roll = Compute.randomInt(sum);

        // for most terrains, this results in "standard/heavy/ultra" versions of the terrain
        // but rubble is implemented weirdly, and there are probably maps that use the current
        // implementation of rubble so here we are
        if (roll < heavyThreshold) {
            return terrainType == Terrains.RUBBLE ? pickRandomRubble() : 1;
        } else if (roll < ultraThreshold) {
            // rubble 6 is considered "ultra"
            return terrainType == Terrains.RUBBLE ? 6 : 2;
        } else {
            return 3;
        }
    }

    /**
     * Worker method to pick out a random usable type of "standard" rubble
     */
    private static int pickRandomRubble() {
        // there are three usable types of rubble, so we pick one
        int roll = Compute.randomInt(3) + 1;

        // rubble 3 looks identical to rubble 6, which is ultra-rough, so we don't want
        // to visually deceive the user, thus 3 becomes 4
        if (roll == 3) {
            roll = 4;
        }

        return roll;
    }

    /**
     * Helper method that places a FOLIAGE_ELEV terrain if necessary
     */
    private static void growTreesIfNecessary(Hex field, int terrainType, int terrainDensity) {
        // light/heavy woods and jungle go up two levels
        if (((terrainType == Terrains.WOODS) || (terrainType == Terrains.JUNGLE))
                && ((terrainDensity == 1) || (terrainDensity == 2))) {
            field.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 2));
        // ultra woods and jungle go up three levels
        } else if ((terrainType == Terrains.WOODS) && (terrainDensity == 3)) {
            field.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 3));
        }
    }
    
    /**
     * Places randomly some connected foliage.
     *
     * @param board The board the terrain goes on.
     * @param terrainType The type of terrain to place {@link Terrains}.
     * @param probMore
     * @param maxHexes Maximum number of hexes this terrain can cover.
     * @param reverseHex
     * @param exclusive Set TRUE if this terrain cannot be combined with any other terrain types.
     */
    protected static void placeFoliage(Board board, int terrainType, int probMore, int minHexes,
                                       int maxHexes, Map<Hex, Point> reverseHex, boolean exclusive) {
        Point p = new Point(Compute.randomInt(board.getWidth()), Compute.randomInt(board.getHeight()));
        int count = minHexes;
        if ((maxHexes - minHexes) > 0) {
            count += Compute.randomInt(maxHexes - minHexes + 1);
        }
        Hex field;

        HashSet<Hex> alreadyUsed = new HashSet<>();
        HashSet<Hex> unUsed = new HashSet<>();
        field = board.getHex(p.x, p.y);
        if (!field.containsTerrain(terrainType)) {
            unUsed.add(field);
        } else {
            findAllUnused(board, terrainType, alreadyUsed, unUsed, field, reverseHex);
        }

        for (int i = 0; i < count; i++) {
            if (unUsed.isEmpty()) {
                return;
            }
            int which = Compute.randomInt(unUsed.size());
            Iterator<Hex> iter = unUsed.iterator();
            for (int n = 0; n < (which - 1); n++) {
                iter.next();
            }
            field = iter.next();
            if (exclusive) {
                field.removeAllTerrains();
            }
            int tempInt = (Compute.randomInt(100) < probMore) ? 2 : 1;
            Terrain tempTerrain = new Terrain(terrainType, tempInt);
            field.addTerrain(tempTerrain);
            field.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 1));
            unUsed.remove(field);
            findAllUnused(board, terrainType, alreadyUsed, unUsed, field, reverseHex);
        }

        
    }

    /**
     * Searching starting from one Hex, all Terrains not matching terrainType,
     * next to one of terrainType.
     *
     * @param terrainType The terrainType which the searching hexes should not have.
     * @param alreadyUsed The hexes which should not looked at (because they are already supposed
     *                    to visited in some way)
     * @param unUsed In this set the resulting hexes are stored. They are stored in addition to all
     *               previously stored.
     * @param searchFrom The Hex where to start
     */
    private static void findAllUnused(Board board, int terrainType, Set<Hex> alreadyUsed,
                                      Set<Hex> unUsed, Hex searchFrom, Map<Hex, Point> reverseHex) {
        Hex field;
        Set<Hex> notYetUsed = new HashSet<>();

        notYetUsed.add(searchFrom);
        do {
            Iterator<Hex> iter = notYetUsed.iterator();
            field = iter.next();
            if (field == null) {
                continue;
            }
            for (int dir = 0; dir < 6; dir++) {
                Point loc = reverseHex.get(field);
                Hex newHex = board.getHexInDir(loc.x, loc.y, dir);
                if ((newHex != null) && (!alreadyUsed.contains(newHex))
                        && (!notYetUsed.contains(newHex))
                        && (!unUsed.contains(newHex))) {
                    ((newHex.containsTerrain(terrainType)) ? notYetUsed : unUsed).add(newHex);
                }
            }
            notYetUsed.remove(field);
            alreadyUsed.add(field);
        } while (!notYetUsed.isEmpty());
    }

    /**
     * add a crater to the board
     */
    public static void addCraters(Board board, int minRadius, int maxRadius, int minCraters,
                                  int maxCraters) {
        // Calculate number of craters to generate.
        int numberCraters = minCraters;
        if (maxCraters > minCraters) {
            numberCraters += Compute.randomInt(maxCraters - minCraters + 1);
        }

        // Stay within the board boundaries.
        int width = board.getWidth();
        int height = board.getHeight();

        Map<Coords, Integer> usedHexes = new HashMap<>();

        // Generate each crater.
        for (int i = 0; i < numberCraters; i++) {

            // Locate the center of the crater.
            Point center = new Point(Compute.randomInt(width), Compute.randomInt(height));

            // What is the diameter of this crater?
            int radius = Compute.randomInt(maxRadius - minRadius + 1) + minRadius;

            // Terrestrial crater depth to radius ratio is typically 1:5 to 1:7.
            // Hexes are 30m across and levels are 6m high.
            // This ends up with rather deep craters (a 6-diameter crater can have a depth of 4-6).  For gamability
            // and verisimilitude, we're making crater's more shallow than is typical (1:8 to 1:10 ratio).
            int divisor = Compute.randomInt(2) + 8;
            int radiusM = radius * 30;
            int maxDepthM = Math.max(6, radiusM / divisor);
            int maxDepth = maxDepthM / 6;

            /* generate CraterProfile */
            int[] cratDepth = new int[radius];
            for (int x = 0; x < radius; x++) {
                cratDepth[x] = craterProfile(x, radius, maxDepth);
            }

            /*
             * btw, I am interested if someone actually reads this comments, so
             * send me and email to f.stock@tu-bs.de, if you do ;-)
             */
            /* now recalculate every hex */
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    int distance = (int) distance(center, new Point(w, h));
                    if (distance < radius) {
                        Hex field = board.getHex(w, h);
                        int baseElevation;

                        // If we've already placed a crater here, find it's original elevation.
                        if (usedHexes.containsKey(field.getCoords())) {
                            baseElevation = usedHexes.get(field.getCoords());
                        } else {
                            // If no crater has been placed here, add this hex's original elevation to our list.
                            baseElevation = field.getLevel();
                            usedHexes.put(field.getCoords(), baseElevation);
                        }

                        // Calculate the crater depth based on the original hex elevation.
                        int newElevation = baseElevation + cratDepth[distance];

                        // If the new elevation is deeper, use it, otherwise keep what we've already calculated.
                        field.setLevel(Math.min(newElevation, field.getLevel()));
                    }
                }
            }
        }
    }

    public static int craterProfile(int distanceFromCenter, int fullRadius, int maxDepth) {
        double depth;

        // If we're at the center, we should use the max depth.
        if (distanceFromCenter == 0) {
            return -maxDepth;
        } else if (distanceFromCenter == fullRadius) { // The edge should have no depth.
            return 0;
        }

        // The crater's floor should be a relatively shallow parabola.
        double radiusPercent = (double) distanceFromCenter / fullRadius;
        if (radiusPercent < 0.75) {
            depth = 0.02 * Math.pow(distanceFromCenter, 2) - maxDepth;

        } else { // The parabola should get steeper the closer to the crater wall you are.
            depth = 0.04 * Math.pow(distanceFromCenter, 2) - maxDepth;
        }

        return (int) Math.round(depth);
    }

    /**
     * The profile of a crater: interior is exp-function, exterior cos function.
     *
     * @param x The x value of the function. range 0..1. 0=center of crater.
     *            1=border of outer wall.
     * @param scale Apply this scale before returning the result (recommend
     *            instead of afterwards scale, cause this way the intern
     *            floating values are scaled, instead of int result).
     * @return The height of the crater at the position x from center. Unscaled,
     *         the results are between -0.5 and 1 (that means, if no scale is
     *         applied -1, 0 or 1).
     */
//    public static int craterProfile(double x, int scale) {
//        double result = 0;
//
//        result = (x < 0.75) ? ((Math.exp(x * 5.0 / 0.75 - 3) - 0.04979) * 1.5 / 7.33926) - 0.5
//                : ((Math.cos((x - 0.75) * 4.0) + 1.0) / 2.0);
//
//        return (int) (result * scale);
//    }

    /**
     * calculate the distance between two points
     *
     * @param p1
     * @param p2
     */
    private static double distance(Point p1, Point p2) {
        double x = p1.x - p2.x;
        double y = p1.y - p2.y;
        return Math.sqrt(x * x + y * y);
    }

    /**
     * Adds an River to the map (if the map is at least 5x5 hexes big). The
     * river has an width of 1-3 hexes (everything else is no more a river). The
     * river goes from one border to another. Nor Params, no results.
     */
    public static void addRiver(Board board, Map<Hex, Point> reverseHex) {
        int minElevation = Integer.MAX_VALUE;
        HashSet<Hex> riverHexes = new HashSet<>();
        Hex field;
        Point p = null;
        int direction = 0;
        int nextLeft = 0;
        int nextRight = 0;

        int width = board.getWidth();
        int height = board.getHeight();

        /* if map is smaller than 5x5 no real space for an river */
        if ((width < 5) || (height < 5)) {
            return;
        }
        /* First select start and the direction */
        switch (Compute.randomInt(4)) {
            case 0:
                p = new Point(0, Compute.randomInt(5) - 2 + height / 2);
                direction = Compute.randomInt(2) + 1;
                nextLeft = direction - 1;
                nextRight = direction + 1;
                break;
            case 1:
                p = new Point(width - 1, Compute.randomInt(5) - 2 + height / 2);
                direction = Compute.randomInt(2) + 4;
                nextLeft = direction - 1;
                nextRight = (direction + 1) % 6;
                break;
            case 2:
            case 3:
                p = new Point(Compute.randomInt(5) - 2 + width / 2, 0);
                direction = 2;
                nextRight = 3;
                nextLeft = 4;
                break;
        } // switch
        /* place the river */
        field = board.getHex(p.x, p.y);

        do {
            /* first the hex itself */
            field.removeAllTerrains();
            field.addTerrain(new Terrain(Terrains.WATER, 1));
            riverHexes.add(field);
            p = reverseHex.get(field);
            /* then maybe the left and right neighbours */
            riverHexes.addAll(extendRiverToSide(board, p, Compute.randomInt(3),
                    nextLeft, reverseHex));
            riverHexes.addAll(extendRiverToSide(board, p, Compute.randomInt(3),
                    nextRight, reverseHex));
            switch (Compute.randomInt(4)) {
                case 0:
                    field = board.getHexInDir(p.x, p.y, (direction + 5) % 6);
                    break;
                case 1:
                    field = board.getHexInDir(p.x, p.y, (direction + 1) % 6);
                    break;
                default:
                    field = board.getHexInDir(p.x, p.y, direction);
                    break;
            }

        } while (field != null);

        /* search the elevation for the river */
        HashSet<Hex> tmpRiverHexes = new HashSet<>(riverHexes);
        while (!tmpRiverHexes.isEmpty()) {
            Iterator<Hex> iter = tmpRiverHexes.iterator();
            field = iter.next();
            if (field.getLevel() < minElevation) {
                minElevation = field.getLevel();
            }
            tmpRiverHexes.remove(field);
            Point thisHex = reverseHex.get(field);
            /* and now the six neighbours */
            for (int i = 0; i < 6; i++) {
                field = board.getHexInDir(thisHex.x, thisHex.y, i);
                if ((field != null) && (field.getLevel() < minElevation)) {
                    minElevation = field.getLevel();
                }
                tmpRiverHexes.remove(field);
            }
        }

        /* now adjust the elevation to same height */
        Iterator<Hex> iter = riverHexes.iterator();
        while (iter.hasNext()) {
            field = iter.next();
            field.setLevel(minElevation);
        }
    }

    /**
     * Extends a river hex to left and right sides.
     *
     * @param hexloc The location of the river hex, from which it should get started.
     * @param width The width to which the river should extend in the direction.
     *            So the actual width of the river is 2*width+1.
     * @param direction Direction to which the river hexes should be extended.
     * @return Hashset with the hexes from the side.
     */
    private static Set<Hex> extendRiverToSide(Board board, Point hexloc, int width, int direction,
                                              Map<Hex, Point> reverseHex) {
        Point current = new Point(hexloc);
        Set<Hex> result = new HashSet<>();
        Hex hex;

        hex = board.getHexInDir(current.x, current.y, direction);
        while ((hex != null) && (width-- > 0)) {
            hex.removeAllTerrains();
            hex.addTerrain(new Terrain(Terrains.WATER, 1));
            result.add(hex);
            current = reverseHex.get(hex);
            hex = board.getHexInDir(current.x, current.y, direction);
        }
        return result;
    }

    /**
     * Flood negative hex levels Shoreline / salt marshes effect Works best with
     * more elevation
     */
    protected static void postProcessFlood(Hex[] hexSet, int modifier) {
        int n;
        Hex field;

        for (n = 0; n < hexSet.length; n++) {
            field = hexSet[n];
            int elev = field.getLevel() - modifier;
            if ((elev == 0) && !(field.containsTerrain(Terrains.WATER))
                    && !(field.containsTerrain(Terrains.PAVEMENT))) {
                field.addTerrain(new Terrain(Terrains.SWAMP, 1));
            } else if (elev < 0) {
                if (elev < -4) {
                    elev = -4;
                }
                field.removeAllTerrains();
                field.addTerrain(new Terrain(Terrains.WATER, -elev));
                field.setLevel(modifier);
            }
        }
    }

    /**
     * Converts water hexes to ice hexes. Works best with snow and ice themes.
     */
    protected static void postProcessDeepFreeze(Hex[] hexSet, int modifier) {
        int n;
        Hex field;
        for (n = 0; n < hexSet.length; n++) {
            field = hexSet[n];
            if (field.containsTerrain(Terrains.WATER)) {
                int level = field.terrainLevel(Terrains.WATER);
                if (modifier != 0) {
                    level -= modifier;
                    field.removeTerrain(Terrains.WATER);
                    if (level > 0) {
                        field.addTerrain(new Terrain(Terrains.WATER, level));
                    }
                }
                field.addTerrain(new Terrain(Terrains.ICE, 1));
            } else if (field.containsTerrain(Terrains.SWAMP)) {
                field.removeTerrain(Terrains.SWAMP);
                if (field.terrainsPresent() == 0) {
                    if (Compute.randomInt(100) < 30) {
                        // if no other terrains present, 30% chance to change to
                        // rough
                        field.addTerrain(new Terrain(Terrains.ROUGH, 1));
                    } else {
                        field.addTerrain(new Terrain(Terrains.ICE, 1));
                    }
                }
            }
        }
    }

    /**
     * Burning woods, with chance to be burnt down already
     */
    protected static void postProcessForestFire(Hex[] hexSet, int modifier) {
        int n;
        Hex field;
        int level, newlevel;
        int severity;

        for (n = 0; n < hexSet.length; n++) {
            field = hexSet[n];
            level = field.terrainLevel(Terrains.WOODS);
            if (level != Terrain.LEVEL_NONE) {
                severity = Compute.randomInt(5) - 2 + modifier;
                newlevel = level - severity;

                if (newlevel <= level) {
                    field.removeTerrain(Terrains.WOODS);
                    field.removeTerrain(Terrains.FOLIAGE_ELEV);
                    if (newlevel <= 0) {
                        field.addTerrain(new Terrain(Terrains.ROUGH, 1));
                    } else {
                        field.addTerrain(new Terrain(Terrains.WOODS, newlevel));
                        field.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, (newlevel == 3) ? 3 : 2));
                        field.addTerrain(new Terrain(Terrains.FIRE, 1));
                    }
                }
            }
        }
    }

    /**
     * Dries up all bodies of water by 1-3 levels. dried up water becomes swamp
     * then rough
     */
    protected static void postProcessDrought(Hex[] hexSet, int modifier) {
        int n;
        Hex field;
        int level, newlevel;
        int severity = 1 + Compute.randomInt(3) + modifier;
        if (severity < 0) {
            return;
        }

        for (n = 0; n < hexSet.length; n++) {
            field = hexSet[n];
            if (field.containsTerrain(Terrains.SWAMP)) {
                field.removeTerrain(Terrains.SWAMP); // any swamps are dried
                                                        // up to hardened mud
                if ((field.terrainsPresent() == 0) && (Compute.randomInt(100) < 30)) {
                    // if no other terrains present, 30% chance to change to
                    // rough
                    field.addTerrain(new Terrain(Terrains.ROUGH, 1));
                }
            }
            level = field.terrainLevel(Terrains.WATER);
            if (level != Terrain.LEVEL_NONE) {
                newlevel = level - severity;
                field.removeTerrain(Terrains.WATER);
                if (newlevel == 0) {
                    field.addTerrain(new Terrain(Terrains.SWAMP, 1));
                } else if (newlevel < 0) {
                    field.addTerrain(new Terrain(Terrains.ROUGH, 1));
                } else {
                    field.addTerrain(new Terrain(Terrains.WATER, newlevel));
                }

                newlevel = Math.min(level, severity);

                field.setLevel(field.getLevel() - newlevel);
            }
        }
    }

    private static boolean hexCouldBeCliff(Board board, Coords c) {
        int elevation = board.getHex(c).getLevel();
        boolean higher = false;
        boolean lower = false;
        int count = 0;
        for (int dir = 0; dir < 6; dir++) {
            Coords t = c.translated(dir);
            if (board.contains(t)) {
                Hex hex = board.getHex(t);
                int el = hex.getLevel();
                if (el > elevation) {
                    lower = true;
                } else if (el < elevation) {
                    higher = true;
                } else {
                    count++;
                }
            }
        }
        return higher && lower && (count <= 3) && (count > 0);
    }

    private static void findCliffNeighbours(Board board, Coords c, List<Coords> candidate,
                                            Set<Coords> ignore) {
        candidate.add(c);
        ignore.add(c);
        int elevation = board.getHex(c).getLevel();
        for (int dir = 0; dir < 6; dir++) {
            Coords t = c.translated(dir);
            if (board.contains(t) && !ignore.contains(t)) {
                if (hexCouldBeCliff(board, t)) {
                    Hex hex = board.getHex(t);
                    int el = hex.getLevel();
                    if (el == elevation) {
                        findCliffNeighbours(board, t, candidate, ignore);
                    }
                } else {
                    ignore.add(t);
                }
            }
        }
    }

    protected static void addCliffs(Board board, int modifier) {
        HashSet<Coords> ignore = new HashSet<>(); // previously considered hexes
        ArrayList<Coords> candidate = new ArrayList<>();
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Coords c = new Coords(x, y);
                int elevation = board.getHex(c).getLevel();
                if (ignore.contains(c)) {
                    continue;
                }
                if (!hexCouldBeCliff(board, c)) {
                    ignore.add(c);
                    continue;
                }

                findCliffNeighbours(board, c, candidate, ignore);
                // is the candidate interesting (at least 3 hexes)?
                if ((candidate.size() >= 3) && (Compute.randomInt(100) < modifier)) {
                    if (elevation > 0) {
                        elevation--;
                    } else {
                        elevation++;
                    }

                    for (Iterator<Coords> e = candidate.iterator(); e.hasNext();) {
                        c = e.next();
                        Hex hex = board.getHex(c);
                        hex.setLevel(elevation);
                    }
                }
                candidate.clear();
            }
        }
    }

    /*
     * adjust the board based on weather conditions
     */
    public static void addWeatherConditions(Board board, Weather weatherCond, Wind windCond) {
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Coords c = new Coords(x, y);
                Hex hex = board.getHex(c);

                //moderate rain - mud in clear hexes, depth 0 water, and dirt roads (not implemented yet)
                if (weatherCond.isModerateRain()) {
                    if ((hex.terrainsPresent() == 0) || (hex.containsTerrain(Terrains.WATER) && (hex.depth() == 0))) {
                        hex.addTerrain(new Terrain(Terrains.MUD, 1));
                        if (hex.containsTerrain(Terrains.WATER)) {
                            hex.removeTerrain(Terrains.WATER);
                        }
                    }
                }

                //heavy rain - mud in all hexes except buildings, depth 1+ water, and non-dirt roads
                //rapids in all depth 1+ water
                if (weatherCond.isHeavyRainOrGustingRain()) {
                    if (hex.containsTerrain(Terrains.WATER) && !hex.containsTerrain(Terrains.RAPIDS) && (hex.depth() > 0)) {
                        hex.addTerrain(new Terrain(Terrains.RAPIDS, 1));
                    } else if (!hex.containsTerrain(Terrains.BUILDING)
                            && !hex.containsTerrain(Terrains.PAVEMENT)
                            && !hex.containsTerrain(Terrains.ROAD)) {
                        hex.addTerrain(new Terrain(Terrains.MUD, 1));
                        if (hex.containsTerrain(Terrains.WATER)) {
                            hex.removeTerrain(Terrains.WATER);
                        }
                    }
                }

                //torrential downpour - mud in all hexes except buildings, depth 1+ water, and non-dirt roads
                //torrent in all depth 1+ water, swamps in all depth 0 water hexes
                if (weatherCond.isDownpour()) {
                    if (hex.containsTerrain(Terrains.WATER) && !(hex.terrainLevel(Terrains.RAPIDS) > 1) && (hex.depth() > 0)) {
                        hex.addTerrain(new Terrain(Terrains.RAPIDS, 2));
                    } else if (hex.containsTerrain(Terrains.WATER)) {
                        hex.addTerrain(new Terrain(Terrains.SWAMP, 1));
                        hex.removeTerrain(Terrains.WATER);
                    } else if (!hex.containsTerrain(Terrains.BUILDING)
                            && !hex.containsTerrain(Terrains.PAVEMENT)
                            && !hex.containsTerrain(Terrains.ROAD)) {
                        hex.addTerrain(new Terrain(Terrains.MUD, 1));
                    }
                }

                // check for rapids/torrents created by wind
                if (windCond.isStrongerThan(Wind.MOD_GALE)
                        && hex.containsTerrain(Terrains.WATER)
                        && (hex.depth() > 0)) {
                    if (windCond.isStorm()) {
                        if (!(hex.terrainLevel(Terrains.RAPIDS) > 1)) {
                            hex.addTerrain(new Terrain(Terrains.RAPIDS, 2));
                        }
                    } else {
                        if (!hex.containsTerrain(Terrains.RAPIDS)) {
                            hex.addTerrain(new Terrain(Terrains.RAPIDS, 1));
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates the elevations
     *
     * @param hilliness The Hilliness
     * @param width The Width of the map.
     * @param height The Height of the map.
     * @param range Max difference between highest and lowest level.
     * @param invertProb Probability for the invertion of the map (0..100)
     * @param invertNegative If 1, invert negative hexes, else do nothing
     * @param elevationMap here is the result stored
     */
    public static void generateElevation(int hilliness, int width, int height, int range,
                                         int invertProb, int invertNegative, int[][] elevationMap,
                                         int algorithm) {
        int minLevel = 0;
        boolean invert = (Compute.randomInt(100) < invertProb);

        /* init elevation map with 0 */
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                elevationMap[w][h] = 0;
            }
        }
        /* generate landscape */
        switch (algorithm) {
            case 0:
                cutSteps(hilliness, width, height, elevationMap);
                break;
            case 1:
                midPoint(hilliness, width, height, elevationMap);
                break;
            case 2:
                cutSteps(hilliness, width, height, elevationMap);
                midPoint(hilliness, width, height, elevationMap);
                break;
            default:
                // Non-hardcoded generators, if we have any
                if ((algorithm > 2) && (algorithm - 3 < elevationGenerators.size())) {
                    elevationGenerators.get(algorithm - 3).generate(hilliness, width, height, elevationMap);
                }
        }

        /* and now normalize it */
        int min = elevationMap[0][0];
        int max = elevationMap[0][0];
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                if (elevationMap[w][h] > max) {
                    max = elevationMap[w][h];
                } else if (elevationMap[w][h] < min) {
                    min = elevationMap[w][h];
                }
            }
        }

        double scale = (double) (range - minLevel) / (double) (max - min);
        int inc = (int) (-scale * min + minLevel);
        int[] elevationCount = new int[range + 1];
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                elevationMap[w][h] = (int) Math.round(elevationMap[w][h] * scale) + inc;
                elevationCount[MathUtility.clamp(elevationMap[w][h], 0, range)]++;
            }
        }

        int mostElevation = 0;
        for (int lvl = 1; lvl <= range; lvl++) {
            if (elevationCount[lvl] > elevationCount[mostElevation]) {
                mostElevation = lvl;
            }
        }

        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                elevationMap[w][h] -= mostElevation;
                if (invert) {
                    elevationMap[w][h] *= -1;
                }
            }
        }
        // invert negative terrain?
        if (invertNegative == 1) {
            for (int w = 0; w < width; w++) {
                for (int h = 0; h < height; h++) {
                    if (elevationMap[w][h] < 0) {
                        elevationMap[w][h] *= -1;
                    }
                }
            }
        }
    }

    public static void generateMountain(Board board, int width, Coords centre, int height,
                                        int capStyle) {
        final int mapW = board.getWidth();
        final int mapH = board.getHeight();

        for (int x = 0; x < mapW; x++) {
            for (int y = 0; y < mapH; y++) {
                Coords c = new Coords(x, y);
                int distance = c.distance(centre);
                int elev = (100 * height * (width - distance)) / width;
                elev = (elev / 100)
                        + (Compute.randomInt(100) < (elev % 100) ? 1 : 0);

                Hex hex = board.getHex(c);

                if (elev >= height - 2) {
                    switch (capStyle) {
                        case MapSettings.MOUNTAIN_SNOWCAPPED:
                            hex.setTheme("snow");
                            break;
                        case MapSettings.MOUNTAIN_VOLCANO_ACTIVE:
                        case MapSettings.MOUNTAIN_VOLCANO_DORMANT:
                            hex.setTheme("lunar");
                            break;
                        case MapSettings.MOUNTAIN_LAKE:
                            int lake = (width / 4);
                            int depth = ((lake - distance) + 1);
                            if (depth < 1) { // eliminates depth 0 water
                                depth = 1;
                            }
                            hex.addTerrain(new Terrain(Terrains.WATER, (depth)));
                            elev -= (Math.abs(lake - elev) - 1);
                            break;
                    }
                }

                if (elev == height) {
                    // for volcanoes, invert the peak
                    switch (capStyle) {
                        case MapSettings.MOUNTAIN_VOLCANO_ACTIVE:
                            hex.removeAllTerrains();
                            hex.addTerrain(new Terrain(Terrains.MAGMA, 2));
                            elev -= 2;
                            break;
                        case MapSettings.MOUNTAIN_VOLCANO_DORMANT:
                            hex.removeAllTerrains();
                            hex.addTerrain(new Terrain(Terrains.MAGMA, 1));
                            elev -= 2;
                            break;
                        case MapSettings.MOUNTAIN_VOLCANO_EXTINCT:
                            hex.setTheme("lunar");
                            elev -= 2;
                            break;
                    }
                }

                if (hex.getLevel() < elev) {
                    hex.setLevel(elev);
                }
            }
        }

    }

    /**
     * Flips the board around the vertical axis (North-for-South) and/or the
     * horizontal axis (East-for-West). The dimensions of the board will remain
     * the same, but the terrain of the hexes will be switched.
     *
     * @param horiz - a <code>boolean</code> value that, if <code>true</code>,
     *            indicates that the board is being flipped North-for-South.
     * @param vert - a <code>boolean</code> value that, if <code>true</code>,
     *            indicates that the board is being flipped East-for-West.
     */
    public static void flip(Board board, boolean horiz, boolean vert) {
        // If we're not flipping around *some* axis, do nothing.
        if (!vert && !horiz) {
            return;
        }

        // We only walk through half the board, but *which* half?
        int stopX;
        int stopY;
        int width = board.getWidth();
        int height = board.getHeight();

        if (horiz) {
            // West half of board.
            stopX = width / 2;
            stopY = height;
        } else {
            // North half of board.
            stopX = width;
            stopY = height / 2;
        }

        // Walk through the current data array and build a new one.
        int newX;
        int newY;
        Hex tempHex;
        Terrain terr;
        for (int oldX = 0; oldX < stopX; oldX++) {
            // Calculate the new X position of the flipped hex.
            if (horiz) {
                newX = width - oldX - 1;
            } else {
                newX = oldX;
            }
            for (int oldY = 0; oldY < stopY; oldY++) {
                // Calculate the new Y position of the flipped hex.
                if (vert) {
                    newY = height - oldY - 1;
                } else {
                    newY = oldY;
                }

                // Swap the old hex for the new hex.
                tempHex = board.getHex(oldX, oldY);
                board.setHex(oldX, oldY, board.getHex(newX, newY));
                board.setHex(newX, newY, tempHex);

                Hex newHex = board.getHex(newX, newY);
                Hex oldHex = board.getHex(oldX, oldY);

                // Update the road exits in the swapped hexes.
                terr = newHex.getTerrain(Terrains.ROAD);
                if (null != terr) {
                    terr.flipExits(horiz, vert);
                }
                terr = oldHex.getTerrain(Terrains.ROAD);
                if (null != terr) {
                    terr.flipExits(horiz, vert);
                }

                // Update the building exits in the swapped hexes.
                terr = newHex.getTerrain(Terrains.BUILDING);
                if (null != terr) {
                    terr.flipExits(horiz, vert);
                }
                terr = oldHex.getTerrain(Terrains.BUILDING);
                if (null != terr) {
                    terr.flipExits(horiz, vert);
                }

                // Update the fuel tank exits in the swapped hexes.
                terr = newHex.getTerrain(Terrains.FUEL_TANK);
                if (null != terr) {
                    terr.flipExits(horiz, vert);
                }
                terr = oldHex.getTerrain(Terrains.FUEL_TANK);
                if (null != terr) {
                    terr.flipExits(horiz, vert);
                }

                // Update the bridge exits in the swapped hexes.
                terr = newHex.getTerrain(Terrains.BRIDGE);
                if (null != terr) {
                    terr.flipExits(horiz, vert);
                }
                terr = oldHex.getTerrain(Terrains.BRIDGE);
                if (null != terr) {
                    terr.flipExits(horiz, vert);
                }
            }
        }
    }

    /**
     * one of the landscape generation algorithms
     */
    protected static void cutSteps(int hilliness, int width, int height, int[][] elevationMap) {
        Point p1, p2;
        int sideA, sideB;
        int type;

        p1 = new Point(0, 0);
        p2 = new Point(0, 0);
        for (int step = 0; step < hilliness * 20; step++) {
            /*
             * select which side should be decremented, and which incremented
             */
            sideA = (Compute.randomInt(2) == 0) ? -1 : 1;
            sideB = -sideA;
            type = Compute.randomInt(6);
            /*
             * 6 different lines in rectangular area from border to border
             * possible
             */
            switch (type) {
                case 0: /* left to upper border */
                    p1.setLocation(0, Compute.randomInt(height));
                    p2.setLocation(Compute.randomInt(width), height - 1);
                    markSides(p1, p2, sideB, sideA, elevationMap, height);
                    markRect(p2.x, width, sideA, elevationMap, height);
                    break;
                case 1: /* upper to lower border */
                    p1.setLocation(Compute.randomInt(width), 0);
                    p2.setLocation(Compute.randomInt(width), height - 1);
                    if (p1.x < p2.x) {
                        markSides(p1, p2, sideA, sideB, elevationMap, height);
                    } else {
                        markSides(p2, p1, sideB, sideA, elevationMap, height);
                    }
                    markRect(0, p1.x, sideA, elevationMap, height);
                    markRect(p2.x, width, sideB, elevationMap, height);
                    break;
                case 2: /* upper to right border */
                    p1.setLocation(Compute.randomInt(width), height - 1);
                    p2.setLocation(width, Compute.randomInt(height));
                    markSides(p1, p2, sideB, sideA, elevationMap, height);
                    markRect(0, p1.x, sideA, elevationMap, height);
                    break;
                case 3: /* left to right border */
                    p1.setLocation(0, Compute.randomInt(height));
                    p2.setLocation(width, Compute.randomInt(height));
                    markSides(p1, p2, sideA, sideB, elevationMap, height);
                    break;
                case 4: /* left to lower border */
                    p1.setLocation(0, Compute.randomInt(height));
                    p2.setLocation(Compute.randomInt(width), 0);
                    markSides(p1, p2, sideB, sideA, elevationMap, height);
                    markRect(p2.x, width, sideB, elevationMap, height);
                    break;
                case 5: /* lower to right border */
                    p1.setLocation(Compute.randomInt(width), 0);
                    p2.setLocation(width, Compute.randomInt(height));
                    markSides(p1, p2, sideB, sideA, elevationMap, height);
                    markRect(0, p1.x, sideB, elevationMap, height);
                    break;
            }

        }
    }

    /**
     * Helper function for the map generator increased a heightmap by a given value
     */
    protected static void markRect(int x1, int x2, int inc, int[][] elevationMap, int height) {
        for (int x = x1; x < x2; x++) {
            for (int y = 0; y < height; y++) {
                elevationMap[x][y] += inc;
            }
        }
    }

    /**
     * Helper function for map generator increases all of one side and decreased
     * on other side
     */
    protected static void markSides(Point p1, Point p2, int upperInc, int lowerInc,
                                    int[][] elevationMap, int height) {
        for (int x = p1.x; x < p2.x; x++) {
            for (int y = 0; y < height; y++) {
                int point = (p2.y - p1.y) / (p2.x - p1.x) * (x - p1.x) + p1.y;
                if (y > point) {
                    elevationMap[x][y] += upperInc;
                } else if (y < point) {
                    elevationMap[x][y] += lowerInc;
                }
            }
        }
    }

    /**
     * midpoint algorithm for landscape generation
     */
    protected static void midPoint(int hilliness, int width, int height, int[][] elevationMap) {
        int size;
        int steps = 1;
        int[][] tmpElevation;

        size = Math.max(width, height);
        while (size > 0) {
            steps++;
            size /= 2;
        }
        size = (1 << steps) + 1;
        tmpElevation = new int[size + 1][size + 1];
        /* init elevation map with 0 */
        for (int w = 0; w < size; w++) {
            for (int h = 0; h < size; h++) {
                if ((w < width) && (h < height)) {
                    tmpElevation[w][h] = elevationMap[w][h];
                } else {
                    tmpElevation[w][h] = 0;
                }
            }
        }
        for (int i = steps; i > 0; i--) {
            midPointStep((double) hilliness / 100, size, 100, tmpElevation, i, true);
        }
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                elevationMap[w][h] = tmpElevation[w][h];
            }
        }
    }

    /**
     * Helper function for landscape generation
     */
    protected static void midPointStep(double fracdim, int size, int delta, int[][] elevationMap,
                                       int step, boolean newBorder) {
        int d1, d2;
        int delta5;
        int x, y;

        d1 = size >> (step - 1);
        d2 = d1 / 2;
        fracdim = (1.0 - fracdim) / 2.0;
        delta = (int) (delta * Math.exp(-0.6931 * fracdim * (2.0 * step - 1)));
        delta5 = delta << 5;
        x = d2;
        do {
            y = d2;
            do {
                elevationMap[x][y] = middleValue(elevationMap[x + d2][y + d2],
                        elevationMap[x + d2][y - d2], elevationMap[x - d2][y
                                + d2], elevationMap[x - d2][y - d2], delta5);
                y += d1;
            } while (y < size - d2);
            x += d1;
        } while (x < size - d2);

        delta = (int) (delta * Math.exp(-0.6931 * fracdim));
        delta5 = delta << 5;
        if (newBorder) {
            x = d2;
            do {
                y = x;
                elevationMap[0][x] = middleValue(elevationMap[0][x + d2],
                        elevationMap[0][x - d2], elevationMap[d2][x], delta5);
                elevationMap[size][x] = middleValue(elevationMap[size - 1][x
                        + d2], elevationMap[size - 1][x - d2],
                        elevationMap[size - d2 - 1][x], delta5);
                y = 0;
                elevationMap[x][0] = middleValue(elevationMap[x + d2][0],
                        elevationMap[x - d2][0], elevationMap[x][d2], delta5);
                elevationMap[x][size] = middleValue(
                        elevationMap[x + d2][size - 1],
                        elevationMap[x - d2][size - 1], elevationMap[x][size
                                - d2 - 1], delta5);
                x += d1;
            } while (x < size - d2);
        }
        diagMid(new Point(d2, d1), d1, d2, delta5, size, elevationMap);
        diagMid(new Point(d1, d2), d1, d2, delta5, size, elevationMap);
    }

    /**
     * calculates the diagonal middlepoints with new values
     *
     * @param p Starting point.
     */
    protected static void diagMid(Point p, int d1, int d2, int delta, int size, int[][] elevationMap) {
        int x = p.x;
        int y;
        int hx = x + d2;
        int hy;

        while ((x < size - d2) && (hx < size)) {
            y = p.y;
            hy = y + d2;
            while ((y < size - d2) && (hy < size)) {
                elevationMap[x][y] = middleValue(elevationMap[x][hy],
                        elevationMap[x][y - d2], elevationMap[hx][y],
                        elevationMap[x - d2][y], delta);
                y += d1;
                hy = y + d2;
            }
            x += d1;
            hx = x + d2;
        }
    }

    /**
     * calculates the arithmetic medium of 3 values and add random value in
     * range of delta.
     */
    protected static int middleValue(int a, int b, int c, int delta) {
        return (((a + b + c) / 3) + normRNG(delta));
    }

    /**
     * calculates the arithmetic medium of 4 values and add random value in
     * range of delta.
     */
    protected static int middleValue(int a, int b, int c, int d, int delta) {
        return (((a + b + c + d) / 4) + normRNG(delta));
    }

    /**
     * Gives a normal distributed Randomvalue, with mediumvalue from 0 and a
     * Varianz of factor.
     *
     * @param factor varianz of of the distribution.
     * @return Random number, most times in the range -factor .. +factor, at
     *         most in the range of -3*factor .. +3*factor.
     */
    private static int normRNG(int factor) {
        factor++;
        return (2 * (Compute.randomInt(factor) + Compute.randomInt(factor) + Compute
                .randomInt(factor)) - 3 * (factor - 1)) / 32;
    }

    /**
     * Figures out the "closest" edge for the given entity on the entity's game board
     * @param entity Entity to evaluate
     * @return the Board.START_ constant representing the "closest" edge
     */
    public static CardinalEdge getClosestEdge(Entity entity) {
        int distanceToWest = entity.getPosition().getX();
        int distanceToEast = entity.getGame().getBoard().getWidth() - entity.getPosition().getX();
        int distanceToNorth = entity.getPosition().getY();
        int distanceToSouth = entity.getGame().getBoard().getHeight() - entity.getPosition().getY();

        boolean closerWestThanEast = distanceToWest < distanceToEast;
        boolean closerNorthThanSouth = distanceToNorth < distanceToSouth;

        int horizontalDistance = Math.min(distanceToWest, distanceToEast);
        int verticalDistance = Math.min(distanceToNorth, distanceToSouth);

        if (horizontalDistance < verticalDistance) {
            return closerWestThanEast ? CardinalEdge.WEST : CardinalEdge.EAST;
        } else {
            return closerNorthThanSouth ? CardinalEdge.NORTH : CardinalEdge.SOUTH;
        }
    }
    
    /**
     * Figures out the "opposite" edge for the given entity.
     * @param entity Entity to evaluate
     * @return the Board.START_ constant representing the "opposite" edge
     */
    public static CardinalEdge determineOppositeEdge(Entity entity) {
        Board board = entity.getGame().getBoard();

        // the easiest part is if the entity is supposed to start on a particular edge. Just return the opposite edge.
        int oppositeEdge = board.getOppositeEdge(entity.getStartingPos());
        if (oppositeEdge != Board.START_NONE) {
            return CardinalEdge.getCardinalEdge(OffBoardDirection.translateBoardStart(oppositeEdge));
        }

        // otherwise, we determine which edge of the board is closest to current position and return the opposite edge
        // in case of tie, vertical distance wins over horizontal distance
        CardinalEdge closestEdge = getClosestEdge(entity);
        return CardinalEdge.getOppositeEdge(closestEdge);
    }

    protected static class Point {
        public int x;
        public int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Point(Point other) {
            x = other.x;
            y = other.y;
        }

        /**
         * Set the location
         *
         * @param x x coordinate
         * @param y y coordinate
         */
        public void setLocation(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

}
