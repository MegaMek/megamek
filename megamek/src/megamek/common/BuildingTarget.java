/*
* MegaMek -
* Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.common;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a single, targetable hex of a building. The building itself may occupy
 * multiple hexes.
 *
 * @author Suvarov454@sourceforge.net (James A. Damour)
 */
public class BuildingTarget implements Targetable {
    @Serial
    private static final long serialVersionUID = 6432766092407639630L;

    /**
     * The coordinates of the building being targeted.
     */
    private Coords position = null;
    private int boardId = 0;

    /**
     * The ID of the building being targeted.
     */
    private int id = Building.UNKNOWN;

    /**
     * The height of the building at the targeted position, used to indicate
     * the number of levels of the building.  A height 0 building is a 1-story
     * (level 1) building.  Bridges will always have a height of 0.
     */
    private int height = Building.UNKNOWN;

    /**
     * The elevation of the building at the targeted position, generally only
     * used by bridges but also for buildings on hexes with depth.
     */
    private int elevation = Building.UNKNOWN;

    /**
     * The name of this hex of the building.
     */
    private String name = null;

    /**
     * The type of attack that is targeting this building.
     */
    private int type;

    /**
     * Initialize this object from the input.
     *
     * @param coords - the <code>Coords</code> of the hext being targeted.
     * @param board  - the game's <code>Board</code> object.
     * @param nType the target type
     * @throws IllegalArgumentException will be thrown if
     *            the given coordinates do not contain a building.
     */
    protected void init(Coords coords, Board board, int nType) {
        position = coords;
        boardId = board.getBoardId();
        type = nType;
        id = HexTarget.locationToId(getBoardLocation());

        // Get the building at the given coordinates.
        Building bldg = board.getBuildingAt(position);
        if (bldg == null) {
            throw new IllegalArgumentException("No building at %s.".formatted(getBoardLocation()));
        }

        name = "Hex %s of %s".formatted(position.getBoardNum(), bldg.toString());
        if (boardId > 0) {
            name += " (Board #%d - %s)".formatted(boardId, board.getBoardName());
        }
        name += switch (nType) {
            case Targetable.TYPE_BLDG_IGNITE -> Messages.getString("BuildingTarget.Ignite");
            case Targetable.TYPE_BUILDING -> Messages.getString("BuildingTarget.Collapse");
            case Targetable.TYPE_BLDG_TAG -> Messages.getString("BuildingTarget.Tag");
            default -> "";
        };

        // Bottom of building is at ground level, top of building is at BLDG_ELEV.
        // Note that height of 0 is a single story building.
        // Bridges are always height 0, and the BRIDGE_ELEV indicates the elevation
        Hex targetHex = board.getHex(position);
        elevation = Math.max(-targetHex.depth(), targetHex.terrainLevel(Terrains.BRIDGE_ELEV));
        height = targetHex.terrainLevel(Terrains.BLDG_ELEV);
        if (height <= 0) {
            height = 0;
        } else {
            height--;
        }
    }

    /**
     * Target a single hex of a building.
     *
     * @param coords - the <code>Coords</code> of the hext being targeted.
     * @param board  - the game's <code>Board</code> object.
     * @param nType  - an <code>int</code> value that indicates whether the
     *               player is attempting to set the building on fire, or not.
     * @throws IllegalArgumentException will be thrown if
     *            the given coordinates do not contain a building.
     */
    public BuildingTarget(Coords coords, Board board, int nType) {
        init(coords, board, nType);
    }

    public BuildingTarget(Game game, BoardLocation boardLocation, int nType) {
        init(boardLocation.coords(), game.getBoard(boardLocation.boardId()), nType);
    }

    /**
     * Target a single hex of a building.
     *
     * @param coords - the <code>Coords</code> of the hext being targeted.
     * @param board  - the game's <code>Board</code> object.
     * @param ignite - a <code>boolean</code> flag that indicates whether the
     *               player is attempting to set the building on fire, or not.
     * @throws IllegalArgumentException will be thrown if
     *            the given coordinates do not contain a building.
     */
    public BuildingTarget(Coords coords, Board board, boolean ignite) {
        init(coords, board, ignite ? Targetable.TYPE_BLDG_IGNITE : Targetable.TYPE_BUILDING);
    }

    // Implementation of Targetable
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
        return id;
    }

    @Override
    public void setId(int newId) {
        id = newId;
    }

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
        return position;
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
        return height;
    }

    @Override
    public int getElevation() {
        return elevation;
    }

    @Override
    public boolean isImmobile() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return name;
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
        return name;
    }

    @Override
    public String specificName() {
        return "";
    }
}
