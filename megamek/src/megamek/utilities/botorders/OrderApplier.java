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
package megamek.utilities.botorders;

import java.util.List;

import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.Princess;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.units.Entity;

/**
 * Gives a bot's unit the orders a player would give it, for scripted headless tests.
 *
 * <p>This is the one seam between the test harness and wherever the bot keeps its orders. Today that is the bot's
 * in-memory waypoint list ({@link UnitBehaviorOrderApplier}); a later branch moves orders onto the unit itself, and
 * only a new implementation of this interface is needed then. Keep implementations thin: no game logic here.</p>
 */
public interface OrderApplier {

    /**
     * Replaces the unit's waypoints.
     *
     * @param bot       the bot that owns the unit
     * @param unit      the bot's own copy of the unit
     * @param waypoints the hexes, in order
     *
     * @return how many of the hexes the bot accepted
     */
    int setWaypoints(Princess bot, Entity unit, List<Coords> waypoints);

    /**
     * Appends to the unit's waypoints.
     *
     * @param bot       the bot that owns the unit
     * @param unit      the bot's own copy of the unit
     * @param waypoints the hexes, in order
     *
     * @return how many of the hexes the bot accepted
     */
    int addWaypoints(Princess bot, Entity unit, List<Coords> waypoints);

    /**
     * Clears the unit's orders.
     *
     * @param bot  the bot that owns the unit
     * @param unit the bot's own copy of the unit
     */
    void clearOrders(Princess bot, Entity unit);

    /**
     * Orders the whole bot to flee toward an edge, as the panel's flee order does; {@link CardinalEdge#NONE} cancels.
     *
     * @param bot  the bot
     * @param edge the edge
     */
    void orderFlee(Princess bot, CardinalEdge edge);

    /**
     * Returns the unit's current waypoint as the bot holds it, whether or not the bot is following it right now.
     *
     * @param bot  the bot that owns the unit
     * @param unit the bot's own copy of the unit
     *
     * @return the head waypoint, or {@code null} when there is none
     */
    @Nullable
    Coords headWaypoint(Princess bot, Entity unit);
}
