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
import java.util.List;

import megamek.common.Coords;

public class SmokeCloud implements Serializable{

    private static final long serialVersionUID = -8937331680271675046L;

    public static final int SMOKE_NONE = 0;
    public static final int SMOKE_LIGHT = 1;
    public static final int SMOKE_HEAVY = 2;
    public static final int SMOKE_LI_LIGHT = 3;
    public static final int SMOKE_LI_HEAVY = 4;
    public static final int SMOKE_CHAFF_LIGHT = 5;
    public static final int SMOKE_GREEN = 6; // Anti-TSM smoke

    private int smokeDuration = 0;
    private final List<Coords> smokeHexList = new ArrayList<>();
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
        this.smokeLevel = Math.min(6, level);
    }
    
    /**
     * Reduces the level of smoke, heavy goes to light, LI heavy goes to LI 
     * light.
     */
    public void reduceSmokeLevel() {
        switch (smokeLevel) {
            case SMOKE_LIGHT:
            case SMOKE_LI_LIGHT:
            case SMOKE_CHAFF_LIGHT:
            default:
                smokeLevel = SMOKE_NONE;
                break;
            case SMOKE_HEAVY:
                smokeLevel = SMOKE_LIGHT;
                break;
            case SMOKE_LI_HEAVY:
                smokeLevel = SMOKE_LI_LIGHT;
                break;
        }
    }
    
    /**
     * Returns the level of smoke, odd levels will correspond to light smoke
     * while even levels will be heavy smoke.
     *
     * @return The smoke level
     */
    public int getSmokeLevel(){
        return smokeLevel;
    }
    
    public void addCoords(Coords coords){
        this.smokeHexList.add(coords);
    }
    
    public void removeCoords(Coords coords){
        this.smokeHexList.remove(coords);
    }
    
    public List<Coords> getCoordsList(){
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