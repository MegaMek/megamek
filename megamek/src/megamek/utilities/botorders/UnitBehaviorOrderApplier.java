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
import megamek.client.bot.princess.UnitBehavior;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.units.Entity;

/**
 * {@link OrderApplier} over the bot's in-memory waypoint list ({@link UnitBehavior}) and flee settings - the same
 * calls the Bot Commands panel's waypoint and flee chat commands make.
 *
 * <p>Uses only methods that exist both before and after the #9038 fix, so the harness also runs on an unfixed
 * build for before/after comparisons. The accepted-waypoint count is worked out here for that reason: before the
 * fix, {@code setEntityWaypoints} returns nothing.</p>
 */
public class UnitBehaviorOrderApplier implements OrderApplier {

    @Override
    public int setWaypoints(Princess bot, Entity unit, List<Coords> waypoints) {
        int accepted = countReachable(bot, unit, waypoints);
        bot.getUnitBehaviorTracker().setEntityWaypoints(unit, waypoints, bot);
        return accepted;
    }

    @Override
    public int addWaypoints(Princess bot, Entity unit, List<Coords> waypoints) {
        int accepted = countReachable(bot, unit, waypoints);
        bot.getUnitBehaviorTracker().addEntityWaypoint(unit, waypoints, bot);
        return accepted;
    }

    @Override
    public void clearOrders(Princess bot, Entity unit) {
        bot.getUnitBehaviorTracker().clearWaypoints(unit);
    }

    @Override
    public void orderFlee(Princess bot, CardinalEdge edge) {
        // the same settings FleeCommand changes
        boolean fleeing = edge != CardinalEdge.NONE;
        String reason = fleeing ? "Scripted flee order - " + edge.name() : "Scripted cancel flee order";
        bot.getBehaviorSettings().setDestinationEdge(edge);
        bot.getBehaviorSettings().setAutoFlee(fleeing);
        bot.setFallBack(fleeing, reason);
        bot.setFleeBoard(fleeing, reason);
    }

    @Override
    public @Nullable Coords headWaypoint(Princess bot, Entity unit) {
        return bot.getUnitBehaviorTracker().getWaypointForEntity(unit).orElse(null);
    }

    private static int countReachable(Princess bot, Entity unit, List<Coords> waypoints) {
        UnitBehavior unitBehavior = bot.getUnitBehaviorTracker();
        int reachable = 0;
        for (Coords waypoint : waypoints) {
            // despite its name, this returns true when the unit can NOT reach the hex
            if (!unitBehavior.isDestinationValidForEntity(unit, waypoint, bot)) {
                reachable++;
            }
        }
        return reachable;
    }
}
