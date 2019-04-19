/*
 * MegaMek - Copyright (C) 2003,2004,2005,2006 Ben Mazur (bmazur@sev.org)
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

import java.io.Serializable;

// This class is for grav decks. It contains the size in meters and a status flag for damage

public class GravDeck implements Serializable {
    
    /**
     * 
     */
    private static final long serialVersionUID = 2108041654790803725L;
    
    private int size; // diameter, in meters
    private boolean damaged; // has this grav deck taken a critical hit?

    public GravDeck(int size, boolean damaged) {
        this.size = size;
        this.damaged = damaged;
    }

    public int getSize() {
        return size;
    }
    
    public boolean getDamaged() {
        return damaged;
    }
    
    public void setDamaged(boolean damaged) {
        this.damaged = damaged;
    }
}
