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

import java.io.*;

public class CriticalSlot
    implements Serializable
{
    public final static int        TYPE_WEAPON        = 0;
    public final static int        TYPE_SYSTEM        = 1;
    public final static int        TYPE_AMMO         = 2;
    public final static int        TYPE_EQUIPMENT    = 3;
    
    private int                    type;
    private int                    index;
    private boolean                doomed;
    private boolean                destroyed;
    
    public CriticalSlot(int type, int index) {
        this.type = type;
        this.index = index;
    }
    
    public int getType() {
        return type;
    }
    
    public int getIndex() {
        return index;
    }
    
    public boolean isDoomed() {
        return doomed;
    }
    
    public void setDoomed(boolean doomed) {
        this.doomed = doomed;
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }
    
    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }
    
    /**
     * Has this slot been damaged?
     */
    public boolean isHit() {
        return doomed | destroyed;
    }
    
    /**
     * Two CriticalSlots are equal if their type and index are equal
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        CriticalSlot other = (CriticalSlot)object;
        return other.getType() == this.type && other.getIndex() == this.index;
    }
}
