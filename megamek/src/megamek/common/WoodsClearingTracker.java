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
package megamek.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.board.BoardLocation;
import megamek.common.units.Terrains;

/**
 * Tracks ongoing woods-clearing operations using chainsaws and dual saws.
 *
 * <p>Per TM pp.241-243, a chainsaw or dual saw can clear a path through wooded hexes.
 * Instead of using the Terrain Factor damage system, a saw takes 2 turns to reduce a wooded hex one level (heavy to
 * light, light to rough). Two units clearing the same hex can reduce this to 1 turn.</p>
 *
 * <p>Work accumulates per-hex and persists even if no entity works the hex for a round
 * (a partially cut tree stays partially cut). Work only progresses when at least one
 * entity actively declares clearing in a given round.</p>
 */
public class WoodsClearingTracker implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Number of work turns needed to clear a hex with a single saw. */
    public static final int TURNS_REQUIRED_SINGLE = 2;

    /** Number of work turns needed when 2+ saws are working the same hex. */
    public static final int TURNS_REQUIRED_MULTI = 1;

    private final Map<BoardLocation, ClearingState> clearingOperations = new HashMap<>();

    /**
     * Tracks the clearing state for a single hex.
     */
    private static class ClearingState implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** Entity IDs that declared clearing this round. */
        final Set<Integer> contributorsThisRound = new HashSet<>();

        /** Entity IDs that declared clearing last round. */
        final Set<Integer> contributorsLastRound = new HashSet<>();

        /** Total work turns accumulated on this hex. */
        int accumulatedWork = 0;
    }

    /**
     * Registers an entity as clearing a specific hex this round.
     *
     * @param entityId  the ID of the entity performing clearing
     * @param targetHex the hex being cleared
     */
    public void declareClearing(int entityId, BoardLocation targetHex) {
        ClearingState state = clearingOperations.computeIfAbsent(targetHex, k -> new ClearingState());
        state.contributorsThisRound.add(entityId);
    }

    /**
     * Processes the round transition for all clearing operations.
     *
     * <p>For each hex that had contributors this round, increments accumulated work.
     * A hex completes when accumulated work reaches the required threshold (2 turns for single unit,
     * 1 turn for 2+ units). Hexes with no contributors this round retain their accumulated work
     * (a partially cut tree stays partially cut) but do not progress.</p>
     *
     * @return list of hex locations that have completed clearing (ready for terrain reduction)
     */
    public List<BoardLocation> processNewRound() {
        List<BoardLocation> completed = new ArrayList<>();
        Iterator<Map.Entry<BoardLocation, ClearingState>> it = clearingOperations.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<BoardLocation, ClearingState> entry = it.next();
            ClearingState state = entry.getValue();

            if (state.contributorsThisRound.isEmpty()) {
                // No one worked this hex this round - work persists but doesn't progress
                state.contributorsLastRound.clear();
            } else {
                // Increment work
                state.accumulatedWork++;

                // Check for completion
                boolean multipleContributors = state.contributorsThisRound.size() >= 2;
                if (multipleContributors || state.accumulatedWork >= TURNS_REQUIRED_SINGLE) {
                    completed.add(entry.getKey());
                    it.remove();
                } else {
                    // Shift current round contributors to last round for next cycle
                    state.contributorsLastRound.clear();
                    state.contributorsLastRound.addAll(state.contributorsThisRound);
                    state.contributorsThisRound.clear();
                }
            }
        }

        return completed;
    }

    /**
     * Checks if an entity declared clearing in the previous round (used for firing penalty).
     *
     * @param entityId the entity ID to check
     *
     * @return true if the entity was clearing last round and should have the running/flank penalty
     */
    public boolean wasClearingLastRound(int entityId) {
        for (ClearingState state : clearingOperations.values()) {
            if (state.contributorsLastRound.contains(entityId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a hex already has accumulated clearing work (partially cut).
     *
     * @param hex the hex to check
     *
     * @return true if the hex has any accumulated work from prior turns
     */
    public boolean hasAccumulatedWork(BoardLocation hex) {
        ClearingState state = clearingOperations.get(hex);
        return state != null && state.accumulatedWork > 0;
    }

    /**
     * Checks if an entity is currently declared as clearing this round.
     *
     * @param entityId the entity ID to check
     *
     * @return true if the entity declared clearing this round
     */
    public boolean isClearingThisRound(int entityId) {
        for (ClearingState state : clearingOperations.values()) {
            if (state.contributorsThisRound.contains(entityId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the hex that a given entity is clearing this round, or null if not clearing.
     *
     * @param entityId the entity ID to check
     *
     * @return the BoardLocation being cleared, or null
     */
    public BoardLocation getClearingTarget(int entityId) {
        for (Map.Entry<BoardLocation, ClearingState> entry : clearingOperations.entrySet()) {
            if (entry.getValue().contributorsThisRound.contains(entityId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Returns all entity IDs currently involved in clearing operations (this round or last round).
     *
     * @return set of entity IDs involved in clearing
     */
    public Set<Integer> getAllClearingEntities() {
        Set<Integer> entities = new HashSet<>();
        for (ClearingState state : clearingOperations.values()) {
            entities.addAll(state.contributorsThisRound);
            entities.addAll(state.contributorsLastRound);
        }
        return entities;
    }

    /**
     * Returns a map of hex locations to the number of turns remaining to complete clearing. Accounts for the number of
     * contributors this round when calculating remaining turns.
     *
     * @return map of hex locations to turns remaining (1 or 2 typically)
     */
    public Map<BoardLocation, Integer> getTurnsRemainingPerHex() {
        Map<BoardLocation, Integer> result = new HashMap<>();
        for (Map.Entry<BoardLocation, ClearingState> entry : clearingOperations.entrySet()) {
            ClearingState state = entry.getValue();
            int turnsNeeded = (state.contributorsThisRound.size() >= 2)
                  ? TURNS_REQUIRED_MULTI : TURNS_REQUIRED_SINGLE;
            // Count current round's contribution toward displayed remaining turns
            int effectiveWork = state.accumulatedWork;
            if (!state.contributorsThisRound.isEmpty()) {
                effectiveWork++;
            }
            int remaining = Math.max(0, turnsNeeded - effectiveWork);
            result.put(entry.getKey(), remaining);
        }
        return result;
    }

    /**
     * Removes tracker entries for hexes that no longer contain woods or jungle. This handles cases where fire or other
     * effects removed the woods before the saw clearing completed.
     *
     * @param hexLookup a function that returns the Hex at a given BoardLocation, or null
     */
    public void removeStaleEntries(java.util.function.Function<BoardLocation, Hex> hexLookup) {
        clearingOperations.entrySet().removeIf(entry -> {
            Hex hex = hexLookup.apply(entry.getKey());
            return hex == null
                  || (!hex.containsTerrain(Terrains.WOODS) && !hex.containsTerrain(Terrains.JUNGLE));
        });
    }

    /**
     * Removes all clearing operations. Used when resetting the game state.
     */
    public void clear() {
        clearingOperations.clear();
    }
}
