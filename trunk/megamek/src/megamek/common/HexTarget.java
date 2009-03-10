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

public class HexTarget implements Targetable {
    /**
     *
     */
    private static final long serialVersionUID = -5742445409423125942L;
    private Coords m_coords;
    private boolean m_bIgnite;
    private int m_elev;
    private int m_type;

    public HexTarget(Coords c, IBoard board, int nType) {
        m_coords = c;
        m_elev = board.getHex(m_coords).getElevation();
        m_type = nType;
        m_bIgnite = (nType == Targetable.TYPE_HEX_IGNITE);
    }

    public int getTargetType() {
        return m_type;
    }

    public int getTargetId() {
        return HexTarget.coordsToId(m_coords);
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
        return (m_type != Targetable.TYPE_MINEFIELD_DELIVER && m_type != Targetable.TYPE_HEX_BOMB);
    }

    public String getDisplayName() {
        String name = "";
        switch (m_type) {
            case (Targetable.TYPE_FLARE_DELIVER):
                name = Messages.getString("HexTarget.DeliverFlare");
                break;
            case (Targetable.TYPE_MINEFIELD_DELIVER):
                name = Messages.getString("HexTarget.DeliverMinefield");
                break;
            case (Targetable.TYPE_HEX_BOMB):
                name = Messages.getString("HexTarget.Bomb");
                break;
            case (Targetable.TYPE_HEX_CLEAR):
                name = Messages.getString("HexTarget.Clear");
                break;
            case (Targetable.TYPE_HEX_IGNITE):
                name = Messages.getString("HexTarget.Ignite");
                break;
            case (Targetable.TYPE_HEX_ARTILLERY):
                name = Messages.getString("HexTarget.Artillery");
                break;
            case Targetable.TYPE_HEX_EXTINGUISH:
                name = Messages.getString("HexTarget.Extinguish");
                break;
            case (Targetable.TYPE_HEX_SCREEN):
                name = Messages.getString("HexTarget.Screen");
                break;
        }
        return "Hex: " + m_coords.getBoardNum() + name;
    }

    public boolean isIgniting() {
        return m_bIgnite;
    }

    /**
     * The transformation encodes the y value in the top 5 decimal digits and
     * the x value in the bottom 5. Could more efficiently encode this by
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

    public int sideTable(Coords src) {
        return ToHitData.SIDE_FRONT;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Targetable#isOffBoard()
     */
    public boolean isOffBoard() {
        return false;
    }
}
