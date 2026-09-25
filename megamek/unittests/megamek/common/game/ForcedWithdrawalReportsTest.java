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
package megamek.common.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Set;

import megamek.common.net.enums.PacketCommand;
import megamek.common.net.marshalling.SanityInputFilter;
import megamek.common.net.packets.Packet;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ForcedWithdrawalReports}, the one place both the WITHDRAWING tag and the honor warning ask whether a
 * unit is withdrawing under Forced Withdrawal.
 */
class ForcedWithdrawalReportsTest {

    private static final int BOT_ID = 2;
    private static final int HUMAN_ID = 1;
    private static final int UNIT_ID = 20;

    private final ForcedWithdrawalReports reports = new ForcedWithdrawalReports();

    private static Entity unit(int unitId, int ownerId) {
        Entity unit = mock(Entity.class);
        when(unit.getId()).thenReturn(unitId);
        when(unit.getOwnerId()).thenReturn(ownerId);
        return unit;
    }

    @Test
    void aUnitTheBotReportedIsWithdrawing() {
        reports.record(BOT_ID, new BotHonorReport(List.of(), true, Set.of(UNIT_ID)));

        assertTrue(reports.isWithdrawing(unit(UNIT_ID, BOT_ID)));
    }

    @Test
    void aUnitTheBotDidNotReportIsNotWithdrawing() {
        reports.record(BOT_ID, new BotHonorReport(List.of(), true, Set.of(UNIT_ID)));

        assertFalse(reports.isWithdrawing(unit(UNIT_ID + 1, BOT_ID)));
    }

    @Test
    void nothingIsWithdrawingBeforeTheBotReports() {
        assertFalse(reports.isWithdrawing(unit(UNIT_ID, BOT_ID)));
        assertFalse(reports.ignoresForcedWithdrawal(BOT_ID), "an unreported bot is assumed to follow the rules");
    }

    @Test
    void aBotIgnoringForcedWithdrawalHasNoWithdrawingUnits() {
        // Even if a stale list came along with it, a bot that ignores the rules has nobody withdrawing.
        reports.record(BOT_ID, new BotHonorReport(List.of(), false, Set.of(UNIT_ID)));

        assertFalse(reports.isWithdrawing(unit(UNIT_ID, BOT_ID)));
        assertTrue(reports.ignoresForcedWithdrawal(BOT_ID));
    }

    @Test
    void aReportOnlyCoversItsOwnBotsUnits() {
        // A human unit that happens to share an ID listed by some bot is not withdrawing.
        reports.record(BOT_ID, new BotHonorReport(List.of(), true, Set.of(UNIT_ID)));

        assertFalse(reports.isWithdrawing(unit(UNIT_ID, HUMAN_ID)));
    }

    @Test
    void aNewReportReplacesTheOldOne() {
        reports.record(BOT_ID, new BotHonorReport(List.of(), true, Set.of(UNIT_ID)));
        reports.record(BOT_ID, new BotHonorReport(List.of(), true, Set.of()));

        assertFalse(reports.isWithdrawing(unit(UNIT_ID, BOT_ID)));
    }

    @Test
    void theReportGetsPastTheServersClassFilter() throws Exception {
        // The server only accepts classes on its allow list and drops the connection of anyone sending anything else.
        // Reading the report back through that same filter is what proves a bot can send it without being kicked.
        BotHonorReport report = new BotHonorReport(List.of(HUMAN_ID), true, Set.of(UNIT_ID, UNIT_ID + 1));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            // The packet exactly as the server relays it to every client.
            output.writeObject(new Packet(PacketCommand.PRINCESS_DISHONORED, BOT_ID, report));
        }
        Object received;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            input.setObjectInputFilter(new SanityInputFilter());
            received = input.readObject();
        }

        Packet receivedPacket = (Packet) received;
        assertEquals(report, receivedPacket.getObject(1));
    }

    @Test
    void returningToTheLobbyForgetsEveryReport() {
        // Unit IDs restart in the next game; a leftover report would tag whichever new unit reuses the ID.
        reports.record(BOT_ID, new BotHonorReport(List.of(), true, Set.of(UNIT_ID)));

        reports.clear();

        assertFalse(reports.isWithdrawing(unit(UNIT_ID, BOT_ID)));
    }
}
