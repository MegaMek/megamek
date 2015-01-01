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

package megamek.common;

/**
 * This class represents a single, possibly multi-hex fuel tank on the board.
 * 
 * @author fastsammy@sourceforge.net (Robin D. Toll)
 */
public class FuelTank extends Building {
    /**
     * 
     */
    private static final long serialVersionUID = 5275543640680231747L;
    private int _magnitude;

    public FuelTank(Coords coords, IBoard board, int structureType,
            int magnitude) {
        super(coords, board, structureType);
        _magnitude = magnitude;
    }

    public int getMagnitude() {
        return _magnitude;
    }

    /*
     * public String toString() { // Assemble the string in pieces. StringBuffer
     * buf = new StringBuffer(); // Add the building type to the buffer. switch
     * (this.getType()) { case Building.LIGHT: buf.append( "Light " ); break;
     * case Building.MEDIUM: buf.append( "Medium " ); break; case
     * Building.HEAVY: buf.append( "Heavy " ); break; case Building.HARDENED:
     * buf.append( "Hardened " ); break; } // Add the building's name.
     * buf.append(this.name); // Return the string. return buf.toString(); }
     */
}
