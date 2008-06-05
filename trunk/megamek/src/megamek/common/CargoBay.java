/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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


/**
 * Represtents a volume of space set aside for carrying ASFs and Small Craft aboard DropShips
 */

public final class CargoBay extends Bay {

  
    /**
     * 
     */
    private static final long serialVersionUID = 4161027191694822726L;

    /**
     * The default constructor is only for serialization.
     */
    protected CargoBay() {
        this.totalSpace = 0;
        this.currentSpace = 0;
    }

    // Public constructors and methods.

    /**
     * Create a space for the given tonnage of troops. For this class, only
     * the weight of the troops (and their equipment) are considered; if
     * you'd like to think that they are stacked like lumber, be my guest.
     *
     * @param   space - The weight of troops (in tons) this space can carry.
     */
    public CargoBay( int space, int doors ) {
        this.totalSpace = space;
        this.currentSpace = space;
        this.doors = doors;
    }

    /**
     * Determines if this object can accept the given unit.  The unit may
     * not be of the appropriate type or there may be no room for the unit.
     *
     * @param   unit - the <code>Entity</code> to be loaded.
     * @return  <code>true</code> if the unit can be loaded,
     *          <code>false</code> otherwise.
     */
    public boolean canLoad( Entity unit ) {
        // Assume that we cannot carry the unit.
        boolean result = false;

        return result;
    }
    
    public String getUnusedString() {
        return "Cargo Space - " + this.currentSpace + " tons";
    }

    public String getType() {
        return "Cargo";
    }
    
} // End package class TroopSpace implements Transporter
