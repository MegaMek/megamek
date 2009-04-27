/*
 * MegaMek - Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;

import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.IBoard;
import megamek.common.IHex;
import megamek.common.ITerrain;
import megamek.common.ITerrainFactory;
import megamek.common.MapSettings;
import megamek.common.Terrains;

/**
 * @author Torren + Coelocanth
 */
public class CityBuilder {

    static final int N = 0;
    static final int NE = 1;
    static final int SE = 2;
    static final int S = 3;
    static final int SW = 4;
    static final int NW = 5;
    // Had to off set West and East as MM doesn't use them for hexes.
    static final int W = 6;
    static final int E = 7;

    private MapSettings mapSettings;
    private IBoard board;
    private HashSet<Coords> cityPlan;

    public CityBuilder(MapSettings mapSettings, IBoard board) {
        super();
        // Auto-generated constructor stub

        this.mapSettings = mapSettings;
        this.board = board;
    }

    /**
     * This function will generate a city with a grid lay out. 4 rounds running
     * North and South and 4 roads running east west
     * 
     * @author Torren (Jason Tighe)
     * @param buildingTemplate
     * @return
     */
    public ArrayList<BuildingTemplate> generateCity(boolean genericRoad) {

        int width = mapSettings.getBoardWidth();
        int height = mapSettings.getBoardHeight();
        int roads = mapSettings.getCityBlocks();
        roads = (roads * Math.min(width, height)) / 16; // scale for bigger maps
        String cityType = mapSettings.getCityType();

        cityPlan = new HashSet<Coords>();
        if (genericRoad) {
            addGenericRoad();
        }

        if (cityType.equalsIgnoreCase("HUB"))
            buildHubCity(width, height, roads);
        else if (cityType.equalsIgnoreCase("METRO"))
            buildMetroCity(width, height);
        else if (cityType.equalsIgnoreCase("GRID"))
            buildGridCity(width, height, (roads + 5) / 6);
        else if (cityType.equalsIgnoreCase("TOWN"))
            return buildTown(width, height, roads, mapSettings.getTownSize());
        else
            return new ArrayList<BuildingTemplate>();

        return placeBuildings(0);
    }

