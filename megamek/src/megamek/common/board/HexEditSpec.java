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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import megamek.common.annotations.Nullable;

/**
 * A Game Master's edit of the board: which hexes to change, and what each of them should hold afterwards.
 *
 * <p>Each hex carries its own terrain rather than the whole edit sharing one set, so a gamemaster painting a river
 * can put deep water in the channel, shallows at the edge and rough ground on the bank in a single action.</p>
 *
 * <p>A hex's entry says what that hex should end up as rather than listing changes to make to it. It ends up holding
 * exactly the terrains named, at the levels named, and nothing else; a terrain that was removed is simply absent.
 * Describing the finished hex rather than a sequence of steps means the result can be checked before any of it is
 * applied.</p>
 *
 * <p>This travels between client and server, so everything it holds must be serializable. It is a message rather than
 * game state and is never written to a savegame.</p>
 */
public class HexEditSpec implements Serializable {

    @Serial
    private static final long serialVersionUID = 2L;

    /** What one hex should end up holding. */
    public static class HexPaint implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** The terrain the hex should end up holding, as terrain type to level. Empty means bare ground. */
        private final Map<Integer, Integer> terrainLevels = new LinkedHashMap<>();

        /**
         * The level the hex should end up at, or {@code null} to leave it at the level it already has.
         *
         * <p>A hex's level and the depth of water in it are two different numbers. On a dam board a reservoir hex
         * reads {@code hex 0101 5 "water:8"}: the ground is at level 5 and the water is 8 deep, so its surface is
         * five levels up and its bottom is three levels below the datum.</p>
         */
        private Integer level;

        /** @return the terrain this hex should end up holding, as terrain type to level */
        public Map<Integer, Integer> getTerrainLevels() {
            return terrainLevels;
        }

        /**
         * Says that the hex should hold the given terrain at the given level.
         *
         * @param terrainType  The terrain, from {@link megamek.common.units.Terrains}
         * @param terrainLevel The level it should be at
         */
        public void setTerrain(int terrainType, int terrainLevel) {
            terrainLevels.put(terrainType, terrainLevel);
        }

        /** @return the level this hex should end up at, or {@code null} to leave it where it is */
        public @Nullable Integer getLevel() {
            return level;
        }

        public void setLevel(@Nullable Integer level) {
            this.level = level;
        }

        /** @return {@code true} when this would leave the hex with no terrain at all, which is bare ground */
        public boolean isBareGround() {
            return terrainLevels.isEmpty();
        }
    }

    /** The board the hexes are on. */
    private int boardId;

    /** Whether this asks to put back the last edit rather than to make a new one. */
    private boolean undoingLastEdit;

    /** Each hex to change, and what it should end up holding, in the order it was painted. */
    private final Map<Coords, HexPaint> paintedHexes = new LinkedHashMap<>();

    /**
     * Creates an edit of hexes on the given board.
     *
     * @param boardId The board the hexes are on
     */
    public HexEditSpec(int boardId) {
        this.boardId = boardId;
    }

    /** @return the board the hexes are on */
    public int getBoardId() {
        return boardId;
    }

    public void setBoardId(int boardId) {
        this.boardId = boardId;
    }

    /** @return each hex to change and what it should end up holding */
    public Map<Coords, HexPaint> getPaintedHexes() {
        return paintedHexes;
    }

    /** @return the hexes this edit applies to */
    public Set<Coords> getCoords() {
        return paintedHexes.keySet();
    }

    /**
     * Says what one hex should end up holding, replacing anything already said about it.
     *
     * @param hex   The hex to change
     * @param paint What it should end up holding
     */
    public void paint(Coords hex, HexPaint paint) {
        paintedHexes.put(hex, paint);
    }

    /** Takes a hex out of the edit, leaving it alone. */
    public void unpaint(Coords hex) {
        paintedHexes.remove(hex);
    }

    /** @return {@code true} when no hex has been painted, so there is nothing to do */
    public boolean isEmpty() {
        return paintedHexes.isEmpty();
    }

    /**
     * @return {@code true} when this asks the server to put the hexes back the way they were before the last edit,
     *       rather than to change them again. The hexes it carries are then ignored.
     */
    public boolean isUndoingLastEdit() {
        return undoingLastEdit;
    }

    /** Makes this an undo of the last edit rather than a new one. */
    public void setUndoingLastEdit(boolean undoingLastEdit) {
        this.undoingLastEdit = undoingLastEdit;
    }
}
