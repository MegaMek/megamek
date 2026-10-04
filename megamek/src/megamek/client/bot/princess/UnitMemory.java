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
package megamek.client.bot.princess;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;

/**
 * What a bot remembers about one of its own units from earlier rounds: the page for that unit in the
 * {@link BotMemory}.
 *
 * <p>A bot otherwise decides every turn from the board as it stands, so it cannot tell a unit that has been
 * marching toward something for three rounds from one that arrived by accident. This page is where such facts are
 * kept. It holds only the last few rounds; a bot needs to know what it was just doing, not its life story.</p>
 */
public class UnitMemory {

    /** How many rounds of movement are kept for each unit. */
    static final int MOVES_REMEMBERED = 5;

    private final int unitId;
    private final Deque<MoveRecord> recentMoves = new ArrayDeque<>();

    /**
     * One round's movement by one unit.
     *
     * @param round    the game round the unit moved in
     * @param from     where the unit started the round, or {@code null} if it was not on the board
     * @param to       where the unit ended its movement, or {@code null} if it left the board
     * @param behavior what the bot had the unit doing when it chose the move, or {@code null} if the bot never
     *                 worked that out for this unit this round
     */
    public record MoveRecord(int round, @Nullable Coords from, @Nullable Coords to,
          @Nullable BehaviorType behavior) {

        /**
         * @return {@code true} if the unit ended the round in the hex it started in
         */
        public boolean stayedPut() {
            return (from != null) && from.equals(to);
        }
    }

    UnitMemory(int unitId) {
        this.unitId = unitId;
    }

    public int getUnitId() {
        return unitId;
    }

    /**
     * Notes a move. A unit can be given more than one path in a round, for instance when its first is cut short;
     * the round then keeps its original starting hex and takes the newest ending hex, so a round is always one
     * record.
     *
     * @param move the move to note
     */
    void rememberMove(MoveRecord move) {
        MoveRecord previous = recentMoves.peekLast();
        if ((previous != null) && (previous.round() == move.round())) {
            recentMoves.removeLast();
            recentMoves.addLast(new MoveRecord(move.round(), previous.from(), move.to(), move.behavior()));
        } else {
            recentMoves.addLast(move);
        }
        while (recentMoves.size() > MOVES_REMEMBERED) {
            recentMoves.removeFirst();
        }
    }

    /**
     * @return the most recent remembered move, or {@code null} if the unit has not moved since the bot took it over
     */
    public @Nullable MoveRecord lastMove() {
        return recentMoves.peekLast();
    }

    /**
     * @param round the game round to look up
     *
     * @return what the unit did in that round, or {@code null} if it did not move then or the round is too long
     *       ago to be remembered
     */
    public @Nullable MoveRecord moveInRound(int round) {
        for (MoveRecord move : recentMoves) {
            if (move.round() == round) {
                return move;
            }
        }
        return null;
    }

    /**
     * @return the remembered moves, oldest first
     */
    public List<MoveRecord> recentMoves() {
        return List.copyOf(recentMoves);
    }
}