    public ArrayList<BuildingTemplate> placeBuildings(int radius) {
        int width = mapSettings.getBoardWidth();
        int height = mapSettings.getBoardHeight();
        ArrayList<BuildingTemplate> buildingList = new ArrayList<BuildingTemplate>();
        HashSet<Coords> buildingUsed = new HashSet<Coords>();

        ArrayList<Coords> coordList = new ArrayList<Coords>();

        Coords centre = new Coords(width / 2, height / 2);
        double falloff = (double) mapSettings.getCityDensity()
                / (double) (radius * radius);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Coords coord = new Coords(x, y);

                if (cityPlan.contains(coord) || buildingUsed.contains(coord)
                        || !board.contains(coord)
                        || !isHexBuildable(board.getHex(coord))) {
                    continue;
                }

                int localdensity = mapSettings.getCityDensity();
                if (radius > 0) {
                    int distance = coord.distance(centre);
                    localdensity = (int) (mapSettings.getCityDensity() - (falloff
                            * distance * distance));
                }

                if (Compute.randomInt(100) > localdensity) {
                    continue; // empty lot
                }
                coordList = new ArrayList<Coords>();
                coordList.add(coord);
                buildingUsed.add(coord);
                while (Compute.randomInt(100) < localdensity) {
                    // try to make a bigger building!
                    int dir = Compute.randomInt(6);
                    Coords next = coord.translated(dir);
                    if (cityPlan.contains(next) || buildingUsed.contains(next)
                            || !board.contains(next)
                            || !isHexBuildable(board.getHex(next))) {
                        break; // oh well, cant expand here
                    }
                    coordList.add(next);
                    buildingUsed.add(next);
                }

                int floors = mapSettings.getCityMaxFloors()
                        - mapSettings.getCityMinFloors();

                if (floors <= 0)
                    floors = mapSettings.getCityMinFloors();
                else
                    floors = Compute.randomInt(floors + 1)
                            + mapSettings.getCityMinFloors();

                int totalCF = mapSettings.getCityMaxCF()
                        - mapSettings.getCityMinCF();

                if (totalCF <= 0)
                    totalCF = mapSettings.getCityMinCF();
                else
                    totalCF = Compute.randomInt(totalCF + 1)
                            + mapSettings.getCityMinCF();

                int type = getBuildingTypeByCF(totalCF);

                buildingList.add(new BuildingTemplate(type, coordList, totalCF,
                        floors, -1));
            }
        }

        return buildingList;
    }

    private void buildGridCity(int maxX, int maxY, int roads) {
        for (int y = 0; y < roads; y++) {
            int startY = Compute.randomInt(maxY / roads) + ((y * maxY) / roads);
            // int start = Compute.randomInt(2);
            Coords coords = new Coords(-1, startY);
            int roadStyle = Compute.randomInt(2) + 1;
            int dir = Compute.randomInt(2) + NE;
            buildStraightRoad(coords, dir, roadStyle);
            startY = Compute.randomInt(maxY / roads) + ((y * maxY) / roads);
            coords = new Coords(maxX, startY);
            dir = Compute.randomInt(2) + SW;
            buildStraightRoad(coords, dir, roadStyle);
        }

        for (int x = 0; x < roads; x++) {
            int startX = Compute.randomInt(maxX / roads) + (x * (maxX / roads));
            Coords coords = new Coords(startX, -1);
            int roadStyle = Compute.randomInt(2) + 1;
            buildStraightRoad(coords, S, roadStyle);
        }
    }

    private ArrayList<BuildingTemplate> buildTown(int maxX, int maxY,
            int roads, int size) {
        buildHubCity(maxX, maxY, roads * size / 100);
        return placeBuildings(Math.min(maxX, maxY) * size / 200);
    }

    private void buildHubCity(int maxX, int maxY, int roads) {
        int midX = maxX / 2;
        int midY = maxY / 2;

        Vector<Integer> directions = new Vector<Integer>(8);

        directions.add(N);
        directions.add(NE);
        directions.add(SE);
        directions.add(S);
        directions.add(SW);
        directions.add(NW);
        directions.add(E);
        directions.add(W);

        roads = Math.max(roads, 4);
        cityPlan.add(new Coords(midX, midY));

        int x = 0;
        int y = 0;
        for (int dir = 0; dir < roads; dir++) {
            int baseDirection = -1;
            int roadStyle = Compute.randomInt(2) + 1;

            if (dir < 8) {
                x = midX;
                y = midY;
                baseDirection = directions.remove(Compute.randomInt(directions
                        .size()));
            } else {
                switch (Compute.randomInt(4)) {
                    case 1:
                        x = Compute.randomInt(maxX);
                        y = -1;
                        baseDirection = S;
                        break;
                    case 2:
                        x = Compute.randomInt(maxX);
                        y = maxY;
                        baseDirection = N;
                        break;
                    case 3:
                        x = -1;
                        y = Compute.randomInt(maxY);
                        baseDirection = NE + Compute.randomInt(2);
                        break;
                    default:
                        x = maxX;
                        y = Compute.randomInt(maxY);
                        baseDirection = SW + Compute.randomInt(2);
                        break;
                }
            }
            Coords coords = new Coords(x, y);

            int nextDirection = baseDirection;
            while (coords.x >= -1 && coords.x <= maxX && coords.y >= -1
                    && coords.y <= maxY) {
                int choice = Compute.randomInt(10);

                if (board.contains(coords)) {
                    // don't change direction offboard
                    if (choice < 4) {
                        // keep going
                    } else if (choice < 6) {
                        // turn left
                        nextDirection = (5 + nextDirection) % 6;
                    } else if (choice < 8) {
                        // turn right
                        nextDirection = (1 + nextDirection) % 6;
                    } else {
                        // turn towards base direction
                        nextDirection = baseDirection;
                    }
                }

                coords = extendRoad(coords, nextDirection, roadStyle);
                if (coords == null
                        || (cityPlan.contains(coords) && x != midX && y != midY)) {
                    break;
                }
                cityPlan.add(coords);

                x = coords.x;
                y = coords.y;
            }

        }
    }

    private void buildMetroCity(int maxX, int maxY) {
        int midX = maxX / 2;
        int midY = maxY / 2;

        cityPlan.add(new Coords(midX, midY));

        // have the city hub be the mid point with all the hexes around it
        // cleared out
        for (int hex = 0; hex < 6; hex++)
            cityPlan.add(new Coords(Coords.xInDir(midX, midY, hex), Coords
                    .yInDir(midX, midY, hex)));

        // first east west road
        Coords coords = new Coords(-1, midY / 2);
        buildStraightRoad(coords, E, 1);

        // second east west road
        coords = new Coords(-1, midY + (midY / 2));
        buildStraightRoad(coords, E, 1);

        // First North South Road
        coords = new Coords(midX / 2, -1);
        buildStraightRoad(coords, S, 1);

        // second North South Road
        coords = new Coords(midX + (midX / 2), -1);
        buildStraightRoad(coords, S, 1);

        for (int dir = 0; dir < 8; dir++) {
            coords = new Coords(midX, midY);
            buildStraightRoad(coords, dir, 2);

        }
    }

    private Coords selectNextGrid(int dir, Coords coords) {
        Coords result = coords.translated(dir);

        if (dir == E)
            result.x++;

        if (dir == W)
            result.x--;

        return result;
    }

    /**
     * @param hex
     * @return true if it is reasonable to build on this hex
     */
    private boolean isHexBuildable(IHex hex) {
        if (hex.containsTerrain(Terrains.WATER)
                || hex.containsTerrain(Terrains.IMPASSABLE)
                || hex.containsTerrain(Terrains.MAGMA)
                || hex.containsTerrain(Terrains.SWAMP)) {
            return false; // uneconomic to build here
        }
        if (hex.getElevation() >= 4) {
            return false; // don't build on mountaintops (aesthetics)
        }
        return true;
    }

    /**
     * @param hex
     * @return true if the hex needs a bridge to cross
     */
    private boolean hexNeedsBridge(IHex hex) {
        if (hex.containsTerrain(Terrains.ROAD)
                || hex.containsTerrain(Terrains.BRIDGE))
            return false;
        return (hex.containsTerrain(Terrains.WATER) || hex
                .containsTerrain(Terrains.MAGMA));
    }

    private void addRoad(IHex hex, int exitDirection, int type) {
        ITerrainFactory tf = Terrains.getTerrainFactory();
        if (hex.containsTerrain(Terrains.WATER)) {
            hex.removeTerrain(Terrains.WATER);
            hex.addTerrain(tf.createTerrain(Terrains.WATER, 0));
            type = 1;
        }
        hex.addTerrain(tf.createTerrain(Terrains.ROAD, type, true,
                (1 << exitDirection) & 63));
    }

    private void addBridge(IHex hex, int exits, int altitude, int cf) {
        ITerrainFactory tf = Terrains.getTerrainFactory();
        int bridgeElevation = altitude - hex.getElevation();

        hex.addTerrain(tf.createTerrain(Terrains.BRIDGE,
                getBuildingTypeByCF(cf), true, (exits & 63)));
        hex.addTerrain(tf.createTerrain(Terrains.BRIDGE_ELEV, bridgeElevation));
        hex.addTerrain(tf.createTerrain(Terrains.BRIDGE_CF, cf));
    }

    private void connectHexes(Coords src, Coords dest, int roadStyle) {
        if (board.contains(src)) {
            IHex hex = board.getHex(src);
            ITerrain t = hex.getTerrain(Terrains.ROAD);
            if (t == null) {
                t = hex.getTerrain(Terrains.BRIDGE);
            }
            if (t == null) {
                addRoad(hex, src.direction(dest), roadStyle);
            } else {
                t.setExit(src.direction(dest), true);
            }
        }
    }

    /**
     * Build a bridge across an obstacle
     * 
     * @todo: use a bridge not a road when bridges are working
     * @param board
     * @param start
     * @param direction
     * @return coordinates to resume roadbuilding
     */
    private Coords tryToBuildBridge(Coords start, int direction) {
        if (!board.contains(start))
            return null;
        Vector<Coords> hexes = new Vector<Coords>(7);
        Coords end = null;
        Coords next = start.translated(direction);
        while (hexes.size() < 6) {
            if (!board.contains(next)) {
                // offboard, why bother?
                break;
            }
            if (!hexNeedsBridge(board.getHex(next))) {
                end = next;
                break;
            }
            hexes.add(next);
            next = next.translated(direction);
        }
        if (end != null) {
            // got start and end, can we make a bridge?
            if (hexes.size() == 0)
                return null;
            int elev1 = board.getHex(start).getElevation();
            int elev2 = board.getHex(end).getElevation();
            int elevBridge = board.getHex(end).terrainLevel(Terrains.BRIDGE);
            if (elevBridge >= 0) {
                if (Math.abs(elev2 + elevBridge - elev1) > 2)
                    return null;
                elev1 = elev2 + elevBridge;
            } else {
                if (Math.abs(elev1 - elev2) > 4) {
                    // nobody could use the bridge, give up
                    return null;
                }
                elev1 = (elev1 + elev2) / 2;
            }
            // build the bridge
            int exits = (1 << direction) | (1 << ((direction + 3) % 6));
            int cf = mapSettings.getCityMinCF()
                    + Compute.randomInt(1 + mapSettings.getCityMaxCF()
                            - mapSettings.getCityMinCF());

            for (Enumeration<Coords> e = hexes.elements(); e.hasMoreElements();) {
                Coords c = e.nextElement();
                addBridge(board.getHex(c), exits, elev1, cf);
            }
            connectHexes(start, hexes.firstElement(), 1);
            connectHexes(end, hexes.lastElement(), 1);
        }
        return end;
    }

    private Coords extendRoad(Coords coords, int nextDirection, int roadStyle) {
        Coords next = selectNextGrid(nextDirection, coords);
        if (board.contains(next) && hexNeedsBridge(board.getHex(next))) {
            if (nextDirection == E || nextDirection == W) {
                nextDirection = coords.direction(next);
            }
            Coords end = tryToBuildBridge(coords, nextDirection);
            return end;
        }
        connectHexes(coords, next, roadStyle);
        connectHexes(next, coords, roadStyle);
        return next;
    }

    private Coords resumeAfterObstacle(Coords coords, int nextDirection) {
        Coords next = selectNextGrid(nextDirection, coords);
        while (board.contains(next) && !isHexBuildable(board.getHex(next))) {
            next = selectNextGrid(nextDirection, next);
        }
        return next;
    }

    private void buildStraightRoad(Coords start, int direction, int roadStyle) {
        Coords coords = start;

        while (coords != null && coords.x <= board.getWidth() && coords.x >= -1
                && coords.y <= board.getHeight() && coords.y >= -1) {
            cityPlan.add(coords);
            Coords next = extendRoad(coords, direction, roadStyle);
            if (next == null) {
                coords = resumeAfterObstacle(coords, direction);
            } else
                coords = next;
        }

    }

    /**
     * Utility function for setting building type from CF table
     * 
     * @param cf
     * @return building type
     */
    public static int getBuildingTypeByCF(int cf) {
        if (cf <= 15)
            return Building.LIGHT;
        if (cf <= 40)
            return Building.MEDIUM;
        if (cf <= 90)
            return Building.HEAVY;
        return Building.HARDENED;
    }

    /**
     * Adds an Road to the map. Goes from one border to another, and has one
     * turn in it. Map must be at least 3x3.
     */
    private void addGenericRoad() {
        Coords c = new Coords(Compute.randomInt(board.getWidth()), Compute
                .randomInt(board.getHeight()));
        int side0 = Compute.randomInt(6);
        int side1 = Compute.randomInt(5);
        if (side1 >= side0) {
            side1++;
        }
        buildStraightRoad(c, side0, 1);
        buildStraightRoad(c, side1, 1);
    }
}
