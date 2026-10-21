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
import java.util.UUID;

import megamek.common.Player;
import megamek.common.Team;
import megamek.common.game.Game;
import megamek.common.game.InitiativeRoll;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Princess#decideToRerollInitiative()}, the Tactical Genius reroll decision. Initiative is won by team
 * rolls, so the decision must read the team rolls and ignore each player's own roll, which only orders the players
 * within a team (issue #9275).
 */
class PrincessTacticalGeniusTest {

    private Princess princess;
    private Game mockGame;
    private GameOptions mockOptions;
    private Player princessPlayer;
    private Player enemyPlayer;
    private Team princessTeam;
    private Team enemyTeam;

    @BeforeEach
    void setUp() {
        princess = spy(new Princess("TestPrincess", UUID.randomUUID().toString(), 1));
        mockGame = mock(Game.class);
        mockOptions = mock(GameOptions.class);
        princessPlayer = mock(Player.class);
        enemyPlayer = mock(Player.class);
        when(enemyPlayer.isEnemyOf(princessPlayer)).thenReturn(true);
        princessTeam = team(princessPlayer);
        enemyTeam = team(enemyPlayer);

        doReturn(mockGame).when(princess).getGame();
        doReturn(princessPlayer).when(princess).getLocalPlayer();
        when(mockGame.getOptions()).thenReturn(mockOptions);
        when(mockGame.hasTacticalGenius(princessPlayer)).thenReturn(true);
        when(mockGame.getTeamForPlayer(princessPlayer)).thenReturn(princessTeam);
        when(mockGame.getTeams()).thenReturn(List.of(princessTeam, enemyTeam));
    }

    private static Team team(Player player) {
        Team team = mock(Team.class);
        when(team.players()).thenReturn(List.of(player));
        return team;
    }

    /** Builds a single-roll initiative sequence with a fixed total and bonus. */
    private static InitiativeRoll roll(int total, int bonus) {
        InitiativeRoll roll = mock(InitiativeRoll.class);
        when(roll.size()).thenReturn(1);
        when(roll.getRoll(0)).thenReturn(total);
        when(roll.getBonus(0)).thenReturn(bonus);
        return roll;
    }

    private void setRolls(InitiativeRoll princessTeamRoll, InitiativeRoll enemyTeamRoll, InitiativeRoll princessOwnRoll,
          InitiativeRoll enemyOwnRoll) {
        when(princessTeam.getInitiative()).thenReturn(princessTeamRoll);
        when(enemyTeam.getInitiative()).thenReturn(enemyTeamRoll);
        when(princessPlayer.getInitiative()).thenReturn(princessOwnRoll);
        when(enemyPlayer.getInitiative()).thenReturn(enemyOwnRoll);
    }

    @Test
    void keepsAWinningTeamRollEvenWhenHerOwnPlayerRollIsLower() {
        // Issue #9275, round 8: the bot team totalled 10 against 8 and won, but OpFor's own player roll was 4
        // against the human's 5. She rerolled on that and threw the win away.
        setRolls(roll(10, 1), roll(8, 5), roll(4, 0), roll(5, 0));

        assertFalse(princess.decideToRerollInitiative());
    }

    @Test
    void rerollsALosingTeamRollEvenWhenHerOwnPlayerRollIsHigher() {
        // Team total 4 (bonus 5) against 8: a fresh 2d6 + 5 beats 8 on a 4 or more, 33 times in 36.
        setRolls(roll(4, 5), roll(8, 0), roll(11, 0), roll(2, 0));

        assertTrue(princess.decideToRerollInitiative());
    }

    @Test
    void doesNotRerollUnderIndividualInitiative() {
        when(mockOptions.booleanOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).thenReturn(true);
        setRolls(roll(4, 5), roll(8, 0), roll(11, 0), roll(2, 0));

        assertFalse(princess.decideToRerollInitiative());
    }

    @Test
    void doesNotRerollWithoutAKnownTeamRoll() {
        setRolls(new InitiativeRoll(), roll(8, 0), roll(2, 0), roll(11, 0));

        assertFalse(princess.decideToRerollInitiative());
    }

    @Test
    void ignoresTeamsWithNoEnemyPlayer() {
        Player allyPlayer = mock(Player.class);
        Team allyTeam = team(allyPlayer);
        InitiativeRoll allyTeamRoll = roll(12, 0);
        when(allyTeam.getInitiative()).thenReturn(allyTeamRoll);
        when(mockGame.getTeams()).thenReturn(List.of(princessTeam, enemyTeam, allyTeam));
        // Princess beats the only enemy; the allied team rolling higher must not make her reroll.
        setRolls(roll(9, 0), roll(6, 0), roll(2, 0), roll(2, 0));

        assertFalse(princess.decideToRerollInitiative());
    }

    @Test
    void theExpectedScoreIncludesTheTeamBonus() {
        // Against a 7, plain 2d6 wins 15 times in 36, ties 6 and loses 15: an even 0. With a +2 bonus it wins on 6+
        // (26 in 36), ties on a 5 (4) and loses on 4 or less (6): (26 - 6) / 36.
        assertEquals(0.0, Princess.expectedScoreOnReroll(0, List.of(7)), 1e-9);
        assertEquals(20.0 / 36.0, Princess.expectedScoreOnReroll(2, List.of(7)), 1e-9);
    }

    @Test
    void alwaysRerollsALossAgainstOneEnemyEvenWithPoorOdds() {
        // Losing 8 to 11, a fresh 2d6 only wins on a 12. That is still better than a certain loss, and the reroll
        // costs nothing, so she takes it. The old rule kept this loss because the odds were under one half.
        assertTrue(Princess.shouldRerollInitiative(8, 0, List.of(11)));
    }

    @Test
    void doesNotRerollWhenNoFreshRollCouldEvenTie() {
        // Against a 13, no 2d6 result without a bonus can win or tie, so a reroll cannot do better than now.
        assertFalse(Princess.shouldRerollInitiative(2, 0, List.of(13)));
    }

    @Test
    void doesNotRerollATie() {
        // Tied 7 against 7: a reroll is as likely to lose as to win, which is no better than the tie.
        assertFalse(Princess.shouldRerollInitiative(7, 0, List.of(7)));
    }

    @Test
    void doesNotRerollAMajorityWinThatARerollWouldRisk() {
        // A 9 beats a 4 and a 5 and loses to a 10: a score of 1. A fresh roll averages 20 / 36, so she keeps it.
        assertFalse(Princess.shouldRerollInitiative(9, 0, List.of(4, 5, 10)));
    }

    @Test
    void rerollsASplitThatARerollIsLikelyToImprove() {
        // An 8 beats a 3 and loses to a 9: a score of 0. A fresh roll averages 12 / 36, so she rerolls.
        assertTrue(Princess.shouldRerollInitiative(8, 0, List.of(3, 9)));
    }

    @Test
    void doesNotRerollWhenTheExpectedScoreOnlyMatchesTheCurrentOne() {
        // An 11 beats a 2 and loses to a 12: a score of 0. A fresh roll also averages exactly 0.
        assertEquals(0.0, Princess.expectedScoreOnReroll(0, List.of(2, 12)), 1e-9);
        assertFalse(Princess.shouldRerollInitiative(11, 0, List.of(2, 12)));
    }
}
