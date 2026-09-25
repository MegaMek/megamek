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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import megamek.common.enums.ForcedWithdrawalOrder;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ForcedWithdrawalTracker}: which of a bot's units withdraw, including the gamemaster's orders, and
 * the list the bot judges attacks against and reports to clients.
 */
class ForcedWithdrawalTrackerTest {

    private Princess princess;

    @BeforeEach
    void setUp() {
        princess = spy(new Princess("TestPrincess", UUID.randomUUID().toString(), 1));
    }

    private static Entity unit(int unitId, boolean isCrippled, ForcedWithdrawalOrder order) {
        Entity unit = mock(Entity.class);
        when(unit.getId()).thenReturn(unitId);
        when(unit.isCrippled(true)).thenReturn(isCrippled);
        when(unit.getForcedWithdrawalOrder()).thenReturn(order);
        return unit;
    }

    @Test
    void aCrippledUnitWithdrawsUnderTheBotsRules() {
        doReturn(true).when(princess).getForcedWithdrawal();

        assertTrue(princess.getForcedWithdrawalTracker()
              .isWithdrawing(unit(1, true, ForcedWithdrawalOrder.BOT_RULES)));
    }

    @Test
    void anUnhurtUnitOrderedToWithdrawDoes() {
        doReturn(true).when(princess).getForcedWithdrawal();

        assertTrue(princess.getForcedWithdrawalTracker()
              .isWithdrawing(unit(1, false, ForcedWithdrawalOrder.WITHDRAW)));
    }

    @Test
    void aCrippledUnitOrderedToFightToTheDeathDoesNot() {
        doReturn(true).when(princess).getForcedWithdrawal();

        assertFalse(princess.getForcedWithdrawalTracker()
              .isWithdrawing(unit(1, true, ForcedWithdrawalOrder.FIGHT_TO_THE_DEATH)));
    }

    @Test
    void theListHoldsOnlyWithdrawingUnits() {
        doReturn(true).when(princess).getForcedWithdrawal();
        List<Entity> ownedUnits = List.of(unit(1, true, ForcedWithdrawalOrder.BOT_RULES),
              unit(2, false, ForcedWithdrawalOrder.BOT_RULES),
              unit(3, false, ForcedWithdrawalOrder.WITHDRAW),
              unit(4, true, ForcedWithdrawalOrder.FIGHT_TO_THE_DEATH));
        doReturn(ownedUnits).when(princess).getEntitiesOwned();

        princess.getForcedWithdrawalTracker().refreshWithdrawingUnits();

        assertEquals(Set.of(1, 3), princess.getForcedWithdrawalTracker().withdrawingUnitIds());
    }

    @Test
    void aBotIgnoringForcedWithdrawalStillListsOrderedUnits() {
        // A Berserk bot's crippled unit fights on, but a gamemaster's order to withdraw overrides the setting.
        doReturn(false).when(princess).getForcedWithdrawal();
        List<Entity> ownedUnits = List.of(unit(1, true, ForcedWithdrawalOrder.BOT_RULES),
              unit(2, false, ForcedWithdrawalOrder.WITHDRAW));
        doReturn(ownedUnits).when(princess).getEntitiesOwned();

        princess.getForcedWithdrawalTracker().refreshWithdrawingUnits();

        assertEquals(Set.of(2), princess.getForcedWithdrawalTracker().withdrawingUnitIds());
    }

    @Test
    void returningToTheLobbyForgetsTheList() {
        princess.getMemory().setCrippledUnits(Set.of(3));

        princess.getForcedWithdrawalTracker().forgetWithdrawingUnits();

        assertTrue(princess.getForcedWithdrawalTracker().withdrawingUnitIds().isEmpty());
    }

    @Test
    void theBotFallsBackWithAUnitOrderedToWithdraw() {
        // The movement decisions ask the tracker, so an order moves the unit without it being crippled.
        doReturn(true).when(princess).getForcedWithdrawal();
        Entity orderedUnit = unit(1, false, ForcedWithdrawalOrder.WITHDRAW);

        assertTrue(princess.wantsToFallBack(orderedUnit));
    }

    @Test
    void theBotDoesNotFallBackWithAUnitOrderedToFightToTheDeath() {
        doReturn(true).when(princess).getForcedWithdrawal();
        Entity fightingUnit = unit(1, true, ForcedWithdrawalOrder.FIGHT_TO_THE_DEATH);

        assertFalse(princess.wantsToFallBack(fightingUnit));
    }
}
