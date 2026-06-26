/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.units;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

import megamek.common.Hex;
import megamek.common.HexTarget;
import megamek.common.Messages;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.game.Game;

/**
 * This class represents a single, targetable hex of a building. The building itself may occupy multiple hexes.
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
    private int id = IBuilding.UNKNOWN;

    /**
     * The height of the building at the targeted position, used to indicate the number of levels of the building.  A
     * height 0 building is a 1-story (level 1) building.  Bridges will always have a height of 0.
     */
    private int height = IBuilding.UNKNOWN;

    /**
     * The elevation of the building at the targeted position, generally only used by bridges but also for buildings on
     * hexes with depth.
     */
    private int elevation = IBuilding.UNKNOWN;

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
     * @param coords - the <code>Coords</code> of the hex being targeted.
     * @param board  - the game's <code>Board</code> object.
     * @param nType  the target type
     *
     * @throws IllegalArgumentException will be thrown if the given coordinates do not contain a building.
     */
    protected void init(Coords coords, Board board, int nType) {
        position = coords;
        boardId = board.getBoardId();
        type = nType;
        id = HexTarget.locationToId(getBoardLocation());

        // Get the building at the given coordinates.
        IBuilding bldg = board.getBuildingAt(position);
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
     * @param coords - the <code>Coords</code> of the hex being targeted.
     * @param board  - the game's <code>Board</code> object.
     * @param nType  - an <code>int</code> value that indicates whether the player is attempting to set the building on
     *               fire, or not.
     *
     * @throws IllegalArgumentException will be thrown if the given coordinates do not contain a building.
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
     * @param coords - the <code>Coords</code> of the hex being targeted.
     * @param board  - the game's <code>Board</code> object.
     * @param ignite - a <code>boolean</code> flag that indicates whether the player is attempting to set the building
     *               on fire, or not.
     *
     * @throws IllegalArgumentException will be thrown if the given coordinates do not contain a building.
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
    public void setOwnerId(int newOwnerId) {}

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
