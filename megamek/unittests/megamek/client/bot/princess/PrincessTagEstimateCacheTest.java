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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.UUID;

import megamek.common.enums.GamePhase;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Issue #9232: the TAG estimates were only cleared when the Targeting phase started, and the server skips that phase
 * in any round where no unit has artillery. The phases that run every round now clear them as well.
 */
class PrincessTagEstimateCacheTest {

    private static final int FIRST_TAG_DAMAGE = 40;
    private static final int LATER_TAG_DAMAGE = 20;

    private Princess princess;
    private Targetable enemyTarget;

    @BeforeEach
    void setUp() {
        princess = spy(new Princess("TestPrincess", UUID.randomUUID().toString(), 1));
        // The phase setup itself is not under test; keep it from planning against an empty game.
        doNothing().when(princess).initMovement();
        doNothing().when(princess).initFiring();
        doNothing().when(princess).postMovementProcessing();
        doNothing().when(princess).sendChat(anyString());

        enemyTarget = mock(Targetable.class);
        when(enemyTarget.getId()).thenReturn(7);
    }

    @AfterEach
    void tearDown() {
        princess.die();
    }

    /**
     * Without a reset, the first TAG value stored for a target makes every lower later value a quarter of itself.
     */
    @Test
    void laterLowerTagValueIsCutToAQuarterWithinOnePhase() {
        assertEquals(FIRST_TAG_DAMAGE, princess.computeTeamTagUtility(enemyTarget, FIRST_TAG_DAMAGE));
        assertEquals(LATER_TAG_DAMAGE / 4, princess.computeTeamTagUtility(enemyTarget, LATER_TAG_DAMAGE));
    }

    @ParameterizedTest
    @EnumSource(value = GamePhase.class, names = { "MOVEMENT", "OFFBOARD", "FIRING" })
    void phasesThatRunEveryRoundClearTheTagValues(GamePhase phase) {
        princess.computeTeamTagUtility(enemyTarget, FIRST_TAG_DAMAGE);

        princess.changePhase(phase);

        assertEquals(LATER_TAG_DAMAGE, princess.computeTeamTagUtility(enemyTarget, LATER_TAG_DAMAGE),
              "a TAG value from an earlier phase must not cut this phase's value");
    }

    @Test
    void physicalPhaseKeepsTheTagValues() {
        princess.computeTeamTagUtility(enemyTarget, FIRST_TAG_DAMAGE);

        princess.changePhase(GamePhase.PHYSICAL);

        assertEquals(LATER_TAG_DAMAGE / 4, princess.computeTeamTagUtility(enemyTarget, LATER_TAG_DAMAGE));
    }

    @Test
    void resetPhasesAreMovementOffboardAndFiring() {
        assertTrue(Princess.isTagEstimateResetPhase(GamePhase.MOVEMENT));
        assertTrue(Princess.isTagEstimateResetPhase(GamePhase.OFFBOARD));
        assertTrue(Princess.isTagEstimateResetPhase(GamePhase.FIRING));
        assertFalse(Princess.isTagEstimateResetPhase(GamePhase.TARGETING),
              "Targeting already clears them in initTargeting");
        assertFalse(Princess.isTagEstimateResetPhase(GamePhase.PHYSICAL));
        assertFalse(Princess.isTagEstimateResetPhase(GamePhase.END));
    }
}
