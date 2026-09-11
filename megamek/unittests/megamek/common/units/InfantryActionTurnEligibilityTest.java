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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Who gets a turn in the Infantry vs Infantry Combat phase: a unit that can join the action where it stands, or an
 * attacker already in one that may still withdraw (TO:AR p. 172). Before this, an engaged attacker had no turn, so
 * the Withdraw button could never be reached.
 */
@DisplayName("Infantry action: turns in the combat phase")
class InfantryActionTurnEligibilityTest {

    private static final int BUILDING_ID = 7;

    private ConvInfantry platoon;

    @BeforeEach
    void beforeEach() {
        // Off the board, so nothing can be reinforced and only the withdrawal path decides eligibility
        Game game = mock(Game.class);
        when(game.hasBoardLocationOf(any())).thenReturn(false);
        when(game.getOptions()).thenReturn(mock(GameOptions.class));
        platoon = new ConvInfantry();
        platoon.setGame(game);
    }

    @Test
    @DisplayName("An engaged attacker gets a turn, so it can withdraw")
    void engagedAttackerGetsATurn() {
        platoon.setInfantryCombatTargetId(BUILDING_ID);
        platoon.setInfantryCombatAttacker(true);

        assertTrue(platoon.canWithdrawFromInfantryAction());
        assertTrue(platoon.isEligibleForInfantryVsInfantry());
    }

    @Test
    @DisplayName("An engaged defender gets none: only attackers withdraw")
    void engagedDefenderGetsNoTurn() {
        platoon.setInfantryCombatTargetId(BUILDING_ID);
        platoon.setInfantryCombatAttacker(false);

        assertFalse(platoon.canWithdrawFromInfantryAction());
        assertFalse(platoon.isEligibleForInfantryVsInfantry());
    }

    @Test
    @DisplayName("An attacker that has announced its withdrawal needs no further turn")
    void withdrawingAttackerGetsNoFurtherTurn() {
        platoon.setInfantryCombatTargetId(BUILDING_ID);
        platoon.setInfantryCombatAttacker(true);
        platoon.setInfantryCombatWantsWithdrawal(true);

        assertFalse(platoon.canWithdrawFromInfantryAction());
        assertFalse(platoon.isEligibleForInfantryVsInfantry());
    }

    @Test
    @DisplayName("A unit in no action, with nothing to join, gets no turn")
    void unengagedUnitWithNothingToJoinGetsNoTurn() {
        assertFalse(platoon.canWithdrawFromInfantryAction());
        assertFalse(platoon.isEligibleForInfantryVsInfantry());
    }
}
