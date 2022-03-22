/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import java.util.HashMap;
import java.util.Map;

public class HexTarget implements Targetable {
    private static final long serialVersionUID = -5742445409423125942L;
    private Coords m_coords;
    private boolean m_bIgnite;
    private int m_type;

    public HexTarget(Coords c, int nType) {
        m_coords = c;
        m_type = nType;
        m_bIgnite = (nType == Targetable.TYPE_HEX_IGNITE);
    }
    
    /**
     * Creates a new HexTarget given a set of coordinates and a type defined in Targetable.
     * the board parameter is ignored.
     */
    @Deprecated
    public HexTarget(Coords c, Board board, int nType) {
        m_coords = c;
        m_type = nType;
        m_bIgnite = (nType == Targetable.TYPE_HEX_IGNITE);
    }

    @Override
    public int getTargetType() {
        return m_type;
    }

    @Override
    public int getTargetId() {
        return HexTarget.coordsToId(m_coords);
    }

    @Override
    public Coords getPosition() {
        return m_coords;
    }

    @Override
    public Map<Integer, Coords> getSecondaryPositions() {
        return new HashMap<>();
    }

    @Override
    public int relHeight() {
        return getHeight() + getElevation();
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getElevation() {
        return 0;
    }

    @Override
    public boolean isImmobile() {
        return ((m_type != Targetable.TYPE_HEX_BOMB) && (m_type != Targetable.TYPE_HEX_AERO_BOMB));
    }

    @Override
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
            case (Targetable.TYPE_HEX_AERO_BOMB):
                name = Messages.getString("HexTarget.Bomb");
                break;
            case (Targetable.TYPE_HEX_TAG):
                name = Messages.getString("HexTarget.Tag");
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
        return c.getY() * 100000 + c.getX();
    }

    // decode 1 number into 2
    public static Coords idToCoords(int id) {
        int y = id / 100000;
        return new Coords(id - (y * 100000), y);
    }

    @Override
    public int sideTable(Coords src) {
        return ToHitData.SIDE_FRONT;
    }

    @Override
    public int sideTable(Coords src, boolean usePrior) {
        return sideTable(src);
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Targetable#isOffBoard()
     */
    @Override
    public boolean isOffBoard() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Targetable#isAirborne()
     */
    @Override
    public boolean isAirborne() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Targetable#isAirborneVTOLorWIGE()
     */
    @Override
    public boolean isAirborneVTOLorWIGE() {
        return false;
    }

    @Override
    public int getAltitude() {
        return 0;
    }
    
    @Override
    public boolean isEnemyOf(Entity other) {
        return true;
    }
}
