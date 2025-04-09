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

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

public class HexTarget implements Targetable {
    @Serial
    private static final long serialVersionUID = -5742445409423125942L;

    private final Coords coords;
    private final int boardId;
    private final boolean isIgnite;
    private final int type;
    private HexTarget originalTarget = null;
    private int targetLevel = 0;

    // Legacy: Needs to be replaced with the other constructors
    public HexTarget(Coords c, int nType) {
        coords = c;
        type = nType;
        isIgnite = (nType == Targetable.TYPE_HEX_IGNITE);
        boardId = 0;
    }

    public HexTarget(Coords c, int boardId, int nType) {
        coords = c;
        this.boardId = boardId;
        type = nType;
        isIgnite = (nType == Targetable.TYPE_HEX_IGNITE);
    }

    public HexTarget(Coords c, Board board, int nType) {
        this(c, board.getBoardId(), nType);
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
        return type;
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
        return coords;
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
        return ((type != Targetable.TYPE_HEX_BOMB) && (type != Targetable.TYPE_HEX_AERO_BOMB));
    }

    @Override
    public String getDisplayName() {
        final String typeString = switch (type) {
            case Targetable.TYPE_FLARE_DELIVER -> Messages.getString("HexTarget.DeliverFlare");
            case Targetable.TYPE_MINEFIELD_DELIVER -> Messages.getString("HexTarget.DeliverMinefield");
            case Targetable.TYPE_HEX_BOMB, Targetable.TYPE_HEX_AERO_BOMB -> Messages.getString("HexTarget.Bomb");
            case Targetable.TYPE_HEX_CLEAR -> Messages.getString("HexTarget.Clear");
            case Targetable.TYPE_HEX_IGNITE -> Messages.getString("HexTarget.Ignite");
            case Targetable.TYPE_HEX_ARTILLERY -> Messages.getString("HexTarget.Artillery");
            case Targetable.TYPE_HEX_EXTINGUISH -> Messages.getString("HexTarget.Extinguish");
            case Targetable.TYPE_HEX_SCREEN -> Messages.getString("HexTarget.Screen");
            case Targetable.TYPE_HEX_TAG -> Messages.getString("HexTarget.Tag");
            default -> "";
        };
        // When the board Id is 0, assume (for now) that there is only one board and no board info is necessary
        return "Hex: " + ((boardId == 0) ? coords : getBoardLocation()) + typeString;
    }

    public boolean isIgniting() {
        return isIgnite;
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

    @Override
    public boolean isOffBoard() {
        return false;
    }

    @Override
    public boolean isAirborne() {
        return false;
    }

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
        originalTarget = target;
    }

    // For artillery leading
    public HexTarget getOriginalTarget() {
        return originalTarget;
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
        return "HexTarget (type %d): %s; Board #%d".formatted(type, getPosition(), boardId);
    }
}
