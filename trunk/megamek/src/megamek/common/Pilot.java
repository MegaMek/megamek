/**
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

import java.io.*;

public class Pilot
    implements Serializable
{
    private String      name;
    private int         gunnery;
    private int         piloting;
    private int         hits; // hits taken
      
    private boolean     unconcious;
    private boolean     doomed;  // scheduled to die at end of phase
    private boolean     dead;
    
    // these are only used on the server:
    private int rollsNeeded; // how many KO rolls needed this turn
    private boolean koThisRound; // did I go KO this game round?
    
    
    public Pilot() {
        this("Unnamed", 4, 5);
    }
  
    public Pilot(String name, int gunnery, int piloting) {
        this.name = name;
        this.gunnery = gunnery;
        this.piloting = piloting;
        hits = 0;
        unconcious = false;
        dead = false;
        rollsNeeded = 0;
        koThisRound = false;
    }
  
    public String getName() {
        return name;
    }
  
    public int getGunnery() {
        return gunnery;
    }
  
    public int getPiloting() {
        return piloting;
    }
  
    public int getHits() {
        return hits;
    }
  
    public void setHits(int hits) {
        this.hits = hits;
    }
  
    public boolean isUnconcious() {
        return unconcious;
    }
    
    public void setUnconcious(boolean unconcious) {
        this.unconcious = unconcious;
    }
    
    public boolean isDead() {
        return dead;
    }
    
    public void setDead(boolean dead) {
        this.dead = dead;
        if (dead) {
            rollsNeeded = 0;
            hits = 6;
        }
    }
    
    public boolean isDoomed() { 
        return doomed;
    }
    
    public void setDoomed(boolean b) {
        doomed = b;
        if (doomed) {
            rollsNeeded = 0;
            hits = 6;
        }
    }
    
    public boolean isActive() {
        return !unconcious && !dead;
    }
    
    public int getRollsNeeded() {
        return rollsNeeded;
    }
    
    public void setRollsNeeded(int rollsNeeded) {
        this.rollsNeeded = rollsNeeded;
    }
    
    public boolean isKoThisRound() {
        return koThisRound;
    }
    
    public void setKoThisRound(boolean koThisRound) {
        this.koThisRound = koThisRound;
    }
    
    public String getDesc() {
        String s = new String(name);
        if (hits > 0) {
            s += " (" + hits + " hit(s)";
            if (isUnconcious()) {
                s += " [ko]";
            } else if (isDead()) {
                s += " [dead]";
            }
            s += ")";
        }
        else if (isUnconcious()) {
            s += " [ko]";
        } else if (isDead()) {
            s += " [dead]";
        }
        return s;
    }
    
    /**
     * Returns whether this pilot has non-standard piloting or gunnery values
     */
    public boolean isCustom() {
        return gunnery != 4 || piloting != 5;
    }
    
    /**
     * Returns the BV multiplyer for this pilot's gunnery/piloting
     */
    public double getBVSkillMultiplier() {
        double multiplier = 1.0;
        
        if (gunnery < 4) {
            multiplier += 0.20 * (4 - gunnery);
        } else {
            multiplier += 0.10 * (4 - gunnery);
        }
        
        multiplier += 0.05 * (5 - piloting);
         
        return multiplier;
    }
}
