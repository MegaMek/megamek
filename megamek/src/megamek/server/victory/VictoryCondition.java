/*
 * Copyright (c) 2007-2008 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server.victory;

import java.util.Map;

import megamek.common.game.Game;

/**
 * Interface for classes judging whether a victory occurred or not. These classes must not modify the game state.
 * Reporting must be done via the given interface.
 * <BR>
 * Note: VictoryConditions may be checked any number of times at various points in the game and implementing classes
 * must be able to deal with this. For example, a VictoryCondition that counts rounds must not assume that is is called
 * only once per round.
 * <BR>
 * Note: VictoryConditions should add proper reports to their resulting VictoryResult. When doing so, note that their
 * results might be filtered in double-blind games. So, the reports should be mostly of the "what is the score"-type
 * (player A occupies the victory location) or fact-type (Player A has destroyed player B's commander).
 * <BR>
 * Note: The context should use simple serializable objects (preferably Integers, Strings, Doubles etc.) to store state
 * between executions if such a feature is absolutely required.
 */
public interface VictoryCondition {

    /**
     * Tests if a victory (or draw) has occurred and returns a properly filled in VictoryResult. Should return
     * {@link VictoryResult#noResult()} if this victory condition has not been met.
     *
     * @param game    The current {@link Game}
     * @param context The context to consider, see {@link VictoryCondition} (currently unused)
     *
     * @return The result of the victory condition test
     */
    VictoryResult checkVictory(Game game, Map<String, Object> context);
}
