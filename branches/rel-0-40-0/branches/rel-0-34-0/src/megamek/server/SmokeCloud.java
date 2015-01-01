/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

package megamek.server;

import java.io.Serializable;
import java.util.ArrayList;

import megamek.common.Coords;

public class SmokeCloud implements Serializable{
    
    /**
     * 
     */
    private static final long serialVersionUID = -8937331680271675046L;
    private int smokeDuration = 0;
    private ArrayList<Coords> smokeHexList = new ArrayList<Coords>();
    private int smokeLevel = 1;
    private boolean didDrift = false;
    
    public SmokeCloud(){
        
    }
    
    public SmokeCloud(Coords coords, int level, int duration){
        this.smokeDuration = duration;
        this.smokeHexList.add(coords);
        this.smokeLevel = level;
    }
    
    public SmokeCloud(ArrayList<Coords> coords, int level, int duration){
        this.smokeDuration = duration;
        this.smokeLevel = level;
        this.smokeHexList.addAll(coords);
    }
    
    public void setSmokeLevel(int level){
        this.smokeLevel = Math.min(2, level);
    }
    
    public int getSmokeLevel(){
        return smokeLevel;
    }
    
    public void addCoords(Coords coords){
        this.smokeHexList.add(coords);
    }
    
    public void removeCoords(Coords coords){
        this.smokeHexList.remove(coords);
    }
    
    public ArrayList<Coords> getCoordsList(){
        return this.smokeHexList;
    }
    
    public void setDuration(int duration){
        this.smokeDuration = duration;
    }
    
    public int getDuration(){
        return this.smokeDuration;
    }
    
    public void setDrift(boolean drift){
        this.didDrift = drift;
    }
    
    public boolean didDrift(){
        return this.didDrift;
    }
}