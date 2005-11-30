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

import java.util.Random;
import java.util.StringTokenizer;
import java.util.HashSet;
import java.util.Vector;

import megamek.common.Coords;
import megamek.common.IBoard;
import megamek.common.ITerrain;
import megamek.common.ITerrainFactory;
import megamek.common.MapSettings;
import megamek.common.Terrains;

/**
 * 
 * @author Torren
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
            cityPlan = buildHubCity(width,height,roads);
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
                
                if ( cityPlan.contains(coord) && board.contains(coord)) {
                    board.getHex(coord).addTerrain(tf.createTerrain(Terrains.ROAD, 2));
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
                    if(cityPlan.contains(next) || buildingUsed.contains(next) || !board.contains(next)) {
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
    
    private static HashSet<Coords> buildHubCity(int maxX, int maxY,int roads){
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
 
        //have the city hub be the mid point with all the hexes around it cleared out
        /*for ( int hex=0; hex < 6; hex++ )
            grid.add(new Coords(Coords.xInDir(midX,midY,hex),Coords.yInDir(midX,midY,hex)));*/
        
        int x=0;
        int y=0;
        for ( int dir = 0; dir < roads; dir++){
            if(dir < 8) {
                x = midX;
                y = midY;
            } else {
                x = r.nextInt(maxX);
                y = r.nextInt(maxY);
            }
            Coords coords = new Coords(x,y);
            
            int baseDirection = -1;
            
            baseDirection = directions.elementAt(dir % directions.size());

            while (x >= 0 && x <= maxX && y >= 0 && y <= maxY ){
                int choice = r.nextInt(8);
                
                switch ( baseDirection ){
                case N:
                    if ( choice < 4 )
                        coords = selectNextGrid(baseDirection,coords);
                    else if ( choice < 6 ){//NE
                        coords = selectNextGrid(NE,coords);
                    }else if ( choice < 8){//NW
                        coords = selectNextGrid(NW,coords);
                    }else if ( choice < 9 ){//SE
                        coords = selectNextGrid(SE,coords);
                    }else{//SW
                        coords = selectNextGrid(SW,coords);
                    }
                    break;
                case NE:
                    if ( choice < 4 )
                        coords = selectNextGrid(baseDirection,coords);
                    else if ( choice < 6 ){//SE
                        coords = selectNextGrid(SE,coords);
                    }else if ( choice < 8){//N
                        coords = selectNextGrid(N,coords);
                    }else if ( choice < 9 ){//SW
                        coords = selectNextGrid(SW,coords);
                    }else{//SW
                        coords = selectNextGrid(NW,coords);
                    }
                    break;
                case SE:
                    if ( choice < 4 )
                        coords = selectNextGrid(baseDirection,coords);
                    else if ( choice < 6 ){//NE
                        coords = selectNextGrid(NE,coords);
                    }else if ( choice < 8){//S
                        coords = selectNextGrid(S,coords);
                    }else if ( choice < 9 ){//N
                        coords = selectNextGrid(N,coords);
                    }else{//SW
                        coords = selectNextGrid(SW,coords);
                    }
                    break;
                case S:
                    if ( choice < 4 )
                        coords = selectNextGrid(baseDirection,coords);
                    else if ( choice < 6 ){//SE
                        coords = selectNextGrid(SE,coords);
                    }else if ( choice < 8){//SW
                        coords = selectNextGrid(SW,coords);
                    }else if ( choice < 9 ){//NE
                        coords = selectNextGrid(NE,coords);
                    }else{//NW
                        coords = selectNextGrid(NW,coords);
                    }
                    break;
                case SW:
                    if ( choice < 4 )
                        coords = selectNextGrid(baseDirection,coords);
                    else if ( choice < 6 ){//S
                        coords = selectNextGrid(S,coords);
                    }else if ( choice < 8){//NW
                        coords = selectNextGrid(NW,coords);
                    }else if ( choice < 9 ){//SE
                        coords = selectNextGrid(SE,coords);
                    }else{//N
                        coords = selectNextGrid(N,coords);
                    }
                    break;
                case NW:
                    if ( choice < 4 )
                        coords = selectNextGrid(baseDirection,coords);
                    else if ( choice < 6 ){//N
                        coords = selectNextGrid(N,coords);
                    }else if ( choice < 8){//SW
                        coords = selectNextGrid(SW,coords);
                    }else if ( choice < 9 ){//S
                        coords = selectNextGrid(S,coords);
                    }else{//NE
                        coords = selectNextGrid(NE,coords);
                    }
                    break;
                case E:
                    if ( choice < 4 )
                        coords = selectNextGrid(baseDirection,coords);
                    else if ( choice < 6 ){//SE
                        coords = selectNextGrid(SE,coords);
                    }else if ( choice < 8){//SW
                        coords = selectNextGrid(SW,coords);
                    }else if ( choice < 9 ){//S
                        coords = selectNextGrid(S,coords);
                    }else{//N
                        coords = selectNextGrid(N,coords);
                    }
                    break;
                case W:
                    if ( choice < 4 )
                        coords = selectNextGrid(baseDirection,coords);
                    else if ( choice < 6 ){//SE
                        coords = selectNextGrid(SE,coords);
                    }else if ( choice < 8){//NE
                        coords = selectNextGrid(NE,coords);
                    }else if ( choice < 9 ){//S
                        coords = selectNextGrid(S,coords);
                    }else{//N
                        coords = selectNextGrid(N,coords);
                    }
                    break;
                }
                if(/*dir >= 8 && */grid.contains(coords) && x!=midX && y!=midY) {
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

    public static Coords selectNextGrid(int dir, Coords coords){
        Coords result = coords.translated(dir);
        
        if ( dir == E )
            result.x++; 
        
        if ( dir == W )
            result.x--; 

        return result; 
    }
}
