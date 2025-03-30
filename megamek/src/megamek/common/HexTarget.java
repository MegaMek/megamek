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
    private final int boardId;
    private boolean m_bIgnite;
    private int m_type;
    private HexTarget originalTarget = null;
    private int targetLevel = 0;

    // Legacy: Needs to be replaced with the other constructor
    public HexTarget(Coords c, int nType) {
        m_coords = c;
        m_type = nType;
        m_bIgnite = (nType == Targetable.TYPE_HEX_IGNITE);
        boardId = 0;
    }

    public HexTarget(Coords c, int boardId, int nType) {
        m_coords = c;
        this.boardId = boardId;
        m_type = nType;
        m_bIgnite = (nType == Targetable.TYPE_HEX_IGNITE);
    }

    public HexTarget(BoardLocation boardLocation, int nType) {
        this(boardLocation.coords(), boardLocation.boardId(), nType);
    }

    @Override
    public int getBoardId() {
        return boardId;
    }

    @Override
    public int getTargetType() {
        return m_type;
    }

    @Override
    public int getId() {
        return locationToId(getBoardLocation());
    }

    @Override
    public void setId(int newId) { }

    @Override
    public int getOwnerId() {
        return Player.PLAYER_NONE;
    }

    @Override
    public void setOwnerId(int newOwnerId) { }

    @Override
    public int getStrength() {
        return 0;
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
        final String name;
        switch (m_type) {
            case Targetable.TYPE_FLARE_DELIVER:
                name = Messages.getString("HexTarget.DeliverFlare");
                break;
            case Targetable.TYPE_MINEFIELD_DELIVER:
                name = Messages.getString("HexTarget.DeliverMinefield");
                break;
            case Targetable.TYPE_HEX_BOMB:
            case Targetable.TYPE_HEX_AERO_BOMB:
                name = Messages.getString("HexTarget.Bomb");
                break;
            case Targetable.TYPE_HEX_CLEAR:
                name = Messages.getString("HexTarget.Clear");
                break;
            case Targetable.TYPE_HEX_IGNITE:
                name = Messages.getString("HexTarget.Ignite");
                break;
            case Targetable.TYPE_HEX_ARTILLERY:
                name = Messages.getString("HexTarget.Artillery");
                break;
            case Targetable.TYPE_HEX_EXTINGUISH:
                name = Messages.getString("HexTarget.Extinguish");
                break;
            case Targetable.TYPE_HEX_SCREEN:
                name = Messages.getString("HexTarget.Screen");
                break;
            case Targetable.TYPE_HEX_TAG:
                name = Messages.getString("HexTarget.Tag");
                break;
            default:
                name = "";
                break;
        }
        return "Hex: " + m_coords.getBoardNum() + name;
    }

    public boolean isIgniting() {
        return m_bIgnite;
    }

    /** Allows a 9999 x 999 map and board IDs up to 200 */
    public static int locationToId(BoardLocation boardLocation) {
        return boardLocation.boardId() * 10000000
                     + boardLocation.coords().getY() * 10000
                     + boardLocation.coords().getX();
    }

    public static BoardLocation idToLocation(int id) {
        int boardId = id / 10000000;
        id -= boardId * 10000000;
        int y = id / 10000;
        int x = id - y * 10000;
        return BoardLocation.of(new Coords(x, y), boardId);
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

    @Override
    public String generalName() {
        return getDisplayName();
    }

    @Override
    public String specificName() {
        return "";
    }

    // For artillery leading
    public void setOriginalTarget(HexTarget target) {
        this.originalTarget = target;
    }

    // For artillery leading
    public HexTarget getOriginalTarget() {
        return this.originalTarget;
    }

    // For bombing
    public int getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(int targetLevel) {
        this.targetLevel = targetLevel;
    }

    @Override
    public String toString() {
        return "HexTarget (type %d): %s; Board #%d".formatted(m_type, getPosition(), boardId);
    }
}
