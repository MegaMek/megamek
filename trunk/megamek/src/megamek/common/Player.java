/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

import java.awt.*;
import java.io.*;
import java.util.*;

/**
 * Represents a player in the game.
 */
public final class Player
    implements Serializable
{
    public static final int     TEAM_NONE = 0;
    
    public static final int     colorRGBs[] = {0x8686BF, 0xF2F261, 0xCC6666,
    0x87BF86, 0xFFFFFF, 0x8FCCCC, 0xF29DC8, 0xF2AA61, 0xBEBEBE, 0x98816B};
    
    public static final String  colorNames[] = {"Blue", "Yellow", "Red",  
    "Green", "White", "Cyan", "Pink", "Orange", "Gray", "Brown"};
    
    private transient Game  game;

    private String          name = "unnamed";
    private int             id;
    
    private int             team = TEAM_NONE;

    private boolean         ready = false;
    private boolean         ghost = false; // disconnected player
    private boolean         observer = false;
    private int             colorIndex = 0;

    // these are game-specific, and maybe should be seperate from the player object
    private int             startingPos = 0;
    private InitiativeRoll  initiative = new InitiativeRoll();
        
    public Player(int id, String name) {
        this.name = name;
        this.id = id;
    }
  
    public void setGame(Game game) {
        this.game = game;
    }
    
    public String getName() {
        return name;
    }
  
    public void setName(String name) {
        this.name = name;
    }
  
    public int getId() {
        return id;
    }
  
    public int getTeam() {
        return team;
    }
  
    public void setTeam(int team) {
        this.team = team;
    }
  
    public boolean isReady() {
        return ready;
    }
  
    public void setReady(boolean ready) {
        this.ready = ready;
    }
  
    public boolean isGhost() {
        return ghost;
    }
  
    public void setGhost(boolean ghost) {
        this.ghost = ghost;
    }
  
    public boolean isObserver() {
        return observer;
    }
  
    public void setObserver(boolean observer) {
        this.observer = observer;
    }
  
    public int getColorIndex() {
        return colorIndex;
    }
  
    public void setColorIndex(int index) {
        this.colorIndex = index;
    }
    
    public InitiativeRoll getInitiative() {
        return initiative;
    }
  
    public int getStartingPos() {
        return startingPos;
    }
  
    public void setStartingPos(int startingPos) {
        this.startingPos = startingPos;
    }
    
    public Color getColor() {
        return new Color(colorRGBs[colorIndex]);
    }
    
    public int getColorRGB() {
        return colorRGBs[colorIndex];
    }
    
    public boolean isEnemyOf(Player other) {
        return (id != other.getId() && (team == TEAM_NONE || team != other.getTeam()));
    }
    
    /**
     * Two players are equal if their ids are equal
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Player other = (Player)object;
        return other.getId() == this.id;
    }
    
    public int hashCode() {
        return getId();
    }
}
