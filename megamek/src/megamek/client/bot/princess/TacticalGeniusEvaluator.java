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

import java.util.Arrays;
import java.util.List;

import megamek.common.game.InitiativeRoll;

/**
 * Decides whether a Tactical Genius initiative reroll is worth taking.
 *
 * <p>An initiative result is scored against each enemy team: +1 for beating it, -1 for losing to it and 0 for a tie
 * (ties are rerolled, so they are even odds). A reroll is worth taking when the expected score after the reroll is
 * higher than the current score. Against a single enemy this means: reroll whenever losing, keep the roll whenever
 * winning. Against several enemies it also weighs the risk of dropping below teams that are currently beaten.</p>
 *
 * <p>Rolls are given as their per-roll totals (dice plus bonus), in order, the same way {@link InitiativeRoll}
 * compares them. Tactical Genius replaces only the last roll, so only that entry changes in the evaluation.</p>
 */
final class TacticalGeniusEvaluator {

    private TacticalGeniusEvaluator() {
    }

    /**
     * @param myRoll     my team's initiative roll
     * @param enemyRolls the initiative rolls of each enemy team, one entry per team
     * @param aptitude   the initiative aptitude SPA of my team's commander (Combat Sense or Combat Paralysis), or an
     *                   empty string
     *
     * @return true if rerolling is expected to improve my initiative standing
     */
    static boolean shouldReroll(InitiativeRoll myRoll, List<InitiativeRoll> enemyRolls, String aptitude) {
        if (myRoll.size() == 0) {
            return false;
        }
        List<int[]> enemies = enemyRolls.stream()
              .filter(roll -> roll.size() > 0)
              .map(TacticalGeniusEvaluator::totals)
              .toList();
        int lastBonus = myRoll.getBonus(myRoll.size() - 1);
        return shouldReroll(totals(myRoll), lastBonus, enemies, aptitude);
    }

    /**
     * @param myRolls    my team's per-roll initiative totals
     * @param lastBonus  the bonus included in my last roll, which a reroll keeps
     * @param enemyRolls each enemy team's per-roll initiative totals
     * @param aptitude   Combat Sense, Combat Paralysis or an empty string
     *
     * @return true if rerolling is expected to improve my initiative standing
     */
    static boolean shouldReroll(int[] myRolls, int lastBonus, List<int[]> enemyRolls, String aptitude) {
        if ((myRolls.length == 0) || enemyRolls.isEmpty()) {
            return false;
        }
        // Compare in whole dice outcomes rather than probabilities to avoid rounding errors
        int[] diceWays = diceWays(aptitude);
        int outcomes = Arrays.stream(diceWays).sum();
        return weightedScoreAfterReroll(myRolls, lastBonus, enemyRolls, diceWays)
              > score(myRolls, enemyRolls) * outcomes;
    }

    /**
     * @return my score against the enemy teams: +1 per team beaten, -1 per team lost to, 0 per tie
     */
    static int score(int[] myRolls, List<int[]> enemyRolls) {
        int score = 0;
        for (int[] enemy : enemyRolls) {
            score += Integer.signum(compare(myRolls, enemy));
        }
        return score;
    }

    /**
     * @return the expected {@link #score(int[], List)} after replacing my last roll with a fresh one
     */
    static double expectedScoreAfterReroll(int[] myRolls, int lastBonus, List<int[]> enemyRolls, String aptitude) {
        int[] diceWays = diceWays(aptitude);
        return (double) weightedScoreAfterReroll(myRolls, lastBonus, enemyRolls, diceWays)
              / Arrays.stream(diceWays).sum();
    }

    /**
     * @return the score after replacing my last roll, summed over every way the dice can fall
     */
    private static int weightedScoreAfterReroll(int[] myRolls, int lastBonus, List<int[]> enemyRolls,
          int[] diceWays) {
        int[] rerolled = myRolls.clone();
        int weightedScore = 0;
        for (int dice = 2; dice <= 12; dice++) {
            rerolled[rerolled.length - 1] = dice + lastBonus;
            weightedScore += diceWays[dice] * score(rerolled, enemyRolls);
        }
        return weightedScore;
    }

    /**
     * @return the number of ways to roll each 2d6 initiative dice total (index 2 to 12), out of 36; with Combat Sense
     *       or Combat Paralysis this is the best or worst two of three dice, out of 216
     */
    static int[] diceWays(String aptitude) {
        int[] ways = new int[13];
        boolean sense = ATOW_COMBAT_SENSE.equals(aptitude);
        boolean paralysis = ATOW_COMBAT_PARALYSIS.equals(aptitude);
        if (sense || paralysis) {
            for (int a = 1; a <= 6; a++) {
                for (int b = 1; b <= 6; b++) {
                    for (int c = 1; c <= 6; c++) {
                        int dropped = sense ? Math.min(a, Math.min(b, c)) : Math.max(a, Math.max(b, c));
                        ways[a + b + c - dropped]++;
                    }
                }
            }
        } else {
            for (int a = 1; a <= 6; a++) {
                for (int b = 1; b <= 6; b++) {
                    ways[a + b]++;
                }
            }
        }
        return ways;
    }

    /** Compares two roll sequences the same way {@link InitiativeRoll#compareTo(InitiativeRoll)} does. */
    private static int compare(int[] mine, int[] theirs) {
        int minSize = Math.min(mine.length, theirs.length);
        for (int i = 0; i < minSize; i++) {
            if (mine[i] != theirs[i]) {
                return mine[i] - theirs[i];
            }
        }
        return 0;
    }

    private static int[] totals(InitiativeRoll roll) {
        int[] totals = new int[roll.size()];
        for (int i = 0; i < totals.length; i++) {
            totals[i] = roll.getRoll(i);
        }
        return totals;
    }
}
