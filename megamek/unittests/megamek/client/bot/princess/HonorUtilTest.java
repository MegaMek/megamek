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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HonorUtil}: under Forced Withdrawal a crippled enemy is broken for good, and a manually shut down
 * enemy counts as surrendered (and so broken) only while it stays shut down.
 */
class HonorUtilTest {

    private static final int ENTITY_ID = 7;
    private static final int OWNER_ID = 3;

    private HonorUtil honorUtil;
    private Entity enemy;

    @BeforeEach
    void setUp() {
        honorUtil = new HonorUtil();
        enemy = mock(Entity.class);
        when(enemy.getId()).thenReturn(ENTITY_ID);
        when(enemy.getOwnerId()).thenReturn(OWNER_ID);
        when(enemy.getDisplayName()).thenReturn("Marauder MAD-3R");
    }

    @Test
    void manuallyShutDownEnemyIsBrokenUnderForcedWithdrawal() {
        when(enemy.isManualShutdown()).thenReturn(true);

        honorUtil.checkEnemyBroken(enemy, true);

        assertTrue(honorUtil.isEnemyBroken(ENTITY_ID, OWNER_ID, true));
    }

    @Test
    void manuallyShutDownEnemyIsNotBrokenWithoutForcedWithdrawal() {
        when(enemy.isManualShutdown()).thenReturn(true);

        honorUtil.checkEnemyBroken(enemy, false);

        assertFalse(honorUtil.isEnemyBroken(ENTITY_ID, OWNER_ID, false));
    }

    @Test
    void surrenderEndsWhenEnemyStartsUpAgain() {
        when(enemy.isManualShutdown()).thenReturn(true);
        honorUtil.checkEnemyBroken(enemy, true);

        when(enemy.isManualShutdown()).thenReturn(false);
        honorUtil.checkEnemyBroken(enemy, true);

        assertFalse(honorUtil.isEnemyBroken(ENTITY_ID, OWNER_ID, true));
    }

    @Test
    void heatShutdownIsNotSurrender() {
        when(enemy.isShutDown()).thenReturn(true);
        when(enemy.isManualShutdown()).thenReturn(false);

        honorUtil.checkEnemyBroken(enemy, true);

        assertFalse(honorUtil.isEnemyBroken(ENTITY_ID, OWNER_ID, true));
    }

    @Test
    void dishonoredOwnerGetsNoQuarterForSurrender() {
        when(enemy.isManualShutdown()).thenReturn(true);
        honorUtil.checkEnemyBroken(enemy, true);

        honorUtil.setEnemyDishonored(OWNER_ID);

        assertFalse(honorUtil.isEnemyBroken(ENTITY_ID, OWNER_ID, true));
    }

    @Test
    void pirateGivesNoQuarterForSurrender() {
        honorUtil.setIAmAPirate(true);
        when(enemy.isManualShutdown()).thenReturn(true);

        honorUtil.checkEnemyBroken(enemy, true);

        assertFalse(honorUtil.isEnemyBroken(ENTITY_ID, OWNER_ID, true));
    }

    @Test
    void surrenderEndsWhenForcedWithdrawalIsTurnedOff() {
        when(enemy.isManualShutdown()).thenReturn(true);
        honorUtil.checkEnemyBroken(enemy, true);

        honorUtil.checkEnemyBroken(enemy, false);

        assertFalse(honorUtil.isEnemyBroken(ENTITY_ID, OWNER_ID, false));
    }

    @Test
    void crippledEnemyStaysBrokenAfterStartingUp() {
        when(enemy.isCrippled()).thenReturn(true);
        when(enemy.isManualShutdown()).thenReturn(true);
        honorUtil.checkEnemyBroken(enemy, true);

        when(enemy.isManualShutdown()).thenReturn(false);
        honorUtil.checkEnemyBroken(enemy, true);

        assertTrue(honorUtil.isEnemyBroken(ENTITY_ID, OWNER_ID, true));
    }
}
