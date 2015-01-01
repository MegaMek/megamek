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

package megamek.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Hex;
import megamek.common.IBoard;
import megamek.common.IHex;
import megamek.common.ITerrain;
import megamek.common.ITerrainFactory;
import megamek.common.MapSettings;
import megamek.common.PlanetaryConditions;
import megamek.common.Terrains;

public class BoardUtilities {
    /**
     * Combines one or more boards into one huge megaboard!
     * 
     * @param width the width of each individual board, before the combine
     * @param height the height of each individual board, before the combine
     * @param sheetWidth how many sheets wide the combined map is
     * @param sheetHeight how many sheets tall the combined map is
     * @param boards an array of the boards to be combined
     */
    public static IBoard combine(int width, int height, int sheetWidth,
            int sheetHeight, IBoard[] boards, int medium) {

        int resultWidth = width * sheetWidth;
        int resultHeight = height * sheetHeight;

        IHex[] resultData = new IHex[resultWidth * resultHeight];
        boolean roadsAutoExit = true;

        // Copy the data from the sub-boards.
        for (int i = 0; i < sheetHeight; i++) {
            for (int j = 0; j < sheetWidth; j++) {
                IBoard b = boards[i * sheetWidth + j];
                if (b.getWidth() != width || b.getHeight() != height) {
                    throw new IllegalArgumentException(
                            "board is the wrong size, expected " + width + "x"
                                    + height + ", got " + b.getWidth() + "x"
                                    + b.getHeight());
                }
                copyBoardInto(resultData, resultWidth, j * width, i * height,
                        boards[i * sheetWidth + j]);
                // Copy in the other board's options.
                if (boards[i * sheetWidth + j].getRoadsAutoExit() == false) {
                    roadsAutoExit = false;
                }
            }
        }

        IBoard result = new Board();
        result.setRoadsAutoExit(roadsAutoExit);
        // Initialize all hexes - buildings, exits, etc
        result.newData(resultWidth, resultHeight, resultData);

        //assuming that the map setting and board types match
        result.setType(medium);
        
        return result;
    }

    /**
     * Copies the data of another board into given array of Hexes, offset by the
     * specified x and y.
     */
    protected static void copyBoardInto(IHex[] dest, int destWidth, int x,
            int y, IBoard copied) {
        for (int i = 0; i < copied.getHeight(); i++) {
            for (int j = 0; j < copied.getWidth(); j++) {
                dest[(i + y) * destWidth + j + x] = copied.getHex(j, i);
            }
        }
    }

