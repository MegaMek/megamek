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
    public static int        colorRGBs[] = {0x8393CA, 0x00BFF3, 0xCCCCCC, 0x3CB878,
                                           0x998675, 0xFBAF5D, 0xF49AC1, 0xFFFFFF, 
                                           0xFFF568};
    public static String    colorNames[] = {"Blue", "Cyan", "Gray", "Green",
                                            "Brown", "Orange", "Pink", 
                                            "White", "Yellow"};

    private String          name = "unnamed";
    private int             id;

    private boolean         ready = false;
    private boolean         ghost = false; // disconnected player
    private boolean         observer = false;
    private int             colorIndex = 0;

    // these are game-specific, and maybe should be seperate from the player object
    private Vector             initVector = new Vector();
    private int             order = 0;
    private int             startingPos = 0;
        
    public Player(int id, String name) {
        this.name = name;
        this.id = id;
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
    
    public void clearInitiative() {
        initVector.removeAllElements();
    }
  
    public int getInitiative(int index) {
        return ((Integer)initVector.elementAt(index)).intValue();
    }
  
    public int getInitiativeSize() {
        return initVector.size();
    }
  
    public Enumeration getInitiatives() {
        return initVector.elements();
    }
  
    public void setInitiative(int init, int index) {
        if (initVector.size() < index + 1) {
            initVector.setSize(index + 1);
        }
        initVector.setElementAt(new Integer(init), index);
    }
    
    public void setInitiativeVector(Vector initVector) {
        this.initVector = initVector;
    }
    
    public int getOrder() {
        return order;
    }
  
    public void setOrder(int order) {
        this.order = order;
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
