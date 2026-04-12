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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 */
package megamek.server.rating;

import java.util.List;

import megamek.common.Player;

/**
 * Snapshot of who played and who won, used to drive rating updates.
 *
 * @param participants players included in the rating pass (typically non-observers)
 * @param winners      players counted as victors; empty when {@code draw} is true
 * @param draw         true when the game ended in a draw
 */
public record MatchResult(List<Player> participants, List<Player> winners, boolean draw) {

    public MatchResult {
        participants = List.copyOf(participants);
        winners = List.copyOf(winners);
    }
}
