/*
 * Copyright (c) 2007-2008 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.victory;

import java.util.Map;

import megamek.common.Game;

/**
 * Interface for classes judging whether a victory occurred or not. These classes must not modify
 * the game state. Reporting must be done via the given interface.
 * <BR>
 * Note: VictoryConditions may be checked any number of times at various points in the game and
 * implementing classes must be able to deal with this. For example, a VictoryCondition that counts
 * rounds must not assume that is is called only once per round.
 * <BR>
 * Note: VictoryConditions should add proper reports to their resulting VictoryResult. When doing so,
 * note that their results might be filtered in double-blind games. So, the reports should be
 * mostly of the "what is the score"-type (player A occupies the victory location) or fact-type
 * (Player A has destroyed player B's commander).
 * <BR>
 * Note: The context should use simple serializable objects (preferably Integers, Strings, Doubles etc.)
 * to store state between executions if such a feature is absolutely required.
 */
public interface VictoryCondition {

    /**
     * Tests if a victory (or draw) has occurred and returns a properly filled in VictoryResult.
     * Should return {@link VictoryResult#noResult()} if this victory condition has not been met.
     *
     * @param game The current {@link Game}
     * @param context The context to consider, see {@link VictoryCondition} (currently unused)
     * @return The result of the victory condition test
     */
    VictoryResult checkVictory(Game game, Map<String, Object> context);
}
