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
package megamek.client.bot.princess.commands;

import megamek.client.bot.Messages;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.princess.UnitBehavior;
import megamek.common.enums.ForcedWithdrawalOrder;
import megamek.common.units.Entity;

/**
 * Builds the bot's reply to a set-waypoints or add-waypoint order, saying what the unit will actually do: how many of
 * the hexes it kept, and whether another order stops it following them for now (issue #9038). Before this, the bot
 * answered "Waypoints set" even when it had thrown every hex away.
 */
final class WaypointReply {

    private WaypointReply() {
    }

    /**
     * @param princess       the bot replying
     * @param unit           the unit the waypoints were given to
     * @param keptCount      how many waypoints the bot kept
     * @param requestedCount how many waypoints the order gave
     * @param keyPrefix      {@code Princess.command.setWaypoints} or {@code Princess.command.addWaypoint}
     *
     * @return the reply to send to chat
     */
    static String build(Princess princess, Entity unit, int keptCount, int requestedCount, String keyPrefix) {
        String unitName = unit.getDisplayName();
        if (keptCount == 0) {
            return Messages.getString(keyPrefix + ".noneReachable", unitName);
        }
        String reply = (keptCount < requestedCount)
              ? Messages.getString(keyPrefix + ".partial", unitName, keptCount, requestedCount)
              : Messages.getString(keyPrefix + ".success", unitName);
        if (unit.getForcedWithdrawalOrder() == ForcedWithdrawalOrder.WITHDRAW) {
            return reply + ' ' + Messages.getString("Princess.command.waypoints.gamemasterWithdraw", unitName);
        }
        if (UnitBehavior.isFleeOrdered(princess)) {
            return reply + ' ' + Messages.getString("Princess.command.waypoints.fleeActive", unitName);
        }
        return reply;
    }
}
