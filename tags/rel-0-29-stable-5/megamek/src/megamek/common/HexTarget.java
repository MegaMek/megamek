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

public class HexTarget implements Targetable
{
    private Coords m_coords;
    private boolean m_bIgnite;
    private int m_elev;
    
    public HexTarget(Coords c, Board board, int nType) {
        m_coords = c;
        m_elev = board.getHex(m_coords).getElevation();
        m_bIgnite = (nType == Targetable.TYPE_HEX_IGNITE);
    }
    
    public HexTarget(Coords c, Board board, boolean bIgnite) {
        m_coords = c;
        m_elev = board.getHex(m_coords).getElevation();
        m_bIgnite = bIgnite;
    }
    
    public int getTargetType() { 
        return m_bIgnite ? Targetable.TYPE_HEX_IGNITE : Targetable.TYPE_HEX_CLEAR;
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
        return "Hex: " + m_coords.getBoardNum() + 
                (m_bIgnite ? " (Ignite)" : " (Clear)");
    }
    
    public boolean isIgniting() {
        return m_bIgnite;
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
