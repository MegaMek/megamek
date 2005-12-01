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

import java.util.Enumeration;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.HashSet;
import java.util.Vector;

import megamek.common.Coords;
import megamek.common.IBoard;
import megamek.common.IHex;
import megamek.common.ITerrain;
import megamek.common.ITerrainFactory;
import megamek.common.MapSettings;
import megamek.common.Terrains;

/**
 * 
 * @author Torren + Coelocanth
 *
 */
public class CityBuilder {

    static final int N  = 0;
    static final int NE = 1;
    static final int SE = 2;
    static final int S  = 3;
    static final int SW = 4;
    static final int NW = 5;
    //Had to off set West and East as MM doesn't use them for hexes.
    static final int W  = 6;
    static final int E  = 7;

    public CityBuilder() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * This function will generate a city with a grid lay out.
     * 4 rounds running North and South and 4 roads running east west
     * 
     * @author Torren (Jason Tighe)
     * @param mapSettings
     * @param buildingTemplate
     * @return
     */
    public static Vector generateCity(MapSettings mapSettings, IBoard board){
        
        
        Vector<BuildingTemplate> buildingList = new Vector<BuildingTemplate>();
        Vector<Integer> buildingTypes = new Vector<Integer>();
        
        int width = mapSettings.getBoardWidth();
        int height = mapSettings.getBoardHeight();
        int roads = mapSettings.getCityBlocks();
        String cityType = mapSettings.getCityType();
        
        HashSet<Coords> cityPlan = new HashSet<Coords>();
        HashSet<Coords> buildingUsed = new HashSet<Coords>();
        
        if ( cityType.equalsIgnoreCase("HUB") )
            cityPlan = buildHubCity(width,height,roads, board);
        else if ( cityType.equalsIgnoreCase("METRO") )
            cityPlan = buildMetroCity(width,height);
        else if ( cityType.equalsIgnoreCase("GRID"))
            cityPlan = buildGridCity(width,height,roads);
        else
            return buildingList;
        
        StringTokenizer types = new StringTokenizer(mapSettings.getCityBuildingType(),",");
        
        while ( types.hasMoreTokens() ) {
            try {
                buildingTypes.add(Integer.parseInt(types.nextToken()));
            }catch(Exception ex) {
                ex.printStackTrace();
            } //someone entered a bad building type.
        }
        
        int typeSize = buildingTypes.size();

        Random r = new Random(System.currentTimeMillis());

        Vector coordList = new Vector();
        
        ITerrainFactory tf = Terrains.getTerrainFactory();
        
        for ( int x = 0; x < width; x++){
            for ( int y = 0; y < height; y++ ){
                Coords coord = new Coords(x,y);
                                
                if(cityPlan.contains(coord)
                        || buildingUsed.contains(coord) 
                        || !board.contains(coord)
                        || !isHexBuildable(board.getHex(coord))) {
                    continue;
                }
                
                if(r.nextInt(100) > mapSettings.getCityDensity()) {
                    continue; //empty lot
                }
                coordList = new Vector();
                coordList.add(coord);
                buildingUsed.add(coord);
                while(r.nextInt(100) < mapSettings.getCityDensity()) {
                    //try to make a bigger building!
                    int dir = r.nextInt(6);
                    Coords next = coord.translated(dir);
                    if(cityPlan.contains(next) 
                            || buildingUsed.contains(next) 
                            || !board.contains(next)
                            || !isHexBuildable(board.getHex(next))) {
                        break; //oh well, cant expand here
                    }
                    coordList.add(next);
                    buildingUsed.add(next);                    
                }

    
                int floors = mapSettings.getCityMaxFloors()-mapSettings.getCityMinFloors();
                
                if ( floors <= 0 )
                    floors = mapSettings.getCityMinFloors();
                else
                    floors = r.nextInt(floors)+mapSettings.getCityMinFloors();
                
                int totalCF = mapSettings.getCityMaxCF()-mapSettings.getCityMinCF();
                
                if ( totalCF <= 0)
                    totalCF = mapSettings.getCityMinCF();
                else
                    totalCF = r.nextInt(totalCF)+mapSettings.getCityMinCF();
                
                int type = 1;
                try{
                    if (typeSize == 1 )
                        type = buildingTypes.elementAt(0);
                    else 
                        type = buildingTypes.elementAt(r.nextInt(typeSize));
                }catch(Exception ex) {
                    ex.printStackTrace();
                } //someone entered a bad building type.
                
                buildingList.add(new BuildingTemplate(type,coordList,totalCF,floors,-1));
            }
        }        
        
        /*System.err.println("BuildingList size: "+buildingList.size());
        
        for ( BuildingTemplate template : buildingList){
            Enumeration coords = template.getCoords();
            while ( coords.hasMoreElements() ){
                Coords tempcoord = (Coords)coords.nextElement();
                System.err.println("Template Coords: "+tempcoord.x+","+tempcoord.y);
            }
        }*/
        return buildingList;
    }

