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

import static megamek.common.options.OptionsConstants.ATOW_COMBAT_PARALYSIS;
import static megamek.common.options.OptionsConstants.ATOW_COMBAT_SENSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import megamek.common.Player;
import megamek.common.game.InitiativeRoll;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TacticalGeniusEvaluator}, the Tactical Genius initiative reroll decision (issue #9275).
 */
class TacticalGeniusEvaluatorTest {

    private static final double DELTA = 1e-9;

    @Test
    void doesNotRerollWinningInitiative() {
        // Issue #9275: Princess rerolled an initiative roll she had already won
        assertFalse(TacticalGeniusEvaluator.shouldReroll(new int[] { 9 }, 0, List.of(new int[] { 5 }), ""));
    }

    @Test
    void rerollsLosingInitiativeAgainstSingleEnemy() {
        // A reroll can only help when losing to the only enemy, even when it is unlikely to win
        assertTrue(TacticalGeniusEvaluator.shouldReroll(new int[] { 5 }, 0, List.of(new int[] { 9 }), ""));
        assertTrue(TacticalGeniusEvaluator.shouldReroll(new int[] { 3 }, 0, List.of(new int[] { 12 }), ""));
    }

    @Test
    void doesNotRerollWhenNoRerollCanWin() {
        // Enemy total of 14 cannot be reached with 2d6 and no bonus
        assertFalse(TacticalGeniusEvaluator.shouldReroll(new int[] { 4 }, 0, List.of(new int[] { 14 }), ""));
    }

    @Test
    void bonusIsKeptOnReroll() {
        // With a +4 bonus, a reroll can beat the enemy's 14
        assertTrue(TacticalGeniusEvaluator.shouldReroll(new int[] { 6 }, 4, List.of(new int[] { 14 }), ""));
    }

    @Test
    void comparesWholeRollSequenceNotJustTheLastRoll() {
        // Won the initial roll 10 vs 7; the later 3 is only a tie-break against some other team
        assertFalse(TacticalGeniusEvaluator.shouldReroll(new int[] { 10, 3 }, 0, List.of(new int[] { 7 }), ""));
        // Tied 8 vs 8 and lost the tie-break 4 vs 9; Tactical Genius replaces the tie-break roll
        assertTrue(TacticalGeniusEvaluator.shouldReroll(new int[] { 8, 4 }, 0, List.of(new int[] { 8, 9 }), ""));
    }

    @Test
    void doesNotRiskMiddlePlaceForLongShot() {
        // Beating one enemy and losing to another: rolling a 2 (tie, then lose to the 12) is as likely as rolling a
        // 12 (beat the 2, tie the 12), so a reroll gains nothing on average
        List<int[]> enemies = List.of(new int[] { 2 }, new int[] { 12 });
        assertEquals(0, TacticalGeniusEvaluator.score(new int[] { 7 }, enemies));
        assertEquals(0, TacticalGeniusEvaluator.expectedScoreAfterReroll(new int[] { 7 }, 0, enemies, ""), DELTA);
        assertFalse(TacticalGeniusEvaluator.shouldReroll(new int[] { 7 }, 0, enemies, ""));
    }

    @Test
    void rerollsWhenLosingToMostEnemies() {
        List<int[]> enemies = List.of(new int[] { 6 }, new int[] { 8 }, new int[] { 9 });
        assertTrue(TacticalGeniusEvaluator.shouldReroll(new int[] { 4 }, 0, enemies, ""));
    }

    @Test
    void noEnemiesOrNoRollMeansNoReroll() {
        assertFalse(TacticalGeniusEvaluator.shouldReroll(new int[] { 4 }, 0, List.of(), ""));
        assertFalse(TacticalGeniusEvaluator.shouldReroll(new InitiativeRoll(),
              List.of(new InitiativeRoll()), ""));
    }

    @Test
    void diceWaysCoverAllOutcomes() {
        assertEquals(36, Arrays.stream(TacticalGeniusEvaluator.diceWays("")).sum());
        assertEquals(216, Arrays.stream(TacticalGeniusEvaluator.diceWays(ATOW_COMBAT_SENSE)).sum());
        assertEquals(216, Arrays.stream(TacticalGeniusEvaluator.diceWays(ATOW_COMBAT_PARALYSIS)).sum());
        assertEquals(6, TacticalGeniusEvaluator.diceWays("")[7]);
        // Combat Sense keeps the best two of three dice: 12 needs at least two sixes
        assertEquals(16, TacticalGeniusEvaluator.diceWays(ATOW_COMBAT_SENSE)[12]);
        assertEquals(16, TacticalGeniusEvaluator.diceWays(ATOW_COMBAT_PARALYSIS)[2]);
    }

    @Test
    void sameOutcomeForEveryRerollIsNotAnImprovement() {
        // Already beating every enemy whatever the reroll; must not reroll due to rounding
        assertFalse(TacticalGeniusEvaluator.shouldReroll(new int[] { 10, 3 }, 0,
              List.of(new int[] { 7 }, new int[] { 5 }), ATOW_COMBAT_SENSE));
    }

    @Test
    void playerCopyKeepsTeamInitiative() {
        Player player = new Player(1, "Princess");
        InitiativeRoll teamRoll = new InitiativeRoll();
        teamRoll.addRoll(3, "");
        player.setTeamInitiative(teamRoll);

        Player copy = player.copy();
        assertEquals(1, copy.getTeamInitiative().size());
        assertEquals(teamRoll.getRoll(0), copy.getTeamInitiative().getRoll(0));
        assertEquals(3, copy.getTeamInitiative().getBonus(0));
    }
}
