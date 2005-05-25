/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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
 * EquipmentType.java
 *
 * Created on April 1, 2002, 1:35 PM
 */

package megamek.common.equip;

import megamek.common.*;

import java.io.Serializable;

/**
 * ########################################
 *
 * @author  Dave
 * @version 
 */
public class EquipmentState implements Serializable {
    
    protected transient EquipmentType type;
    protected int mode;        //Equipment's current state.
    protected Mounted location = null;// Reference to this states mounted location
    
    public EquipmentState(Mounted location, EquipmentType type) {
        this.location = location;
        this.type = type;
        if (type.hasModes())
            mode = 0;
        else
            mode = -1;
    }
    
    public Mounted getLocation() {
        return location;
    }
    
    
    // This returns EquipmentMode for the mode
    public EquipmentMode activeMode() {
        return type.getMode(mode);
    }
    
    // This returns the same as above.  It's used by subclasses to do pending
    // modes
    public EquipmentMode curMode() {
        return type.getMode(mode);
    }
    
    public int switchMode() {
        if (type.hasModes()) {
            int nMode = 0;
            nMode = (mode + 1) % type.getModesCount();
            setMode(nMode);
            return nMode;
        }
        return -1;
    }
    
    public int setMode(String s) {
        for (int x = 0, e = type.getModesCount(); x < e; x++) {
            if (type.getMode(x).equals(s)) {
                setMode(x);
                return x;
            }
        }
        return -1;
    }
    
    public void setMode(int n) {
        if (type.hasModes()) 
            mode = n;
    }
    
    
    // Called when the mounted is restored after de-serialization
    public void setType(EquipmentType type) {
        this.type = type;
    }
    
}
