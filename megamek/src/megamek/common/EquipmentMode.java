/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import java.util.Hashtable;

/**
 * Modes can the equipment be in
 */
public class EquipmentMode {

    /**
     * Hash of all modes
     */
    protected static Hashtable modesHash = new Hashtable();

    /**
     * Unique internal mode identifier. Used as the part of the key to look for
     * the displayable name presented to user.
     */
    protected String name;
    
    /**
     * Protected constructor since we don't allow direct creation of the mode.
     * Modes available via <code>getMode()</code>
     * 
     * @param name unique mode identifier
     */
    protected EquipmentMode(String name) {
        this.name = name;
    }
    
    /**
     * @return mode identifier
     */
    public String getName() {
        return name;
    }
    
    /**
     * @return the displayable name presented by the GUI to the user.
     */
    public String getDisplayableName() {
        String result = EquipmentMessages.getString("EquipmentMode."+name);
        if (result != null)
            return result;
        else
            return name;
    }
    
    /**
     * @param name mode name
     * @return mode that corresponds to given name
     */
    public static EquipmentMode getMode(String name) {
        EquipmentMode mode = (EquipmentMode) modesHash.get(name);
        if (mode == null) {
            mode = new EquipmentMode(name); 
            modesHash.put(name, mode);            
        }
        return mode;
    }

    public boolean equals (String s) {
        return name.equals(s);
    }
}

