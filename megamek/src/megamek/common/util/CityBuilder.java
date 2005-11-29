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
import java.util.TreeSet;
import java.util.Vector;

import megamek.common.Coords;
import megamek.common.MapSettings;

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
    public static Vector generateCity(MapSettings mapSettings){
        
        
        Vector<BuildingTemplate> buildingList = new Vector<BuildingTemplate>();
        Vector<String> buildingTypes = new Vector<String>();
        
        int width = mapSettings.getBoardWidth();
        int height = mapSettings.getBoardHeight();
        int roads = mapSettings.getCityBlocks();
        String cityType = mapSettings.getCityType();
        
        TreeSet<String> cityPlan = new TreeSet<String>();
        
        if ( cityType.equalsIgnoreCase("HUB") )
            cityPlan = buildHubCity(width,height,roads);
        else if ( cityType.equalsIgnoreCase("METRO") )
            cityPlan = buildMetroCity(width,height);
        else if ( cityType.equalsIgnoreCase("GRID"))
            cityPlan = buildGridCity(width,height,roads);
        else
            return buildingList;
        
        StringTokenizer types = new StringTokenizer(mapSettings.getCityBuildingType(),",");
        
        while ( types.hasMoreTokens() )
            buildingTypes.add(types.nextToken());
        
        int typeSize = buildingTypes.size();

        Random r = new Random(System.currentTimeMillis());

        Coords coord = new Coords();
        String stringCoord = "";
        Vector coordList = new Vector();
        
        for ( int x = 0; x < width; x++){
            for ( int y = 0; y < height; y++ ){
                stringCoord = Integer.toString(x)+","+Integer.toString(y);
                
                if ( cityPlan.contains(stringCoord) )
                    continue;
                
                coordList = new Vector();
                coordList.add(new Coords(x,y));
    
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
                        type = Integer.parseInt(buildingTypes.elementAt(0));
                    else 
                        type = Integer.parseInt(buildingTypes.elementAt(r.nextInt(typeSize)));
                }catch(Exception ex){} //someone entered a bad building type.
                
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

    private static TreeSet<String> buildGridCity(int maxX, int maxY,int roads){
        TreeSet<String> grid = new TreeSet<String>();
        
        Random r = new Random(System.currentTimeMillis());
        //north south lanes first
        for( int y = 0; y < roads; y++){
            int startY = r.nextInt(maxY/roads)+(y*(maxY/roads));
            for ( int x = 0; x < maxX; x++){
                grid.add(Integer.toString(x)+","+Integer.toString(startY));
            }
                
        }
        
        for ( int x = 0; x < roads; x++){
            int startX = r.nextInt(maxX/roads)+(x*(maxX/roads));
            for ( int y = 0; y < maxY; y++ ){
                grid.add(Integer.toString(startX)+","+Integer.toString(y));
            }
        }
        
        return grid;
    }
    
    private static TreeSet<String> buildHubCity(int maxX, int maxY,int roads){
        TreeSet<String> grid = new TreeSet<String>();
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
        
        roads = Math.min(roads,8);
        Random r = new Random(System.currentTimeMillis());
        grid.add(Integer.toString(midX)+","+Integer.toString(midY));
 
        //have the city hub be the mid point with all the hexes around it cleared out
        for ( int hex=0; hex < 6; hex++ )
            grid.add(Integer.toString(Coords.xInDir(midX,midY,hex))+","+Integer.toString(Coords.yInDir(midX,midY,hex)));
        
        for ( int dir = 0; dir < roads; dir++){
            String coords = Integer.toString(midX)+","+Integer.toString(midY);
            int x = midX;
            int y = midY;
            
            int baseDirection = -1;
            
            if ( directions.size() > 1)
                baseDirection = directions.remove(r.nextInt(directions.size()));
            else
                baseDirection = directions.remove(0);

            while (x >= 0 && x <= maxX && y >= 0 && y <= maxY ){
                int choice = r.nextInt(10);
                
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
                    coords = selectNextGrid(baseDirection,coords);
                    break;
                case W:
                    coords = selectNextGrid(baseDirection,coords);
                    break;
                }
                grid.add(coords);
                //System.err.println(coords);
                StringTokenizer coord = new StringTokenizer(coords,",");
                
                x = Integer.parseInt(coord.nextToken());
                y = Integer.parseInt(coord.nextToken());
            }
            
        }
        return grid;
    }
    
    private static TreeSet<String> buildMetroCity(int maxX, int maxY){
        TreeSet<String> grid = new TreeSet<String>();
        int midX = maxX/2;
        int midY = maxY/2;
        
        grid.add(Integer.toString(midX)+","+Integer.toString(midY));
 
        //have the city hub be the mid point with all the hexes around it cleared out
        for ( int hex=0; hex < 6; hex++ )
            grid.add(Integer.toString(Coords.xInDir(midX,midY,hex))+","+Integer.toString(Coords.yInDir(midX,midY,hex)));

        //first east west road 
        for ( int x=0; x < maxX; x++)
            grid.add(Integer.toString(x)+","+Integer.toString(midY/2));
        
        //second east west road 
        for ( int x=0; x < maxX; x++)
            grid.add(Integer.toString(x)+","+Integer.toString(midY+(midY/2)));
        
        //First North South Road
        for ( int y=0; y < maxY; y++)
            grid.add(Integer.toString(midX/2)+","+Integer.toString(y));

        //second North South Road
        for ( int y=0; y < maxY; y++)
            grid.add(Integer.toString(midX+(midX/2))+","+Integer.toString(y));

        for ( int dir = 0; dir < 8; dir++){
            String coords = Integer.toString(midX)+","+Integer.toString(midY);
            int x = midX;
            int y = midY;
            
            while (x >= 0 && x <= maxX && y >= 0 && y <= maxY ){
                
                coords = selectNextGrid(dir,coords);
                grid.add(coords);
                //System.err.println(coords);
                StringTokenizer coord = new StringTokenizer(coords,",");
                
                x = Integer.parseInt(coord.nextToken());
                y = Integer.parseInt(coord.nextToken());
            }
            
        }
        return grid;
    }

    public static String selectNextGrid(int dir, String coords){
        StringTokenizer coord = new StringTokenizer(coords,",");
        
        //System.err.println("Dir: "+dir);
        
        int x = Integer.parseInt(coord.nextToken());
        int y = Integer.parseInt(coord.nextToken());
        
        if ( dir == E )
            return Integer.toString(++x)+","+Integer.toString(y); 
        
        if ( dir == W )
            return Integer.toString(--x)+","+Integer.toString(y); 

        return Integer.toString(Coords.xInDir(x,y,dir))+","+Integer.toString(Coords.yInDir(x,y,dir)); 
    }
}
