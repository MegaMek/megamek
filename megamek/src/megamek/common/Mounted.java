/*
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

/*
 * Mounted.java
 *
 * Created on April 1, 2002, 1:29 PM
 */

package megamek.common;

import java.io.*;

/**
 * This describes equipment mounted on a mech.
 *
 * @author  Ben
 * @version 
 */
public class Mounted implements Serializable{
    
    private boolean usedThisTurn = false;
    private boolean destroyed = false;
    private transient boolean hit = false;
    private transient boolean missing = false;
    
    private int shotsLeft; // for ammo
    
    private int location;
    private boolean rearMounted;
    
    private Mounted linked = null; // for ammo, or artemis
    
    private transient EquipmentType type;
    private String typeName;
    

    /** Creates new Mounted */
    public Mounted(EquipmentType type) {
        this.type = type;
        this.typeName = type.getName();
        
        if (type instanceof AmmoType) {
            shotsLeft = ((AmmoType)type).getShots();
        }
    }

    /**
     * Restores the equipment from the name
     */
    public void restore() {
        this.type = EquipmentType.getByInternalName(typeName);
        
        if (this.type == null) {
            System.err.println("Mounted.restore: could not restore equipment type \"" + typeName + "\"");
        }
    }
    
    public EquipmentType getType() {
        return type;
    }
    
    /**
     * Shortcut to type.getName()
     */
    public String getName() {
        return type.getName();
    }
    
    public String getDesc() {
        StringBuffer desc = new StringBuffer(type.getDesc());
        if (destroyed) {
            desc.insert(0, "*");
        } else if (usedThisTurn) {
            desc.insert(0, "+");
        }
        if (rearMounted) {
            desc.append(" (R)");
        }
        if (type instanceof AmmoType) {
            desc.append(" (");
            desc.append(shotsLeft);
            desc.append(")");
        }
        return desc.toString();
    }
    
    public boolean isReady() {
        return !usedThisTurn && !destroyed;
    }
    
    public boolean isUsedThisTurn() {
        return usedThisTurn;
    }
    
    public void setUsedThisTurn(boolean usedThisTurn) {
        this.usedThisTurn = usedThisTurn;
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }
    
    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }
    
    public boolean isHit() {
        return hit;
    }
    
    public void setHit(boolean hit) {
        this.hit = hit;
    }
    
    public boolean isMissing() {
        return missing;
    }
    
    public void setMissing(boolean missing) {
        this.missing = missing;
    }
    
    public int getShotsLeft() {
        return shotsLeft;
    }
    
    public void setShotsLeft(int shotsLeft) {
        this.shotsLeft = shotsLeft;
    }
    
    public int getLocation() {
        return location;
    }
    
    public boolean isRearMounted() {
        return rearMounted;
    }
    
    public void setLocation(int location) {
        setLocation(location, false);
    }
    
    public void setLocation(int location, boolean rearMounted) {
        this.location = location;
        this.rearMounted = rearMounted;
    }
    
    public Mounted getLinked() {
        return linked;
    }

    public void setLinked(Mounted linked) {
        this.linked = linked;
    }
}
