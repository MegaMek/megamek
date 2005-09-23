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
    public final static int        TYPE_SYSTEM       = 0;
    public final static int        TYPE_EQUIPMENT    = 1;
    
    private int                    type;
    private int                    index;
    private boolean                hit; // hit
    private boolean                missing; // location destroyed
    private boolean                destroyed;
    private boolean                hittable; // false = hits rerolled
    private boolean                useless; //true = breached
    
    public CriticalSlot(int type, int index) {
        this(type, index, true);
    }
    
    public CriticalSlot(int type, int index, boolean hittable) {
        this.type = type;
        this.index = index;
        this.hittable = hittable;
    }
    
    public int getType() {
        return type;
    }
    
    public int getIndex() {
        return index;
    }
    
    public boolean isHit() {
        return hit;
    }
    
    public void setHit(boolean hit) {
        this.hit = hit;
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }
    
    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }
    
    public boolean isMissing() {
        return missing;
    }
    
    public void setMissing(boolean missing) {
        this.missing = missing;
    }

    public boolean isBreached() {
        return useless;
    }

    public void setBreached(boolean breached) {
        this.useless = breached;
    }

    /**
     * Has this slot been damaged?
     */
    public boolean isDamaged() {
        return hit || missing || destroyed;
    }
    
    /**
     * Can this slot be hit by a critical hit roll?
     */
    public boolean isHittable() {
        return hittable && !hit && !destroyed;
    }
    
    /**
     * Was this critical slot ever hittable?
     */
    public boolean isEverHittable() {
        return hittable;
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
