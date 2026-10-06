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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.client.bot.Messages;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.Princess;
import megamek.common.enums.ForcedWithdrawalOrder;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WaypointReply}: the bot's answer to a waypoint order says what the unit will actually do (issue
 * #9038), where it used to say "Waypoints set" even after dropping every hex.
 */
class WaypointReplyTest {

    private static final String SET_WAYPOINTS = "Princess.command.setWaypoints";

    private Princess princess;
    private BehaviorSettings behaviorSettings;
    private Entity champion;

    @BeforeEach
    void setUp() {
        behaviorSettings = new BehaviorSettings();
        princess = mock(Princess.class);
        when(princess.getBehaviorSettings()).thenReturn(behaviorSettings);
        champion = mock(Entity.class);
        when(champion.getDisplayName()).thenReturn("Champion CHP-2N");
        when(champion.getForcedWithdrawalOrder()).thenReturn(ForcedWithdrawalOrder.BOT_RULES);
    }

    @Test
    void allHexesKeptIsASuccess() {
        assertEquals(Messages.getString("Princess.command.setWaypoints.success", "Champion CHP-2N"),
              WaypointReply.build(princess, champion, 2, 2, SET_WAYPOINTS));
    }

    @Test
    void noHexKeptSaysTheWaypointsWereNotSet() {
        assertEquals(Messages.getString("Princess.command.setWaypoints.noneReachable", "Champion CHP-2N"),
              WaypointReply.build(princess, champion, 0, 1, SET_WAYPOINTS));
    }

    @Test
    void someHexesDroppedSaysHowMany() {
        assertEquals(Messages.getString("Princess.command.setWaypoints.partial", "Champion CHP-2N", 1, 3),
              WaypointReply.build(princess, champion, 1, 3, SET_WAYPOINTS));
    }

    @Test
    void anActiveFleeOrderIsMentioned() {
        behaviorSettings.setAutoFlee(true);
        behaviorSettings.setDestinationEdge(CardinalEdge.NORTH);

        String reply = WaypointReply.build(princess, champion, 1, 1, SET_WAYPOINTS);

        assertTrue(reply.endsWith(Messages.getString("Princess.command.waypoints.fleeActive", "Champion CHP-2N")));
    }

    @Test
    void aGamemastersWithdrawOrderIsMentioned() {
        when(champion.getForcedWithdrawalOrder()).thenReturn(ForcedWithdrawalOrder.WITHDRAW);

        String reply = WaypointReply.build(princess, champion, 1, 1, SET_WAYPOINTS);

        assertTrue(reply.endsWith(
              Messages.getString("Princess.command.waypoints.gamemasterWithdraw", "Champion CHP-2N")));
    }
}
