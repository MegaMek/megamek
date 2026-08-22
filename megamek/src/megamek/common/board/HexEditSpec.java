/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.board;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Game Master's edit of one or more hexes: which hexes to change, and what terrain they should hold afterwards.
 *
 * <p>The edit says what the hex should end up as rather than listing changes to make to it. A hex ends up holding
 * exactly the terrains named here, at the levels named here, and nothing else; a terrain the gamemaster removed is
 * simply absent from the list. Describing the finished hex rather than a sequence of steps means the result can be
 * checked before any of it is applied, and means the same edit can be sent to several hexes at once.</p>
 *
 * <p>This travels between client and server, so everything it holds must be serializable.</p>
 */
public class HexEditSpec implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The hexes to change. */
    private final List<Coords> coords = new ArrayList<>();

    /** The board the hexes are on. */
    private int boardId;

    /** The terrain each edited hex should end up holding, as terrain type to level. */
    private final Map<Integer, Integer> terrainLevels = new HashMap<>();

    /**
     * Creates an edit of the given hexes on the given board.
     *
     * @param boardId The board the hexes are on
     */
    public HexEditSpec(int boardId) {
        this.boardId = boardId;
    }

    /** @return the hexes this edit applies to */
    public List<Coords> getCoords() {
        return coords;
    }

    /** Adds a hex for this edit to apply to. */
    public void addCoords(Coords hex) {
        if (!coords.contains(hex)) {
            coords.add(hex);
        }
    }

    /** Stops this edit applying to the given hex. */
    public void removeCoords(Coords hex) {
        coords.remove(hex);
    }

    /** @return the board the hexes are on */
    public int getBoardId() {
        return boardId;
    }

    public void setBoardId(int boardId) {
        this.boardId = boardId;
    }

    /** @return the terrain each edited hex should end up holding, as terrain type to level */
    public Map<Integer, Integer> getTerrainLevels() {
        return terrainLevels;
    }

    /**
     * Says that the edited hexes should hold the given terrain at the given level.
     *
     * @param terrainType  The terrain, from {@link megamek.common.units.Terrains}
     * @param terrainLevel The level it should be at
     */
    public void setTerrain(int terrainType, int terrainLevel) {
        terrainLevels.put(terrainType, terrainLevel);
    }

    /** Says that the edited hexes should not hold the given terrain at all. */
    public void removeTerrain(int terrainType) {
        terrainLevels.remove(terrainType);
    }

    /** @return {@code true} when this edit would leave the hexes with no terrain at all, which is bare ground */
    public boolean isClearingHexes() {
        return terrainLevels.isEmpty();
    }
}
