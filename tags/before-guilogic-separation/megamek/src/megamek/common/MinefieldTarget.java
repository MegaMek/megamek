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

public class MinefieldTarget implements Targetable
{
    private Coords m_coords;
    private int m_elev;
    
    public MinefieldTarget(Coords c, Board board) {
        m_coords = c;
        m_elev = board.getHex(m_coords).getElevation();
    }
    
    public int getTargetType() { 
        return Targetable.TYPE_MINEFIELD_CLEAR;
    }
    
    public int getTargetId() {
        return coordsToId(m_coords);
    }
    
    public Coords getPosition() {
        return m_coords;
    }
    
    public int absHeight() {
        return getHeight() + getElevation();
    }
    
    public int getHeight() {
        return 0;
    }

    public int getElevation() {
        return m_elev;
    }
    
    public boolean isImmobile() {
        return true;
    }
    
    public String getDisplayName() {
        return "Minefield: " + m_coords.getBoardNum() + " (Clear)";
    }
    
    /**
     * The transformation encodes the y value in the top 5 decimal digits and
     * the x value in the bottom 5.  Could more efficiently encode this by
     * partitioning the binary representation, but this is more human readable
     * and still allows for a 99999x99999 hex map.
     */
     
    // encode 2 numbers into 1
    public static int coordsToId(Coords c) {
        return c.y * 100000 + c.x;
    }
    
    // decode 1 number into 2
    public static Coords idToCoords(int id) {
        int y = id / 100000;
        return new Coords(id - (y * 100000), y);
    }    
}