    private static HashSet<Coords> buildGridCity(int maxX, int maxY,int roads){
        HashSet<Coords> grid = new HashSet<Coords>();
        
        Random r = new Random(System.currentTimeMillis());
        //north south lanes first
        for( int y = 0; y < roads; y++){
            int startY = r.nextInt(maxY/roads)+(y*(maxY/roads));
            for ( int x = 0; x < maxX; x++){
                grid.add(new Coords(x,startY));
            }
                
        }
        
        for ( int x = 0; x < roads; x++){
            int startX = r.nextInt(maxX/roads)+(x*(maxX/roads));
            for ( int y = 0; y < maxY; y++ ){
                grid.add(new Coords(startX,y));
            }
        }
        
        return grid;
    }
    
    private static HashSet<Coords> buildHubCity(int maxX, int maxY,int roads,IBoard board){
        HashSet<Coords> grid = new HashSet<Coords>();
        int midX = maxX/2;
        int midY = maxY/2;
        
        Vector<Integer> directions = new Vector<Integer>(8);
        
        directions.add(N);
        directions.add(NE);
        directions.add(SE);
        directions.add(S);
        directions.add(SW);
        directions.add(NW);
        directions.add(E);
        directions.add(W);
        
        roads = Math.max(roads,8);
        Random r = new Random(System.currentTimeMillis());
        grid.add(new Coords(midX,midY));
 
        int x=0;
        int y=0;
        for ( int dir = 0; dir < roads; dir++){
            int baseDirection = -1;
            int roadStyle = r.nextInt(2) + 1;
            
            if(dir < 8) {
                x = midX;
                y = midY;
                baseDirection = directions.remove(r.nextInt(directions.size()));
            } else {
                switch(r.nextInt(4)) {
                case 1:
                    x = r.nextInt(maxX);
                    y = -1;
                    baseDirection = S;
                    break;
                case 2:
                    x = r.nextInt(maxX);
                    y = maxY;
                    baseDirection = N;
                    break;
                case 3:
                    x = -1;
                    y = r.nextInt(maxY);
                    baseDirection = NE + r.nextInt(2);
                    break;
                default:
                    x = maxX;
                    y = r.nextInt(maxY);
                    baseDirection = SW + r.nextInt(2);
                    break;
                }
            }
            Coords coords = new Coords(x,y);
            Coords next = null;
            
            int nextDirection = baseDirection;
            while (coords.x >= -1 && coords.x <= maxX && coords.y >= -1 && coords.y <= maxY ){
                int choice = r.nextInt(10);

                if(board.contains(coords)) {
                    //don't change direction offboard
                    if(choice < 4) {
                        //keep going
                    } else if (choice < 6) {
                        // turn left
                        nextDirection = (5 + nextDirection) % 6;
                    } else if (choice < 8) {
                        //turn right
                        nextDirection = (1 + nextDirection) % 6;
                    } else {
                        //turn towards base direction
                        nextDirection = baseDirection;
                    }
                }
                next = selectNextGrid(nextDirection,coords);
                if(board.contains(next) && hexNeedsBridge(board.getHex(next))) {
                    Coords end = tryToBuildBridge(board, coords, nextDirection); 
                    if(null == end) break;
                    coords = end;
                } else {
                    connectHexes(board, coords, next, roadStyle);
                    connectHexes(board, next, coords, roadStyle);
                    coords = next;
                }
                if(/*dir >= 8 &&*/ grid.contains(coords) && x!=midX && y!=midY) {
                    break;
                }
                grid.add(coords);
                
                x = coords.x;
                y = coords.y;
            }
            
        }
        return grid;
    }
    
    private static HashSet<Coords> buildMetroCity(int maxX, int maxY){
        HashSet<Coords> grid = new HashSet<Coords>();
        int midX = maxX/2;
        int midY = maxY/2;
        
        grid.add(new Coords(midX,midY));
 
        //have the city hub be the mid point with all the hexes around it cleared out
        for ( int hex=0; hex < 6; hex++ )
            grid.add(new Coords(Coords.xInDir(midX,midY,hex),Coords.yInDir(midX,midY,hex)));

        //first east west road 
        for ( int x=0; x < maxX; x++)
            grid.add(new Coords(x,midY/2));
        
        //second east west road 
        for ( int x=0; x < maxX; x++)
            grid.add(new Coords(x,midY+(midY/2)));
        
        //First North South Road
        for ( int y=0; y < maxY; y++)
            grid.add(new Coords(midX/2,y));

        //second North South Road
        for ( int y=0; y < maxY; y++)
            grid.add(new Coords(midX+(midX/2),y));

        for ( int dir = 0; dir < 8; dir++){
            Coords coords = new Coords(midX,midY);
            int x = midX;
            int y = midY;
            
            while (x >= 0 && x <= maxX && y >= 0 && y <= maxY ){
                
                coords = selectNextGrid(dir,coords);
                grid.add(coords);
                //System.err.println(coords);
                
                x = coords.x;
                y = coords.y;
            }
            
        }
        return grid;
    }