    /**
     * Generates a Random Board
     * 
     * @param width The width of the generated Board.
     * @param height The height of the gernerated Board.
     * @param steps how often the iterative method should be repeated
     */
    public static IBoard generateRandom(MapSettings mapSettings) {
        int elevationMap[][] = new int[mapSettings.getBoardWidth()][mapSettings
                .getBoardHeight()];
        double sizeScale = (double) (mapSettings.getBoardWidth() * mapSettings
                .getBoardHeight())
                / ((double) (16 * 17));

        generateElevation(mapSettings.getHilliness(), mapSettings
                .getBoardWidth(), mapSettings.getBoardHeight(), mapSettings
                .getRange() + 1, mapSettings.getProbInvert(), mapSettings
                .getInvertNegativeTerrain(), elevationMap, mapSettings
                .getAlgorithmToUse());

        IHex[] nb = new IHex[mapSettings.getBoardWidth()
                * mapSettings.getBoardHeight()];
        int index = 0;
        for (int h = 0; h < mapSettings.getBoardHeight(); h++) {
            for (int w = 0; w < mapSettings.getBoardWidth(); w++) {
                if(mapSettings.getMedium() == MapSettings.MEDIUM_SPACE) {
                    nb[index++] = new Hex(0,"space:1",mapSettings.getTheme());
                } else {
                    nb[index++] = new Hex(elevationMap[w][h], "", mapSettings
                            .getTheme());
                }
            }
        }

        IBoard result = new Board(mapSettings.getBoardWidth(), mapSettings
                .getBoardHeight(), nb);
        
        if(mapSettings.getMedium() == MapSettings.MEDIUM_SPACE) {
            result.setType(Board.T_SPACE);
            return result;
        }
        
        /* initalize reverseHex */
        HashMap<IHex, Point> reverseHex = new HashMap<IHex, Point>(2
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

        /* Add the woods */
        int count = mapSettings.getMinForestSpots();
        if (mapSettings.getMaxForestSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxForestSpots());
        }
        count *= sizeScale;
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.WOODS,
                    mapSettings.getProbHeavy(), mapSettings.getMinForestSize(),
                    mapSettings.getMaxForestSize(), reverseHex, true);
        }
        /* Add the rough */
        count = mapSettings.getMinRoughSpots();
        if (mapSettings.getMaxRoughSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxRoughSpots());
        }
        count *= sizeScale;
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.ROUGH, 0, mapSettings
                    .getMinRoughSize(), mapSettings.getMaxRoughSize(),
                    reverseHex, true);
        }
        /* Add the swamp */
        count = mapSettings.getMinSwampSpots();
        if (mapSettings.getMaxSwampSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxSwampSpots());
        }
        count *= sizeScale;
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.SWAMP, 0, mapSettings
                    .getMinSwampSize(), mapSettings.getMaxSwampSize(),
                    reverseHex, false); // can stack with woods or roughs
        }

        // Add the Fortified hexes
        count = mapSettings.getMinFortifiedSpots();
        if (mapSettings.getMaxFortifiedSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxFortifiedSpots());
        }
        count *= sizeScale;
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.FORTIFIED, 0, mapSettings
                    .getMinFortifiedSize(), mapSettings.getMaxFortifiedSize(),
                    reverseHex, false);
        }

        // Add the rubble
        count = mapSettings.getMinRubbleSpots();
        if (mapSettings.getMaxRubbleSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxRubbleSpots());
        }
        count *= sizeScale;
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.RUBBLE, 0, mapSettings
                    .getMinRubbleSize(), mapSettings.getMaxRubbleSize(),
                    reverseHex, true);
        }

        /* Add the water */
        count = mapSettings.getMinWaterSpots();
        if (mapSettings.getMaxWaterSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxWaterSpots());
        }
        count *= sizeScale;
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.WATER, mapSettings.getProbDeep(),
                    mapSettings.getMinWaterSize(), mapSettings
                            .getMaxWaterSize(), reverseHex, true);
        }
        /* Add the pavements */
        count = mapSettings.getMinPavementSpots();
        if (mapSettings.getMaxPavementSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxPavementSpots());
        }
        count *= sizeScale;
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.PAVEMENT, 0, mapSettings
                    .getMinPavementSize(), mapSettings.getMaxPavementSize(),
                    reverseHex, true);
        }

        /* Add the ice */
        count = mapSettings.getMinIceSpots();
        if (mapSettings.getMaxIceSpots() > 0) {
            count += Compute.randomInt(mapSettings.getMaxIceSpots());
        }
        count *= sizeScale;
        for (int i = 0; i < count; i++) {
            placeSomeTerrain(result, Terrains.ICE, 0, mapSettings
                    .getMinIceSize(), mapSettings.getMaxIceSize(), reverseHex,
                    true);
        }

        /* Add the craters */
        if (Compute.randomInt(100) < mapSettings.getProbCrater()) {
            addCraters(result, mapSettings.getMinRadius(), mapSettings
                    .getMaxRadius(),
                    (int) (mapSettings.getMinCraters() * sizeScale),
                    (int) (mapSettings.getMaxCraters() * sizeScale));
        }

        /* Add the river */
        if (Compute.randomInt(100) < mapSettings.getProbRiver()) {
            addRiver(result, reverseHex);
        }

        /* Add special effects */
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

        /* Add the road */
        boolean roadNeeded = false;
        if (Compute.randomInt(100) < mapSettings.getProbRoad()) {
            roadNeeded = true;
        }

        // add buildings
        ArrayList<BuildingTemplate> buildings = mapSettings.getBoardBuildings();
        CityBuilder cityBuilder = new CityBuilder(mapSettings, result);
        if (buildings.size() == 0) {
            buildings = cityBuilder.generateCity(roadNeeded);
        }
        for (int i = 0; i < buildings.size(); i++) {
            placeBuilding(result, (buildings.get(i)));
        }
        return result;
    }

    private static void placeBuilding(IBoard board, BuildingTemplate building) {
        int type = building.getType();
        int cf = building.getCF();
        int height = building.getHeight();
        ITerrainFactory tf = Terrains.getTerrainFactory();
        ArrayList<IHex> hexes = new ArrayList<IHex>();
        int level = 0;
        for (Iterator<Coords> i = building.getCoords(); i.hasNext();) {
            Coords c = i.next();
            IHex hex = board.getHex(c);
            // work out exits...
            int exits = 0;
            for (int dir = 0; dir < 6; dir++) {
                if (building.containsCoords(c.translated(dir))) {
                    exits |= (1 << dir);
                }
            }

            // remove everything
            hex.removeAllTerrains();
            hex.addTerrain(tf.createTerrain(Terrains.PAVEMENT, 1));
            hex.addTerrain(tf.createTerrain(Terrains.BUILDING, type, true,
                    exits));
            hex.addTerrain(tf.createTerrain(Terrains.BLDG_CF, cf));
            hex.addTerrain(tf.createTerrain(Terrains.BLDG_ELEV, height));
            // hex.addTerrain(tf.createTerrain(Terrains.BLDG_BASEMENT,
            // building.getBasement()));
            hexes.add(hex);
            level += hex.getElevation();
        }
        // set everything to the same level
        for (int j = 0; j < hexes.size(); j++) {
            hexes.get(j).setElevation(level / hexes.size());
        }
    }

    /**
     * Places randomly some connected Woods.
     * 
     * @param probHeavy The probability that a wood is a heavy wood (in %).
     * @param maxWoods Maximum Number of Woods placed.
     */
    protected static void placeSomeTerrain(IBoard board, int terrainType,
            int probMore, int minHexes, int maxHexes,
            HashMap<IHex, Point> reverseHex, boolean exclusive) {
        Point p = new Point(Compute.randomInt(board.getWidth()), Compute
                .randomInt(board.getHeight()));
        int count = minHexes;
        if ((maxHexes - minHexes) > 0) {
            count += Compute.randomInt(maxHexes - minHexes);
        }
        IHex field;

        HashSet<IHex> alreadyUsed = new HashSet<IHex>();
        HashSet<IHex> unUsed = new HashSet<IHex>();
        field = board.getHex(p.x, p.y);
        if (!field.containsTerrain(terrainType)) {
            unUsed.add(field);
        } else {
            findAllUnused(board, terrainType, alreadyUsed, unUsed, field,
                    reverseHex);
        }
        ITerrainFactory f = Terrains.getTerrainFactory();
        for (int i = 0; i < count; i++) {
            if (unUsed.isEmpty()) {
                return;
            }
            int which = Compute.randomInt(unUsed.size());
            Iterator<IHex> iter = unUsed.iterator();
            for (int n = 0; n < (which - 1); n++)
                iter.next();
            field = iter.next();
            if (exclusive) {
                field.removeAllTerrains();
            }
            int tempInt = (Compute.randomInt(100) < probMore) ? 2 : 1;
            ITerrain tempTerrain = f.createTerrain(terrainType, tempInt);
            field.addTerrain(tempTerrain);
            unUsed.remove(field);
            findAllUnused(board, terrainType, alreadyUsed, unUsed, field,
                    reverseHex);
        }

        if (terrainType == Terrains.WATER) {
            /*
             * if next to an Water Hex is an lower lvl lower the hex. First we
             * search for lowest Hex next to the lake
             */
            int min = Integer.MAX_VALUE;
            Iterator<IHex> iter = unUsed.iterator();
            while (iter.hasNext()) {
                field = iter.next();
                if (field.getElevation() < min) {
                    min = field.getElevation();
                }
            }
            iter = alreadyUsed.iterator();
            while (iter.hasNext()) {
                field = iter.next();
                field.setElevation(min);
            }

        }
    }

    /**
     * Searching starting from one Hex, all Terrains not matching terrainType,
     * next to one of terrainType.
     * 
     * @param terrainType The terrainType which the searching hexes should not
     *            have.
     * @param alreadyUsed The hexes which should not looked at (because they are
     *            already supposed to visited in some way)
     * @param unUsed In this set the resulting hexes are stored. They are stored
     *            in addition to all previously stored.
     * @param searchFrom The Hex where to start
     */
    private static void findAllUnused(IBoard board, int terrainType,
            HashSet<IHex> alreadyUsed, HashSet<IHex> unUsed, IHex searchFrom,
            HashMap<IHex, Point> reverseHex) {
        IHex field;
        HashSet<IHex> notYetUsed = new HashSet<IHex>();

        notYetUsed.add(searchFrom);
        do {
            Iterator<IHex> iter = notYetUsed.iterator();
            field = iter.next();
            if (field == null) {
                continue;
            }
            for (int dir = 0; dir < 6; dir++) {
                Point loc = reverseHex.get(field);
                IHex newHex = board.getHexInDir(loc.x, loc.y, dir);
                if ((newHex != null) && (!alreadyUsed.contains(newHex))
                        && (!notYetUsed.contains(newHex))
                        && (!unUsed.contains(newHex))) {
                    ((newHex.containsTerrain(terrainType)) ? notYetUsed
                            : unUsed).add(newHex);
                }
            }
            notYetUsed.remove(field);
            alreadyUsed.add(field);
        } while (!notYetUsed.isEmpty());
    }

    /**
     * add a crater to the board
     */
    public static void addCraters(IBoard board, int minRadius, int maxRadius,
            int minCraters, int maxCraters) {
        int numberCraters = minCraters;
        if (maxCraters > minCraters) {
            numberCraters += Compute.randomInt(maxCraters - minCraters);
        }
        for (int i = 0; i < numberCraters; i++) {
            int width = board.getWidth();
            int height = board.getHeight();
            Point center = new Point(Compute.randomInt(width), Compute
                    .randomInt(height));

            int radius = Compute.randomInt(maxRadius - minRadius + 1)
                    + minRadius;
            int maxLevel = 3;
            if (radius < 3) {
                maxLevel = 1;
            }
            if ((radius >= 3) && (radius <= 8)) {
                maxLevel = 2;
            }
            if (radius > 14) {
                maxLevel = 4;
            }
            int maxHeight = Compute.randomInt(maxLevel) + 1;
            /* generate CraterProfile */
            int cratHeight[] = new int[radius];
            for (int x = 0; x < radius; x++) {
                cratHeight[x] = craterProfile((double) x / (double) radius,
                        maxHeight);
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
                        IHex field = board.getHex(w, h);
                        field.setElevation(// field.getElevation() +
                                cratHeight[distance]);
                    }
                }
            }
        }
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
    public static int craterProfile(double x, int scale) {
        double result = 0;

        result = (x < 0.75) ? ((Math.exp(x * 5.0 / 0.75 - 3) - 0.04979) * 1.5 / 7.33926) - 0.5
                : ((Math.cos((x - 0.75) * 4.0) + 1.0) / 2.0);

        return (int) (result * scale);
    }

    /**
     * calculate the distance between two points
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
    public static void addRiver(IBoard board, HashMap<IHex, Point> reverseHex) {
        int minElevation = Integer.MAX_VALUE;
        HashSet<IHex> riverHexes = new HashSet<IHex>();
        IHex field;
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
        ITerrainFactory f = Terrains.getTerrainFactory();
        do {
            /* first the hex itself */
            field.removeAllTerrains();
            field.addTerrain(f.createTerrain(Terrains.WATER, 1));
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
        HashSet<IHex> tmpRiverHexes = new HashSet<IHex>(riverHexes);
        while (!tmpRiverHexes.isEmpty()) {
            Iterator<IHex> iter = tmpRiverHexes.iterator();
            field = iter.next();
            if (field.getElevation() < minElevation) {
                minElevation = field.getElevation();
            }
            tmpRiverHexes.remove(field);
            Point thisHex = reverseHex.get(field);
            /* and now the six neighbours */
            for (int i = 0; i < 6; i++) {
                field = board.getHexInDir(thisHex.x, thisHex.y, i);
                if ((field != null) && (field.getElevation() < minElevation)) {
                    minElevation = field.getElevation();
                }
                tmpRiverHexes.remove(field);
            }
        }

        /* now adjust the elevation to same height */
        Iterator<IHex> iter = riverHexes.iterator();
        while (iter.hasNext()) {
            field = iter.next();
            field.setElevation(minElevation);
        }

        return;
    }

    /**
     * Extends a river hex to left and right sides.
     * 
     * @param hexloc The location of the river hex, from which it should get
     *            started.
     * @param width The width to wich the river should extend in the direction.
     *            So the actual width of the river is 2*width+1.
     * @param direction Direction too which the riverhexes should be extended.
     * @return Hashset with the hexes from the side.
     */
    private static HashSet<IHex> extendRiverToSide(IBoard board, Point hexloc,
            int width, int direction, HashMap<IHex, Point> reverseHex) {
        Point current = new Point(hexloc);
        HashSet<IHex> result = new HashSet<IHex>();
        IHex hex;

        hex = board.getHexInDir(current.x, current.y, direction);
        while ((hex != null) && (width-- > 0)) {
            hex.removeAllTerrains();
            hex.addTerrain(Terrains.getTerrainFactory().createTerrain(
                    Terrains.WATER, 1));
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
    protected static void postProcessFlood(IHex[] hexSet, int modifier) {
        int n;
        IHex field;
        ITerrainFactory f = Terrains.getTerrainFactory();
        for (n = 0; n < hexSet.length; n++) {
            field = hexSet[n];
            int elev = field.getElevation() - modifier;
            if (elev == 0 && !(field.containsTerrain(Terrains.WATER))
                    && !(field.containsTerrain(Terrains.PAVEMENT))) {
                field.addTerrain(f.createTerrain(Terrains.SWAMP, 1));
            } else if (elev < 0) {
                if (elev < -4)
                    elev = -4;
                field.removeAllTerrains();
                field.addTerrain(f.createTerrain(Terrains.WATER, -elev));
                field.setElevation(modifier);
            }
        }
    }

    /**
     * Converts water hexes to ice hexes. Works best with snow&ice theme.
     */
    protected static void postProcessDeepFreeze(IHex[] hexSet, int modifier) {
        int n;
        IHex field;
        ITerrainFactory f = Terrains.getTerrainFactory();
        for (n = 0; n < hexSet.length; n++) {
            field = hexSet[n];
            if (field.containsTerrain(Terrains.WATER)) {
                int level = field.terrainLevel(Terrains.WATER);
                if (modifier != 0) {
                    level -= modifier;
                    field.removeTerrain(Terrains.WATER);
                    if (level > 0) {
                        field.addTerrain(f.createTerrain(Terrains.WATER,
                                        level));
                    }
                }
                field.addTerrain(f.createTerrain(Terrains.ICE, 1));
            } else if (field.containsTerrain(Terrains.SWAMP)) {
                field.removeTerrain(Terrains.SWAMP);
                if (field.terrainsPresent() == 0) {
                    if (Compute.randomInt(100) < 30) {
                        // if no other terrains present, 30% chance to change to
                        // rough
                        field.addTerrain(f.createTerrain(Terrains.ROUGH, 1));
                    } else {
                        field.addTerrain(f.createTerrain(Terrains.ICE, 1));
                    }
                }
            }
        }
    }

    /**
     * Burning woods, with chance to be burnt down already
     */
    protected static void postProcessForestFire(IHex[] hexSet, int modifier) {
        int n;
        IHex field;
        int level, newlevel;
        int severity;
        ITerrainFactory f = Terrains.getTerrainFactory();
        for (n = 0; n < hexSet.length; n++) {
            field = hexSet[n];
            level = field.terrainLevel(Terrains.WOODS);
            if (level != ITerrain.LEVEL_NONE) {
                severity = Compute.randomInt(5) - 2 + modifier;
                newlevel = level - severity;

                if (newlevel <= level) {
                    field.removeTerrain(Terrains.WOODS);
                    if (newlevel <= 0) {
                        field.addTerrain(f.createTerrain(Terrains.ROUGH, 1));
                    } else {
                        field.addTerrain(f.createTerrain(Terrains.WOODS,
                                newlevel));
                        field.addTerrain(f.createTerrain(Terrains.FIRE, 1));
                    }
                }
            }
        }
    }

    /**
     * Dries up all bodies of water by 1-3 levels. dried up water becomes swamp
     * then rough
     */
    protected static void postProcessDrought(IHex[] hexSet, int modifier) {
        int n;
        IHex field;
        int level, newlevel;
        int severity = 1 + Compute.randomInt(3) + modifier;
        if (severity < 0)
            return;
        ITerrainFactory f = Terrains.getTerrainFactory();
        for (n = 0; n < hexSet.length; n++) {
            field = hexSet[n];
            if (field.containsTerrain(Terrains.SWAMP)) {
                field.removeTerrain(Terrains.SWAMP); // any swamps are dried
                                                        // up to hardened mud
                if (field.terrainsPresent() == 0 && Compute.randomInt(100) < 30) {
                    // if no other terrains present, 30% chance to change to
                    // rough
                    field.addTerrain(f.createTerrain(Terrains.ROUGH, 1));
                }
            }
            level = field.terrainLevel(Terrains.WATER);
            if (level != ITerrain.LEVEL_NONE) {
                newlevel = level - severity;
                field.removeTerrain(Terrains.WATER);
                if (newlevel == 0) {
                    field.addTerrain(f.createTerrain(Terrains.SWAMP, 1));
                } else if (newlevel < 0) {
                    field.addTerrain(f.createTerrain(Terrains.ROUGH, 1));
                } else {
                    field.addTerrain(f.createTerrain(Terrains.WATER, newlevel));
                }
                if (level > severity)
                    newlevel = severity;
                else
                    newlevel = level;

                field.setElevation(field.getElevation() - newlevel);
            }
        }
    }

    private static boolean hexCouldBeCliff(IBoard board, Coords c) {
        int elevation = board.getHex(c).getElevation();
        boolean higher = false;
        boolean lower = false;
        int count = 0;
        for (int dir = 0; dir < 6; dir++) {
            Coords t = c.translated(dir);
            if (board.contains(t)) {
                IHex hex = board.getHex(t);
                int el = hex.getElevation();
                if (el > elevation) {
                    lower = true;
                } else if (el < elevation) {
                    higher = true;
                } else {
                    count++;
                }
            }
        }
        return higher && lower && count <= 3 && count > 0;
    }

    private static void findCliffNeighbours(IBoard board, Coords c,
            ArrayList<Coords> candidate, HashSet<Coords> ignore) {
        candidate.add(c);
        ignore.add(c);
        int elevation = board.getHex(c).getElevation();
        for (int dir = 0; dir < 6; dir++) {
            Coords t = c.translated(dir);
            if (board.contains(t) && !ignore.contains(t)) {
                if (hexCouldBeCliff(board, t)) {
                    IHex hex = board.getHex(t);
                    int el = hex.getElevation();
                    if (el == elevation) {
                        findCliffNeighbours(board, t, candidate, ignore);
                    }
                } else
                    ignore.add(t);
            }
        }
    }

    protected static void addCliffs(IBoard board, int modifier) {
        HashSet<Coords> ignore = new HashSet<Coords>(); // previously considered
                                                        // hexes
        ArrayList<Coords> candidate = new ArrayList<Coords>();
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Coords c = new Coords(x, y);
                int elevation = board.getHex(c).getElevation();
                if (ignore.contains(c))
                    continue;
                if (!hexCouldBeCliff(board, c)) {
                    ignore.add(c);
                    continue;
                }

                findCliffNeighbours(board, c, candidate, ignore);
                // is the candidate interesting (at least 3 hexes)?
                if (candidate.size() >= 3 && Compute.randomInt(100) < modifier) {
                    if (elevation > 0)
                        elevation--;
                    else
                        elevation++;
                    for (Iterator<Coords> e = candidate.iterator(); e.hasNext();) {
                        c = e.next();
                        IHex hex = board.getHex(c);
                        hex.setElevation(elevation);
                    }
                }
                candidate.clear();
            }
        }
    }
    
    /*
     * adjust the board based on weather conditions
     */
    public static void addWeatherConditions(IBoard board, int weatherCond, int windCond) {
        ITerrainFactory tf = Terrains.getTerrainFactory();
        
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Coords c = new Coords(x, y);
                IHex hex = board.getHex(c);
                
                //moderate rain - mud in clear hexes, depth 0 water, and dirt roads (not implemented yet)
                if(weatherCond == PlanetaryConditions.WE_MOD_RAIN) {
                    if(hex.terrainsPresent() == 0 || (hex.containsTerrain(Terrains.WATER) && hex.depth() == 0)) {
                        hex.addTerrain(tf.createTerrain(Terrains.MUD, 1));
                        if(hex.containsTerrain(Terrains.WATER)) {
                            hex.removeTerrain(Terrains.WATER);
                        }
                    }
                }   
                
                //heavy rain - mud in all hexes except buildings, depth 1+ water, and non-dirt roads
                //rapids in all depth 1+ water
                if(weatherCond == PlanetaryConditions.WE_HEAVY_RAIN) {
                    if(hex.containsTerrain(Terrains.WATER) && !hex.containsTerrain(Terrains.RAPIDS) && hex.depth() > 0) {
                        hex.addTerrain(tf.createTerrain(Terrains.RAPIDS, 1));
                    }
                    else if(!hex.containsTerrain(Terrains.BUILDING) && !hex.containsTerrain(Terrains.ROAD)) {
                        hex.addTerrain(tf.createTerrain(Terrains.MUD, 1));
                        if(hex.containsTerrain(Terrains.WATER)) {
                            hex.removeTerrain(Terrains.WATER);
                        }
                    }
                }
                
                //torrential downpour - mud in all hexes except buildings, depth 1+ water, and non-dirt roads
                //torrent in all depth 1+ water, swamps in all depth 0 water hexes
                if(weatherCond == PlanetaryConditions.WE_DOWNPOUR) {
                    if(hex.containsTerrain(Terrains.WATER) && !(hex.terrainLevel(Terrains.RAPIDS) > 1) && hex.depth() > 0) {
                        hex.addTerrain(tf.createTerrain(Terrains.RAPIDS, 2));
                    }
                    else if(hex.containsTerrain(Terrains.WATER)) {
                        hex.addTerrain(tf.createTerrain(Terrains.SWAMP, 1));
                        hex.removeTerrain(Terrains.WATER);
                    }
                    else if(!hex.containsTerrain(Terrains.BUILDING) && !hex.containsTerrain(Terrains.ROAD)) {
                        hex.addTerrain(tf.createTerrain(Terrains.MUD, 1));
                    }
                }
                
                //check for rapids/torrents created by wind
                if(windCond > PlanetaryConditions.WI_MOD_GALE 
                        && hex.containsTerrain(Terrains.WATER) && hex.depth() > 0) {
                    
                    if(windCond > PlanetaryConditions.WI_STORM) {
                        if(!(hex.terrainLevel(Terrains.RAPIDS) > 1)) {
                            hex.addTerrain(tf.createTerrain(Terrains.RAPIDS, 2));
                        }
                    } else {
                        if(!hex.containsTerrain(Terrains.RAPIDS)) {
                            hex.addTerrain(tf.createTerrain(Terrains.RAPIDS, 1));
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
     * @param range Max difference betweenn highest and lowest level.
     * @param invertProb Probability for the invertion of the map (0..100)
     * @param invertNegate If 1, invert negative hexes, else do nothing
     * @param elevationMap here is the result stored
     */
    public static void generateElevation(int hilliness, int width, int height,
            int range, int invertProb, int invertNegative,
            int elevationMap[][], int algorithm) {
        int minLevel = 0;
        int maxLevel = range;
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
        }

        /* and now normalize it */
        int min = elevationMap[0][0];
        int max = elevationMap[0][0];
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                if (elevationMap[w][h] > max) {
                    max = elevationMap[w][h];
                }
                if (elevationMap[w][h] < min) {
                    min = elevationMap[w][h];
                }
            }
        }

        double scale = (double) (maxLevel - minLevel) / (double) (max - min);
        int inc = (int) (-scale * min + minLevel);
        int[] elevationCount = new int[maxLevel + 1];
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                elevationMap[w][h] *= scale;
                elevationMap[w][h] += inc;
                elevationCount[elevationMap[w][h]]++;
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

    public static void generateMountain(IBoard board, int width, Coords centre,
            int height, int capStyle) {
        final int mapW = board.getWidth();
        final int mapH = board.getHeight();

        ITerrainFactory tf = Terrains.getTerrainFactory();

        for (int x = 0; x < mapW; x++) {
            for (int y = 0; y < mapH; y++) {
                Coords c = new Coords(x, y);
                int distance = c.distance(centre);
                int elev = (100 * height * (width - distance)) / width;
                elev = (elev / 100)
                        + (Compute.randomInt(100) < (elev % 100) ? 1 : 0);

                IHex hex = board.getHex(c);

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
                            hex.addTerrain(tf.createTerrain(Terrains.WATER,
                                    (depth)));
                            elev -= (Math.abs(lake - elev) - 1);
                            break;
                    }
                }
                if (elev == height) {
                    // for volcanoes, invert the peak
                    switch (capStyle) {
                        case MapSettings.MOUNTAIN_VOLCANO_ACTIVE:
                            hex.removeAllTerrains();
                            hex.addTerrain(tf.createTerrain(Terrains.MAGMA, 2));
                            elev -= 2;
                            break;
                        case MapSettings.MOUNTAIN_VOLCANO_DORMANT:
                            hex.removeAllTerrains();
                            hex.addTerrain(tf.createTerrain(Terrains.MAGMA, 1));
                            elev -= 2;
                            break;
                        case MapSettings.MOUNTAIN_VOLCANO_EXTINCT:
                            hex.setTheme("lunar");
                            elev -= 2;
                            break;
                    }
                }

                if (hex.getElevation() < elev)
                    hex.setElevation(elev);
            }
        }

    }

    /**
     * Flips the board around the vertical axis (North-for-South) and/or the
     * horizontal axis (East-for-West). The dimensions of the board will remain
     * the same, but the terrain of the hexes will be swiched.
     * 
     * @param horiz - a <code>boolean</code> value that, if <code>true</code>,
     *            indicates that the board is being flipped North-for-South.
     * @param vert - a <code>boolean</code> value that, if <code>true</code>,
     *            indicates that the board is being flipped East-for-West.
     */
    public static void flip(IBoard board, boolean horiz, boolean vert) {
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
        IHex tempHex;
        ITerrain terr;
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

                IHex newHex = board.getHex(newX, newY);
                IHex oldHex = board.getHex(oldX, oldY);

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
    protected static void cutSteps(int hilliness, int width, int height,
            int elevationMap[][]) {
        Point p1, p2;
        int sideA, sideB;
        int type;

        p1 = new Point(0, 0);
        p2 = new Point(0, 0);
        for (int step = 0; step < hilliness * 20; step++) {
            /*
             * select which side should be decremented, and which increemented
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
     * Helper function for the map generator increased a heightmap my a given
     * value
     */
    protected static void markRect(int x1, int x2, int inc,
            int elevationMap[][], int height) {
        for (int x = x1; x < x2; x++) {
            for (int y = 0; y < height; y++) {
                elevationMap[x][y] += inc;
            }
        }
    }

    /**
     * Helper function for map generator inreases all of one side and decreased
     * on other side
     */
    protected static void markSides(Point p1, Point p2, int upperInc,
            int lowerInc, int elevationMap[][], int height) {
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
     * midpoint algorithm for landscape generartion
     */
    protected static void midPoint(int hilliness, int width, int height,
            int elevationMap[][]) {
        int size;
        int steps = 1;
        int tmpElevation[][];

        size = (width > height) ? width : height;
        while (size > 0) {
            steps++;
            size /= 2;
        }
        size = (1 << steps) + 1;
        tmpElevation = new int[size + 1][size + 1];
        /* init elevation map with 0 */
        for (int w = 0; w < size; w++)
            for (int h = 0; h < size; h++)
                if ((w < width) && (h < height)) {
                    tmpElevation[w][h] = elevationMap[w][h];
                } else {
                    tmpElevation[w][h] = 0;
                }
        for (int i = steps; i > 0; i--) {
            midPointStep((double) hilliness / 100, size, 100, tmpElevation, i,
                    true);
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
    protected static void midPointStep(double fracdim, int size, int delta,
            int elevationMap[][], int step, boolean newBorder) {
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
    protected static void diagMid(Point p, int d1, int d2, int delta, int size,
            int elevationMap[][]) {
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
        int result = (((a + b + c) / 3) + normRNG(delta));
        return result;
    }

    /**
     * calculates the arithmetic medium of 4 values and add random value in
     * range of delta.
     */
    protected static int middleValue(int a, int b, int c, int d, int delta) {
        int result = (((a + b + c + d) / 4) + normRNG(delta));
        return result;
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

    protected static class Point {

        public int x;
        public int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Point(Point other) {
            this.x = other.x;
            this.y = other.y;
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
