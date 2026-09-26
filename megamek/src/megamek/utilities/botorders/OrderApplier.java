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
 * Gives a bot's unit the orders a player would give it, for scripted headless tests, and reads back the orders the
 * unit holds so the trace can show them.
 *
 * <p>This is the one seam between the test harness and wherever the bot keeps its orders. On the #9038 fix branch
 * that was the bot's in-memory waypoint list; since the unit orders model, orders live on the unit
 * ({@link UnitOrdersApplier}). Keep implementations thin: no game logic here.</p>
 */
public interface OrderApplier {

    /**
     * Applies one scripted unit order (anything except test damage and the bot-wide flee).
     *
     * @param bot        the bot that owns the unit
     * @param botUnit    the bot's own copy of the unit
     * @param serverUnit the server's copy of the unit
     * @param order      the order
     * @param round      the current round
     *
     * @return a short description of the result, for the log and the trace
     */
    String apply(Princess bot, Entity botUnit, Entity serverUnit, ScriptedOrder order, int round);

    /**
     * Orders the whole bot to flee toward an edge, as the panel's flee order does; {@link CardinalEdge#NONE} cancels.
     *
     * @param bot  the bot
     * @param edge the edge
     */
    void orderFlee(Princess bot, CardinalEdge edge);

    /**
     * Returns the orders the unit holds right now.
     *
     * @param bot  the bot that owns the unit
     * @param unit the bot's own copy of the unit
     *
     * @return the unit's orders
     */
    OrderSnapshot snapshot(Princess bot, Entity unit);

    /**
     * The orders a unit holds at one moment, as written to the trace.
     *
     * @param headWaypoint  the next waypoint, or {@code null}
     * @param route         every waypoint still to go, in order
     * @param priority      the route priority, or an empty string when the model has none
     * @param paused        whether a Pause order holds the unit
     * @param stopped       whether a Stop order holds the unit this round
     * @param edgeOrder     MOVE_TO, EXIT_BY or NONE (empty when the model has none)
     * @param edge          the edge of the edge order, or an empty string
     * @param facingMoving  the ordered facing while moving, 0-5, or -1 for automatic
     * @param facingStopped the ordered facing when stopped, 0-5, or -1 for automatic
     */
    record OrderSnapshot(@Nullable Coords headWaypoint, List<Coords> route, String priority, boolean paused,
          boolean stopped, String edgeOrder, String edge, int facingMoving, int facingStopped) {

        /** A snapshot with only a waypoint list, for a model without the other orders. */
        public static OrderSnapshot ofRoute(List<Coords> route) {
            return new OrderSnapshot(route.isEmpty() ? null : route.getFirst(), route, "", false, false, "", "", -1,
                  -1);
        }
    }
}