    private static Coords selectNextGrid(int dir, Coords coords){
        Coords result = coords.translated(dir);
        
        if ( dir == E )
            result.x++; 
        
        if ( dir == W )
            result.x--; 

        return result; 
    }
    
    /**
     * 
     * @param hex
     * @return true if it is reasonable to build on this hex
     */
    private static boolean isHexBuildable(IHex hex) {
        if(hex.containsTerrain(Terrains.WATER)
            || hex.containsTerrain(Terrains.IMPASSABLE)
            || hex.containsTerrain(Terrains.MAGMA)
            || hex.containsTerrain(Terrains.SWAMP)) {
            return false; //uneconomic to build here
        }
        if(hex.getElevation() >= 4) {
            return false; //don't build on mountaintops (aesthetics)
        }
        return true;
    }
    
    /**
     * 
     * @param hex
     * @return true if the hex needs a bridge to cross
     */
    private static boolean hexNeedsBridge(IHex hex) {
        if(hex.containsTerrain(Terrains.ROAD)
                || hex.containsTerrain(Terrains.BRIDGE))
            return false;
        return(hex.containsTerrain(Terrains.WATER)
                || hex.containsTerrain(Terrains.MAGMA));
    }
    
    private static void addRoad(IHex hex, int exitDirection, int type) {
        ITerrainFactory tf = Terrains.getTerrainFactory();
        if(hex.containsTerrain(Terrains.WATER)) {
            hex.removeTerrain(Terrains.WATER);
            hex.addTerrain(tf.createTerrain(Terrains.WATER, 0));
            type = 1;
        }
        hex.addTerrain(tf.createTerrain(Terrains.ROAD, type, true, (1<<exitDirection) & 63));
    }
    
    private static void addBridge(IHex hex, int exits, int altitude) {
        ITerrainFactory tf = Terrains.getTerrainFactory();
        int bridgeElevation = altitude = hex.getElevation(); //TODO
        hex.setElevation(altitude); //TODO: bridge el instead
        if(hex.containsTerrain(Terrains.WATER)) {
            hex.removeTerrain(Terrains.WATER);
            hex.addTerrain(tf.createTerrain(Terrains.WATER, 0));
        }
        hex.addTerrain(tf.createTerrain(Terrains.ROAD, 1, true, (exits & 63)));
    }
    
    private static void connectHexes(IBoard board, Coords src, Coords dest, int roadStyle) {
        if(board.contains(src)) {
            IHex hex = board.getHex(src);
            ITerrain t = hex.getTerrain(Terrains.ROAD);
            if(t == null) {
                t = hex.getTerrain(Terrains.BRIDGE);
            }
            if(t == null) {
                addRoad(hex, src.direction(dest), roadStyle);
            } else {
                t.setExit(src.direction(dest), true);
            }
        }
    }
    
    /**
     * Build a bridge across an obstacle
     * @todo: use a bridge not a road when bridges are working
     * @param board
     * @param start
     * @param direction
     * @return coordinates to resume roadbuilding
     */
    private static Coords tryToBuildBridge(IBoard board, Coords start, int direction) {
        if(!board.contains(start))return null;
        Vector<Coords> hexes = new Vector(7);
        Coords end = null;
        Coords next = start.translated(direction);
        while(hexes.size() < 6) {
            if(!board.contains(next)) {
                //offboard, why bother?
                break;
            }
            if(!hexNeedsBridge(board.getHex(next))) {
                end = next;
                break;
            }
            hexes.add(next);
            next = next.translated(direction);
        }
        if(end != null) {
            //got start and end, can we make a bridge?
            int elev1 = board.getHex(start).getElevation();
            int elev2 = board.getHex(end).getElevation();
            int elevBridge = board.getHex(end).terrainLevel(Terrains.BRIDGE);
            if(elevBridge >=0) {
                if(Math.abs(elev2 + elevBridge - elev1) > 2)
                    return null;
                elev1 = elev2 + elevBridge;
            } else {
                if(Math.abs(elev1-elev2) > 4) {
                    //nobody could use the bridge, give up
                    return null;
                }
            elev1 = (elev1 + elev2) / 2;
            }
            //build the bridge
            int exits = (1<<direction) | (1<<((direction + 3) %6));
            for(Enumeration<Coords> e=hexes.elements();e.hasMoreElements();) {
                Coords c = e.nextElement();
                addBridge(board.getHex(c), exits, elev1);
            }
            connectHexes(board, start, hexes.firstElement(), 1);
            connectHexes(board, end, hexes.lastElement(), 1);
        }
        return end;
    }
}
